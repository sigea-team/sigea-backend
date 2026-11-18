--liquibase formatted sql

-- =====================================================================
-- HU-25 (RF46) - Aplicar encuestas de satisfacción
-- Las tablas encuestas, preguntas_encuesta y respuestas_encuesta ya existían (sin uso). Se completan para:
--   * el ciclo borrador → publicada → cerrada (Criterio 1),
--   * preguntas de escala con rango, de opción única con sus opciones y obligatorias u opcionales,
--   * respuestas anónimas o identificadas según la configuración (Criterio 2),
--   * un registro de quién respondió, que evita respuestas duplicadas sin ligar la persona a sus respuestas
--     cuando la encuesta es anónima.
-- =====================================================================

--changeset SIGEA:hu25-001-encuestas-configuracion
--comment: Criterio 1: configuración de la encuesta. El estado 'activa' (valor por defecto anterior, sin uso) pasa a
--comment: 'publicada'; las encuestas nuevas nacen en 'borrador'. fecha_cierre es opcional: después de esa fecha no
--comment: se reciben respuestas. creada_por queda en NULL si se elimina el usuario.
ALTER TABLE "encuestas" ADD COLUMN IF NOT EXISTS "descripcion" TEXT;
ALTER TABLE "encuestas" ADD COLUMN IF NOT EXISTS "anonima" BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE "encuestas" ADD COLUMN IF NOT EXISTS "fecha_creacion" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE "encuestas" ADD COLUMN IF NOT EXISTS "fecha_publicacion" TIMESTAMP;
ALTER TABLE "encuestas" ADD COLUMN IF NOT EXISTS "fecha_cierre" TIMESTAMP;
ALTER TABLE "encuestas" ADD COLUMN IF NOT EXISTS "creada_por" INT;
ALTER TABLE "encuestas" ADD CONSTRAINT "encuestas_creada_por_fkey"
    FOREIGN KEY ("creada_por") REFERENCES "usuarios" ("usuario_id") ON DELETE SET NULL;
UPDATE "encuestas" SET "estado" = 'publicada', "fecha_publicacion" = COALESCE("fecha_publicacion", CURRENT_TIMESTAMP)
WHERE "estado" = 'activa';
UPDATE "encuestas" SET "estado" = 'borrador' WHERE "estado" NOT IN ('borrador', 'publicada', 'cerrada');
ALTER TABLE "encuestas" ALTER COLUMN "estado" SET DEFAULT 'borrador';
ALTER TABLE "encuestas" ADD CONSTRAINT "encuestas_estado_check"
    CHECK ("estado" IN ('borrador', 'publicada', 'cerrada'));
ALTER TABLE "encuestas" ADD CONSTRAINT "encuestas_publicacion_check"
    CHECK ("estado" = 'borrador' OR "fecha_publicacion" IS NOT NULL);
CREATE INDEX IF NOT EXISTS "idx_encuestas_evento" ON "encuestas" ("evento_id");
CREATE INDEX IF NOT EXISTS "idx_encuestas_actividad" ON "encuestas" ("actividad_id");
--rollback DROP INDEX IF EXISTS "idx_encuestas_actividad";
--rollback DROP INDEX IF EXISTS "idx_encuestas_evento";
--rollback ALTER TABLE "encuestas" DROP CONSTRAINT IF EXISTS "encuestas_publicacion_check";
--rollback ALTER TABLE "encuestas" DROP CONSTRAINT IF EXISTS "encuestas_estado_check";
--rollback ALTER TABLE "encuestas" ALTER COLUMN "estado" SET DEFAULT 'activa';
--rollback UPDATE "encuestas" SET "estado" = 'activa' WHERE "estado" = 'publicada';
--rollback ALTER TABLE "encuestas" DROP CONSTRAINT IF EXISTS "encuestas_creada_por_fkey";
--rollback ALTER TABLE "encuestas" DROP COLUMN IF EXISTS "creada_por";
--rollback ALTER TABLE "encuestas" DROP COLUMN IF EXISTS "fecha_cierre";
--rollback ALTER TABLE "encuestas" DROP COLUMN IF EXISTS "fecha_publicacion";
--rollback ALTER TABLE "encuestas" DROP COLUMN IF EXISTS "fecha_creacion";
--rollback ALTER TABLE "encuestas" DROP COLUMN IF EXISTS "anonima";
--rollback ALTER TABLE "encuestas" DROP COLUMN IF EXISTS "descripcion";

--changeset SIGEA:hu25-002-preguntas-escala-obligatoria
--comment: Preguntas obligatorias u opcionales. Las de escala guardan su rango (0 a 10, mínimo < máximo); las demás
--comment: no llevan rango. Las preguntas existentes de escala (sin uso) quedan en 1 a 5.
ALTER TABLE "preguntas_encuesta" ADD COLUMN IF NOT EXISTS "obligatoria" BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE "preguntas_encuesta" ADD COLUMN IF NOT EXISTS "escala_minima" INT;
ALTER TABLE "preguntas_encuesta" ADD COLUMN IF NOT EXISTS "escala_maxima" INT;
UPDATE "preguntas_encuesta" SET "escala_minima" = 1, "escala_maxima" = 5
WHERE "tipo_respuesta" = 'escala' AND ("escala_minima" IS NULL OR "escala_maxima" IS NULL);
UPDATE "preguntas_encuesta" SET "escala_minima" = NULL, "escala_maxima" = NULL WHERE "tipo_respuesta" <> 'escala';
ALTER TABLE "preguntas_encuesta" ADD CONSTRAINT "preguntas_encuesta_escala_check" CHECK (
    ("tipo_respuesta" = 'escala' AND "escala_minima" IS NOT NULL AND "escala_maxima" IS NOT NULL
        AND "escala_minima" >= 0 AND "escala_maxima" <= 10 AND "escala_minima" < "escala_maxima")
    OR ("tipo_respuesta" <> 'escala' AND "escala_minima" IS NULL AND "escala_maxima" IS NULL));
CREATE INDEX IF NOT EXISTS "idx_preguntas_encuesta_encuesta" ON "preguntas_encuesta" ("encuesta_id", "orden");
--rollback DROP INDEX IF EXISTS "idx_preguntas_encuesta_encuesta";
--rollback ALTER TABLE "preguntas_encuesta" DROP CONSTRAINT IF EXISTS "preguntas_encuesta_escala_check";
--rollback ALTER TABLE "preguntas_encuesta" DROP COLUMN IF EXISTS "escala_maxima";
--rollback ALTER TABLE "preguntas_encuesta" DROP COLUMN IF EXISTS "escala_minima";
--rollback ALTER TABLE "preguntas_encuesta" DROP COLUMN IF EXISTS "obligatoria";

--changeset SIGEA:hu25-003-opciones-pregunta-encuesta
--comment: Opciones de las preguntas de tipo 'opcion' (selección única). Se borran con la pregunta.
CREATE TABLE IF NOT EXISTS "opciones_pregunta_encuesta" (
  "opcion_id" SERIAL PRIMARY KEY,
  "pregunta_id" INT NOT NULL REFERENCES "preguntas_encuesta" ("pregunta_id") ON DELETE CASCADE,
  "texto" VARCHAR(200) NOT NULL,
  "orden" INT NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS "idx_opciones_pregunta_encuesta_pregunta" ON "opciones_pregunta_encuesta" ("pregunta_id", "orden");
--rollback DROP TABLE IF EXISTS "opciones_pregunta_encuesta";

--changeset SIGEA:hu25-004-respuestas-anonimas-y-consolidables
--comment: Criterio 2: en una encuesta anónima la respuesta se guarda sin persona (persona_id NULL); el índice único
--comment: (pregunta_id, persona_id) sigue impidiendo duplicados en las identificadas. valor_escala y opcion_id
--comment: permiten consolidar sin interpretar el texto (Criterio 4). respuesta conserva el texto legible.
ALTER TABLE "respuestas_encuesta" ALTER COLUMN "persona_id" DROP NOT NULL;
ALTER TABLE "respuestas_encuesta" ADD COLUMN IF NOT EXISTS "valor_escala" INT;
ALTER TABLE "respuestas_encuesta" ADD COLUMN IF NOT EXISTS "opcion_id" INT;
ALTER TABLE "respuestas_encuesta" ADD CONSTRAINT "respuestas_encuesta_opcion_id_fkey"
    FOREIGN KEY ("opcion_id") REFERENCES "opciones_pregunta_encuesta" ("opcion_id") ON DELETE CASCADE;
ALTER TABLE "respuestas_encuesta" ADD CONSTRAINT "respuestas_encuesta_valor_escala_check"
    CHECK ("valor_escala" IS NULL OR "valor_escala" BETWEEN 0 AND 10);
CREATE UNIQUE INDEX IF NOT EXISTS "respuestas_encuesta_pregunta_id_persona_id_idx"
    ON "respuestas_encuesta" ("pregunta_id", "persona_id");
--rollback ALTER TABLE "respuestas_encuesta" DROP CONSTRAINT IF EXISTS "respuestas_encuesta_valor_escala_check";
--rollback ALTER TABLE "respuestas_encuesta" DROP CONSTRAINT IF EXISTS "respuestas_encuesta_opcion_id_fkey";
--rollback ALTER TABLE "respuestas_encuesta" DROP COLUMN IF EXISTS "opcion_id";
--rollback ALTER TABLE "respuestas_encuesta" DROP COLUMN IF EXISTS "valor_escala";
--rollback DELETE FROM "respuestas_encuesta" WHERE "persona_id" IS NULL;
--rollback ALTER TABLE "respuestas_encuesta" ALTER COLUMN "persona_id" SET NOT NULL;

--changeset SIGEA:hu25-005-participaciones-encuesta
--comment: Quién respondió cada encuesta (una vez por persona; CU-22 flujo alterno 1.1). En las anónimas es el único
--comment: vínculo con la persona y no apunta a sus respuestas. Se borra con la encuesta.
CREATE TABLE IF NOT EXISTS "participaciones_encuesta" (
  "participacion_id" SERIAL PRIMARY KEY,
  "encuesta_id" INT NOT NULL REFERENCES "encuestas" ("encuesta_id") ON DELETE CASCADE,
  "persona_id" INT NOT NULL REFERENCES "personas" ("persona_id"),
  "fecha_respuesta" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "participaciones_encuesta_encuesta_persona_key" UNIQUE ("encuesta_id", "persona_id")
);
CREATE INDEX IF NOT EXISTS "idx_participaciones_encuesta_persona" ON "participaciones_encuesta" ("persona_id");
--rollback DROP TABLE IF EXISTS "participaciones_encuesta";

--changeset SIGEA:hu25-006-permiso-encuestas
--comment: Permiso para configurar, publicar, cerrar y consultar los resultados de las encuestas. Asignación inicial:
--comment: quien edita eventos (organizador/administrador). Se ajusta por rol en HU-02.
INSERT INTO "permisos" ("codigo", "modulo", "descripcion") VALUES
  ('ENCUESTAS_GESTIONAR', 'ENCUESTAS', 'Configurar y publicar encuestas de satisfacción y consultar sus resultados')
ON CONFLICT ("codigo") DO NOTHING;
INSERT INTO "roles_permisos" ("rol_id", "permiso_id")
SELECT DISTINCT rp."rol_id", p."permiso_id"
FROM "roles_permisos" rp
JOIN "permisos" origen ON origen."permiso_id" = rp."permiso_id" AND origen."codigo" = 'EVENTOS_EDITAR'
JOIN "permisos" p ON p."codigo" = 'ENCUESTAS_GESTIONAR'
ON CONFLICT DO NOTHING;
--rollback DELETE FROM "roles_permisos" WHERE "permiso_id" IN (SELECT "permiso_id" FROM "permisos" WHERE "codigo" = 'ENCUESTAS_GESTIONAR');
--rollback DELETE FROM "permisos" WHERE "codigo" = 'ENCUESTAS_GESTIONAR';
