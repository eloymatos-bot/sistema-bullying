-- Ejecutar con una cuenta administradora sobre la base del proyecto.
-- No crea logins ni contraseñas. Reutiliza el usuario SQL del backend.
USE [SistemaBullying];
GO
IF DATABASE_PRINCIPAL_ID(N'bullying_app') IS NULL
    THROW 51020, N'Primero configura el usuario SQL bullying_app para el backend.', 1;
GRANT SELECT, INSERT, UPDATE ON dbo.usuarios TO [bullying_app];
GRANT SELECT ON dbo.roles TO [bullying_app];
GRANT SELECT, INSERT ON dbo.casos TO [bullying_app];
GRANT SELECT, INSERT ON dbo.estudiantes TO [bullying_app];
GRANT SELECT, INSERT ON dbo.caso_estudiantes TO [bullying_app];
GRANT SELECT ON dbo.tipos_incidente TO [bullying_app];
GRANT SELECT ON dbo.estados_caso TO [bullying_app];
-- No se concede DELETE, ALTER ni permisos de seguimiento de la actividad 14.
GO
