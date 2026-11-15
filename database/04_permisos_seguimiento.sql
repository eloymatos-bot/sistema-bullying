-- Aplicar con una cuenta administradora. No cambia tablas ni datos.
USE [SistemaBullying];
GO
GRANT SELECT, INSERT ON dbo.seguimientos TO [bullying_app];
GRANT UPDATE (estado_codigo,proximo_seguimiento,cerrado_en) ON dbo.casos TO [bullying_app];
GO
