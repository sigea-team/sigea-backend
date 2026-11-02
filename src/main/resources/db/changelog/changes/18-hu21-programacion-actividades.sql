--liquibase formatted sql

-- =====================================================================
-- HU-21 - Programar actividades controlando conflictos y actividades simultáneas
-- =====================================================================

--changeset SIGEA:hu21-001-sala-permite-simultaneidad
--comment: HU-21 Criterio 3. Indica si una sala puede alojar varias actividades a la misma hora cuando todas están
--comment: marcadas como simultáneas (p. ej. una sala virtual con varias salas de reunión). Por defecto no lo admite.
ALTER TABLE "salas" ADD COLUMN IF NOT EXISTS "permite_simultaneidad" BOOLEAN NOT NULL DEFAULT false;
--rollback ALTER TABLE "salas" DROP COLUMN IF EXISTS "permite_simultaneidad";

--changeset SIGEA:hu21-002-indices-conflictos-actividades
--comment: HU-21 Criterios 2 y 5. Índices para buscar rápido los cruces por sala y por ponente en una fecha.
CREATE INDEX IF NOT EXISTS "idx_actividades_sala_fecha" ON "actividades" ("sala_id", "fecha");
CREATE INDEX IF NOT EXISTS "idx_actividades_ponente_fecha" ON "actividades" ("ponente_id", "fecha");
--rollback DROP INDEX IF EXISTS "idx_actividades_ponente_fecha";
--rollback DROP INDEX IF EXISTS "idx_actividades_sala_fecha";
