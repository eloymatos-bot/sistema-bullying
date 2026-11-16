# Informe final · Sistema de seguimiento de casos de bullying

**Instituto Valle Grande · Prototipos e innovación tecnológica · Semana 17**
**Integrantes:** Eloy Matos y Viviana Cubas
**Entrega prevista:** 16 de noviembre de 2026

## Problema y objetivo

Cuando el registro del incidente, sus responsables y las acciones se manejan por separado,
es difícil saber qué atención está pendiente. El prototipo reúne el caso, las personas
involucradas y su seguimiento. El objetivo es que la institución pueda consultar quién
debe actuar, en qué plazo y qué evidencia de atención quedó registrada.

## Solución y alcance

Angular presenta el panel, los formularios y reportes. Spring Boot autentica con sesiones,
valida cada solicitud y aplica permisos. SQL Server conserva usuarios, estudiantes, casos,
participaciones, acciones y acuerdos. La interfaz usa un proxy local para acceder a la API.

- Casos: registro, búsqueda, asignación, acciones y cierre con historial consultable.
- Estudiantes: alta, búsqueda, filtros, edición y estado activo. Las correcciones se reflejan
  en las participaciones existentes. Un control de versión evita sobrescribir cambios ajenos.
- Acuerdos: descripción, responsable, plazo y constancia de cumplimiento. Un caso abierto
  con acuerdos pendientes no puede cerrarse. Los casos anteriores conservan su historial.
- Reportes: año de registro, grado y sección. Los filtros coinciden en un mismo estudiante y
  el total cuenta casos únicos. Los resultados respetan el alcance de cada usuario.
- Seguridad: sesión de usuario, protección de escritura mediante CSRF y permisos por perfil.

El alcance se limita a una institución y una instalación académica. Los filtros escolares
reflejan el directorio actual. Quedan fuera matrícula histórica, adjuntos, notificaciones
externas y puesta en producción. Las estadísticas de demostración no son resultados de campo.

## Integración y evidencias

El código y las migraciones están en la versión de entrega del repositorio. La contribución
de pantalla de estudiantes se recuperó del trabajo publicado por Viviana en la rama `fe`.
La preparación local conserva las cuentas y aplica las migraciones de manera repetible.
La preparación final completa formulario y lista de acuerdos y deja documentados los créditos
y el alcance comprobado. Ver [evidencia de pruebas](EVIDENCIA_ENTREGA.md).

## Entregables

- [Manual de usuario](MANUAL_USUARIO.md) e [inicio rápido](INICIO_RAPIDO.md).
- [Guion de exposición y comprobación previa](GUION_DEMO.md).
- [Campos y criterios de aceptación](ALCANCE_Y_CRITERIOS.md).
- Cronograma de la semana 17 y repositorio GitHub con los fuentes.

La exposición y la atención de observaciones se registran cuando ocurran.
