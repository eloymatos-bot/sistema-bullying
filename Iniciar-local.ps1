param(
    [switch]$CrearAdministrador,
    [string]$Nombres,
    [string]$Apellidos,
    [string]$Email,
    [string]$Password
)
$ErrorActionPreference='Stop'
Push-Location $PSScriptRoot
try {
    $localConfig = Join-Path $PSScriptRoot '.local'
    if (-not (Test-Path -LiteralPath (Join-Path $localConfig 'application-local.properties'))) {
        $localConfig = Join-Path (Split-Path $PSScriptRoot -Parent) '.local'
    }
    if (-not (Test-Path -LiteralPath (Join-Path $localConfig 'application-local.properties'))) {
        throw 'Falta .local/application-local.properties. Consulta README.md para configurar SQL Server.'
    }
    $javaCommand = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
    if (-not (Test-Path -LiteralPath 'target/sistema-bullying-backend-0.0.1-SNAPSHOT.jar')) {
        throw 'Primero compila el proyecto con mvn package.'
    }
    if ($CrearAdministrador) {
        $env:BOOTSTRAP_ADMIN='true'
        if ($Nombres) { $env:ADMIN_NOMBRES=$Nombres } else { $env:ADMIN_NOMBRES=Read-Host 'Nombres del administrador' }
        if ($Apellidos) { $env:ADMIN_APELLIDOS=$Apellidos } else { $env:ADMIN_APELLIDOS=Read-Host 'Apellidos del administrador' }
        if ($Email) { $env:ADMIN_EMAIL=$Email } else { $env:ADMIN_EMAIL=Read-Host 'Correo del administrador' }
        if ($Password) { $env:ADMIN_PASSWORD=$Password } else {
            $securePassword=Read-Host 'Contraseña (mínimo 12 caracteres, máximo 72 bytes UTF-8)' -AsSecureString
            $secretPointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
            try { $env:ADMIN_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($secretPointer) }
            finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secretPointer) }
        }
    }
    $configLocation = 'file:' + ($localConfig.Replace('\','/').TrimEnd('/') + '/')
    & $javaCommand -jar 'target/sistema-bullying-backend-0.0.1-SNAPSHOT.jar' '--spring.profiles.active=local' "--spring.config.additional-location=$configLocation"
    if ($LASTEXITCODE -ne 0) { throw 'El backend terminó con un error. Revisa el mensaje anterior.' }
} finally {
    if ($CrearAdministrador) {
        Remove-Item Env:BOOTSTRAP_ADMIN,Env:ADMIN_NOMBRES,Env:ADMIN_APELLIDOS,Env:ADMIN_EMAIL,Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue
    }
    Pop-Location
}
