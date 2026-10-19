# Trabajo pendiente del equipo

Plan preparado el 18/10/2026. Responsables: Eloy y Viviana, según el cronograma.
Este documento asigna trabajo futuro; no atribuye a Viviana código ya desarrollado
ni presenta pruebas, manuales o exposiciones pendientes como terminados.

## Responsabilidades y evidencias

| Actividad | Responsable | Trabajo que debe realizar | Entregable verificable |
|---|---|---|---|
| 13. Demostración del miércoles | Ambos | Viviana explica el problema, los usuarios y demuestra un caso desde el registro hasta el seguimiento. Eloy prepara el entorno, explica la conexión Angular/API/SQL Server y responde la parte técnica. Ensayar juntos antes de clase. | Guion con la parte de cada integrante y recorrido probado; después, observaciones reales del profesor. |
| 18. Integración en develop | Eloy; revisión de Viviana | Eloy integra y compila. Viviana descarga develop y revisa las instrucciones de instalación; registra lo que no entiende o no funciona. | Integración publicada y registro de revisión de Viviana. La revisión de Viviana sigue pendiente. |
| 19. Pruebas y correcciones | Ambos | Viviana ejecuta pruebas manuales de la matriz de abajo y registra resultados y errores. Eloy corrige los errores técnicos. Viviana repite los casos corregidos. | Matriz con fecha, responsable, resultado y evidencias; incidencias con pasos para reproducir y comprobación posterior. |
| 20. Manual y documentación | Viviana: manual; Eloy: documentación técnica | Viviana redacta el manual con capturas propias, pasos de uso y mensajes de error. Eloy revisa que los pasos correspondan al sistema y documenta instalación, API y base. | `docs/MANUAL_USUARIO.md` y documentación técnica, con aportes identificables. Ambos pendientes. |
| 21. Informe y exposición final | Ambos | Viviana redacta problema, objetivos, recorrido de uso y resultados de las pruebas que ejecutó. Eloy redacta arquitectura, datos y validaciones técnicas. Ambos revisan y exponen su parte. | Informe, diapositivas y reparto de exposición revisados. Pendientes. |
| 22. Ajustes del profesor | Ambos | Registrar la observación concreta; Eloy corrige código y Viviana verifica y actualiza el manual cuando corresponda. | Lista de observaciones con evidencia de resolución. Depende de la evaluación. |

## Prioridad de Viviana antes del miércoles

1. Descargar `develop` y comprobar que puede acceder al sistema con una cuenta
   propia autorizada, coordinando la instalación con Eloy si utiliza otro equipo.
2. Ejecutar primero P01–P06, capturar la evidencia y registrar errores reales.
3. Empezar el manual: acceso, navegación, registro de caso y seguimiento.
4. Preparar y ensayar su demostración; explicar qué probó, qué encontró y qué falta.

Continuar P07–P12, completar el manual y preparar el informe conforme al cronograma.
Si una prueba falla, conservar la evidencia, abrir una incidencia y volver a probar
cuando esté corregida. No modificar ni borrar casos ajenos para hacer pruebas.

## Matriz manual para completar por Viviana

Usar estudiantes y casos ficticios con un prefijo propio, por ejemplo `QA-VIV-001`.
Anotar la cuenta o rol usado sin registrar contraseñas. Las pruebas que necesiten
perfiles diferentes requieren cuentas preparadas por administración.

| ID | Prueba | Resultado esperado | Resultado real / evidencia |
|---|---|---|---|
| P01 | Acceder con datos válidos e intentar con contraseña incorrecta. | Acceso correcto y rechazo claro de credenciales incorrectas. | Pendiente |
| P02 | Cerrar sesión e intentar volver a una pantalla protegida. | Solicita iniciar sesión de nuevo; no permite consultar información protegida. | Pendiente |
| P03 | Registrar un caso ficticio con afectado y responsable. Recargar. | El caso conserva sus datos y aparece en el listado. | Pendiente |
| P04 | Enviar un caso sin afectado o con fecha futura. | Rechaza el registro e indica el dato que debe corregirse. | Pendiente |
| P05 | Buscar el caso por código o estudiante; combinar filtros. | Muestra resultados correspondientes y comunica cuando no hay coincidencias. | Pendiente |
| P06 | Guardar una acción y avanzar a evaluación; recargar. | La acción y el estado se conservan en el historial. | Pendiente |
| P07 | Avanzar secuencialmente hasta cerrado e intentar registrar otra acción. | Conserva el historial; un caso cerrado queda para consulta. | Pendiente |
| P08 | Revisar alertas vencidas, próximas y sin fecha con los datos ficticios. | El detalle del caso coincide con la clase de alerta; cerrados no aparecen. | Pendiente |
| P09 | Cambiar el año de reportes e imprimir/guardar PDF. | Cambian los datos del año y el documento se puede leer sin navegación superpuesta. | Pendiente |
| P10 | Con administrador, crear un usuario ficticio, editarlo y desactivar/reactivar. | Cambios persistentes y mensajes claros; no permite desactivarse a sí mismo. | Pendiente |
| P11 | Con docente, intentar entrar a usuarios y a un caso ajeno. | No concede administración de usuarios ni acceso a casos fuera de su ámbito. | Pendiente |
| P12 | Abrir las pantallas en móvil o ventana estrecha. | Formularios y botones utilizables; contenido principal sin desbordamiento. | Pendiente |

Para cada ejecución registrar fecha, persona, navegador, pasos, resultado real,
captura y, si falla, enlace a una incidencia. No incluir datos personales reales,
contraseñas ni archivos de configuración privada en las capturas.

## Cómo mostrar los aportes

Viviana debe guardar sus pruebas, manual y correcciones documentales desde su propia
cuenta de GitHub, usando una rama personal y una solicitud de revisión hacia
`develop`. En cada aporte describirá lo que realmente realizó. Eloy revisará los
cambios antes de integrarlos. Las capturas deben corresponder a pruebas ejecutadas,
no a resultados supuestos. Las actividades se marcarán como hechas al verificarlas.
