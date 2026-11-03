$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repositoryRoot
try {
    & docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose 설정을 확인해주세요.' }
    $output = & docker compose ps --all --format json
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose 상태를 조회하지 못했습니다.' }
    # Compose 버전에 따라 JSON 배열 또는 줄마다 JSON 객체를 반환한다.
    $json = ($output | Out-String).Trim()
    $services = if ($json.StartsWith('[')) { @($json | ConvertFrom-Json) }
                else { @($output | Where-Object { $_.Trim() } | ForEach-Object { $_ | ConvertFrom-Json }) }
    foreach ($name in @('frontend', 'backend', 'mysql', 'redis')) {
        $instances = @($services | Where-Object { $_.Service -eq $name })
        if ($instances.Count -ne 1 -or $instances[0].State -ne 'running' -or $instances[0].Health -ne 'healthy') {
            throw "서비스가 실행 중이거나 healthy 상태가 아닙니다: $name. docs/docker-development.md를 확인해주세요."
        }
        Write-Output "$name : running / healthy"
    }
    foreach ($url in @('http://127.0.0.1:3000/', 'http://127.0.0.1:8080/swagger-ui/index.html', 'http://127.0.0.1:8080/api-docs')) {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 15 -MaximumRedirection 0
        if ($response.StatusCode -ne 200) { throw "HTTP 검사 실패: $url" }
        if ($url.EndsWith('/api-docs')) {
            $schema = $response.Content | ConvertFrom-Json
            if ($schema.openapi -notlike '3.*' -or -not $schema.paths) { throw 'OpenAPI 응답 형식이 올바르지 않습니다.' }
        }
        Write-Output "HTTP 200 : $url"
    }
    $pong = & docker compose exec -T redis redis-cli ping
    if ($LASTEXITCODE -ne 0 -or ($pong | Out-String).Trim() -ne 'PONG') { throw 'Redis PING 검사 실패' }
    Write-Output 'Redis : PONG'
    Write-Output '실행 상태 검사 통과. 현재 소스와 이미지의 일치 및 로그인·업무 기능의 성공을 증명하지는 않습니다.'
}
finally { Pop-Location }
