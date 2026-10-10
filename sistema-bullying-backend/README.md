# Sistema de bullying: backend

Proyecto Spring Boot separado de Angular. Entrega de la actividad 8 del cronograma:
diseño de la base de datos y scripts guardados dentro del backend.

## Ubicación

```text
sistema-bullying-backend/
  pom.xml
  src/main/java/pe/vallegrande/bullying/BullyingApplication.java
  src/main/resources/application.properties
  database/
    00_crear_base.sql
    01_esquema_inicial.sql
    DISENO.md
```

- SQL Server local: instancia detectada en el registro de Windows `ELOYSERVER`.
- Servidor esperado en SSMS: `.\ELOYSERVER` (conectividad todavía por verificar).
- Nombre propuesto de la base: `SistemaBullying`.
- Los archivos físicos se almacenarán en las rutas predeterminadas de SQL Server,
  no en Angular, en el backend ni en GitHub.
- Scripts y diseño: [database/DISENO.md](database/DISENO.md).

## Alcance actual

El proyecto tiene una clase de arranque mínima. Todavía no incluye API HTTP,
autenticación, entidades JPA ni conexión SQL. Esas tareas comienzan en la semana 11.
Los scripts no se ejecutan automáticamente al arrancar Spring Boot.

Base técnica: Spring Boot 4.1.1, Java 17 o superior y Maven 3.6.3 o superior.
Se fija Java 17 como versión de compilación; el equipo dispone de un JDK 25.

```powershell
mvn package
```

## Crear la base cuando corresponda la actividad 9

1. Conectarse desde SSMS a `.\ELOYSERVER` con una cuenta autorizada.
2. Revisar y ejecutar `database/00_crear_base.sql`.
3. Revisar y ejecutar `database/01_esquema_inicial.sql` completo.
4. Actualizar el explorador y verificar las 8 tablas y los catálogos.

El segundo script se ejecuta en una transacción y se detiene si ya existen tablas
del proyecto. No se debe usar para actualizar un esquema con datos; los cambios
posteriores se harán mediante scripts de migración.

Para consultar la ubicación física después de crearla:

```sql
USE [SistemaBullying];
SELECT name, physical_name FROM sys.database_files;
```

No se incluyen contraseñas ni usuarios de prueba. El inicio de sesión deberá
guardar hashes de contraseñas generados por Spring Security, nunca texto plano.

## Validación de esta entrega

- `mvn -o -B package`: compilación y empaquetado correctos con JDK 25.
- Ambos scripts pasaron el análisis de sintaxis T-SQL con Microsoft ScriptDom.
- No se han ejecutado los scripts en SQL Server. La conectividad, creación de
  tablas y pruebas de integridad con datos corresponden a la actividad 9.

## Referencias técnicas

- [Requisitos de Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html).
- [Claves foráneas en SQL Server](https://learn.microsoft.com/en-us/sql/relational-databases/tables/create-foreign-key-relationships?view=sql-server-ver17).
- [Restricciones de integridad](https://learn.microsoft.com/en-us/sql/relational-databases/tables/unique-constraints-and-check-constraints?view=sql-server-ver17).
