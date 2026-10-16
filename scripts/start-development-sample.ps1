param([switch]$ResetData, [switch]$Web)
$ErrorActionPreference = 'Stop'
$previousPublicUrl = $env:APP_PUBLIC_URL
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $sampleEnvironment = Join-Path $projectRoot '.env.sample'
    if (-not (Test-Path -LiteralPath $sampleEnvironment)) {
        $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        $databaseBytes = New-Object byte[] 32
        $accountBytes = New-Object byte[] 24
        $random.GetBytes($databaseBytes)
        $random.GetBytes($accountBytes)
        $databaseSecret = [Convert]::ToBase64String($databaseBytes)
        $accountSecret = [Convert]::ToBase64String($accountBytes)
        $random.Dispose()
        @"
DB_PASSWORD=$databaseSecret
UTAPED_SEED_PASSWORD=$accountSecret
WEB_PORT=64431
BACKEND_PORT=18082
DB_PORT=15435
MAIL_UI_PORT=18027
MAIL_SMTP_PORT=11027
APP_PUBLIC_URL=http://127.0.0.1:64431
COOKIE_SECURE=false
SIGNING_ALLOW_HTTP=true
"@ | Set-Content -LiteralPath $sampleEnvironment -Encoding utf8
    }
    if ($ResetData) {
        docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml stop backend frontend
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo detener la aplicación de ejemplo antes de reiniciar los datos.' }
        # Only this explicitly named disposable project and database can be reset.
        @'
BEGIN;
DO $$
DECLARE row record;
BEGIN
    IF current_database() <> 'utaped_dev' THEN RAISE EXCEPTION 'Reset requires utaped_dev'; END IF;
    FOR row IN SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename<>'flyway_schema_history'
    LOOP EXECUTE format('TRUNCATE TABLE public.%I CASCADE',row.tablename); END LOOP;
END;
$$;
COMMIT;
'@ | docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml exec -T db psql -v ON_ERROR_STOP=1 -U utaped -d utaped_dev
        if ($LASTEXITCODE -ne 0) { throw 'No se reiniciaron los datos de ejemplo.' }
    }
    $webPortLine = Get-Content -LiteralPath $sampleEnvironment | Where-Object { $_ -match '^WEB_PORT=' }
    $webPort = if ($webPortLine) { $webPortLine -replace '^WEB_PORT=', '' } else { '64431' }
    $env:APP_PUBLIC_URL = if ($Web) { "http://127.0.0.1:$webPort" } else { 'http://127.0.0.1:5173' }
    if (-not $Web) {
        docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml --profile web stop frontend
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo detener el frontend Docker.' }
    }
    $services = if ($Web) { @('db', 'mail', 'backend', 'frontend') } else { @('db', 'mail', 'backend') }
    docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml --profile web up -d --build @services
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el entorno de ejemplo.' }
    if ($Web) {
        docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml restart frontend
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo actualizar la conexión del frontend al backend.' }
    }
    if ($ResetData) {
        docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml restart backend
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo reiniciar la aplicación para regenerar las semillas.' }
    }
    $backendPortLine = Get-Content -LiteralPath $sampleEnvironment | Where-Object { $_ -match '^BACKEND_PORT=' }
    $backendPort = if ($backendPortLine) { $backendPortLine -replace '^BACKEND_PORT=', '' } else { '18082' }
    $ready = $false
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$backendPort/api/auth/csrf" -TimeoutSec 3
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) { throw 'El backend no estuvo disponible a tiempo. Revisa sus logs antes de iniciar sesión.' }
    if ($Web) { Write-Output "Frontend Docker: http://127.0.0.1:$webPort" }
    else { Write-Output 'Backend listo. Ejecuta npm run dev desde frontend (http://127.0.0.1:5173).' }
    Write-Output 'Credenciales conservadas en .env.sample (archivo ignorado).'
    Write-Output 'Usuarios: dev.admin, dev.principal, dev.coordinator, dev.reviewer, dev.approver, dev.member, dev.outsider.'
} finally {
    $env:APP_PUBLIC_URL = $previousPublicUrl
    Pop-Location
}
