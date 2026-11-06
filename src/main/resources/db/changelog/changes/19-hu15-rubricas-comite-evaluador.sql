--liquibase formatted sql

-- =====================================================================
-- HU-15 (RF22 + RF23) - Configurar rúbricas de evaluación y comité evaluador
-- =====================================================================

--changeset SIGEA:hu15-001-escala-criterios-rubrica
--comment: HU-15 Criterio 1. Cada criterio de la rúbrica define la escala con la que se califica. Se limita al
--comment: rango 0-10 que ya usa el registro de evaluaciones (HU-17). Los criterios existentes quedan con escala 0-10.
ALTER TABLE "criterios_rubrica" ADD COLUMN IF NOT EXISTS "escala_minima" NUMERIC(5,2) NOT NULL DEFAULT 0;
ALTER TABLE "criterios_rubrica" ADD COLUMN IF NOT EXISTS "escala_maxima" NUMERIC(5,2) NOT NULL DEFAULT 10;
ALTER TABLE "criterios_rubrica" ADD CONSTRAINT "criterios_rubrica_escala_check"
    CHECK ("escala_minima" >= 0 AND "escala_maxima" <= 10 AND "escala_maxima" > "escala_minima");
--rollback ALTER TABLE "criterios_rubrica" DROP CONSTRAINT IF EXISTS "criterios_rubrica_escala_check";
--rollback ALTER TABLE "criterios_rubrica" DROP COLUMN IF EXISTS "escala_maxima";
--rollback ALTER TABLE "criterios_rubrica" DROP COLUMN IF EXISTS "escala_minima";

--changeset SIGEA:hu15-002-una-rubrica-activa-por-convocatoria
--comment: HU-15. Solo una rúbrica activa por convocatoria (la que usa HU-17 para calificar). Si por datos cargados a
--comment: mano hubiera varias activas, se deja activa la más reciente antes de crear el índice.
UPDATE "rubricas_evaluacion" r SET "activo" = false
WHERE r."activo" = true
  AND EXISTS (SELECT 1 FROM "rubricas_evaluacion" r2
              WHERE r2."convocatoria_id" = r."convocatoria_id" AND r2."activo" = true AND r2."rubrica_id" > r."rubrica_id");
CREATE UNIQUE INDEX IF NOT EXISTS "uq_rubricas_evaluacion_activa_convocatoria"
    ON "rubricas_evaluacion" ("convocatoria_id") WHERE "activo" = true;
--rollback DROP INDEX IF EXISTS "uq_rubricas_evaluacion_activa_convocatoria";

--changeset SIGEA:hu15-003-indice-evaluaciones-criterio
--comment: HU-15 Criterio 4. Se cuentan las evaluaciones registradas con los criterios de una rúbrica para advertir
--comment: el impacto de modificarla.
CREATE INDEX IF NOT EXISTS "idx_evaluaciones_criterio" ON "evaluaciones" ("criterio_id");
--rollback DROP INDEX IF EXISTS "idx_evaluaciones_criterio";

--changeset SIGEA:hu15-004-permiso-configurar-evaluacion
--comment: Permiso para configurar la rúbrica y el comité evaluador de una convocatoria. Se ajusta por rol desde HU-02.
INSERT INTO "permisos" ("codigo", "modulo", "descripcion") VALUES
  ('EVALUACION_CONFIGURAR', 'EVALUACION', 'Configurar la rúbrica de evaluación y el comité evaluador de una convocatoria')
ON CONFLICT ("codigo") DO NOTHING;
--rollback DELETE FROM "permisos" WHERE "codigo" = 'EVALUACION_CONFIGURAR';

--changeset SIGEA:hu15-005-asignar-permiso-configurar-evaluacion
--comment: Asignación inicial: quien gestiona convocatorias (administrador/organizador) configura su evaluación.
INSERT INTO "roles_permisos" ("rol_id", "permiso_id")
SELECT DISTINCT rp."rol_id", p."permiso_id"
FROM "roles_permisos" rp
JOIN "permisos" origen ON origen."permiso_id" = rp."permiso_id" AND origen."codigo" = 'CONVOCATORIAS_GESTIONAR'
JOIN "permisos" p ON p."codigo" = 'EVALUACION_CONFIGURAR'
ON CONFLICT DO NOTHING;
--rollback DELETE FROM "roles_permisos" WHERE "permiso_id" IN (SELECT "permiso_id" FROM "permisos" WHERE "codigo" = 'EVALUACION_CONFIGURAR');
