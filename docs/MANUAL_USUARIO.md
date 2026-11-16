# Manual de usuario · Sistema de convivencia escolar

**Prototipo académico · Instituto Valle Grande · Entrega de la semana 17**

El sistema permite registrar incidentes, proteger su historial y organizar el trabajo de seguimiento.
Usa datos ficticios durante la demostración.

| Perfil | Acceso |
|---|---|
| Administración | Casos, estudiantes, acuerdos, reportes y cuentas de usuario. |
| Convivencia | Casos, estudiantes, acuerdos y reportes. |
| Docente o tutor | Registra casos, consulta los propios o asignados y registra acciones y acuerdos cuando es el responsable del caso. Solo consulta estudiantes vinculados a sus casos. |

## Entrar y registrar un caso

1. Inicia el proyecto siguiendo [Inicio rápido](INICIO_RAPIDO.md) y abre http://127.0.0.1:4200.
2. Entra con el correo y la contraseña de tu instalación. Haz clic en **Registrar caso**.
3. Indica fecha, lugar, tipo, responsable y descripción. Añade al menos un estudiante afectado.
   Reutiliza el registro existente si corresponde. Guarda y anota el código del caso.
4. En **Casos** puedes buscar por código y abrir el detalle para ver estudiantes e historial.

## Gestionar estudiantes

En **Estudiantes**, busca por nombres y apellidos o filtra por grado y sección. Pulsa
**Nuevo estudiante** para registrar, o **Editar** para corregir los datos. Selecciona un grado
del 1 al 5. La cuenta debe pertenecer a Administración o Convivencia para guardar.

Si otra persona editó el mismo registro, revisa los datos actuales antes de volver a guardar.
Al desactivar un estudiante deja de aparecer en el selector para nuevos casos, pero sus
participaciones anteriores permanecen. La opción **Activar** lo vuelve a habilitar.

## Seguimiento y acuerdos

1. En el detalle, **Registrar acción** permite añadir una acción y una próxima fecha. Los
   estados avanzan de Reportado a En evaluación, En seguimiento y Cerrado.
2. En **Acuerdos de atención**, indica qué se hará, quién es responsable y fecha límite.
3. Cuando se cumpla, pulsa **Marcar cumplido**. Quedarán registrados el autor y la fecha.
4. Si quedan acuerdos pendientes, el sistema impide cerrar. Completa los acuerdos y vuelve a
   registrar la acción de cierre. Un caso cerrado conserva su historial para consulta.

## Alertas y reportes

- **Alertas:** revisa próximas atenciones, vencimientos y casos sin fecha programada. Desde
  el resultado abre el caso para registrar la atención.
- **Reportes:** selecciona año y, si corresponde, grado y sección, y pulsa **Consultar**.
  Filtra por grado y sección actuales de un mismo estudiante vinculado, en cualquier
  participación. Cuenta una vez cada caso, aunque coincidan varios alumnos.
- **Imprimir / guardar PDF:** usa la opción del navegador. El reporte muestra los filtros aplicados.
- Usa **Cerrar sesión** al terminar.

Si el sitio no abre, confirma que frontend y backend estén iniciados. Si no puedes guardar
un cambio, revisa el mensaje y tu rol; vuelve a iniciar sesión si la sesión venció.
