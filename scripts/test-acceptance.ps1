param([switch]$KeepOnFailure)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$failed = $true
$previous = @{}
foreach ($key in @('SAMPLE_BASE_URL', 'SAMPLE_COMPOSE_PROJECT', 'SAMPLE_ENV_FILE')) {
    $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
}
Push-Location $root
$composeArguments = @('compose', '--env-file', '.env.acceptance', '-p', 'utaped-acceptance', '-f', 'compose.yml', '-f', 'compose.sample.yml', '--profile', 'web')
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$databaseBytes = New-Object byte[] 32
$accountBytes = New-Object byte[] 24
$random.GetBytes($databaseBytes)
$random.GetBytes($accountBytes)
$databaseSecret = [Convert]::ToBase64String($databaseBytes)
$accountSecret = [Convert]::ToBase64String($accountBytes)
$random.Dispose()

try {
    if (Test-Path -LiteralPath '.env.acceptance') {
        & docker @composeArguments down --volumes --remove-orphans
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo limpiar el entorno de aceptación anterior.' }
    }
    @"
DB_PASSWORD=$databaseSecret
UTAPED_SEED_PASSWORD=$accountSecret
WEB_PORT=24431
BACKEND_PORT=28082
DB_PORT=25435
MAIL_UI_PORT=28027
MAIL_SMTP_PORT=21027
APP_PUBLIC_URL=http://127.0.0.1:24431
COOKIE_SECURE=false
SIGNING_ALLOW_HTTP=true
"@ | Set-Content -LiteralPath '.env.acceptance' -Encoding utf8
    & docker @composeArguments up -d --build
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el entorno de aceptación.' }
    $ready = $false
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:24431/api/auth/csrf' -TimeoutSec 3
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) { throw 'El backend de aceptación no estuvo disponible a tiempo.' }
    $env:SAMPLE_BASE_URL = 'http://127.0.0.1:24431'
    $env:SAMPLE_COMPOSE_PROJECT = 'utaped-acceptance'
    $env:SAMPLE_ENV_FILE = Join-Path $root '.env.acceptance'
    Push-Location frontend
    try {
        npx playwright test --config playwright.sample.config.ts
        if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de aceptación.' }
    } finally { Pop-Location }
    $failed = $false
} finally {
    if ($failed -and $KeepOnFailure) { Write-Warning 'Se conserva utaped-acceptance para diagnóstico. La siguiente ejecución lo recreará.' }
    elseif (Test-Path -LiteralPath '.env.acceptance') {
        & docker @composeArguments down --volumes --remove-orphans
        if ($LASTEXITCODE -ne 0) { Write-Error 'No se pudo limpiar utaped-acceptance.' }
    }
    foreach ($key in $previous.Keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
    Pop-Location
}
