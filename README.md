# sistema-bullying
Sistema web para registrar y dar seguimiento a casos de bullying en instituciones educativas de Cañete. Desarrollado con Angular, Spring Boot y SQL Server.

El proyecto Maven está en `sistema-bullying-backend/`. Para compilar desde esta
carpeta: `mvn -f sistema-bullying-backend/pom.xml package`.
Para iniciar: `./Iniciar-local.ps1`. El asistente acepta la configuración privada
en `.local/` de esta carpeta o dentro del proyecto Maven.
Para crear el primer administrador: `./Iniciar-local.ps1 -CrearAdministrador`.
Si ya existe uno activo, el asistente conserva esa cuenta.
