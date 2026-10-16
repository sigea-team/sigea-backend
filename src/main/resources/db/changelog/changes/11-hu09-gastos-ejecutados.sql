--liquibase formatted sql

-- =====================================================================
-- HU-09 (RF11 + RF12) - Registrar gastos ejecutados y calcular la ejecución presupuestal
-- =====================================================================

--changeset SIGEA:hu09-001-indices-gastos-ejecutados
--comment: HU-09. El acumulado ejecutado se calcula con SUM(valor) por rubro y por evento; PostgreSQL no indexa
--comment: automáticamente las llaves foráneas.
CREATE INDEX IF NOT EXISTS "idx_gastos_ejecutados_rubro" ON "gastos_ejecutados" ("rubro_id", "fecha_gasto" DESC);
CREATE INDEX IF NOT EXISTS "idx_gastos_ejecutados_registrado_por" ON "gastos_ejecutados" ("registrado_por");
--rollback DROP INDEX IF EXISTS "idx_gastos_ejecutados_registrado_por";
--rollback DROP INDEX IF EXISTS "idx_gastos_ejecutados_rubro";
