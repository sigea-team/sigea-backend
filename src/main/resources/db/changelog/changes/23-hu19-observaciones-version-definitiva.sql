--liquibase formatted sql

-- =====================================================================
-- HU-19 (RF28 + RF29) - Registrar observaciones/ajustes y aprobar la versión definitiva
-- =====================================================================

--changeset SIGEA:hu19-001-observaciones-propuesta-ajustes
--comment: HU-19 Criterios 1, 2 y 4. La tabla observaciones_propuesta ya existía (sin uso). Se completa así:
--comment: - comite_evaluador_id pasa a opcional: el administrador también registra observaciones sin ser evaluador.
--comment: - usuario_id: quién registró la observación (evaluador o administrador).
--comment: - requiere_ajuste: true = ajuste requerido (bloquea la aprobación hasta resolverse); false = comentario.
--comment: - version_atencion_id: versión que subió el autor para atender la observación (Criterio 2).
--comment: - fecha_resolucion / resuelto_por: cuándo y quién la dio por resuelta.
ALTER TABLE "observaciones_propuesta" ALTER COLUMN "comite_evaluador_id" DROP NOT NULL;
ALTER TABLE "observaciones_propuesta" ADD COLUMN IF NOT EXISTS "usuario_id" INT;
ALTER TABLE "observaciones_propuesta" ADD COLUMN IF NOT EXISTS "requiere_ajuste" BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE "observaciones_propuesta" ADD COLUMN IF NOT EXISTS "version_atencion_id" INT;
ALTER TABLE "observaciones_propuesta" ADD COLUMN IF NOT EXISTS "fecha_resolucion" TIMESTAMP;
ALTER TABLE "observaciones_propuesta" ADD COLUMN IF NOT EXISTS "resuelto_por" INT;
UPDATE "observaciones_propuesta" SET "fecha_resolucion" = "fecha"
 WHERE "resuelto" = true AND "fecha_resolucion" IS NULL;
UPDATE "observaciones_propuesta" SET "fecha_resolucion" = NULL WHERE "resuelto" = false;
ALTER TABLE "observaciones_propuesta" ADD CONSTRAINT "fk_observaciones_propuesta_usuario"
    FOREIGN KEY ("usuario_id") REFERENCES "usuarios" ("usuario_id") ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE "observaciones_propuesta" ADD CONSTRAINT "fk_observaciones_propuesta_version_atencion"
    FOREIGN KEY ("version_atencion_id") REFERENCES "versiones_propuesta" ("version_id") ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE "observaciones_propuesta" ADD CONSTRAINT "fk_observaciones_propuesta_resuelto_por"
    FOREIGN KEY ("resuelto_por") REFERENCES "usuarios" ("usuario_id") ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE "observaciones_propuesta" ADD CONSTRAINT "observaciones_propuesta_resolucion_check"
    CHECK (("resuelto" = false AND "fecha_resolucion" IS NULL) OR ("resuelto" = true AND "fecha_resolucion" IS NOT NULL));
ALTER TABLE "observaciones_propuesta" ADD CONSTRAINT "observaciones_propuesta_descripcion_check"
    CHECK (length(trim("descripcion")) > 0) NOT VALID;
CREATE INDEX IF NOT EXISTS "idx_observaciones_propuesta_version" ON "observaciones_propuesta" ("version_id");
CREATE INDEX IF NOT EXISTS "idx_observaciones_propuesta_version_atencion" ON "observaciones_propuesta" ("version_atencion_id");
--rollback DROP INDEX IF EXISTS "idx_observaciones_propuesta_version_atencion";
--rollback DROP INDEX IF EXISTS "idx_observaciones_propuesta_version";
--rollback ALTER TABLE "observaciones_propuesta" DROP CONSTRAINT IF EXISTS "observaciones_propuesta_descripcion_check";
--rollback ALTER TABLE "observaciones_propuesta" DROP CONSTRAINT IF EXISTS "observaciones_propuesta_resolucion_check";
--rollback ALTER TABLE "observaciones_propuesta" DROP CONSTRAINT IF EXISTS "fk_observaciones_propuesta_resuelto_por";
--rollback ALTER TABLE "observaciones_propuesta" DROP CONSTRAINT IF EXISTS "fk_observaciones_propuesta_version_atencion";
--rollback ALTER TABLE "observaciones_propuesta" DROP CONSTRAINT IF EXISTS "fk_observaciones_propuesta_usuario";
--rollback ALTER TABLE "observaciones_propuesta" DROP COLUMN IF EXISTS "resuelto_por";
--rollback ALTER TABLE "observaciones_propuesta" DROP COLUMN IF EXISTS "fecha_resolucion";
--rollback ALTER TABLE "observaciones_propuesta" DROP COLUMN IF EXISTS "version_atencion_id";
--rollback ALTER TABLE "observaciones_propuesta" DROP COLUMN IF EXISTS "requiere_ajuste";
--rollback ALTER TABLE "observaciones_propuesta" DROP COLUMN IF EXISTS "usuario_id";
--rollback DELETE FROM "observaciones_propuesta" WHERE "comite_evaluador_id" IS NULL;
--rollback ALTER TABLE "observaciones_propuesta" ALTER COLUMN "comite_evaluador_id" SET NOT NULL;

--changeset SIGEA:hu19-002-aprobacion-version-definitiva
--comment: HU-19 Criterio 3. Fecha y usuario de la aprobación de la versión definitiva (es_definitiva ya existe desde
--comment: HU-14). Una versión definitiva siempre tiene fecha de aprobación.
ALTER TABLE "versiones_propuesta" ADD COLUMN IF NOT EXISTS "fecha_definitiva" TIMESTAMP;
ALTER TABLE "versiones_propuesta" ADD COLUMN IF NOT EXISTS "aprobada_por" INT;
UPDATE "versiones_propuesta" SET "fecha_definitiva" = "fecha_creacion"
 WHERE "es_definitiva" = true AND "fecha_definitiva" IS NULL;
ALTER TABLE "versiones_propuesta" ADD CONSTRAINT "fk_versiones_propuesta_aprobada_por"
    FOREIGN KEY ("aprobada_por") REFERENCES "usuarios" ("usuario_id") ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE "versiones_propuesta" ADD CONSTRAINT "versiones_propuesta_fecha_definitiva_check"
    CHECK ("es_definitiva" = false OR "fecha_definitiva" IS NOT NULL);
--rollback ALTER TABLE "versiones_propuesta" DROP CONSTRAINT IF EXISTS "versiones_propuesta_fecha_definitiva_check";
--rollback ALTER TABLE "versiones_propuesta" DROP CONSTRAINT IF EXISTS "fk_versiones_propuesta_aprobada_por";
--rollback ALTER TABLE "versiones_propuesta" DROP COLUMN IF EXISTS "aprobada_por";
--rollback ALTER TABLE "versiones_propuesta" DROP COLUMN IF EXISTS "fecha_definitiva";

--changeset SIGEA:hu19-003-permiso-aprobar-version-definitiva
--comment: Permiso para dar por resueltas las observaciones y aprobar la versión definitiva. Se ajusta por rol en HU-02.
INSERT INTO "permisos" ("codigo", "modulo", "descripcion") VALUES
  ('PROPUESTAS_APROBAR_DEFINITIVA', 'EVALUACION', 'Aprobar la versión definitiva de las propuestas con los ajustes resueltos')
ON CONFLICT ("codigo") DO NOTHING;
--rollback DELETE FROM "permisos" WHERE "codigo" = 'PROPUESTAS_APROBAR_DEFINITIVA';

--changeset SIGEA:hu19-004-asignar-permiso-aprobar-version-definitiva
--comment: Asignación inicial: quien gestiona convocatorias (administrador/organizador) aprueba la versión definitiva.
INSERT INTO "roles_permisos" ("rol_id", "permiso_id")
SELECT DISTINCT rp."rol_id", p."permiso_id"
FROM "roles_permisos" rp
JOIN "permisos" origen ON origen."permiso_id" = rp."permiso_id" AND origen."codigo" = 'CONVOCATORIAS_GESTIONAR'
JOIN "permisos" p ON p."codigo" = 'PROPUESTAS_APROBAR_DEFINITIVA'
ON CONFLICT DO NOTHING;
--rollback DELETE FROM "roles_permisos" WHERE "permiso_id" IN (SELECT "permiso_id" FROM "permisos" WHERE "codigo" = 'PROPUESTAS_APROBAR_DEFINITIVA');
