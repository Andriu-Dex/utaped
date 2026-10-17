param([switch]$KeepOnFailure)
$ErrorActionPreference = 'Stop'
$failed = $true
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    if (-not (Test-Path -LiteralPath '.env.e2e')) {
        $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        $dbBytes = New-Object byte[] 32
        $adminBytes = New-Object byte[] 24
        $random.GetBytes($dbBytes)
        $random.GetBytes($adminBytes)
        $dbPassword = [Convert]::ToBase64String($dbBytes)
        $adminPassword = [Convert]::ToBase64String($adminBytes)
        $random.Dispose()
        @"
DB_PASSWORD=$dbPassword
BOOTSTRAP_ADMIN_EMAIL=admin-e2e@example.invalid
BOOTSTRAP_ADMIN_PASSWORD=$adminPassword
APP_PUBLIC_URL=http://127.0.0.1:15173
COOKIE_SECURE=false
WEB_PORT=24430
BACKEND_PORT=28081
DB_PORT=25433
MAIL_UI_PORT=28025
MAIL_SMTP_PORT=21025
"@ | Set-Content -LiteralPath '.env.e2e' -Encoding utf8
    }
    $entries = Get-Content -LiteralPath '.env.e2e'
    $apiPort = ($entries | Where-Object { $_ -match '^BACKEND_PORT=' }) -replace '^BACKEND_PORT=', ''
    $mailPort = ($entries | Where-Object { $_ -match '^MAIL_UI_PORT=' }) -replace '^MAIL_UI_PORT=', ''
    $env:E2E_API_URL = "http://127.0.0.1:$apiPort"
    $env:E2E_MAIL_URL = "http://127.0.0.1:$mailPort"
    docker compose -p utaped-e2e --env-file .env.e2e --profile web down --volumes
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo limpiar el entorno E2E anterior.' }
    docker compose -p utaped-e2e --env-file .env.e2e up --build -d
    if ($LASTEXITCODE -ne 0) { throw 'E2E stack did not start' }
    $ready = $false
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing "$env:E2E_API_URL/api/auth/csrf" -TimeoutSec 3
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) { throw 'El backend E2E no estuvo disponible a tiempo.' }
    Push-Location frontend
    try {
        npm ci
        if ($LASTEXITCODE -ne 0) { throw 'Dependencies did not install' }
        npx playwright install chromium
        if ($LASTEXITCODE -ne 0) { throw 'Chromium did not install' }
        npm run test:e2e
        $result = $LASTEXITCODE
    } finally { Pop-Location }
    $failed = $result -ne 0
} finally {
    if ($failed -and $KeepOnFailure) { Write-Warning 'Se conserva utaped-e2e para diagnóstico.' }
    else {
        docker compose -p utaped-e2e --env-file .env.e2e --profile web down --volumes
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo limpiar utaped-e2e.' }
    }
    Pop-Location
}

exit $result
