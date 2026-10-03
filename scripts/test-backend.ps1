$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    docker compose -f compose.test.yml up --abort-on-container-exit --exit-code-from tests --attach tests
    $result = $LASTEXITCODE
    docker compose -f compose.test.yml down
    exit $result
} finally { Pop-Location }
