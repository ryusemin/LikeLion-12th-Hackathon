param(
    [ValidateSet('All', 'Backend', 'Frontend', 'Browser', 'Runtime')]
    [string]$Target = 'All'
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $repositoryRoot '.local-tools'
$environmentNames = @('JAVA_HOME', 'PATH', 'GRADLE_USER_HOME', 'npm_config_cache', 'CI', 'DISABLE_ESLINT_PLUGIN')
$originalEnvironment = @{}
foreach ($environmentName in $environmentNames) {
    $originalEnvironment[$environmentName] = [Environment]::GetEnvironmentVariable($environmentName, 'Process')
}

try {
    if ($Target -eq 'All') {
        & (Join-Path $PSScriptRoot 'tests/verify-runtime.test.ps1')
    }
    if ($Target -eq 'Runtime') {
        & (Join-Path $PSScriptRoot 'verify-runtime.ps1')
    }
    if ($Target -in @('All', 'Backend')) {
        $localJdk = Get-ChildItem $toolsRoot -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
        $compiler = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
        $compilerVersion = (& $compiler -version | Out-String).Trim()
        if ($LASTEXITCODE -ne 0 -or $compilerVersion -notmatch '^javac 17\.') {
            throw 'JDK 17 is required. Set JAVA_HOME or prepare the project-local JDK described in docs/development.md.'
        }
        if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $toolsRoot 'gradle' }

        Push-Location (Join-Path $repositoryRoot 'Backend')
        try {
            & .\gradlew.bat test bootJar --no-daemon --console=plain
            if ($LASTEXITCODE -ne 0) { throw "Backend verification failed ($LASTEXITCODE)." }
        }
        finally { Pop-Location }
    }

    if ($Target -in @('All', 'Frontend', 'Browser')) {
        $localNode = Get-ChildItem $toolsRoot -Directory -Filter 'node-v24.*-win-x64' -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($localNode) { $env:PATH = $localNode.FullName + ';' + $env:PATH }
        $nodeVersion = (& node --version | Out-String).Trim()
        $expectedMajor = (Get-Content (Join-Path $repositoryRoot 'Frontend/.nvmrc') -Raw).Trim()
        if ($LASTEXITCODE -ne 0 -or $nodeVersion -notmatch "^v$([regex]::Escape($expectedMajor))\.") {
            throw "Node $expectedMajor is required. Select the runtime described in docs/development.md."
        }
        $npmVersion = (& npm.cmd --version | Out-String).Trim()
        if ($LASTEXITCODE -ne 0 -or $npmVersion -notmatch '^11\.') { throw 'npm 11 is required.' }
        if (-not (Test-Path (Join-Path $repositoryRoot 'Frontend/node_modules/.bin/react-scripts.cmd'))) {
            throw 'Frontend dependencies are missing. Run npm ci in Frontend first.'
        }
        if (-not $env:npm_config_cache) { $env:npm_config_cache = Join-Path $toolsRoot 'npm-cache' }

        Push-Location (Join-Path $repositoryRoot 'Frontend')
        try {
            if ($Target -in @('All', 'Frontend')) {
                $env:CI = 'true'
                & npm.cmd run lint
                if ($LASTEXITCODE -ne 0) { throw "Frontend lint failed ($LASTEXITCODE)." }
                & npm.cmd run test:ci
                if ($LASTEXITCODE -ne 0) { throw "Frontend tests failed ($LASTEXITCODE)." }
                # 앞 단계에서 ESLint 오류를 검사하므로 빌드에서는 중복 검사를 생략한다.
                $env:DISABLE_ESLINT_PLUGIN = 'true'
                & npm.cmd run build
                if ($LASTEXITCODE -ne 0) { throw "Frontend build failed ($LASTEXITCODE)." }
            }
            if ($Target -in @('All', 'Browser')) {
                # 로컬·CI 모두 현재 소스로 전용 검증 서버를 새로 띄운다.
                [Environment]::SetEnvironmentVariable('CI', $originalEnvironment['CI'], 'Process')
                & npm.cmd run test:e2e
                if ($LASTEXITCODE -ne 0) { throw "Browser tests failed ($LASTEXITCODE)." }
            }
        }
        finally { Pop-Location }
    }
}
finally {
    foreach ($environmentName in $environmentNames) {
        [Environment]::SetEnvironmentVariable($environmentName, $originalEnvironment[$environmentName], 'Process')
    }
}
