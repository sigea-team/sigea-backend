--liquibase formatted sql

--changeset SIGEA:hu05-001-tipos-actividad-nombre-ci
--comment: HU-05 Criterio 3. El nombre de un tipo de actividad es único por evento SIN distinguir mayúsculas
--comment: ("Taller" y "taller" son duplicados). Complementa la validación de TipoActividadService y evita duplicados
--comment: por peticiones simultáneas. El índice original (evento_id, nombre) se conserva.
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM (SELECT 1 FROM "tipos_actividad" GROUP BY "evento_id", LOWER("nombre") HAVING COUNT(*) > 1) d
CREATE UNIQUE INDEX IF NOT EXISTS "ux_tipos_actividad_evento_nombre_ci"
    ON "tipos_actividad" ("evento_id", LOWER("nombre"));
--rollback DROP INDEX IF EXISTS "ux_tipos_actividad_evento_nombre_ci";

--changeset SIGEA:hu05-002-lineas-tematicas-nombre-ci
--comment: HU-05 Criterio 3. El nombre de una línea temática es único por evento SIN distinguir mayúsculas.
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM (SELECT 1 FROM "lineas_tematicas" GROUP BY "evento_id", LOWER("nombre") HAVING COUNT(*) > 1) d
CREATE UNIQUE INDEX IF NOT EXISTS "ux_lineas_tematicas_evento_nombre_ci"
    ON "lineas_tematicas" ("evento_id", LOWER("nombre"));
--rollback DROP INDEX IF EXISTS "ux_lineas_tematicas_evento_nombre_ci";

--changeset SIGEA:hu05-003-indices-uso-parametros
--comment: HU-05 Criterio 2. Índices sobre las FK que se consultan para saber si un tipo o línea está en uso
--comment: (PostgreSQL no indexa automáticamente las columnas de llave foránea).
CREATE INDEX IF NOT EXISTS "idx_actividades_tipo_actividad" ON "actividades" ("tipo_actividad_id");
CREATE INDEX IF NOT EXISTS "idx_actividades_linea_tematica" ON "actividades" ("linea_tematica_id");
CREATE INDEX IF NOT EXISTS "idx_propuestas_linea_tematica" ON "propuestas" ("linea_tematica_id");
--rollback DROP INDEX IF EXISTS "idx_propuestas_linea_tematica";
--rollback DROP INDEX IF EXISTS "idx_actividades_linea_tematica";
--rollback DROP INDEX IF EXISTS "idx_actividades_tipo_actividad";
