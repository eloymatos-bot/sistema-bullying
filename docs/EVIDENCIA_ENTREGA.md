# Comprobaciones de entrega · 15/11/2026

Aplicación local en el navegador integrado, con Angular, Spring Boot y SQL Server.
Se usaron datos ficticios. El caso CB-70 se conserva como evidencia local; ese caso
no forma parte de la carga inicial de otras instalaciones.

| Comprobación | Resultado observado |
|---|---|
| Inicio y cierre de sesión | La cuenta local accedió al panel. Tras cerrar sesión, abrir la ruta de estudiantes redirigió al login. |
| Alta, filtro y edición de estudiantes | Se creó «Demo Entrega S17 Ficticio» en 2.º A. El directorio lo encontró por nombre. Se guardó en 3.º C y la fila mantuvo esos datos tras recargar. |
| Registro de caso | El formulario rechazó un envío sin agregar afectado. Al agregarlo, guardó el caso ficticio CB-70. |
| Seguimiento | CB-70 avanzó de Reportado a En evaluación y En seguimiento; el historial mostró las acciones. |
| Acuerdo y cierre | Se creó con responsable y fecha. El cierre se rechazó mientras estaba pendiente. Al marcarlo cumplido, el cierre se guardó y ambos resultados persistieron tras recargar. |
| Reportes | El año de prueba mostró 53 casos; el filtro 3.º C mostró 5. Una sección sin coincidencias mostró el estado vacío. |
| Navegación | Abrieron las pantallas de Alertas y Usuarios con el perfil administrador. |
| Compilación y datos | Maven y Angular compilaron. La migración 07 se aplicó sobre la base existente sin eliminar sus 52 casos previos. |
| Regresión de estudiantes | La suite HTTP/SQL pasó: roles, CSRF, filtros, vínculos, validaciones y conflictos. |
| Regresión del resto de pantallas | Pasaron controles de usuarios, alertas y seguimiento, reporte a PDF, permisos de docente y pantallas a 390 px. La primera pasada detectó desbordamiento en reportes y el ajuste de filtros lo corrigió. |

Las capturas de la sesión se entregan por separado. La exposición al profesor y
las observaciones externas no se dan por realizadas con estas comprobaciones.
