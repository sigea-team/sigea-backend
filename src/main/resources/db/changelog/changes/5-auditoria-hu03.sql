--liquibase formatted sql

-- =====================================================================
-- HU-03 (RF56) - Registrar auditoría sobre operaciones críticas
-- =====================================================================

--changeset HU03:001-indices-auditoria
-- Índices para los filtros del Criterio 2 (usuario, fecha y tipo de operación).
CREATE INDEX IF NOT EXISTS "idx_auditoria_fecha_hora" ON "auditoria" ("fecha_hora" DESC);
CREATE INDEX IF NOT EXISTS "idx_auditoria_usuario" ON "auditoria" ("usuario_id");
CREATE INDEX IF NOT EXISTS "idx_auditoria_accion" ON "auditoria" ("accion");

--changeset HU03:002-auditoria-inmutable splitStatements:false
-- Criterio 3: ningún registro de auditoría puede modificarse ni eliminarse,
-- ni siquiera por fuera de la API (consola SQL, otro servicio, bug en el código).
CREATE OR REPLACE FUNCTION "fn_auditoria_inmutable"() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de auditoría son inmutables: operación % no permitida sobre la tabla auditoria', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS "trg_auditoria_inmutable" ON "auditoria";
CREATE TRIGGER "trg_auditoria_inmutable"
    BEFORE UPDATE OR DELETE ON "auditoria"
    FOR EACH ROW EXECUTE FUNCTION "fn_auditoria_inmutable"();

DROP TRIGGER IF EXISTS "trg_auditoria_no_truncate" ON "auditoria";
CREATE TRIGGER "trg_auditoria_no_truncate"
    BEFORE TRUNCATE ON "auditoria"
    FOR EACH STATEMENT EXECUTE FUNCTION "fn_auditoria_inmutable"();
--rollback DROP TRIGGER IF EXISTS "trg_auditoria_no_truncate" ON "auditoria";
--rollback DROP TRIGGER IF EXISTS "trg_auditoria_inmutable" ON "auditoria";
--rollback DROP FUNCTION IF EXISTS "fn_auditoria_inmutable"();

--changeset HU03:003-permiso-auditoria-ver
-- Permiso para consultar el log (Criterio 2). Se asigna a los roles desde HU-02.
INSERT INTO "permisos" ("codigo", "modulo", "descripcion") VALUES
  ('AUDITORIA_VER', 'AUDITORIA', 'Consultar y filtrar el log de auditoría de operaciones críticas')
ON CONFLICT ("codigo") DO NOTHING;
