# Actividad 24: gestión de estudiantes

Aplicar `sistema-bullying-backend/database/06_gestion_estudiantes.sql` con una cuenta
administradora antes de iniciar el backend actualizado. Es repetible: añade
`estudiantes.version` si falta, sin modificar nombres ni vínculos existentes.
Concede UPDATE únicamente de nombres/apellidos/grado/sección a `bullying_app` y las
cuentas `bullying_local_*` creadas por el asistente. Para otra cuenta SQL, conceder
manualmente `GRANT UPDATE (nombres,apellidos,grado,seccion) ON dbo.estudiantes TO [USUARIO];`.
La cuenta de aplicación necesita además los permisos SELECT/INSERT ya existentes.
El asistente `Preparar-proyecto.ps1` aplica la migración también en instalaciones nuevas.

Todas las rutas requieren sesión. POST y PUT requieren el token de `/api/auth/csrf`
obtenido después del login. No enviar identidad ni rol del autor desde Angular.

| Método / ruta | Resultado |
|---|---|
| GET /api/students/directory?q=&grado=2&seccion=A&offset=0&limit=20 | Página con items, total, offset y limit; grado/sección opcionales. |
| GET /api/students/{id} | Datos y versión; 404 si no existe o queda fuera del alcance. |
| POST /api/students | Alta por administrador/convivencia; 201 y cabecera Location. |
| PUT /api/students/{id} | Edición por administrador/convivencia; 200 y versión nueva. |
| GET /api/students | Selector anterior, sin cambiar su contrato. |

Alta:

```json
{"nombres":"Ejemplo","apellidos":"Ficticio","grado":2,"seccion":"a"}
```

Respuesta: id, nombres, apellidos, grado, seccion (A), activo y version.
Para editar, enviar los cuatro campos anteriores más `version` de la última
consulta, por ejemplo `"version":"000000000000001a"`. La versión del ejemplo no
sirve como valor real: debe copiarse de la respuesta del alumno seleccionado.

Docente puede consultar únicamente alumnos activos de casos registrados/asignados,
pero no administrar el directorio. Administración/convivencia consultan todos,
incluidos inactivos; no hay endpoint para activarlos, desactivarlos ni borrarlos.

Errores: 400 datos, búsqueda >100 o paginación inválida; 401 sesión; 403 rol/CSRF;
404 inexistente/ajeno; 409 versión antigua. La pantalla 25 debe conservar el borrador
ante 409 y ofrecer recargar; después de guardar debe actualizar la versión local.
GET admite limit 1–100; orden estable por apellidos, nombres e id. `%`, `_` y `[` en
la búsqueda se interpretan literalmente, no como comodines SQL.

La edición muestra los datos actuales del estudiante en casos antiguos; no cambia
la identidad, las participaciones ni las acciones registradas. Buscar antes de
crear para reutilizar alumnos existentes; los nombres iguales no implican duplicado.

Prueba reproducible en base local de desarrollo con datos existentes:

```powershell
$env:TEST_BASE_URL='http://127.0.0.1:8080'
$env:TEST_SQL_SERVER='lpc:.\TU_INSTANCIA'
# JAVA_HOME y SQLCMD_PATH configurados en este equipo.
python sistema-bullying-backend/tests/integration_students.py
```

La prueba crea cuentas y alumnos ficticios con prefijo aleatorio; retira solo sus
propios registros al terminar, incluso si falla. No requiere borrar administradores.
