$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$environmentPath = Join-Path $repositoryRoot '.env'
$templatePath = Join-Path $repositoryRoot '.env.example'

# 기존 개인 설정은 덮어쓰지 않습니다.
if (Test-Path -LiteralPath $environmentPath) {
    Write-Output '기존 루트 .env를 유지합니다.'
    exit 0
}

$template = [IO.File]::ReadAllText($templatePath)
$generator = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    foreach ($key in @('DOCKER_MYSQL_PASSWORD', 'DOCKER_MYSQL_ROOT_PASSWORD', 'DOCKER_JWT_SECRET')) {
        $bytes = New-Object byte[] 32
        $generator.GetBytes($bytes)
        $value = [Convert]::ToBase64String($bytes)
        $pattern = '(?m)^' + [regex]::Escape($key) + '=.*$'
        if (-not [regex]::IsMatch($template, $pattern)) { throw "예제에 $key 항목이 없습니다." }
        $template = [regex]::Replace($template, $pattern, [Text.RegularExpressions.MatchEvaluator]{
            param($match)
            $key + '=' + $value
        })
    }
}
finally { $generator.Dispose() }

# 동시에 실행되더라도 기존 파일을 덮어쓰지 않도록 새 파일로만 생성합니다.
$stream = [IO.File]::Open($environmentPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
$writer = [IO.StreamWriter]::new($stream, [Text.UTF8Encoding]::new($false))
try { $writer.Write($template) }
finally { $writer.Dispose() }
Write-Output 'Docker용 루트 .env를 생성했습니다. 비밀값은 출력하지 않습니다.'
