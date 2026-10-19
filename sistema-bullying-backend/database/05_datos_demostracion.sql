-- Carga manual para la demostración local: 50 CASOS FICTICIOS.
-- No reemplaza registros ni modifica cuentas. No se ejecuta al iniciar Spring Boot.
-- Los registros se identifican con DEMO-PRESENTACION-50-V1 en observaciones.
-- Repetir el script conserva el conjunto existente y no agrega duplicados.
USE [SistemaBullying];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;

DECLARE @marker NVARCHAR(100)=N'DEMO-PRESENTACION-50-V1';
DECLARE @today DATE=CONVERT(DATE,DATEADD(HOUR,-5,SYSUTCDATETIME()));
DECLARE @actor BIGINT=(SELECT MIN(id) FROM dbo.usuarios WHERE activo=1 AND rol_codigo='ADMINISTRADOR');
IF @actor IS NULL THROW 51030,N'Se necesita un administrador activo para registrar la demostración.',1;

BEGIN TRY
 BEGIN TRANSACTION;
 DECLARE @existing INT=(SELECT COUNT(*) FROM dbo.casos WITH (UPDLOCK,HOLDLOCK) WHERE observaciones=@marker);
 IF @existing=50
 BEGIN
  COMMIT;
  PRINT N'Los 50 casos de demostración ya existen. No se agregaron duplicados.';
  RETURN;
 END;
 IF @existing<>0 THROW 51031,N'Existe un conjunto parcial de demostración. Revisarlo antes de continuar.',1;

 DECLARE @i INT=1;
 WHILE @i<=50
 BEGIN
  DECLARE @state VARCHAR(30)=CASE WHEN @i<=12 THEN 'REPORTADO' WHEN @i<=22 THEN 'EN_EVALUACION' WHEN @i<=40 THEN 'EN_SEGUIMIENTO' ELSE 'CERRADO' END;
  DECLARE @age INT=CASE WHEN @i<=12 THEN 3+@i%10 WHEN @i<=22 THEN 12+@i%24 WHEN @i<=40 THEN 20+(@i*7)%120 ELSE 25+(@i*11)%145 END;
  DECLARE @registered DATE=DATEADD(DAY,-@age,@today);
  DECLARE @incident DATE=DATEADD(DAY,-1,@registered);
  -- 10:00 de Perú = 15:00 UTC. Los sellos de la base se almacenan en UTC.
  DECLARE @created DATETIME2(0)=DATEADD(HOUR,15,CAST(@registered AS DATETIME2(0)));
  DECLARE @type VARCHAR(30)=CASE @i%6 WHEN 0 THEN 'AGRESION_FISICA' WHEN 1 THEN 'AGRESION_VERBAL' WHEN 2 THEN 'EXCLUSION_SOCIAL' WHEN 3 THEN 'CIBERBULLYING' WHEN 4 THEN 'AMENAZAS' ELSE 'BURLAS_REITERADAS' END;
  DECLARE @place NVARCHAR(200)=N'[DEMO] '+CASE @i%6 WHEN 0 THEN N'Patio principal' WHEN 1 THEN N'Aula de secundaria' WHEN 2 THEN N'Cancha deportiva' WHEN 3 THEN N'Entorno digital simulado' WHEN 4 THEN N'Pasillo del pabellón' ELSE N'Biblioteca escolar' END;
  DECLARE @description NVARCHAR(4000)=N'CASO FICTICIO DE DEMOSTRACIÓN '+FORMAT(@i,'00')+N'. '+CASE @i%6
   WHEN 0 THEN N'Se simula un reporte de empujones entre estudiantes durante el recreo. Se requiere registrar la atención y los acuerdos.'
   WHEN 1 THEN N'Se simula un reporte de expresiones ofensivas durante una actividad escolar. Se recoge información para evaluar la situación.'
   WHEN 2 THEN N'Se simula un reporte de exclusión reiterada de un estudiante en actividades grupales.'
   WHEN 3 THEN N'Se simula un reporte de mensajes ofensivos en un grupo digital ficticio. No se adjuntan mensajes ni datos reales.'
   WHEN 4 THEN N'Se simula un reporte de comentarios intimidantes entre estudiantes. Se organiza la atención con el personal responsable.'
   ELSE N'Se simula un reporte de burlas repetidas durante una actividad de aula. Se documentan hechos y acciones de atención.' END
   +N' Todos los nombres, hechos y participantes de este registro son inventados.';
  DECLARE @next DATE=CASE WHEN @state IN ('REPORTADO','CERRADO') THEN NULL WHEN @i%4=0 THEN NULL WHEN @i%3=0 THEN DATEADD(DAY,-(1+@i%6),@today) ELSE DATEADD(DAY,@i%7,@today) END;
  DECLARE @closed DATETIME2(0)=CASE WHEN @state='CERRADO' THEN DATEADD(DAY,10+@i%14,@created) ELSE NULL END;

  INSERT dbo.casos(fecha_incidente,lugar,tipo_codigo,descripcion,observaciones,estado_codigo,responsable_id,registrado_por,creado_en,proximo_seguimiento,cerrado_en)
    VALUES(@incident,@place,@type,@description,@marker,@state,@actor,@actor,@created,@next,@closed);
  DECLARE @case BIGINT=CONVERT(BIGINT,SCOPE_IDENTITY());
  INSERT dbo.estudiantes(nombres,apellidos,grado,seccion,creado_en)
    VALUES(N'Estudiante Demo '+FORMAT(@i*2-1,'000'),N'Ficticio',1+@i%5,NCHAR(65+@i%3),@created);
  DECLARE @affected BIGINT=CONVERT(BIGINT,SCOPE_IDENTITY());
  INSERT dbo.estudiantes(nombres,apellidos,grado,seccion,creado_en)
    VALUES(N'Estudiante Demo '+FORMAT(@i*2,'000'),N'Ficticio',1+@i%5,NCHAR(65+@i%3),@created);
  DECLARE @other BIGINT=CONVERT(BIGINT,SCOPE_IDENTITY());
  INSERT dbo.caso_estudiantes(caso_id,estudiante_id,participacion)
    VALUES(@case,@affected,'AFECTADO'),(@case,@other,CASE WHEN @i%3=0 THEN 'TESTIGO' ELSE 'PRESUNTO_AGRESOR' END);

  IF @state<>'REPORTADO'
  BEGIN
   INSERT dbo.seguimientos(caso_id,registrado_por,accion,fecha_accion,registrado_en,estado_anterior,estado_nuevo,proxima_fecha)
    VALUES(@case,@actor,N'[DEMO] Se realizó la entrevista inicial y se recopiló información para evaluar el caso ficticio.',DATEADD(DAY,2,@registered),DATEADD(DAY,2,@created),'REPORTADO','EN_EVALUACION',CASE WHEN @state='EN_EVALUACION' THEN @next ELSE DATEADD(DAY,5,@registered) END);
  END;
  IF @state IN ('EN_SEGUIMIENTO','CERRADO')
  BEGIN
   INSERT dbo.seguimientos(caso_id,registrado_por,accion,fecha_accion,registrado_en,estado_anterior,estado_nuevo,proxima_fecha)
    VALUES(@case,@actor,N'[DEMO] Se acordaron medidas de acompañamiento y una revisión del bienestar de los estudiantes ficticios.',DATEADD(DAY,5,@registered),DATEADD(DAY,5,@created),'EN_EVALUACION','EN_SEGUIMIENTO',DATEADD(DAY,8,@registered));
  END;
  IF @state='EN_SEGUIMIENTO'
  BEGIN
   DECLARE @last DATE=CASE WHEN @next<@today THEN DATEADD(DAY,-1,@next) ELSE DATEADD(DAY,-(@i%5),@today) END;
   INSERT dbo.seguimientos(caso_id,registrado_por,accion,fecha_accion,registrado_en,proxima_fecha)
    VALUES(@case,@actor,N'[DEMO] Se revisaron los acuerdos y se registró el avance de la atención simulada.',@last,DATEADD(HOUR,16,CAST(@last AS DATETIME2(0))),@next);
  END;
  IF @state='CERRADO'
  BEGIN
   INSERT dbo.seguimientos(caso_id,registrado_por,accion,fecha_accion,registrado_en,estado_anterior,estado_nuevo)
    VALUES(@case,@actor,N'[DEMO] Se verificó el cumplimiento de los acuerdos y se cerró este caso ficticio.',CONVERT(DATE,@closed),@closed,'EN_SEGUIMIENTO','CERRADO');
  END;
  SET @i+=1;
 END;
 IF (SELECT COUNT(*) FROM dbo.casos WHERE observaciones=@marker)<>50 THROW 51032,N'El total de casos ficticios no coincide.',1;
 COMMIT;
 PRINT N'Carga completada: 50 casos, 100 estudiantes ficticios y 94 acciones de seguimiento.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK;
 THROW;
END CATCH;
GO

