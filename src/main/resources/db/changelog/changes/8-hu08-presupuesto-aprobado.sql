--liquibase formatted sql

--changeset DF:hu08-001-presupuesto-aprobado-historial
--comment: HU-08 Criterio 2. Permitir conservar versiones anteriores del presupuesto aprobado para el historial de cambios.
--comment: Se reemplaza la restricción UNIQUE estricta sobre evento_id por un índice único parcial donde estado = 'aprobado'.
--comment: De esta manera, solo puede existir una versión oficial vigente por evento, pero se conservan todas las versiones anteriores con estado = 'historico'.
ALTER TABLE "presupuestos_aprobados" DROP CONSTRAINT IF EXISTS "presupuestos_aprobados_evento_id_key";
DROP INDEX IF EXISTS "presupuestos_aprobados_evento_id_key";
CREATE UNIQUE INDEX IF NOT EXISTS "ux_presupuestos_aprobados_oficial"
    ON "presupuestos_aprobados" ("evento_id")
    WHERE "estado" = 'aprobado';
CREATE INDEX IF NOT EXISTS "idx_presupuestos_aprobados_evento" ON "presupuestos_aprobados" ("evento_id");
--rollback DROP INDEX IF EXISTS "idx_presupuestos_aprobados_evento";
--rollback DROP INDEX IF EXISTS "ux_presupuestos_aprobados_oficial";
--rollback ALTER TABLE "presupuestos_aprobados" ADD CONSTRAINT "presupuestos_aprobados_evento_id_key" UNIQUE ("evento_id");
