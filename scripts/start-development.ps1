param(
    [switch]$Vite,
    [switch]$PreparedDocuments,
    [switch]$ResetData,
    [switch]$Force,
    [switch]$Stop
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $envFile = Join-Path $projectRoot '.env'
    $envExample = Join-Path $projectRoot '.env.example'

    if ($Stop) {
        Write-Output 'Deteniendo el entorno UTAPED (conservando volúmenes y datos)...'
        docker compose -p utaped -f compose.yml stop
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo detener el proyecto utaped.' }
        Write-Output 'Contenedores detenidos correctamente.'
        return
    }

    if (-not (Test-Path -LiteralPath $envFile)) {
        if (-not (Test-Path -LiteralPath $envExample)) { throw 'No se encontró la plantilla .env.example.' }
        $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        $dbBytes = New-Object byte[] 32
        $seedBytes = New-Object byte[] 24
        $random.GetBytes($dbBytes)
        $random.GetBytes($seedBytes)
        $dbSecret = [Convert]::ToBase64String($dbBytes)
        $seedSecret = [Convert]::ToBase64String($seedBytes)
        $random.Dispose()

        $template = Get-Content -LiteralPath $envExample -Raw -Encoding utf8
        $template = $template -replace 'DB_PASSWORD=replace-with-a-random-local-password', "DB_PASSWORD=$dbSecret"
        $template = $template -replace 'UTAPED_SEED_PASSWORD=replace-with-a-random-account-seed-password', "UTAPED_SEED_PASSWORD=$seedSecret"
        Set-Content -LiteralPath $envFile -Value $template -Encoding utf8
        Write-Output 'Se ha generado un nuevo archivo .env con credenciales seguras únicas para este entorno.'
    }

    if ($ResetData) {
        $composeJson = docker compose -p utaped -f compose.yml config --format json | Out-String | ConvertFrom-Json
        $activeProfiles = $composeJson.services.backend.environment.SPRING_PROFILES_ACTIVE
        if ($activeProfiles -notlike '*development-seed*') {
            throw 'El reinicio de datos (-ResetData) solo está permitido en un entorno con SPRING_PROFILES_ACTIVE=development-seed en la configuración efectiva de Docker.'
        }
        $cookieSecure = $composeJson.services.backend.environment.COOKIE_SECURE
        if ($cookieSecure -eq 'true' -or $cookieSecure -eq $true) {
            throw 'El reinicio de datos (-ResetData) está bloqueado porque COOKIE_SECURE=true en la configuración efectiva de Docker (entorno productivo detectado).'
        }
        if (-not $Force) {
            Write-Warning "ADVERTENCIA: -ResetData truncará todas las tablas operativas de la base de datos de desarrollo (conservando las 7 cuentas de desarrollo y sus credenciales)."
            Write-Warning "Esta acción es irreversible y eliminará todos los documentos, revisiones y datos existentes."
            $confirm = Read-Host "Escriba 'CONFIRMAR' para proceder con el reinicio de datos"
            if ($confirm -ne 'CONFIRMAR') {
                Write-Output "Operación de reinicio cancelada por el usuario."
                return
            }
        }
        Write-Output 'Deteniendo backend y frontend antes de reiniciar datos...'
        docker compose -p utaped -f compose.yml stop backend frontend
        docker compose -p utaped -f compose.yml up -d db
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo asegurar el contenedor de base de datos antes del reinicio.' }
        @'
BEGIN;
DO $$
DECLARE
    targets text;
BEGIN
    IF current_database() NOT IN ('utaped', 'utaped_dev') THEN
        RAISE EXCEPTION 'Reset requires utaped or utaped_dev database';
    END IF;
    IF EXISTS (SELECT 1 FROM app_user WHERE system_role='ADMIN' AND email NOT LIKE '%@utaped.example.invalid') THEN
        RAISE EXCEPTION 'Reinicio abortado: la base contiene cuentas administrativas no pertenecientes a las semillas de desarrollo.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email='admin@utaped.example.invalid') THEN
        RAISE EXCEPTION 'Reinicio abortado: no se encontró la cuenta administrativa semilla dev.admin; la base no parece ser un entorno de desarrollo.';
    END IF;
    SELECT string_agg(format('public.%I', tablename), ',') INTO targets
    FROM pg_tables
    WHERE schemaname = 'public' AND tablename NOT IN ('app_user', 'flyway_schema_history');
    IF targets IS NOT NULL THEN
        EXECUTE 'TRUNCATE TABLE ' || targets || ' CASCADE';
    END IF;
END;
$$;
DELETE FROM app_user WHERE id NOT IN (
    '6544faba-e4ec-3b8d-a800-38fad0a5647a',
    'e99ce527-0aa3-38be-b511-93e5bf7421bb',
    '08580022-d519-306e-89e3-26608b289c46',
    '1f3429a2-6531-3fe8-9bf1-2974f431646f',
    '42b355a8-42b2-3193-b2ba-2dcf1666ed5f',
    'e6c0b3cf-ab90-3d3f-a30f-4c24ef1db381',
    '286a182e-c0b8-32f0-b6c3-4dd8c37d5751'
);
COMMIT;
'@ | docker compose -p utaped -f compose.yml exec -T db psql -v ON_ERROR_STOP=1 -U utaped -d utaped
        if ($LASTEXITCODE -ne 0) { throw 'No se pudieron reiniciar los datos.' }
        Write-Output 'Datos reiniciados a valores semilla limpios (credenciales de desarrollo conservadas).'
    }

    $webPort = '64430'
    $backendPort = '18080'
    $mailUiPort = '18025'
    if (Test-Path -LiteralPath $envFile) {
        foreach ($line in (Get-Content -LiteralPath $envFile)) {
            $trimmed = $line.Trim()
            if ($trimmed -match '^WEB_PORT=(.+)$') { $webPort = $Matches[1].Trim() }
            if ($trimmed -match '^BACKEND_PORT=(.+)$') { $backendPort = $Matches[1].Trim() }
            if ($trimmed -match '^MAIL_UI_PORT=(.+)$') { $mailUiPort = $Matches[1].Trim() }
        }
    }

    $previousPrepared = $env:UTAPED_SEED_DOCUMENTS
    $previousPublicUrl = $env:APP_PUBLIC_URL
    $env:UTAPED_SEED_DOCUMENTS = if ($PreparedDocuments) { 'true' } else { 'false' }
    $env:APP_PUBLIC_URL = if ($Vite) { 'http://127.0.0.1:5173' } else { "http://127.0.0.1:$webPort" }

    if ($Vite) {
        docker compose -p utaped -f compose.yml stop frontend 2>$null
        docker compose -p utaped -f compose.yml up -d --build db mail backend
    } else {
        docker compose -p utaped -f compose.yml up -d --build
    }
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el proyecto utaped.' }

    if ($ResetData) {
        docker compose -p utaped -f compose.yml restart backend
    }

    Write-Output 'Esperando a que el backend esté disponible...'
    $ready = $false
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$backendPort/api/auth/csrf" -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                if (-not $PreparedDocuments) { $ready = $true; break }
                $seedCount = docker compose -p utaped -f compose.yml exec -T db psql -U utaped -d utaped -At -c "SELECT count(*) FROM audit_event WHERE action='DEVELOPMENT_DOCUMENTS_CREATED'" 2>$null
                if ($LASTEXITCODE -eq 0 -and "$seedCount".Trim() -eq '1') { $ready = $true; break }
                Start-Sleep -Seconds 2
            }
        } catch { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) { throw 'El backend no estuvo disponible a tiempo. Revisa sus logs: docker compose logs backend' }

    Write-Output '=================================================='
    Write-Output ' UTAPED está listo para su uso local'
    Write-Output '=================================================='
    if ($Vite) {
        Write-Output "Backend listo (puerto $backendPort)."
        Write-Output 'Para el frontend interactivo: cd frontend; npm run dev'
        Write-Output 'URL de desarrollo Vite: http://127.0.0.1:5173'
    } else {
        Write-Output "Aplicación web: http://127.0.0.1:$webPort"
    }
    Write-Output "Bandeja de correo local (Mailpit): http://127.0.0.1:$mailUiPort"
    Write-Output 'Usuarios de desarrollo:'
    Write-Output '  dev.admin (Administrador), dev.principal (Elaborador),'
    Write-Output '  dev.coordinator (Coordinador), dev.reviewer (Revisor),'
    Write-Output '  dev.approver (Aprobador), dev.member (Integrante), dev.outsider'
    Write-Output 'Contraseña de desarrollo: consultar UTAPED_SEED_PASSWORD en el archivo local .env'
    Write-Output 'Para detener el entorno conservando datos: ./scripts/start-development.ps1 -Stop'
    Write-Output '=================================================='
} finally {
    $env:UTAPED_SEED_DOCUMENTS = $previousPrepared
    $env:APP_PUBLIC_URL = $previousPublicUrl
    Pop-Location
}
