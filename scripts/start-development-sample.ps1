param([switch]$ResetData, [switch]$Web, [switch]$PreparedDocuments)
Write-Warning 'start-development-sample.ps1 está deprecado; use ./scripts/start-development.ps1.'
& (Join-Path $PSScriptRoot 'start-development.ps1') -ResetData:$ResetData -PreparedDocuments:$PreparedDocuments -Vite:(-not $Web)
