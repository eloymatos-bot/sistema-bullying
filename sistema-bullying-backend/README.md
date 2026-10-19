# Sistema de bullying: backend

Spring Boot 4.1.1, Java 17+ y SQL Server local. Angular está conectado mediante
el proxy `/api` y se incluye junto al backend en la rama `develop`.

## Avance

- Actividad 8: diseño de ocho tablas y scripts en `database/`.
- Actividad 9: base `SistemaBullying` creada y probada.
- Actividad 10: usuarios e inicio/cierre de sesión implementados y probados contra SQL Server.
- Actividad 11: registro y consulta de casos, estudiantes, filtros y acceso por rol.
- Actividad 14: seguimiento transaccional, historial, estados y versiones.
- Actividades 16–17: consultas de alertas y reportes, con acceso por usuario.
- Administración: directorio, edición de usuarios y activación/desactivación.

## Preparación y arranque

```powershell
mvn package
```

La configuración de este equipo ya está en `.local/application-local.properties`,
excluida de Git. Los permisos de la cuenta SQL del backend están en
`database/03_permisos_aplicacion.sql`: usuarios, consulta de catálogos y registro/consulta
de casos y estudiantes. No puede crear/borrar tablas ni eliminar registros.
SQL Server escucha para este proyecto solo en la interfaz local.

En otro equipo, crea el archivo anterior con tus propios valores:

```properties
spring.datasource.url=jdbc:sqlserver://127.0.0.1:14333;databaseName=SistemaBullying;encrypt=true;trustServerCertificate=true;loginTimeout=5
spring.datasource.username=TU_USUARIO_SQL
spring.datasource.password=TU_CONTRASENA_SQL
```

El puerto debe coincidir con tu instalación. La aceptación del certificado del
ejemplo es solo para desarrollo local. En despliegue usa un certificado confiable
y `trustServerCertificate=false`. También puedes configurar `DB_URL`, `DB_USER`
y `DB_PASSWORD` fuera de archivos.

### Primer administrador

En una base nueva sin administrador activo, ejecuta una vez:

```powershell
.\Iniciar-local.ps1 -CrearAdministrador
```

Pide nombres, apellidos, correo y contraseña oculta. No guarda la contraseña en
archivos: la base recibe un hash BCrypt. El comando termina tras crear el usuario
y rechaza ejecutarse si ya existe un administrador activo. No hay registro público
de administradores ni contraseña inicial compartida.

Después inicia el servidor:

```powershell
.\Iniciar-local.ps1
```

La API escucha en `http://127.0.0.1:8080`. `GET /api/auth/csrf` permite comprobar
que responde. Las pantallas están en Angular. Detener con Ctrl+C.
El script usa `JAVA_HOME` si está definido.

## API de la actividad 10

| Método y ruta | Acceso | Función |
|---|---|---|
| `GET /api/auth/csrf` | Público | Devuelve `headerName` y `token`; establece sesión. |
| `POST /api/auth/login` | Público, con CSRF | Formulario `email` y `password`, tipo `application/x-www-form-urlencoded`. |
| `GET /api/auth/me` | Autenticado | ID, nombres, apellidos, correo, rol y estado. |
| `POST /api/auth/logout` | Con CSRF | Invalida sesión y cookie; responde 204. |
| `GET /api/users?offset=0&limit=50` | Administrador | Lista usuarios; máximo 100 por consulta. |
| `POST /api/users` | Administrador, con CSRF | Crea usuario; responde 201. |
| `PATCH /api/users/{id}/status` | Administrador, con CSRF | Activa/desactiva mediante `{"activo":true}`. |

El alta recibe JSON con `nombres`, `apellidos`, `correo`, `password` y `rol`.
Roles: `ADMINISTRADOR`, `CONVIVENCIA`, `DOCENTE_TUTOR`. Los dos últimos pueden
iniciar sesión y consultar su perfil, pero no administrar otros usuarios.

## Sesiones y seguridad

- Conservar cookie `JSESSIONID` y enviar `X-CSRF-TOKEN` en POST/PATCH.
  Obtener un token nuevo después del login y del logout.
- Spring Security renueva el ID de sesión al autenticar.
- Sesión inactiva: 30 minutos. Cookie HttpOnly y SameSite=Strict.
- BCrypt factor 12; contraseña de al menos 12 caracteres y máximo 72 bytes UTF-8.
  La API nunca devuelve contraseñas ni hashes.
- Correos normalizados y únicos; SQL parametrizado.
- Desactivar una cuenta impide nuevos logins y revoca acceso en la siguiente solicitud.
- El administrador no puede desactivarse a sí mismo; un bloqueo transaccional
  protege que quede al menos un administrador activo.
- Máximo 15 intentos de login por IP/minuto, incluidos los exitosos. El contador
  está en memoria; varias instancias requerirían un almacén compartido.
- Errores de login genéricos. Estados: 400 datos, 401 sesión, 403 permiso/CSRF,
  409 conflicto y 429 límite de intentos.
- API ligada a 127.0.0.1. En despliegue usar HTTPS y `COOKIE_SECURE=true`.
- CORS acepta `FRONTEND_ORIGIN` (por defecto localhost:4200). En la integración
  usar proxy Angular para `/api` y el mismo hostname; no mezclar localhost y
  127.0.0.1 para las cookies SameSite.

Pendiente: recuperación de contraseña mediante autoservicio. Las pantallas de
login, casos, seguimiento, alertas, reportes y usuarios ya están conectadas.

## API de la actividad 11

Todas estas rutas requieren sesión. El alta también requiere CSRF.

| Método y ruta | Función |
|---|---|
| `POST /api/cases` | Registra caso y participaciones en una transacción; devuelve 201 y Location. |
| `GET /api/cases/{id}` | Detalle y estudiantes involucrados. |
| `GET /api/cases` | Lista con `q`, `estado`, `tipo`, `desde`, `hasta`, `offset` y `limit`. |
| `GET /api/cases/catalogs` | Tipos, estados y participaciones permitidas. |
| `GET /api/cases/responsibles` | Personal activo: ID, nombres y rol; admite `q`, `offset`, `limit`. |
| `GET /api/students` | Estudiantes disponibles; admite `q`, `offset`, `limit`. |

La política inicial permite a administración y convivencia consultar todos los casos.
Los docentes solo consultan los que registraron o tienen asignados. Un detalle ajeno
responde 404. La búsqueda de estudiantes aplica el mismo ámbito; permite registrar
estudiantes nuevos dentro del alta del caso. Los nombres no son identificadores únicos:
reutilizar `estudianteId` cuando corresponda para evitar duplicados.

Ejemplo ficticio de alta (sustituir responsableId por un ID activo):

```json
{
  "fechaIncidente": "2026-10-09",
  "lugar": "Patio",
  "tipoCodigo": "AGRESION_VERBAL",
  "descripcion": "Descripción del incidente reportado.",
  "observaciones": null,
  "responsableId": 1,
  "estudiantes": [{
    "nuevoEstudiante": {"nombres": "Ejemplo", "apellidos": "Ficticio", "grado": 2, "seccion": "A"},
    "participacion": "AFECTADO"
  }]
}
```

Para uno existente usar `{"estudianteId":123,"participacion":"AFECTADO"}`.
Cada participación indica un ID existente o un estudiante nuevo, nunca ambos.
Se exige al menos un afectado, fecha no futura, responsable activo y 1–50 participaciones.
El autor se toma de la sesión; el estado inicial siempre es REPORTADO.
La respuesta de detalle incluye `caso`, `descripcion`, `observaciones` y `estudiantes`.
Los listados de casos y estudiantes incluyen `items`, `total`, `offset`, `limit` (máximo 100).
La búsqueda de casos usa código, lugar o nombre del estudiante; los filtros se combinan.
Los cambios de estado y seguimientos se describen a continuación. No se expone borrado de casos.

## Seguimiento y consultas agregadas

| Método y ruta | Función |
|---|---|
| `GET /api/cases/{id}/followups` | Versión hexadecimal, estado, próxima fecha, permiso de edición e historial. |
| `POST /api/cases/{id}/followups` | Guarda acción y caso en una transacción; devuelve 201. |
| `GET /api/alerts` | Resumen y listado paginado; `q`, `clase`, `responsableId`, `offset`, `limit`. |
| `GET /api/reports?year=2026` | Estadísticas del año de registro en hora de Perú, con ámbito del usuario. |
| `GET /api/users/directory` | Solo administrador; `q`, `rol`, `activo`, `offset`, `limit`, totales y listado sin hashes. |
| `PUT /api/users/{id}` | Solo administrador; edita nombres, apellidos, correo y rol, conserva contraseña y actividad. |

El seguimiento recibe `version`, `accion`, `fechaAccion`, `estado`, `proximaFecha`.
Es obligatorio conservar el estado o avanzar un paso: REPORTADO → EN_EVALUACION →
EN_SEGUIMIENTO → CERRADO. Un caso cerrado queda para consulta. No hay reaperturas.
La fecha de acción va del incidente a hoy (America/Lima). La próxima fecha es
opcional y no puede ser anterior a hoy. Cerrar exige próxima fecha nula.
Se bloquea el caso para guardar, verifica ROWVERSION y agrega el historial en la
misma transacción. Versión desactualizada: 409 sin insertar acciones parciales.
Administración y convivencia editan; docentes solo si son responsables asignados.

Alertas: VENCIDA, PROXIMA, EVALUACION y SIN_FECHA. Las dos últimas no tienen plazo
de vencimiento inventado. «Atendidas» cuenta casos con acciones fechadas en el mes.
Los contadores de alertas son del ámbito completo; los filtros solo afectan el listado.
Los reportes filtran por fecha de creación del caso en Perú, no por fecha del incidente;
estados y cierres son los actuales de esos registros, incluso si cerraron después.
Los promedios usan segundos transcurridos / 86400, solo para casos cerrados.

## Base de datos

1. `database/00_crear_base.sql`: crea la base si no existe.
2. `database/01_esquema_inicial.sql`: ocho tablas, once claves foráneas y catálogos.
3. `database/02_verificar_base.sql`: prueba restricciones y revierte datos ficticios.
4. `database/03_permisos_aplicacion.sql`: permisos mínimos para el usuario SQL `bullying_app`, previamente creado por el administrador.
5. `database/04_permisos_seguimiento.sql`: lectura/inserción de historial y actualización de estado/fechas de casos.
6. `database/DISENO.md`: campos, relaciones y reglas.

Ejecutar manualmente con una cuenta autorizada. Spring Boot no crea ni altera
el esquema. El script inicial se detiene si ya hay tablas; usar migraciones después.

## Verificación

- Compilación y empaquetado Maven con JDK 25 y objetivo Java 17.
- Scripts ejecutados en SQL Server real, con opciones SET para el índice calculado.
- Pruebas HTTP: `tests/integration_auth.py` y `tests/integration_cases.py` (Python estándar). Requieren API local
  en ejecución, base de desarrollo sin administradores, `JAVA_HOME`,
  `TEST_SQL_SERVER` y `sqlcmd` (o `SQLCMD_PATH`).
- Verifican login, sesión, CSRF, roles, altas, duplicados, contraseñas,
  desactivación/reactivación, logout y límite de intentos.
- La suite de casos verifica persistencia, asignación, permisos por docente, filtros,
  paginación, búsquedas literales, validaciones y ausencia de datos parciales al rechazar altas.
- Crean cuentas, casos y estudiantes ficticios con prefijo aleatorio y los eliminan al terminar;
  no borran datos ajenos a la prueba. Dejan saltos normales en IDENTITY.
- La cuenta usada para las pruebas SQL necesita permiso de eliminar sus datos
  ficticios; la cuenta SQL del backend no tiene ese permiso.

```powershell
$env:TEST_SQL_SERVER='lpc:.\TU_INSTANCIA'
python tests/integration_auth.py
# Esperar un minuto o reiniciar el backend antes de ejecutar la siguiente suite.
python tests/integration_cases.py
```

El límite de login permanece un minuto tras la prueba; espera o reinicia el backend
antes de repetirla. Nunca subir `.local/`, credenciales o registros locales a GitHub.

## Referencias

- [Spring Security: login](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/form.html).
- [Spring Security: CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html).
- [Microsoft JDBC: conexión](https://learn.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url?view=sql-server-ver17).
