# API de acuerdos y reportes · S17

Aplica `database/06_gestion_estudiantes.sql` y `database/07_acuerdos.sql` antes de
arrancar el backend. En instalaciones preparadas con el asistente, ejecutar
`Preparar-proyecto.ps1` aplica ambos scripts sin vaciar la base. Para una cuenta SQL
personalizada, concede SELECT/INSERT de acuerdos, UPDATE de cumplido_en y
cumplido_por, y UPDATE de activo en estudiantes, además de los permisos existentes.

| Ruta | Respuesta y reglas |
|---|---|
| GET /api/cases/{id}/agreements | 200, `puedeEditar` e `items`. La lista sigue el alcance del caso. |
| POST /api/cases/{id}/agreements | 201. JSON con `descripcion`, `responsableId`, `fechaLimite`. Responsable activo y plazo desde hoy en Lima. |
| PATCH /api/cases/{id}/agreements/{agreementId}/complete | 200. JSON con `version` del acuerdo; registra autor y fecha. |
| GET /api/reports?year=2026&grado=3&seccion=C | 200. Año obligatorio y filtros opcionales combinados; sin coincidencias devuelve totales en cero. |

Se requiere sesión y token CSRF para escribir. Administración, convivencia y el
docente responsable pueden gestionar un caso abierto. Un docente que lo reportó,
pero no es responsable, solo consulta. Una entidad ajena o inexistente devuelve
404; un intento de escritura sin permiso devuelve 403. Un acuerdo cumplido o
desactualizado y una escritura sobre caso cerrado devuelven 409. El seguimiento
rechaza con 400 el cierre con acuerdos pendientes. El servidor serializa alta,
cumplimiento y cierre bajo el bloqueo del caso para evitar cruces.

Los reportes cuentan cada caso una vez; grado y sección deben coincidir en el
mismo estudiante vinculado. Estado, tipos, meses y comparación anual usan la
misma población de casos. El grado y la sección provienen del directorio actual.
