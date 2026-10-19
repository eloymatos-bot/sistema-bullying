param([switch]$CrearAdministrador)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'sistema-bullying-backend/Iniciar-local.ps1') -CrearAdministrador:$CrearAdministrador
