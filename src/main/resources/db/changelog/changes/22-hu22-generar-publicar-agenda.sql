--liquibase formatted sql

-- =====================================================================
-- HU-22 (RF38 + RF39) - Generar y publicar la agenda del evento
-- =====================================================================

--changeset SIGEA:hu22-001-actividades-por-programar
--comment: HU-22 Criterios 1, 2 y 4. Permite registrar actividades sin fecha, horario ni sala (estado 'por_programar')
--comment: para que la generación automática las ubique. Una actividad en cualquier otro estado sigue exigiendo los
--comment: cuatro datos. duracion_minutos guarda cuánto dura la actividad para poder ubicarla; se rellena con la
--comment: duración de las actividades que ya existen. El CHECK hora_fin > hora_inicio se mantiene (con NULL no aplica).
ALTER TABLE "actividades" ADD COLUMN IF NOT EXISTS "duracion_minutos" INT;
UPDATE "actividades"
   SET "duracion_minutos" = (EXTRACT(EPOCH FROM ("hora_fin" - "hora_inicio")) / 60)::INT
 WHERE "duracion_minutos" IS NULL AND "hora_inicio" IS NOT NULL AND "hora_fin" IS NOT NULL;
ALTER TABLE "actividades" ALTER COLUMN "fecha" DROP NOT NULL;
ALTER TABLE "actividades" ALTER COLUMN "hora_inicio" DROP NOT NULL;
ALTER TABLE "actividades" ALTER COLUMN "hora_fin" DROP NOT NULL;
ALTER TABLE "actividades" ALTER COLUMN "sala_id" DROP NOT NULL;
ALTER TABLE "actividades" ADD CONSTRAINT "actividades_duracion_check"
    CHECK ("duracion_minutos" IS NULL OR ("duracion_minutos" > 0 AND "duracion_minutos" <= 1440));
ALTER TABLE "actividades" ADD CONSTRAINT "actividades_programacion_completa_check"
    CHECK (("estado" = 'por_programar' AND "duracion_minutos" IS NOT NULL)
        OR ("estado" <> 'por_programar' AND "fecha" IS NOT NULL AND "hora_inicio" IS NOT NULL
            AND "hora_fin" IS NOT NULL AND "sala_id" IS NOT NULL));
--rollback DELETE FROM "actividades" WHERE "estado" = 'por_programar';
--rollback ALTER TABLE "actividades" DROP CONSTRAINT IF EXISTS "actividades_programacion_completa_check";
--rollback ALTER TABLE "actividades" DROP CONSTRAINT IF EXISTS "actividades_duracion_check";
--rollback ALTER TABLE "actividades" ALTER COLUMN "sala_id" SET NOT NULL;
--rollback ALTER TABLE "actividades" ALTER COLUMN "hora_fin" SET NOT NULL;
--rollback ALTER TABLE "actividades" ALTER COLUMN "hora_inicio" SET NOT NULL;
--rollback ALTER TABLE "actividades" ALTER COLUMN "fecha" SET NOT NULL;
--rollback ALTER TABLE "actividades" DROP COLUMN IF EXISTS "duracion_minutos";

--changeset SIGEA:hu22-002-actividades-asignacion-automatica
--comment: HU-22 Criterio 1. Marca las actividades que ubicó la generación automática, para que el organizador las
--comment: revise y ajuste antes de publicar. Pasa a false cuando el organizador la programa a mano (HU-21).
ALTER TABLE "actividades" ADD COLUMN IF NOT EXISTS "asignacion_automatica" BOOLEAN NOT NULL DEFAULT false;
CREATE INDEX IF NOT EXISTS "idx_actividades_evento_estado" ON "actividades" ("evento_id", "estado");
--rollback DROP INDEX IF EXISTS "idx_actividades_evento_estado";
--rollback ALTER TABLE "actividades" DROP COLUMN IF EXISTS "asignacion_automatica";

--changeset SIGEA:hu22-003-indice-inscritos-actividad
--comment: HU-22 Criterio 5. Índice para buscar rápido los inscritos de una actividad al notificar un cambio.
CREATE INDEX IF NOT EXISTS "idx_inscripciones_actividad_actividad" ON "inscripciones_actividad" ("actividad_id");
--rollback DROP INDEX IF EXISTS "idx_inscripciones_actividad_actividad";
