param([switch]$Web)
& (Join-Path $PSScriptRoot 'start-development-sample.ps1') -Web:$Web
