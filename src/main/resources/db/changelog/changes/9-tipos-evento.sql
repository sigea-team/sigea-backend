--liquibase formatted sql

-- =====================================================================
-- Módulo de eventos: catálogo de tipos de evento.
-- El tipo del evento deja de ser texto libre (eventos.tipo) y pasa a elegirse de un catálogo
-- administrable (tipos_evento), referenciado por eventos.tipo_evento_id. Permite incorporar nuevos
-- tipos de evento sin modificar el software (RNF15).
-- =====================================================================

--changeset SIGEA:tipos-evento-001-tabla
--comment: Catálogo global de tipos de evento (congreso, seminario, taller, etc.).
CREATE TABLE IF NOT EXISTS "tipos_evento" (
    "tipo_evento_id" serial PRIMARY KEY,
    "nombre"         varchar(50)  NOT NULL,
    "descripcion"    varchar(255)
);
-- Nombre único sin distinguir mayúsculas ("Congreso" y "congreso" son el mismo tipo).
CREATE UNIQUE INDEX IF NOT EXISTS "ux_tipos_evento_nombre_ci" ON "tipos_evento" (LOWER("nombre"));
--rollback DROP TABLE IF EXISTS "tipos_evento";

--changeset SIGEA:tipos-evento-002-datos-iniciales
--comment: 1) Conserva como tipos los textos que ya tienen los eventos registrados (sin repetir, sin espacios sobrantes).
--comment: 2) Agrega los tipos de evento que menciona el documento del proyecto (sección 1.1), si no existen.
INSERT INTO "tipos_evento" ("nombre")
SELECT DISTINCT ON (LOWER(REGEXP_REPLACE(TRIM("tipo"), '\s+', ' ', 'g')))
       LEFT(REGEXP_REPLACE(TRIM("tipo"), '\s+', ' ', 'g'), 50)
FROM "eventos"
WHERE "tipo" IS NOT NULL AND TRIM("tipo") <> ''
ORDER BY LOWER(REGEXP_REPLACE(TRIM("tipo"), '\s+', ' ', 'g')), "evento_id";

INSERT INTO "tipos_evento" ("nombre")
SELECT v.nombre
FROM (VALUES ('Conferencia'), ('Seminario'), ('Jornada académica'), ('Encuentro de investigación'),
             ('Taller'), ('Simposio'), ('Hackatón'), ('Encuentro de semilleros')) AS v(nombre)
WHERE NOT EXISTS (SELECT 1 FROM "tipos_evento" t WHERE LOWER(t."nombre") = LOWER(v.nombre));
--rollback DELETE FROM "tipos_evento";

--changeset SIGEA:tipos-evento-003-fk-eventos
--comment: Cada evento referencia su tipo por ID. Se llena a partir del texto actual de eventos.tipo.
--comment: Es nullable en la base de datos (como lo era eventos.tipo); la API lo exige al crear o editar.
ALTER TABLE "eventos" ADD COLUMN IF NOT EXISTS "tipo_evento_id" integer;
UPDATE "eventos" e
SET "tipo_evento_id" = t."tipo_evento_id"
FROM "tipos_evento" t
WHERE e."tipo" IS NOT NULL
  AND LOWER(t."nombre") = LOWER(LEFT(REGEXP_REPLACE(TRIM(e."tipo"), '\s+', ' ', 'g'), 50));
ALTER TABLE "eventos" ADD CONSTRAINT "eventos_tipo_evento_id_fkey"
    FOREIGN KEY ("tipo_evento_id") REFERENCES "tipos_evento"("tipo_evento_id");
-- Índice para contar los eventos que usan un tipo antes de eliminarlo.
CREATE INDEX IF NOT EXISTS "idx_eventos_tipo_evento" ON "eventos" ("tipo_evento_id");
--rollback DROP INDEX IF EXISTS "idx_eventos_tipo_evento";
--rollback ALTER TABLE "eventos" DROP CONSTRAINT IF EXISTS "eventos_tipo_evento_id_fkey";
--rollback ALTER TABLE "eventos" DROP COLUMN IF EXISTS "tipo_evento_id";

--changeset SIGEA:tipos-evento-004-eliminar-tipo-texto
--comment: La columna de texto libre queda reemplazada por tipo_evento_id (una sola fuente de verdad).
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM "eventos" WHERE "tipo" IS NOT NULL AND TRIM("tipo") <> '' AND "tipo_evento_id" IS NULL
ALTER TABLE "eventos" DROP COLUMN IF EXISTS "tipo";
--rollback ALTER TABLE "eventos" ADD COLUMN "tipo" varchar(50);
--rollback UPDATE "eventos" e SET "tipo" = t."nombre" FROM "tipos_evento" t WHERE e."tipo_evento_id" = t."tipo_evento_id";
