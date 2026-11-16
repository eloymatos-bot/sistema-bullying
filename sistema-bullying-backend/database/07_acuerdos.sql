USE SistemaBullying;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.acuerdos', N'U') IS NULL
CREATE TABLE dbo.acuerdos (
    id bigint IDENTITY PRIMARY KEY,
    caso_id bigint NOT NULL REFERENCES dbo.casos(id),
    descripcion nvarchar(2000) NOT NULL CHECK (LEN(LTRIM(RTRIM(descripcion)))>0),
    responsable_id bigint NOT NULL REFERENCES dbo.usuarios(id),
    fecha_limite date NOT NULL,
    creado_por bigint NOT NULL REFERENCES dbo.usuarios(id),
    creado_en datetime2 NOT NULL DEFAULT SYSUTCDATETIME(),
    cumplido_en datetime2 NULL,
    cumplido_por bigint NULL REFERENCES dbo.usuarios(id),
    version rowversion NOT NULL,
    CONSTRAINT CK_acuerdos_cumplimiento CHECK ((cumplido_en IS NULL AND cumplido_por IS NULL) OR
                                               (cumplido_en IS NOT NULL AND cumplido_por IS NOT NULL))
);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.acuerdos') AND name=N'IX_acuerdos_caso')
    CREATE INDEX IX_acuerdos_caso ON dbo.acuerdos(caso_id,cumplido_en,id);
DECLARE @u sysname, @sql nvarchar(max);
DECLARE users_app CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.database_principals
    WHERE type='S' AND (name=N'bullying_app' OR name LIKE N'bullying[_]local[_]%');
OPEN users_app;
FETCH NEXT FROM users_app INTO @u;
WHILE @@FETCH_STATUS=0 BEGIN
    SET @sql=N'GRANT SELECT,INSERT ON dbo.acuerdos TO '+QUOTENAME(@u)
        +N'; GRANT UPDATE (cumplido_en,cumplido_por) ON dbo.acuerdos TO '+QUOTENAME(@u)
        +N'; GRANT UPDATE (activo) ON dbo.estudiantes TO '+QUOTENAME(@u);
    EXEC sys.sp_executesql @sql;
    FETCH NEXT FROM users_app INTO @u;
END;
CLOSE users_app;
DEALLOCATE users_app;
COMMIT;
GO
