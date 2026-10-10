# Diseño de la base de datos - actividad 8

## Objetivo y alcance

Registrar casos de bullying, sus estudiantes involucrados, el responsable y las
acciones de seguimiento. El diseño parte de la ficha de diagnóstico y de los
campos del frontend Angular existente.

Para el primer prototipo se asume **una institución educativa por instalación**.
Los estudiantes no tienen cuentas de acceso. Los usuarios son personal autorizado.
Si se necesitan varias instituciones en una misma base, se deberá extender el modelo
antes de usarlo con datos reales.

## Tablas y datos

| Tabla | Clave | Información y relaciones |
|---|---|---|
| `roles` | `codigo` | Administrador, convivencia y docente/tutor. |
| `usuarios` | `id` | Nombres, apellidos, correo único, hash de contraseña, rol, activo y fecha de registro. Un rol por usuario. |
| `estudiantes` | `id` | Nombres, apellidos, grado de secundaria (1-5), sección, activo y fecha de registro. No se exige DNI para este prototipo. |
| `tipos_incidente` | `codigo` | Los seis tipos presentes en Angular. |
| `estados_caso` | `codigo` | Reportado, en evaluación, en seguimiento y cerrado. |
| `casos` | `id` | Código calculado `CB-<id>`, fecha del incidente, lugar, tipo, descripción, observaciones, estado, responsable, registrador, creación, próxima atención, cierre y versión. |
| `caso_estudiantes` | `caso_id`, `estudiante_id`, `participacion` | Relación de varios estudiantes por caso y varios casos por estudiante. Participación: afectado, presunto agresor o testigo. |
| `seguimientos` | `id` | Caso, autor, acción, fecha de acción, registro, próxima fecha y cambio de estado cuando corresponda. |

Los nombres y descripciones usan `NVARCHAR` para conservar tildes. Las claves
internas de catálogo usan códigos estables sin tildes; las etiquetas sí las incluyen.
El SQL contiene el detalle exacto de tipos, longitudes, valores nulos y restricciones.

## Relaciones

- Un rol tiene muchos usuarios.
- Un usuario puede registrar muchos casos y ser responsable de muchos casos.
- Cada caso tiene un tipo y un estado actual.
- Un caso tiene muchos estudiantes mediante `caso_estudiantes`.
- Un estudiante puede aparecer en muchos casos, incluso con varias participaciones
  dentro del mismo caso; no se permite repetir la misma combinación.
- Cada caso tiene muchos seguimientos; cada seguimiento tiene un autor.
- Los estados anterior y nuevo de un seguimiento apuntan a `estados_caso`.

## Reglas que impone SQL Server

1. Correo de usuario único, sin distinguir mayúsculas y minúsculas.
2. Claves foráneas: no se aceptan usuarios, casos, estados o estudiantes inexistentes.
3. Estado inicial por defecto: `REPORTADO`.
4. Grado entre 1 y 5; textos principales no vacíos; participación dentro del catálogo.
5. Un caso cerrado requiere fecha de cierre y no mantiene próxima fecha de atención.
   Un caso abierto no puede tener fecha de cierre.
6. Un cambio de estado de seguimiento guarda ambos estados y deben ser distintos;
   una acción sin cambio deja ambos campos vacíos.
7. No se borran automáticamente registros relacionados: no hay borrado en cascada.
8. `ROWVERSION` permite detectar ediciones simultáneas del caso. No es una fecha.

## Reglas que implementará Spring Boot

Estas reglas están diseñadas, pero **todavía no están implementadas en una API**:

- Normalizar y validar el correo; generar y verificar hashes de contraseña mediante
  Spring Security. La restricción de longitud de SQL no demuestra que un hash sea válido.
- Comprobar permisos por rol y usuario activo en cada operación; el frontend no decide el rol.
- Registrar cada caso con al menos un estudiante afectado y un responsable activo.
- Exigir fechas de incidente y acción no futuras, con referencia al día en `America/Lima`.
- Crear casos siempre en `REPORTADO`. Ruta inicial propuesta:
  `REPORTADO -> EN_EVALUACION -> EN_SEGUIMIENTO -> CERRADO`.
  Cierres anticipados o reaperturas requieren definir una regla antes de habilitarlos.
- En una única transacción, agregar el seguimiento y actualizar estado, cierre y
  próxima fecha de `casos`; usar `version` para rechazar cambios concurrentes.
  Validar que el estado anterior coincida con el estado actual del caso.
- Conservar el historial; no exponer borrado o edición libre de seguimientos.
- Desactivar usuarios y estudiantes en lugar de borrar su historial.

Los sellos de registro usan UTC (`SYSUTCDATETIME`); las fechas de incidente y
atención son fechas calendario. La API deberá convertir los sellos para mostrarlos en Lima.

## Alertas y reportes en las etapas posteriores

No se crean tablas redundantes para estadísticas. Se calcularán a partir de casos,
tipos, estados y fechas. Las alertas de atención vencida partirán de
`casos.proximo_seguimiento` en casos abiertos. Para detectar casos sin ninguna fecha
programada se deberá acordar un plazo configurable desde su registro o última acción;
la ficha no establece un número de días y no se inventa uno en este diseño.

## Correspondencia con Angular

| Campo actual | Destino |
|---|---|
| `date`, `place`, `description`, `observations` | `casos.fecha_incidente`, `lugar`, `descripcion`, `observaciones` |
| `type` | Código de `tipos_incidente`; mostrar su `nombre` en pantalla. |
| `responsible` | `casos.responsable_id`; el selector deberá enviar el ID, no el nombre. |
| `students` (texto libre actual) | Selección estructurada de estudiantes y participación en `caso_estudiantes`. |
| `status` | Código de `estados_caso`; mostrar su `nombre` en pantalla. |
| Acción de seguimiento | `seguimientos.accion`, autor y fechas. |

La adaptación de Angular se hará en la integración prevista por el cronograma.
Las tablas de seguimiento están diseñadas para evitar rehacer el modelo, pero no
significa que las funciones de seguimiento, alertas o reportes estén terminadas.

## Estado de entrega

- Actividad 8: modelo y scripts preparados.
- Actividad 9: base `SistemaBullying` creada y verificada en SQL Server local el
  10 de octubre de 2026. Ocho tablas, once claves foráneas y catálogos iniciales.
  Las pruebas de integridad están en `02_verificar_base.sql` y no dejan datos ficticios.
- Actividades 10 y 11: pendientes de desarrollar usuarios/login y registro/consulta de casos.

Los scripts solo contienen catálogos. No se incorporan los nombres de ejemplo del
frontend ni datos reales de menores a la base.
