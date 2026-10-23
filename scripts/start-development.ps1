param([switch]$Web, [switch]$PreparedDocuments)
& (Join-Path $PSScriptRoot 'start-development-sample.ps1') -Web:$Web -PreparedDocuments:$PreparedDocuments
