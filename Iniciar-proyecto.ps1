. (Join-Path $PSScriptRoot 'scripts/Entorno.ps1')
$java = Get-Java
$npm = Find-Tool 'npm.cmd'
$jar = Join-Path $BackendRoot 'target/sistema-bullying-backend-0.0.1-SNAPSHOT.jar'
$localDir = Join-Path $ProjectRoot '.local'
if (!(Test-Path -LiteralPath (Join-Path $localDir 'application-local.properties')) -or !(Test-Path -LiteralPath $jar) -or !(Test-Path -LiteralPath (Join-Path $FrontendRoot 'node_modules'))) {
    throw 'Primero ejecuta .\Preparar-proyecto.ps1.'
}
foreach ($port in @(8080,4200)) { if (Test-LocalPort $port) { throw "El puerto $port ya esta ocupado. Deten la otra aplicacion antes de iniciar." } }
$configLocation = 'file:' + ($localDir.Replace('\','/').TrimEnd('/') + '/')
$backendProcess = $null
try {
    $backendProcess = Start-Process -FilePath $java -ArgumentList @('-jar', ('"' + $jar + '"'), '--spring.profiles.active=local', ('"--spring.config.additional-location=' + $configLocation + '"')) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $localDir 'backend.log') -RedirectStandardError (Join-Path $localDir 'backend-error.log')
    $ready = $false
    for ($attempt=0; $attempt -lt 60; $attempt++) {
        if ($backendProcess.HasExited) { throw 'El backend no pudo arrancar. Revisa .local/backend.log y .local/backend-error.log.' }
        try { $response = Invoke-WebRequest 'http://127.0.0.1:8080/api/auth/csrf' -UseBasicParsing -TimeoutSec 2; if ($response.StatusCode -eq 200) { $ready = $true; break } } catch { }
        Start-Sleep -Seconds 1
    }
    if (!$ready) { throw 'El backend no respondio a tiempo. Revisa SQL Server y los registros en .local/.' }
    Write-Host 'Backend listo. Angular se inicia ahora. Cuando termine, abre http://localhost:4200'
    Write-Host 'Usa la cuenta creada en esta computadora. Ctrl+C detiene la sesion de trabajo.'
    Push-Location $FrontendRoot
    try { & $npm start; if ($LASTEXITCODE -ne 0) { throw 'Angular termino con un error.' } } finally { Pop-Location }
} finally {
    if ($backendProcess -and !$backendProcess.HasExited) { Stop-Process -Id $backendProcess.Id -ErrorAction SilentlyContinue }
}
