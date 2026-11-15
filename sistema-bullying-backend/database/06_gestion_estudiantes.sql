-- Actividad 24. Migracion repetible, sin borrar estudiantes ni participaciones.
-- Ejecutar como administrador sobre SistemaBullying antes del backend actualizado.
USE [SistemaBullying];
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.estudiantes',N'U') IS NULL
    THROW 51030,N'Primero aplica el esquema inicial de la base.',1;
IF COL_LENGTH(N'dbo.estudiantes',N'version') IS NULL
    ALTER TABLE dbo.estudiantes ADD version ROWVERSION NOT NULL;
-- Solo las cuentas SQL de la aplicacion creadas por los asistentes del proyecto.
-- Otros nombres de usuario SQL requieren el mismo GRANT manual del administrador.
DECLARE @user sysname;
DECLARE @statement nvarchar(max);
DECLARE app_users CURSOR LOCAL FAST_FORWARD FOR
    SELECT name FROM sys.database_principals
    WHERE type='S' AND (name=N'bullying_app' OR name LIKE N'bullying[_]local[_]%');
OPEN app_users;
FETCH NEXT FROM app_users INTO @user;
WHILE @@FETCH_STATUS=0
BEGIN
    SET @statement=N'GRANT UPDATE (nombres,apellidos,grado,seccion) ON dbo.estudiantes TO '+QUOTENAME(@user);
    EXEC sys.sp_executesql @statement;
    FETCH NEXT FROM app_users INTO @user;
END;
CLOSE app_users;
DEALLOCATE app_users;
COMMIT;
GO
