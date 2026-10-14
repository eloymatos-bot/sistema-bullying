# Convivencia escolar — Angular conectado a Spring Boot

Actividad 12: login y casos conectados a SQL Server a través del backend de la rama
`be`. Diseño revisado con el MCP de Figma el 14/10/2026.

## Iniciar en este proyecto

1. En la carpeta de Spring Boot, compilar con `mvn package` si aún no existe el JAR.
   La base y los permisos se preparan con los scripts `database/` del backend.
2. Si aún no existe un administrador, ejecutar allí `./Iniciar-local.ps1 -CrearAdministrador`.
   Introducir nombres, apellidos, correo y contraseña privada (mínimo 12 caracteres).
   El comando no crea un administrador si ya existe uno activo.
3. En esa misma carpeta ejecutar `./Iniciar-local.ps1` para iniciar el backend.
4. En la carpeta Angular, ejecutar `npm ci` si faltan dependencias y después `npm start`.
5. Abrir `http://localhost:4200` e ingresar con la cuenta creada.

En el equipo del proyecto las carpetas Angular y `sistema-bullying-backend` son
hermanas. En GitHub, Angular está en la raíz de la rama `fe`; el backend está en
`sistema-bullying-backend/` de la rama `be`.

`proxy.conf.json` envía `/api` a `127.0.0.1:8080`. Mantener un solo hostname durante
la sesión. No se incluyen credenciales SQL ni contraseñas en Angular.
La aplicación usa renderizado en el navegador: no requiere SSR para pantallas privadas.
En despliegue, servir `dist/cerro-alegre/browser`, configurar fallback a `index.html`
y un proxy de `/api` al backend con HTTPS. El proxy de desarrollo no se aplica al build.

## Funciones comprobadas

- Login real, cookie de sesión HttpOnly, CSRF, restauración al recargar y logout.
- Rutas protegidas; identidad y rol vienen del servidor. No se elige el rol desde el login.
- «Recordar mi correo» guarda únicamente el correo, nunca la contraseña ni permisos.
- Registro transaccional de casos con responsable activo y estudiantes nuevos o existentes.
- Participación de cada estudiante, grado y sección; al menos un afectado obligatorio.
- Confirmación de guardado solo después de la respuesta del servidor; bloqueo de doble envío.
- Consulta del caso seleccionado, filtros por texto, estado, tipo y rango de fechas y paginación.
- Resumen de estados basado en el total del servidor, limitado al ámbito del usuario.
- Mensajes de carga, error, falta de permisos y resultados vacíos.

Los docentes ven los casos que registraron o tienen asignados. Administración y
convivencia consultan todos. La API sigue siendo la autoridad para los permisos.
Los listados cargan los afectados desde el detalle de cada caso (hasta diez por página).
Una futura optimización puede incluirlos en el resumen para reducir solicitudes.

## Ajustes respecto del prototipo

Se conservan la paleta, tipografías, estructura de paneles y navegación de Figma.
Los SVG del login y de búsqueda están descargados en `public/assets/figma`.
Las fuentes se cargan desde Google Fonts con alternativas locales si no hay conexión.
El selector de rol del login se reemplaza por una explicación: el permiso pertenece
a la cuenta autenticada. El campo libre de estudiantes pasa a datos estructurados.
Los filtros muestran las opciones implementadas en la API (tipo y rango de fechas).

Seguimientos, cambios de estado, alertas, reportes y la pantalla de administración
de usuarios todavía no están conectados. Se muestran como pendientes; ya no anuncian
guardados ficticios ni presentan ejemplos como datos reales. Los componentes antiguos
de esas pantallas se conservan en el código para las próximas actividades.

## Verificación

`npm run build` compila la versión de producción.
`tests/run_e2e.py` prueba Angular, Spring Boot y SQL Server reales con Edge mediante
Playwright. Requiere ambos servidores iniciados, Python, Node, Playwright disponible
para Node, `JAVA_HOME`, `TEST_SQL_SERVER` y `SQLCMD_PATH` (o sqlcmd en PATH).
Solo debe ejecutarse sobre una base de desarrollo sin usuarios. Genera credenciales
aleatorias en memoria y elimina únicamente sus datos ficticios al terminar.
Las capturas quedan en `.local/e2e`, excluidas de GitHub.

Pruebas: rechazo de credenciales incorrectas, rutas protegidas, alta con afectado,
persistencia y recarga, búsqueda, filtros, detalle inexistente, resumen actualizado,
logout, acceso restringido de docentes y presentación móvil sin desbordamiento.

Referencia: https://www.figma.com/design/LsFEGyJIy1aRbBTWh5dTXH/Dashboard?node-id=0-1
