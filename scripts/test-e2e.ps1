$ErrorActionPreference = 'Stop'
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
BACKEND_PORT=18080
DB_PORT=15433
MAIL_UI_PORT=18025
MAIL_SMTP_PORT=11025
"@ | Set-Content -LiteralPath '.env.e2e' -Encoding utf8
    }
    docker compose -p utaped-e2e --env-file .env.e2e up --build -d
    if ($LASTEXITCODE -ne 0) { throw 'E2E stack did not start' }
    # Only the dedicated test project's throttle rows are cleared for repeatable validation.
    docker compose -p utaped-e2e --env-file .env.e2e exec -T db psql -U utaped -d utaped -c 'DELETE FROM auth_throttle'
    Push-Location frontend
    try {
        npm ci
        if ($LASTEXITCODE -ne 0) { throw 'Dependencies did not install' }
        npx playwright install chromium
        if ($LASTEXITCODE -ne 0) { throw 'Chromium did not install' }
        npm run test:e2e
        $result = $LASTEXITCODE
    } finally { Pop-Location }
    exit $result
} finally {
    docker compose -p utaped-e2e --env-file .env.e2e --profile web down
    Pop-Location
}
