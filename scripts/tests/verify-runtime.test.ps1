$ErrorActionPreference = 'Stop'
$runtimeScript = Join-Path (Split-Path -Parent $PSScriptRoot) 'verify-runtime.ps1'
$originalLocation = (Get-Location).Path

# 실제 Docker나 HTTP에 접근하지 않고 비정상 상태에서의 실패를 확인한다.
function docker {
    $global:LASTEXITCODE = 0
    if ($args[0] -eq 'compose' -and $args[1] -eq 'config') { return }
    if ($args[0] -ne 'compose' -or $args[1] -ne 'ps') { throw "상태 확인 이후 명령을 실행하면 안 됩니다: $($args -join ' ')" }
    $services = @('frontend', 'backend', 'mysql', 'redis') | ForEach-Object {
        if ($_ -ne $missingService) {
            @{ Service = $_; State = 'running'; Health = $(if ($_ -eq $unhealthyService) { 'unhealthy' } else { 'healthy' }) }
        }
    }
    if ($jsonLines) { $services | ForEach-Object { $_ | ConvertTo-Json -Compress } }
    else { ConvertTo-Json -InputObject @($services) -Compress }
}

foreach ($case in @('missing', 'unhealthy')) {
    $script:missingService = if ($case -eq 'missing') { 'backend' } else { '' }
    $script:unhealthyService = if ($case -eq 'unhealthy') { 'backend' } else { '' }
    $script:jsonLines = $case -eq 'unhealthy'
    $failure = $null
    try { & $runtimeScript | Out-Null } catch { $failure = $_.Exception.Message }
    if (-not $failure -or $failure -notlike '*healthy*backend*') { throw "예상한 실패가 아닙니다 ($case): $failure" }
    if ((Get-Location).Path -ne $originalLocation) { throw '실패 후 작업 경로가 복원되지 않았습니다.' }
    Write-Output "통과: $case 상태에서 HTTP 검사 전에 실패하고 작업 경로 복원"
}
