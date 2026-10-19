# Sistema de gestión de casos de bullying

Aplicación para registrar y dar seguimiento a casos de bullying en instituciones
educativas de Cañete. La rama `develop` reúne Angular, Spring Boot y los scripts de
SQL Server; `fe`, `be` y `db` conservan sus historiales de trabajo.

## Organización

| Carpeta | Contenido |
|---|---|
| `Sistema_Bullying_Figma_Sin_Errores/` | Angular: login, inicio, casos, seguimiento, alertas, reportes y usuarios. |
| `sistema-bullying-backend/` | Proyecto Maven de Spring Boot, API, seguridad y pruebas. |
| `sistema-bullying-backend/database/` | Diseño y scripts SQL 00–05, incluidos los 50 casos ficticios opcionales. |
| `docs/` | Reparto de trabajo y entregables pendientes. |

## Preparación en otro equipo

Requisitos: Node.js compatible con Angular 19, npm, Java 17 o superior, Maven y
SQL Server. La base de datos se ejecuta en SQL Server, fuera de Angular.

1. Descargar la rama `develop`.
2. Seguir `sistema-bullying-backend/README.md` para crear la base, configurar un
   usuario SQL con los permisos de los scripts 03 y 04 y guardar la conexión en
   `.local/application-local.properties`. Usar valores propios; no subir secretos.
3. Compilar desde la raíz: `mvn -f sistema-bullying-backend/pom.xml package`.
4. En una base nueva, crear el primer administrador con
   `./Iniciar-local.ps1 -CrearAdministrador`. Si ya existe uno activo, se conserva.
5. Iniciar la API con `./Iniciar-local.ps1`.
6. En otra terminal, entrar en `Sistema_Bullying_Figma_Sin_Errores`, ejecutar
   `npm ci` y después `npm start`.
7. Abrir `http://localhost:4200`. Angular redirige `/api` a `127.0.0.1:8080`.
   Mantener el mismo hostname del navegador durante la sesión.

El script SQL 05 es opcional, manual e idempotente. Crea datos claramente ficticios
para la presentación; no se ejecuta al iniciar Spring Boot.

## Validación y siguiente paso

La actividad 18 integra el código de las ramas `be` y `fe`. El historial de `db`
ya pertenece a `be`; se conservan los scripts iniciales y sus mejoras posteriores.
La compilación de ambas aplicaciones se verifica antes de actualizar `develop`.
Las pruebas finales de aceptación de la actividad 19 y la presentación de la
actividad 13 siguen pendientes.

El reparto acordado para el trabajo pendiente y una matriz de pruebas sin resultados
inventados están en [docs/TRABAJO_EQUIPO.md](docs/TRABAJO_EQUIPO.md).
