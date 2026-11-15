-- Activar / desactivar estudiantes. Migracion repetible: no borra ni modifica datos.
-- Ejecutar como administrador sobre SistemaBullying DESPUES de 06_gestion_estudiantes.sql.
USE [SistemaBullying];
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH(N'dbo.estudiantes',N'version') IS NULL
    THROW 51031,N'Primero aplica 06_gestion_estudiantes.sql.',1;
-- Solo las cuentas SQL de la aplicacion creadas por los asistentes del proyecto.
-- Otros nombres de usuario SQL requieren: GRANT UPDATE (activo) ON dbo.estudiantes TO [USUARIO];
DECLARE @user sysname;
DECLARE @statement nvarchar(max);
DECLARE app_users CURSOR LOCAL FAST_FORWARD FOR
    SELECT name FROM sys.database_principals
    WHERE type='S' AND (name=N'bullying_app' OR name LIKE N'bullying[_]local[_]%');
OPEN app_users;
FETCH NEXT FROM app_users INTO @user;
WHILE @@FETCH_STATUS=0
BEGIN
    SET @statement=N'GRANT UPDATE (activo) ON dbo.estudiantes TO '+QUOTENAME(@user);
    EXEC sys.sp_executesql @statement;
    FETCH NEXT FROM app_users INTO @user;
END;
CLOSE app_users;
DEALLOCATE app_users;
COMMIT;
GO
