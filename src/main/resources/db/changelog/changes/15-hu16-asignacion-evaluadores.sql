--liquibase formatted sql

-- =====================================================================
-- HU-16 (RF24) - Asignar propuestas a evaluadores
-- =====================================================================

--changeset SIGEA:hu16-001-tabla-historial-asignaciones
--comment: HU-16 Criterio 3. Historial de cada asignación y reasignación de una propuesta a un evaluador del
--comment: comité: evaluador anterior y nuevo, si se confirmó un conflicto de interés, motivo, usuario y fecha.
--comment: Al reasignar, la fila de asignaciones_evaluacion se actualiza (cambia el evaluador) y aquí queda la traza.
CREATE TABLE IF NOT EXISTS "historial_asignaciones" (
  "historial_id" SERIAL PRIMARY KEY,
  "asignacion_id" INT NOT NULL,
  "propuesta_id" INT NOT NULL,
  "tipo_operacion" VARCHAR(20) NOT NULL CHECK (tipo_operacion IN ('asignacion','reasignacion')),
  "comite_evaluador_anterior_id" INT,
  "comite_evaluador_nuevo_id" INT NOT NULL,
  "conflicto_advertido" BOOLEAN NOT NULL DEFAULT false,
  "motivo" VARCHAR(255),
  "usuario_id" INT,
  "fecha_hora" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT "fk_historial_asignaciones_asignacion" FOREIGN KEY ("asignacion_id")
      REFERENCES "asignaciones_evaluacion" ("asignacion_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE,
  CONSTRAINT "fk_historial_asignaciones_propuesta" FOREIGN KEY ("propuesta_id")
      REFERENCES "propuestas" ("propuesta_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE,
  CONSTRAINT "fk_historial_asignaciones_evaluador_anterior" FOREIGN KEY ("comite_evaluador_anterior_id")
      REFERENCES "comites_evaluadores" ("comite_evaluador_id") ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE,
  CONSTRAINT "fk_historial_asignaciones_evaluador_nuevo" FOREIGN KEY ("comite_evaluador_nuevo_id")
      REFERENCES "comites_evaluadores" ("comite_evaluador_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE,
  CONSTRAINT "fk_historial_asignaciones_usuario" FOREIGN KEY ("usuario_id")
      REFERENCES "usuarios" ("usuario_id") ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE
);
CREATE INDEX IF NOT EXISTS "idx_historial_asignaciones_propuesta" ON "historial_asignaciones" ("propuesta_id", "fecha_hora" DESC);
CREATE INDEX IF NOT EXISTS "idx_historial_asignaciones_asignacion" ON "historial_asignaciones" ("asignacion_id", "fecha_hora" DESC);
--rollback DROP TABLE IF EXISTS "historial_asignaciones";

--changeset SIGEA:hu16-002-historial-asignaciones-inmutable splitStatements:false
--comment: Un registro del historial de asignaciones no se puede modificar. Solo se borra en cascada.
CREATE OR REPLACE FUNCTION "fn_historial_asignaciones_inmutable"() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'El historial de asignaciones es inmutable: no se permite UPDATE sobre historial_asignaciones'
        USING ERRCODE = 'insufficient_privilege';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS "trg_historial_asignaciones_inmutable" ON "historial_asignaciones";
CREATE TRIGGER "trg_historial_asignaciones_inmutable"
    BEFORE UPDATE ON "historial_asignaciones"
    FOR EACH ROW EXECUTE FUNCTION "fn_historial_asignaciones_inmutable"();
--rollback DROP TRIGGER IF EXISTS "trg_historial_asignaciones_inmutable" ON "historial_asignaciones";
--rollback DROP FUNCTION IF EXISTS "fn_historial_asignaciones_inmutable"();

--changeset SIGEA:hu16-003-indice-bandeja-evaluador
--comment: HU-16 Criterio 1. La bandeja del evaluador consulta sus asignaciones por comite_evaluador_id.
CREATE INDEX IF NOT EXISTS "idx_asignaciones_evaluacion_evaluador" ON "asignaciones_evaluacion" ("comite_evaluador_id", "fecha_asignacion" DESC);
--rollback DROP INDEX IF EXISTS "idx_asignaciones_evaluacion_evaluador";

--changeset SIGEA:hu16-004-permiso-asignar-propuestas
--comment: Permiso para asignar y reasignar propuestas a evaluadores. Se ajusta por rol desde HU-02.
INSERT INTO "permisos" ("codigo", "modulo", "descripcion") VALUES
  ('PROPUESTAS_ASIGNAR', 'EVALUACION', 'Asignar y reasignar propuestas a los evaluadores del comité')
ON CONFLICT ("codigo") DO NOTHING;
--rollback DELETE FROM "permisos" WHERE "codigo" = 'PROPUESTAS_ASIGNAR';

--changeset SIGEA:hu16-005-asignar-permiso-asignar-propuestas
--comment: Asignación inicial: quien gestiona convocatorias (organizador/administrador) asigna sus propuestas.
INSERT INTO "roles_permisos" ("rol_id", "permiso_id")
SELECT DISTINCT rp."rol_id", p."permiso_id"
FROM "roles_permisos" rp
JOIN "permisos" origen ON origen."permiso_id" = rp."permiso_id" AND origen."codigo" = 'CONVOCATORIAS_GESTIONAR'
JOIN "permisos" p ON p."codigo" = 'PROPUESTAS_ASIGNAR'
ON CONFLICT DO NOTHING;
--rollback DELETE FROM "roles_permisos" WHERE "permiso_id" IN (SELECT "permiso_id" FROM "permisos" WHERE "codigo" = 'PROPUESTAS_ASIGNAR');
