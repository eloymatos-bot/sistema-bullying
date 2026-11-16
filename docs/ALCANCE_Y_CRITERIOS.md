# Actividad 23: campos, permisos y criterios de aceptación

Definición del 19/10/2026. Una institución por instalación; entrega completa hasta
S17. Se reutilizan el login, los casos y los seguimientos existentes. Las tareas
24–31 implementan esta definición; definirlas no significa que ya estén programadas.

## Estudiantes (24–26)

| Campo | Regla |
|---|---|
| id | Generado por SQL Server. Identifica al estudiante; los nombres no son una clave única. |
| nombres / apellidos | Obligatorios, hasta 100 caracteres cada uno, espacios exteriores retirados, sin controles. |
| grado | Entero de 1 a 5, correspondiente a secundaria. |
| seccion | Obligatoria, hasta 10 caracteres, espacios exteriores retirados; nuevas altas/ediciones guardan mayúsculas. |
| activo | Administración y convivencia pueden activarlo o desactivarlo con control de versión para conservar el historial. |
| version | ROWVERSION de SQL Server, representada por 16 caracteres hexadecimales. Obligatoria al editar. |

Administración y convivencia registran y editan en el directorio. Docentes consultan
solo estudiantes activos vinculados a casos que registraron o tienen asignados.
Se mantiene la capacidad existente de crear un estudiante al reportar un caso,
incluidos docentes; eso no concede edición libre del directorio.

No se exige DNI, contacto familiar, fecha de nacimiento, matrícula anual ni nuevas
cuentas de estudiantes. Dos alumnos con iguales nombres pueden existir; la pantalla
debe permitir buscar antes de crear y seleccionar por id. No se deduplican ni fusionan
registros automáticamente. No hay borrado de estudiantes ni cambios de sus vínculos.

La corrección de nombres/grado/sección se muestra también en los casos relacionados.
Los casos conservan sus id e historial; no se guarda una copia de la matrícula de
cada año. Grado y sección siempre representan los datos actuales del directorio.

Criterios:

- Una alta válida devuelve 201 con Location, id y versión; consultar de nuevo conserva los datos.
- Una edición válida conserva id, actividad, fecha de alta, participaciones y seguimientos.
- Una versión antigua devuelve 409; no sobrescribe la edición más reciente.
- Datos inválidos devuelven 400 sin inserciones ni modificaciones parciales.
- Anónimo no consulta datos; docente no crea/edita por los endpoints del directorio.
- Docente no consulta el detalle de un alumno ajeno (404) ni lo encuentra en el directorio.
- Búsqueda literal por nombre/apellidos, filtros de grado/sección y paginación se combinan.
- Las API previas de selección de estudiantes y creación de casos siguen disponibles.
- Se conservan los estudiantes de demostración y los casos existentes al migrar.

## Acuerdos de atención (27–29, pendientes)

Datos previstos: id, casoId, descripción obligatoria (1–2000 caracteres),
responsableId de un usuario activo, fechaLimite obligatoria (hoy o posterior al alta),
cumplido (inicialmente falso), cumplidoEn (UTC al marcar cumplimiento), creadoPor,
creadoEn y version. Las fechas calendario se interpretan en America/Lima.

Administración y convivencia gestionan acuerdos; docente solo en casos que tenga
asignados. Consultas respetan el alcance del caso. El servidor toma el autor de la
sesión. Un caso cerrado no admite nuevos acuerdos ni cambios. No se borran acuerdos
ni se reescribe su descripción después del alta. Marcar cumplimiento es una acción
única con versión; volver a marcar no crea otro cumplimiento. Deshacerlo queda fuera.

Al cerrar un caso se deberá impedir el cierre si quedan acuerdos pendientes, con un
mensaje claro. No cambia el orden de estados del seguimiento. Migración: los casos
anteriores empiezan sin acuerdos y pueden seguir usando su flujo habitual.

Criterios: guardar y recargar acuerdo; rechazo de fecha/responsable inválidos;
permisos por caso; conflicto 409 ante versión antigua; autor y sellos desde servidor;
caso cerrado sin edición; cierre rechazado con acuerdos pendientes y permitido
cuando todos están cumplidos. Revisar estas reglas al comenzar 27 antes de migrar.

## Filtros de reportes (30, pendientes)

Se conserva el año de registro del caso (hora de Perú). Se agregan grado (1–5) y
sección actual del estudiante como filtros opcionales. Ambos deben coincidir sobre
un mismo estudiante vinculado, en cualquier participación. Un caso con dos o más
alumnos coincidentes se cuenta una sola vez mediante EXISTS; conserva el alcance
del usuario. No equivale a una estadística histórica de matrícula ni exclusivamente
a víctimas. La pantalla y la impresión deben explicar ese criterio.

Criterios: total = suma por tipos = suma por estados = suma de registros mensuales;
sin duplicación por múltiples estudiantes; filtros combinados, valores vacíos y
sin resultados; impresión muestra año y filtros aplicados; permisos sin cambios.

## Entregables y límites de esfuerzo

- Eloy: definición (23), API/migración de estudiantes (24), integración (26),
  acuerdos de servidor (27), integración (29), filtros (30) e integración final (31).
- Viviana: pantallas de estudiantes (25) y acuerdos (28), pruebas manuales y manual,
  con aportes propios. Su disponibilidad continúa por confirmar.
- Para cada tarea: fuentes en GitHub, instrucciones reproducibles y evidencia de
  comprobación antes de marcar Hecho. Pruebas y borradores durante el desarrollo;
  S17 reserva la revisión final y la presentación.

El presupuesto de 1–2 horas por semana de Eloy es una estimación de sesiones con
apoyo de herramientas. Revisar el tamaño de cada tarea si excede la sesión; no
reducir comprobaciones para sostener una estimación. Adjuntos, avisos externos,
auditoría completa, matrícula anual y despliegue público no forman parte de esta ampliación.
