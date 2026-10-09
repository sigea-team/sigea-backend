--liquibase formatted sql

-- =====================================================================
-- HU-07 - Elaborar y administrar el presupuesto preliminar del evento
-- =====================================================================

--changeset SIGEA:hu07-001-rubros-nombre-unico-activo
--comment: HU-07 Criterio 2. "Eliminar" un rubro es un borrado lógico (activo = false): el borrado físico
--comment: arrastraría sus gastos_ejecutados (ON DELETE CASCADE) y rompería el historial.
--comment: El índice original (evento_id, nombre) incluía los rubros eliminados e impedía volver a crear uno
--comment: con el mismo nombre. El índice parcial solo aplica a los rubros vigentes y no distingue mayúsculas.
DROP INDEX IF EXISTS "rubros_presupuestales_evento_id_nombre_idx";
CREATE UNIQUE INDEX IF NOT EXISTS "ux_rubros_presupuestales_nombre_activo"
    ON "rubros_presupuestales" ("evento_id", lower("nombre"))
    WHERE "activo" = true;
CREATE INDEX IF NOT EXISTS "idx_rubros_presupuestales_evento" ON "rubros_presupuestales" ("evento_id");
--rollback DROP INDEX IF EXISTS "idx_rubros_presupuestales_evento";
--rollback DROP INDEX IF EXISTS "ux_rubros_presupuestales_nombre_activo";
--rollback CREATE UNIQUE INDEX IF NOT EXISTS "rubros_presupuestales_evento_id_nombre_idx" ON "rubros_presupuestales" ("evento_id", "nombre");

--changeset SIGEA:hu07-002-tabla-historial-rubros
--comment: HU-07 Criterio 2. Historial de cada creación, edición y eliminación de un rubro del presupuesto
--comment: preliminar: valores anteriores y nuevos, total del presupuesto antes y después, usuario y fecha.
CREATE TABLE IF NOT EXISTS "historial_rubros" (
  "historial_id" SERIAL PRIMARY KEY,
  "rubro_id" INT NOT NULL,
  "evento_id" INT NOT NULL,
  "tipo_operacion" VARCHAR(20) NOT NULL CHECK (tipo_operacion IN ('creacion','edicion','eliminacion')),
  "nombre_anterior" VARCHAR(100),
  "cantidad_anterior" NUMERIC(10,2),
  "valor_unitario_anterior" NUMERIC(14,2),
  "nombre_nuevo" VARCHAR(100),
  "cantidad_nueva" NUMERIC(10,2),
  "valor_unitario_nuevo" NUMERIC(14,2),
  "total_presupuesto_anterior" NUMERIC(18,2) NOT NULL CHECK (total_presupuesto_anterior >= 0),
  "total_presupuesto_nuevo" NUMERIC(18,2) NOT NULL CHECK (total_presupuesto_nuevo >= 0),
  "motivo" VARCHAR(255),
  "usuario_id" INT,
  "fecha_hora" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT "ck_historial_rubros_valores_por_operacion" CHECK (
       (tipo_operacion = 'creacion'    AND nombre_anterior IS NULL     AND nombre_nuevo IS NOT NULL)
    OR (tipo_operacion = 'edicion'     AND nombre_anterior IS NOT NULL AND nombre_nuevo IS NOT NULL)
    OR (tipo_operacion = 'eliminacion' AND nombre_anterior IS NOT NULL AND nombre_nuevo IS NULL)
  ),
  CONSTRAINT "fk_historial_rubros_rubro" FOREIGN KEY ("rubro_id")
      REFERENCES "rubros_presupuestales" ("rubro_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE,
  CONSTRAINT "fk_historial_rubros_evento" FOREIGN KEY ("evento_id")
      REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE,
  CONSTRAINT "fk_historial_rubros_usuario" FOREIGN KEY ("usuario_id")
      REFERENCES "usuarios" ("usuario_id") ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE
);
CREATE INDEX IF NOT EXISTS "idx_historial_rubros_evento" ON "historial_rubros" ("evento_id", "fecha_hora" DESC);
CREATE INDEX IF NOT EXISTS "idx_historial_rubros_rubro" ON "historial_rubros" ("rubro_id", "fecha_hora" DESC);
--rollback DROP TABLE IF EXISTS "historial_rubros";

--changeset SIGEA:hu07-003-historial-rubros-inmutable splitStatements:false
--comment: Un registro del historial no se puede modificar. Solo se borra junto con su evento o rubro (cascada).
CREATE OR REPLACE FUNCTION "fn_historial_rubros_inmutable"() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'El historial de rubros presupuestales es inmutable: no se permite UPDATE sobre historial_rubros'
        USING ERRCODE = 'insufficient_privilege';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS "trg_historial_rubros_inmutable" ON "historial_rubros";
CREATE TRIGGER "trg_historial_rubros_inmutable"
    BEFORE UPDATE ON "historial_rubros"
    FOR EACH ROW EXECUTE FUNCTION "fn_historial_rubros_inmutable"();
--rollback DROP TRIGGER IF EXISTS "trg_historial_rubros_inmutable" ON "historial_rubros";
--rollback DROP FUNCTION IF EXISTS "fn_historial_rubros_inmutable"();

--changeset SIGEA:hu07-004-permisos-presupuesto
--comment: Permisos del módulo PRESUPUESTO. Se asignan a los roles desde la administración de roles (HU-02).
INSERT INTO "permisos" ("codigo", "modulo", "descripcion") VALUES
  ('PRESUPUESTO_VER', 'PRESUPUESTO', 'Consultar el presupuesto preliminar del evento y el historial de sus rubros'),
  ('PRESUPUESTO_GESTIONAR', 'PRESUPUESTO', 'Registrar, editar y eliminar rubros del presupuesto preliminar del evento')
ON CONFLICT ("codigo") DO NOTHING;
--rollback DELETE FROM "permisos" WHERE "codigo" IN ('PRESUPUESTO_VER', 'PRESUPUESTO_GESTIONAR');

--changeset SIGEA:hu07-005-asignar-permisos-presupuesto
--comment: Asignación inicial: quien ya edita eventos (organizador/administrador) gestiona su presupuesto, y quien
--comment: consulta eventos puede ver el presupuesto. Después se ajusta desde HU-02 sin tocar la base de datos.
INSERT INTO "roles_permisos" ("rol_id", "permiso_id")
SELECT DISTINCT rp."rol_id", p."permiso_id"
FROM "roles_permisos" rp
JOIN "permisos" origen ON origen."permiso_id" = rp."permiso_id"
JOIN "permisos" p ON (origen."codigo" = 'EVENTOS_EDITAR' AND p."codigo" IN ('PRESUPUESTO_GESTIONAR', 'PRESUPUESTO_VER'))
                  OR (origen."codigo" = 'EVENTOS_VER'    AND p."codigo" = 'PRESUPUESTO_VER')
ON CONFLICT DO NOTHING;
--rollback DELETE FROM "roles_permisos" WHERE "permiso_id" IN (SELECT "permiso_id" FROM "permisos" WHERE "codigo" IN ('PRESUPUESTO_VER', 'PRESUPUESTO_GESTIONAR'));
