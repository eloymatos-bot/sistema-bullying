# Inicio en otra computadora Windows

## Una sola vez

1. Instala Java 17 o superior, Maven, Node.js 22 LTS y el motor SQL Server Developer
   o Express. Instala SSMS para comprobar y administrar la instancia. Java, Maven y
   Node/npm deben estar en PATH. Abre Windows PowerShell. El asistente usa la
   conexion SQL integrada de PowerShell y no requiere instalar `sqlcmd.exe`.
2. En SSMS comprueba que puedes entrar a tu instancia local con autenticacion de
   Windows y administrar la base. Copia el nombre de esa instancia.
3. En las propiedades del servidor de SSMS > Seguridad habilita autenticacion de
   SQL Server y Windows (modo mixto). En SQL Server Configuration Manager habilita
   TCP/IP para tu instancia y configura un puerto fijo en la direccion 127.0.0.1.
   Anota ese puerto y reinicia la instancia. Si necesitas ayuda, coordina con Eloy.
   El asistente no instala SQL Server ni cambia servicios o protocolos.
4. Clona el proyecto:

   ```powershell
   git clone --branch develop https://github.com/eloymatos-bot/sistema-bullying.git
   cd sistema-bullying
   Set-ExecutionPolicy -Scope Process RemoteSigned
   .\Preparar-proyecto.ps1 -DatosDemo
   ```

El asistente pide el nombre de servidor que usas en SSMS y el puerto TCP local.
Crea la base y las tablas solo si faltan,
genera una cuenta SQL exclusiva con permisos limitados, instala las dependencias
y compila. Finalmente pide tus nombres, correo y una clave de al menos 12 caracteres
para crear TU administrador del dashboard. La clave SQL se genera automaticamente;
es distinta de la clave con la que entras al dashboard.

Si PowerShell bloquea los archivos descargados, revisa los scripts y usa
`Unblock-File .\Preparar-proyecto.ps1`, `Unblock-File .\Iniciar-proyecto.ps1`,
`Unblock-File .\Iniciar-local.ps1`, `Unblock-File .\scripts\Entorno.ps1` y
`Unblock-File .\sistema-bullying-backend\Iniciar-local.ps1`.
Si la politica local lo requiere, ejecuta en esa terminal
`Set-ExecutionPolicy -Scope Process RemoteSigned`; una politica de la institucion
debe gestionarse con su administrador.

La opcion `-DatosDemo` carga los 50 casos ficticios de demostracion. Es opcional
y repetirla no duplica la carga. En SSMS puedes actualizar el nodo Bases de datos
y abrir `SistemaBullying → Tablas` para comprobar el resultado.
Repetir la preparacion conserva la configuracion privada y las cuentas existentes;
no restablece claves. No reutilices el archivo privado de otra computadora.

## Arranque clasico en dos terminales

Abre las dos terminales en la raiz del repositorio. Usa este recorrido para mostrar
en vivo como se inicia cada parte.

1. En la primera terminal, ejecuta:

   ```powershell
   Set-ExecutionPolicy -Scope Process RemoteSigned
   .\Iniciar-local.ps1
   ```

   Deja esta terminal abierta. Espera a que Spring Boot arranque en el puerto 8080.
2. En la segunda terminal, ejecuta:

   ```powershell
   cd .\Sistema_Bullying_Figma_Sin_Errores
   npm.cmd start
   ```

   En PowerShell, `npm.cmd start` ejecuta el mismo script que `npm start` y
   evita que Windows elija la variante `npm.ps1`. Abre http://127.0.0.1:4200
   cuando Angular indique que esta listo. Para detener, presiona Ctrl+C en cada terminal.

## Arranque alternativo con un comando

```powershell
.\Iniciar-proyecto.ps1
```

Espera el mensaje de Angular y abre http://127.0.0.1:4200. Ingresa con el correo y
la clave que elegiste. Al terminar presiona Ctrl+C en la terminal (confirma la
finalizacion del proceso si Windows la solicita). El backend iniciado por el
asistente se detiene al salir. La base queda en tu SQL Server local.

## Si no puedes entrar

- Error generico: revisa que SQL Server este iniciado, el puerto correcto y la
  terminal de arranque sin errores. Mira `.local/backend.log` y `backend-error.log`.
- Credenciales incorrectas: usa la cuenta creada en TU base; clonar GitHub no copia
  la cuenta ni los datos de Eloy. No se escribe un hash en el campo contrasena.
- Puerto ocupado: cierra el servidor anterior y repite el inicio; no arranques dos.
- Esquema parcial: el asistente se detiene para conservar datos; coordina la
  revision con Eloy, sin borrar la base.
- Configuracion existente con conexion incorrecta: corrige `.local/application-local.properties`
  usando el ejemplo de la raiz. No lo subas a GitHub.

Primera preparacion requiere Internet para descargar dependencias. La puesta en
marcha completa debe comprobarse en tu equipo; la instalacion de SQL Server varia.
