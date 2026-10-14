param([switch]$ResetData)
$ErrorActionPreference = 'Stop'
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
    docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml --profile web up -d --build
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el entorno de ejemplo.' }
    if ($ResetData) {
        docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml restart backend
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo reiniciar la aplicación para regenerar las semillas.' }
    }
    Write-Output 'Entorno de ejemplo iniciado. URL y contraseña local en .env.sample (archivo ignorado).'
    Write-Output 'Usuarios: dev.admin, dev.principal, dev.coordinator, dev.reviewer, dev.approver, dev.member, dev.outsider.'
} finally { Pop-Location }
