--liquibase formatted sql

-- =====================================================================
-- HU-18 (RF27) - Clasificar propuestas según su resultado
-- =====================================================================

--changeset SIGEA:hu18-001-umbrales-clasificacion-convocatoria
--comment: HU-18 Criterio 1. Umbrales con los que se clasifica la puntuación ponderada (escala 0-10) de las propuestas
--comment: de cada convocatoria: >= umbral_aceptacion -> aprobada; >= umbral_ajustes -> aprobada_con_ajustes; si no,
--comment: rechazada. Los valores por defecto (7 y 5) son los que usaba HU-17, así las convocatorias existentes no cambian.
ALTER TABLE "convocatorias" ADD COLUMN IF NOT EXISTS "umbral_aceptacion" NUMERIC(4,2) NOT NULL DEFAULT 7;
ALTER TABLE "convocatorias" ADD COLUMN IF NOT EXISTS "umbral_ajustes" NUMERIC(4,2) NOT NULL DEFAULT 5;
ALTER TABLE "convocatorias" ADD CONSTRAINT "convocatorias_umbrales_clasificacion_check"
    CHECK ("umbral_ajustes" >= 0 AND "umbral_ajustes" <= "umbral_aceptacion" AND "umbral_aceptacion" <= 10);
--rollback ALTER TABLE "convocatorias" DROP CONSTRAINT IF EXISTS "convocatorias_umbrales_clasificacion_check";
--rollback ALTER TABLE "convocatorias" DROP COLUMN IF EXISTS "umbral_ajustes";
--rollback ALTER TABLE "convocatorias" DROP COLUMN IF EXISTS "umbral_aceptacion";

--changeset SIGEA:hu18-002-estado-clasificacion-resultado
--comment: HU-18. ajuste_manual: la clasificación la fijó el administrador con un motivo (Criterio 2) y el recálculo
--comment: automático de HU-17 no la reemplaza. fecha_clasificacion: cuándo se aplicó la clasificación a la propuesta
--comment: (NULL = calculada pero aún no aplicada).
ALTER TABLE "resultados_evaluacion" ADD COLUMN IF NOT EXISTS "ajuste_manual" BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE "resultados_evaluacion" ADD COLUMN IF NOT EXISTS "fecha_clasificacion" TIMESTAMP;
--rollback ALTER TABLE "resultados_evaluacion" DROP COLUMN IF EXISTS "fecha_clasificacion";
--rollback ALTER TABLE "resultados_evaluacion" DROP COLUMN IF EXISTS "ajuste_manual";

--changeset SIGEA:hu18-003-tabla-historial-clasificaciones
--comment: HU-18 Criterio 2. Historial de cada clasificación aplicada a una propuesta: automática (por umbrales) o
--comment: manual (con motivo obligatorio). Se enlaza a la propuesta y no al resultado, porque HU-15 borra el resultado
--comment: al reemplazar una rúbrica en uso y el historial debe conservarse.
CREATE TABLE IF NOT EXISTS "historial_clasificaciones" (
  "historial_clasificacion_id" SERIAL PRIMARY KEY,
  "propuesta_id" INT NOT NULL,
  "tipo" VARCHAR(20) NOT NULL CHECK (tipo IN ('automatica','manual')),
  "clasificacion_anterior" VARCHAR(30) CHECK (clasificacion_anterior IN ('aprobada','aprobada_con_ajustes','rechazada')),
  "clasificacion_nueva" VARCHAR(30) NOT NULL CHECK (clasificacion_nueva IN ('aprobada','aprobada_con_ajustes','rechazada')),
  "puntuacion_ponderada" NUMERIC(5,2) NOT NULL,
  "umbral_aceptacion" NUMERIC(4,2) NOT NULL,
  "umbral_ajustes" NUMERIC(4,2) NOT NULL,
  "motivo" TEXT,
  "usuario_id" INT,
  "fecha_hora" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT "historial_clasificaciones_motivo_manual_check"
      CHECK ("tipo" <> 'manual' OR ("motivo" IS NOT NULL AND length(trim("motivo")) >= 10)),
  CONSTRAINT "fk_historial_clasificaciones_propuesta" FOREIGN KEY ("propuesta_id")
      REFERENCES "propuestas" ("propuesta_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE,
  CONSTRAINT "fk_historial_clasificaciones_usuario" FOREIGN KEY ("usuario_id")
      REFERENCES "usuarios" ("usuario_id") ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE
);
CREATE INDEX IF NOT EXISTS "idx_historial_clasificaciones_propuesta" ON "historial_clasificaciones" ("propuesta_id", "fecha_hora" DESC);
--rollback DROP TABLE IF EXISTS "historial_clasificaciones";

--changeset SIGEA:hu18-004-historial-clasificaciones-inmutable splitStatements:false
--comment: Un registro del historial de clasificaciones no se puede modificar. Solo se borra en cascada con la propuesta.
CREATE OR REPLACE FUNCTION "fn_historial_clasificaciones_inmutable"() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'El historial de clasificaciones es inmutable: no se permite UPDATE sobre historial_clasificaciones'
        USING ERRCODE = 'insufficient_privilege';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS "trg_historial_clasificaciones_inmutable" ON "historial_clasificaciones";
CREATE TRIGGER "trg_historial_clasificaciones_inmutable"
    BEFORE UPDATE ON "historial_clasificaciones"
    FOR EACH ROW EXECUTE FUNCTION "fn_historial_clasificaciones_inmutable"();
--rollback DROP TRIGGER IF EXISTS "trg_historial_clasificaciones_inmutable" ON "historial_clasificaciones";
--rollback DROP FUNCTION IF EXISTS "fn_historial_clasificaciones_inmutable"();

--changeset SIGEA:hu18-005-permiso-clasificar-propuestas
--comment: Permiso para configurar umbrales, aplicar y ajustar la clasificación de propuestas. Se ajusta por rol en HU-02.
INSERT INTO "permisos" ("codigo", "modulo", "descripcion") VALUES
  ('PROPUESTAS_CLASIFICAR', 'EVALUACION', 'Clasificar las propuestas evaluadas y ajustar manualmente su clasificación')
ON CONFLICT ("codigo") DO NOTHING;
--rollback DELETE FROM "permisos" WHERE "codigo" = 'PROPUESTAS_CLASIFICAR';

--changeset SIGEA:hu18-006-asignar-permiso-clasificar-propuestas
--comment: Asignación inicial: quien gestiona convocatorias (administrador/organizador) clasifica sus propuestas.
INSERT INTO "roles_permisos" ("rol_id", "permiso_id")
SELECT DISTINCT rp."rol_id", p."permiso_id"
FROM "roles_permisos" rp
JOIN "permisos" origen ON origen."permiso_id" = rp."permiso_id" AND origen."codigo" = 'CONVOCATORIAS_GESTIONAR'
JOIN "permisos" p ON p."codigo" = 'PROPUESTAS_CLASIFICAR'
ON CONFLICT DO NOTHING;
--rollback DELETE FROM "roles_permisos" WHERE "permiso_id" IN (SELECT "permiso_id" FROM "permisos" WHERE "codigo" = 'PROPUESTAS_CLASIFICAR');
