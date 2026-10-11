# Sistema de bullying: backend

Spring Boot 4.1.1, Java 17+ y SQL Server local. Angular se mantiene separado y se
conectará en las actividades de integración del cronograma.

## Avance

- Actividad 8: diseño de ocho tablas y scripts en `database/`.
- Actividad 9: base `SistemaBullying` creada y probada.
- Actividad 10: usuarios e inicio/cierre de sesión implementados y probados contra SQL Server.
- Registro y consulta de casos (actividad 11): pendiente.

## Preparación y arranque

```powershell
mvn package
```

La configuración de este equipo ya está en `.local/application-local.properties`,
excluida de Git. La cuenta SQL del backend solo tiene SELECT/INSERT/UPDATE sobre
`usuarios` y SELECT sobre `roles`; no puede crear/borrar tablas ni eliminar registros.
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

Todavía no se ha creado una cuenta personal. Ejecuta una vez:

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

Pendientes: recuperación de contraseña, edición de roles, registro/consulta de
casos e integración de pantallas. Las rutas de casos aún no están habilitadas.

## Base de datos

1. `database/00_crear_base.sql`: crea la base si no existe.
2. `database/01_esquema_inicial.sql`: ocho tablas, once claves foráneas y catálogos.
3. `database/02_verificar_base.sql`: prueba restricciones y revierte datos ficticios.
4. `database/DISENO.md`: campos, relaciones y reglas.

Ejecutar manualmente con una cuenta autorizada. Spring Boot no crea ni altera
el esquema. El script inicial se detiene si ya hay tablas; usar migraciones después.

## Verificación

- Compilación y empaquetado Maven con JDK 25 y objetivo Java 17.
- Scripts ejecutados en SQL Server real, con opciones SET para el índice calculado.
- Pruebas HTTP: `tests/integration_auth.py` (Python estándar). Requieren API local
  en ejecución, base de desarrollo sin administradores, `JAVA_HOME`,
  `TEST_SQL_SERVER` y `sqlcmd` (o `SQLCMD_PATH`).
- Verifican login, sesión, CSRF, roles, altas, duplicados, contraseñas,
  desactivación/reactivación, logout y límite de intentos.
- Crean cuentas ficticias con prefijo aleatorio y las eliminan al terminar;
  no borran usuarios ajenos a la prueba. Dejan saltos normales en IDENTITY.
- La cuenta usada para las pruebas SQL necesita permiso de eliminar sus datos
  ficticios; la cuenta SQL del backend no tiene ese permiso.

```powershell
$env:TEST_SQL_SERVER='lpc:.\TU_INSTANCIA'
python tests/integration_auth.py
```

El límite de login permanece un minuto tras la prueba; espera o reinicia el backend
antes de repetirla. Nunca subir `.local/`, credenciales o registros locales a GitHub.

## Referencias

- [Spring Security: login](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/form.html).
- [Spring Security: CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html).
- [Microsoft JDBC: conexión](https://learn.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url?view=sql-server-ver17).
