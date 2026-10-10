-- Actividad 9: ejecutar cuando corresponda crear la base en SQL Server local.
-- Abrir en SSMS conectado a .\ELOYSERVER. No establece rutas físicas:
-- SQL Server utiliza las carpetas de datos y registro de su instancia.
USE [master];
GO
IF DB_ID(N'SistemaBullying') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE [SistemaBullying]');
END;
GO
