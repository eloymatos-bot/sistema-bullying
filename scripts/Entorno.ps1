$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$BackendRoot = Join-Path $ProjectRoot 'sistema-bullying-backend'
$FrontendRoot = Join-Path $ProjectRoot 'Sistema_Bullying_Figma_Sin_Errores'
function Find-Tool([string]$Name, [string[]]$Candidates = @()) {
    $found = Get-Command $Name -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    foreach ($candidate in $Candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) { return $candidate }
    }
    throw "Falta $Name. Consulta docs/INICIO_RAPIDO.md para instalarlo o agregarlo a PATH."
}
function Get-Java {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME 'bin/java.exe' }
    return Find-Tool 'java.exe' $candidates
}
function Test-LocalPort([int]$Port) {
    $client = New-Object Net.Sockets.TcpClient
    try { $task = $client.ConnectAsync('127.0.0.1', $Port); return ($task.Wait(800) -and $client.Connected) }
    catch { return $false } finally { $client.Dispose() }
}
