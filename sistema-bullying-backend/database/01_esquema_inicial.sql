-- Diseño inicial, actividad 8. Ejecutar después de 00_crear_base.sql.
-- No borra ni reemplaza tablas existentes. Si ya fue aplicado, se detiene.
USE [SistemaBullying];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF DB_NAME() <> N'SistemaBullying'
    THROW 51000, N'Conéctate a SistemaBullying antes de ejecutar el esquema.', 1;

IF EXISTS (
    SELECT 1 FROM sys.tables
    WHERE schema_id = SCHEMA_ID(N'dbo')
      AND name IN (N'roles', N'usuarios', N'estudiantes', N'tipos_incidente',
                   N'estados_caso', N'casos', N'caso_estudiantes', N'seguimientos')
)
    THROW 51001, N'Existen tablas del proyecto. Usa una migración; este script no sobrescribe datos.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    CREATE TABLE dbo.roles (
        codigo VARCHAR(30) NOT NULL CONSTRAINT PK_roles PRIMARY KEY,
        nombre NVARCHAR(80) NOT NULL CONSTRAINT UQ_roles_nombre UNIQUE
    );

    CREATE TABLE dbo.usuarios (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_usuarios PRIMARY KEY,
        nombres NVARCHAR(100) NOT NULL,
        apellidos NVARCHAR(100) NOT NULL,
        correo NVARCHAR(254) COLLATE Latin1_General_100_CI_AS NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        rol_codigo VARCHAR(30) NOT NULL,
        activo BIT NOT NULL CONSTRAINT DF_usuarios_activo DEFAULT (1),
        creado_en DATETIME2(0) NOT NULL CONSTRAINT DF_usuarios_creado DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_usuarios_correo UNIQUE (correo),
        CONSTRAINT FK_usuarios_roles FOREIGN KEY (rol_codigo) REFERENCES dbo.roles(codigo),
        CONSTRAINT CK_usuarios_nombres CHECK (LEN(LTRIM(RTRIM(nombres))) > 0),
        CONSTRAINT CK_usuarios_apellidos CHECK (LEN(LTRIM(RTRIM(apellidos))) > 0),
        CONSTRAINT CK_usuarios_correo CHECK (LEN(LTRIM(RTRIM(correo))) > 0),
        CONSTRAINT CK_usuarios_hash CHECK (LEN(password_hash) >= 60)
    );

    CREATE TABLE dbo.estudiantes (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_estudiantes PRIMARY KEY,
        nombres NVARCHAR(100) NOT NULL,
        apellidos NVARCHAR(100) NOT NULL,
        grado TINYINT NOT NULL,
        seccion NVARCHAR(10) NOT NULL,
        activo BIT NOT NULL CONSTRAINT DF_estudiantes_activo DEFAULT (1),
        creado_en DATETIME2(0) NOT NULL CONSTRAINT DF_estudiantes_creado DEFAULT SYSUTCDATETIME(),
        CONSTRAINT CK_estudiantes_nombres CHECK (LEN(LTRIM(RTRIM(nombres))) > 0),
        CONSTRAINT CK_estudiantes_apellidos CHECK (LEN(LTRIM(RTRIM(apellidos))) > 0),
        CONSTRAINT CK_estudiantes_grado CHECK (grado BETWEEN 1 AND 5),
        CONSTRAINT CK_estudiantes_seccion CHECK (LEN(LTRIM(RTRIM(seccion))) > 0)
    );

    CREATE TABLE dbo.tipos_incidente (
        codigo VARCHAR(30) NOT NULL CONSTRAINT PK_tipos_incidente PRIMARY KEY,
        nombre NVARCHAR(80) NOT NULL CONSTRAINT UQ_tipos_incidente_nombre UNIQUE
    );

    CREATE TABLE dbo.estados_caso (
        codigo VARCHAR(30) NOT NULL CONSTRAINT PK_estados_caso PRIMARY KEY,
        nombre NVARCHAR(50) NOT NULL CONSTRAINT UQ_estados_caso_nombre UNIQUE
    );

    CREATE TABLE dbo.casos (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_casos PRIMARY KEY,
        codigo AS ('CB-' + CONVERT(VARCHAR(20), id)) PERSISTED,
        fecha_incidente DATE NOT NULL,
        lugar NVARCHAR(200) NOT NULL,
        tipo_codigo VARCHAR(30) NOT NULL,
        descripcion NVARCHAR(4000) NOT NULL,
        observaciones NVARCHAR(2000) NULL,
        estado_codigo VARCHAR(30) NOT NULL CONSTRAINT DF_casos_estado DEFAULT ('REPORTADO'),
        responsable_id BIGINT NOT NULL,
        registrado_por BIGINT NOT NULL,
        creado_en DATETIME2(0) NOT NULL CONSTRAINT DF_casos_creado DEFAULT SYSUTCDATETIME(),
        proximo_seguimiento DATE NULL,
        cerrado_en DATETIME2(0) NULL,
        version ROWVERSION NOT NULL,
        CONSTRAINT UQ_casos_codigo UNIQUE (codigo),
        CONSTRAINT FK_casos_tipo FOREIGN KEY (tipo_codigo) REFERENCES dbo.tipos_incidente(codigo),
        CONSTRAINT FK_casos_estado FOREIGN KEY (estado_codigo) REFERENCES dbo.estados_caso(codigo),
        CONSTRAINT FK_casos_responsable FOREIGN KEY (responsable_id) REFERENCES dbo.usuarios(id),
        CONSTRAINT FK_casos_registrador FOREIGN KEY (registrado_por) REFERENCES dbo.usuarios(id),
        CONSTRAINT CK_casos_lugar CHECK (LEN(LTRIM(RTRIM(lugar))) > 0),
        CONSTRAINT CK_casos_descripcion CHECK (LEN(LTRIM(RTRIM(descripcion))) > 0),
        CONSTRAINT CK_casos_cierre CHECK (
            (estado_codigo = 'CERRADO' AND cerrado_en IS NOT NULL AND proximo_seguimiento IS NULL)
            OR (estado_codigo <> 'CERRADO' AND cerrado_en IS NULL)
        ),
        CONSTRAINT CK_casos_fecha_cierre CHECK (cerrado_en IS NULL OR cerrado_en >= creado_en),
        CONSTRAINT CK_casos_proxima_fecha CHECK (proximo_seguimiento IS NULL OR proximo_seguimiento >= fecha_incidente)
    );

    CREATE TABLE dbo.caso_estudiantes (
        caso_id BIGINT NOT NULL,
        estudiante_id BIGINT NOT NULL,
        participacion VARCHAR(30) NOT NULL,
        CONSTRAINT PK_caso_estudiantes PRIMARY KEY (caso_id, estudiante_id, participacion),
        CONSTRAINT FK_caso_estudiantes_caso FOREIGN KEY (caso_id) REFERENCES dbo.casos(id),
        CONSTRAINT FK_caso_estudiantes_estudiante FOREIGN KEY (estudiante_id) REFERENCES dbo.estudiantes(id),
        CONSTRAINT CK_caso_estudiantes_participacion CHECK (
            participacion IN ('AFECTADO', 'PRESUNTO_AGRESOR', 'TESTIGO')
        )
    );

    CREATE TABLE dbo.seguimientos (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_seguimientos PRIMARY KEY,
        caso_id BIGINT NOT NULL,
        registrado_por BIGINT NOT NULL,
        accion NVARCHAR(4000) NOT NULL,
        fecha_accion DATE NOT NULL,
        registrado_en DATETIME2(0) NOT NULL CONSTRAINT DF_seguimientos_fecha DEFAULT SYSUTCDATETIME(),
        estado_anterior VARCHAR(30) NULL,
        estado_nuevo VARCHAR(30) NULL,
        proxima_fecha DATE NULL,
        CONSTRAINT FK_seguimientos_caso FOREIGN KEY (caso_id) REFERENCES dbo.casos(id),
        CONSTRAINT FK_seguimientos_usuario FOREIGN KEY (registrado_por) REFERENCES dbo.usuarios(id),
        CONSTRAINT FK_seguimientos_anterior FOREIGN KEY (estado_anterior) REFERENCES dbo.estados_caso(codigo),
        CONSTRAINT FK_seguimientos_nuevo FOREIGN KEY (estado_nuevo) REFERENCES dbo.estados_caso(codigo),
        CONSTRAINT CK_seguimientos_accion CHECK (LEN(LTRIM(RTRIM(accion))) > 0),
        CONSTRAINT CK_seguimientos_estados CHECK (
            (estado_anterior IS NULL AND estado_nuevo IS NULL)
            OR (estado_anterior IS NOT NULL AND estado_nuevo IS NOT NULL AND estado_anterior <> estado_nuevo)
        ),
        CONSTRAINT CK_seguimientos_proxima_fecha CHECK (proxima_fecha IS NULL OR proxima_fecha >= fecha_accion),
        CONSTRAINT CK_seguimientos_cierre CHECK (estado_nuevo IS NULL OR estado_nuevo <> 'CERRADO' OR proxima_fecha IS NULL)
    );

    CREATE INDEX IX_usuarios_rol ON dbo.usuarios(rol_codigo);
    CREATE INDEX IX_casos_responsable_estado ON dbo.casos(responsable_id, estado_codigo);
    CREATE INDEX IX_casos_estado_fecha ON dbo.casos(estado_codigo, fecha_incidente);
    CREATE INDEX IX_casos_proximo ON dbo.casos(proximo_seguimiento) INCLUDE (estado_codigo, responsable_id);
    CREATE INDEX IX_caso_estudiantes_estudiante ON dbo.caso_estudiantes(estudiante_id, caso_id);
    CREATE INDEX IX_seguimientos_caso_fecha ON dbo.seguimientos(caso_id, registrado_en, id);

    INSERT INTO dbo.roles(codigo, nombre) VALUES
        ('ADMINISTRADOR', N'Administrador'),
        ('CONVIVENCIA', N'Responsable de convivencia'),
        ('DOCENTE_TUTOR', N'Docente / tutor');

    INSERT INTO dbo.estados_caso(codigo, nombre) VALUES
        ('REPORTADO', N'Reportado'),
        ('EN_EVALUACION', N'En evaluación'),
        ('EN_SEGUIMIENTO', N'En seguimiento'),
        ('CERRADO', N'Cerrado');

    INSERT INTO dbo.tipos_incidente(codigo, nombre) VALUES
        ('AGRESION_FISICA', N'Agresión física'),
        ('AGRESION_VERBAL', N'Agresión verbal'),
        ('EXCLUSION_SOCIAL', N'Exclusión social'),
        ('CIBERBULLYING', N'Ciberbullying'),
        ('AMENAZAS', N'Amenazas'),
        ('BURLAS_REITERADAS', N'Burlas reiteradas');

    COMMIT TRANSACTION;
    PRINT N'Esquema creado: 8 tablas y catálogos iniciales. No se crearon usuarios ni casos reales.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
