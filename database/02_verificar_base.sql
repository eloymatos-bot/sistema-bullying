-- Comprobación de estructura y restricciones. Los datos ficticios se revierten.
USE [SistemaBullying];
GO
SET NOCOUNT ON;
SET XACT_ABORT OFF;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;

IF DB_NAME() <> N'SistemaBullying'
    THROW 51010, N'La base seleccionada no es SistemaBullying.', 1;

IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo')
    AND name IN (N'roles', N'usuarios', N'estudiantes', N'tipos_incidente',
                 N'estados_caso', N'casos', N'caso_estudiantes', N'seguimientos')) <> 8
    THROW 51011, N'Faltan tablas del proyecto.', 1;
IF (SELECT COUNT(*) FROM dbo.roles) <> 3
    OR (SELECT COUNT(*) FROM dbo.estados_caso) <> 4
    OR (SELECT COUNT(*) FROM dbo.tipos_incidente) <> 6
    THROW 51012, N'Los catálogos iniciales no coinciden con el diseño.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @correo NVARCHAR(254) = CONVERT(NVARCHAR(36), NEWID()) + N'@example.invalid';
    INSERT dbo.usuarios(nombres, apellidos, correo, password_hash, rol_codigo, activo)
        VALUES (N'Prueba', N'Temporal', @correo, REPLICATE('x', 60), 'ADMINISTRADOR', 0);
    DECLARE @usuario BIGINT = SCOPE_IDENTITY();
    INSERT dbo.estudiantes(nombres, apellidos, grado, seccion)
        VALUES (N'Estudiante ficticio', N'Temporal', 3, N'A');
    DECLARE @estudiante BIGINT = SCOPE_IDENTITY();
    INSERT dbo.casos(fecha_incidente, lugar, tipo_codigo, descripcion, responsable_id, registrado_por)
        VALUES ('20261001', N'Aula de prueba', 'AGRESION_VERBAL', N'Caso ficticio de validación', @usuario, @usuario);
    DECLARE @caso BIGINT = SCOPE_IDENTITY();
    IF NOT EXISTS (SELECT 1 FROM dbo.casos WHERE id = @caso AND estado_codigo = 'REPORTADO'
                   AND codigo = 'CB-' + CONVERT(VARCHAR(20), @caso))
        THROW 51013, N'Falló el estado o código inicial.', 1;

    INSERT dbo.caso_estudiantes(caso_id, estudiante_id, participacion)
        VALUES (@caso, @estudiante, 'AFECTADO');

    DECLARE @pruebas TABLE (id INT IDENTITY PRIMARY KEY, nombre NVARCHAR(100), sentencia NVARCHAR(MAX));
    INSERT @pruebas(nombre, sentencia) VALUES
      (N'Correo duplicado, incluso en mayúsculas',
       N'INSERT dbo.usuarios(nombres,apellidos,correo,password_hash,rol_codigo) VALUES (N''Otra'',N''Prueba'',UPPER(@correo),REPLICATE(''x'',60),''ADMINISTRADOR'');'),
      (N'Responsable inexistente', N'UPDATE dbo.casos SET responsable_id=-1 WHERE id=@caso;'),
      (N'Estado inexistente', N'UPDATE dbo.casos SET estado_codigo=''INVALIDO'' WHERE id=@caso;'),
      (N'Cierre sin fecha', N'UPDATE dbo.casos SET estado_codigo=''CERRADO'' WHERE id=@caso;'),
      (N'Grado fuera de secundaria', N'UPDATE dbo.estudiantes SET grado=6 WHERE id=@estudiante;'),
      (N'Participación duplicada', N'INSERT dbo.caso_estudiantes VALUES (@caso,@estudiante,''AFECTADO'');'),
      (N'Participación inválida', N'INSERT dbo.caso_estudiantes VALUES (@caso,@estudiante,''INVALIDA'');'),
      (N'Descripción vacía', N'UPDATE dbo.casos SET descripcion=N''   '' WHERE id=@caso;'),
      (N'Cambio de estado incompleto', N'INSERT dbo.seguimientos(caso_id,registrado_por,accion,fecha_accion,estado_nuevo) VALUES (@caso,@usuario,N''Prueba'',''20261002'',''EN_EVALUACION'');');

    DECLARE @i INT=1, @n INT=(SELECT COUNT(*) FROM @pruebas), @sql NVARCHAR(MAX), @nombre NVARCHAR(100), @rechazado BIT;
    WHILE @i <= @n
    BEGIN
        SELECT @sql=sentencia, @nombre=nombre FROM @pruebas WHERE id=@i;
        SET @rechazado=0;
        BEGIN TRY
            EXEC sys.sp_executesql @sql,
                N'@caso BIGINT,@usuario BIGINT,@estudiante BIGINT,@correo NVARCHAR(254)',
                @caso,@usuario,@estudiante,@correo;
        END TRY
        BEGIN CATCH
            IF ERROR_NUMBER() NOT IN (547,2601,2627) THROW;
            SET @rechazado=1;
        END CATCH;
        IF @rechazado=0 THROW 51014, N'Una restricción aceptó datos inválidos.', 1;
        PRINT N'OK: ' + @nombre;
        SET @i+=1;
    END;

    INSERT dbo.seguimientos(caso_id,registrado_por,accion,fecha_accion,estado_anterior,estado_nuevo)
        VALUES (@caso,@usuario,N'Evaluación ficticia','20261002','REPORTADO','EN_EVALUACION');
    UPDATE dbo.casos SET estado_codigo='EN_EVALUACION' WHERE id=@caso;
    INSERT dbo.seguimientos(caso_id,registrado_por,accion,fecha_accion,estado_anterior,estado_nuevo)
        VALUES (@caso,@usuario,N'Seguimiento ficticio','20261003','EN_EVALUACION','EN_SEGUIMIENTO');
    UPDATE dbo.casos SET estado_codigo='EN_SEGUIMIENTO' WHERE id=@caso;
    INSERT dbo.seguimientos(caso_id,registrado_por,accion,fecha_accion,estado_anterior,estado_nuevo)
        VALUES (@caso,@usuario,N'Cierre ficticio','20261004','EN_SEGUIMIENTO','CERRADO');
    UPDATE dbo.casos SET estado_codigo='CERRADO',cerrado_en=SYSUTCDATETIME(),proximo_seguimiento=NULL WHERE id=@caso;
    IF (SELECT COUNT(*) FROM dbo.seguimientos WHERE caso_id=@caso) <> 3
        THROW 51015, N'Falló el historial del caso.', 1;

    ROLLBACK TRANSACTION;
    PRINT N'OK: registro, relaciones, estados e historial. Datos ficticios revertidos.';
    PRINT N'Las pruebas pueden dejar saltos en los valores IDENTITY; es normal.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
SET XACT_ABORT ON;
GO
