param([switch]$DatosDemo)
. (Join-Path $PSScriptRoot 'scripts/Entorno.ps1')
$java = Get-Java
$npm = Find-Tool 'npm.cmd'
$maven = Find-Tool 'mvn.cmd'
$sqlcmd = Find-Tool 'sqlcmd.exe' @("$env:ProgramFiles/Microsoft SQL Server/Client SDK/ODBC/180/Tools/Binn/SQLCMD.EXE", "$env:ProgramFiles/Microsoft SQL Server/Client SDK/ODBC/170/Tools/Binn/SQLCMD.EXE")
if (Test-LocalPort 8080) { throw 'Deten primero el backend del puerto 8080 antes de preparar el proyecto.' }
Write-Host 'Requisitos encontrados. SQL Server debe estar instalado y tu cuenta Windows debe poder administrar la instancia local.'
$instance = Read-Host 'Instancia local como en SSMS (ejemplo: .\SQLEXPRESS; Enter para localhost)'
if ([string]::IsNullOrWhiteSpace($instance)) { $instance = 'localhost' }
$hostPart = ($instance -split '[\\,]')[0]
if ($hostPart -notin @('.', 'localhost', '127.0.0.1', $env:COMPUTERNAME)) { throw 'Este asistente prepara solo SQL Server en tu computadora.' }
$portText = Read-Host 'Puerto TCP local configurado en SQL Server (Enter: 1433)'
if (!$portText) { $portText = '1433' }
$sqlPort = 0
if (![int]::TryParse($portText, [ref]$sqlPort) -or $sqlPort -lt 1 -or $sqlPort -gt 65535) { throw 'Puerto no valido.' }
function Invoke-SetupSql([string]$Query) {
    $result = $Query | & $sqlcmd -S $instance -E -C -b -l 10 -h -1 -W
    if ($LASTEXITCODE -ne 0) { throw 'SQL Server rechazo la preparacion. Revisa la instancia, permisos y mensaje anterior.' }
    return (($result | Out-String).Trim())
}
function Invoke-SqlFile([string]$Name) {
    & $sqlcmd -S $instance -E -C -b -l 10 -i (Join-Path $BackendRoot "database/$Name")
    if ($LASTEXITCODE -ne 0) { throw "No se pudo aplicar $Name. No se borraron datos existentes." }
}
$localDir = Join-Path $ProjectRoot '.local'
$config = Join-Path $localDir 'application-local.properties'
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
# La carpeta privada solo es accesible para la cuenta Windows que ejecuta el asistente.
$acl = New-Object Security.AccessControl.DirectorySecurity
$identity = [Security.Principal.WindowsIdentity]::GetCurrent().User
$acl.SetOwner($identity)
$acl.SetAccessRuleProtection($true, $false)
$rule = New-Object Security.AccessControl.FileSystemAccessRule($identity, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
$acl.AddAccessRule($rule)
Set-Acl -LiteralPath $localDir -AclObject $acl
if (!(Test-Path -LiteralPath $config)) {
    $mode = Invoke-SetupSql "SET NOCOUNT ON; SELECT CONVERT(int,SERVERPROPERTY('IsIntegratedSecurityOnly'));"
    if ($mode -ne '0') { throw 'Activa modo mixto en Propiedades del servidor > Seguridad de SSMS y reinicia SQL Server. Consulta INICIO_RAPIDO.md. No se cambia esa configuracion automaticamente.' }
    if (!(Test-LocalPort $sqlPort)) { throw "SQL Server no escucha en 127.0.0.1:$sqlPort. Configura TCP/IP y su puerto local segun la guia." }
}
Invoke-SqlFile '00_crear_base.sql'
$tableCount = Invoke-SetupSql "SET NOCOUNT ON; USE SistemaBullying; SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo') AND name IN ('roles','usuarios','estudiantes','tipos_incidente','estados_caso','casos','caso_estudiantes','seguimientos');"
if ($tableCount -eq '0') { Invoke-SqlFile '01_esquema_inicial.sql' }
elseif ($tableCount -ne '8') { throw 'La base tiene un esquema parcial. Revisalo con Eloy; no se reemplazan tablas existentes.' }
if (!(Test-Path -LiteralPath $config)) {
    $suffix = [Guid]::NewGuid().ToString('N').Substring(0,12)
    $login = 'bullying_local_' + $suffix
    $secret = 'Bg9!' + [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N').Substring(0,8)
    # La clave viaja por entrada estandar, nunca en argumentos ni en salida.
    Invoke-SetupSql "USE master; CREATE LOGIN [$login] WITH PASSWORD=N'$secret', CHECK_POLICY=ON; USE SistemaBullying; CREATE USER [$login] FOR LOGIN [$login]; GRANT SELECT ON dbo.roles TO [$login]; GRANT SELECT,INSERT,UPDATE ON dbo.usuarios TO [$login]; GRANT SELECT,INSERT ON dbo.casos TO [$login]; GRANT SELECT,INSERT ON dbo.estudiantes TO [$login]; GRANT SELECT,INSERT ON dbo.caso_estudiantes TO [$login]; GRANT SELECT ON dbo.tipos_incidente TO [$login]; GRANT SELECT ON dbo.estados_caso TO [$login]; GRANT SELECT,INSERT ON dbo.seguimientos TO [$login]; GRANT UPDATE (estado_codigo,proximo_seguimiento,cerrado_en) ON dbo.casos TO [$login];" | Out-Null
    $settings = "spring.datasource.url=jdbc:sqlserver://127.0.0.1:$sqlPort;databaseName=SistemaBullying;encrypt=true;trustServerCertificate=true;loginTimeout=5`nspring.datasource.username=$login`nspring.datasource.password=$secret`n"
    [IO.File]::WriteAllText($config, $settings, (New-Object Text.UTF8Encoding($false)))
    $secret = $null; $settings = $null
} else { Write-Host 'Se conserva la conexion privada existente.' }
Push-Location $BackendRoot
try { & $maven package; if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion del backend.' } } finally { Pop-Location }
Push-Location $FrontendRoot
try { & $npm ci; if ($LASTEXITCODE -ne 0) { throw 'Fallo la instalacion de Angular.' } } finally { Pop-Location }
$admins = Invoke-SetupSql "SET NOCOUNT ON; USE SistemaBullying; SELECT COUNT(*) FROM dbo.usuarios WHERE activo=1 AND rol_codigo='ADMINISTRADOR';"
if ($admins -eq '0') {
    Write-Host 'Crea ahora tu cuenta del dashboard. Recuerda el correo y la clave que elijas.'
    & (Join-Path $ProjectRoot 'Iniciar-local.ps1') -CrearAdministrador
} else { Write-Host 'Ya existe un administrador activo. Se conserva su cuenta y su clave.' }
if ($DatosDemo) { Invoke-SqlFile '05_datos_demostracion.sql' }
Write-Host 'Preparacion terminada. Ejecuta .\Iniciar-proyecto.ps1 y entra con TU cuenta.'
