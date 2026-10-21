param([switch]$KeepOnFailure)
$ErrorActionPreference = 'Stop'
$failed = $true
$result = 1
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    docker volume create utaped-maven-cache | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la caché Maven.' }
    docker compose -f compose.test.yml up --abort-on-container-exit --exit-code-from tests --attach tests
    $result = $LASTEXITCODE
    $failed = $result -ne 0
} finally {
    if ($failed -and $KeepOnFailure) { Write-Warning 'Se conserva utaped-tests para diagnóstico.' }
    else {
        docker compose -f compose.test.yml down --volumes
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo detener el entorno de pruebas backend.' }
    }
    Pop-Location
}
exit $result
