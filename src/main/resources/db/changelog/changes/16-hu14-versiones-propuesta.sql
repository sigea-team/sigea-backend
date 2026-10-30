--liquibase formatted sql

-- =====================================================================
-- HU-14 (RF19, RF20, RF21) - Postular propuesta con documentos y versionamiento
-- =====================================================================

--changeset SIGEA:hu14-001-versiones-propuesta-trazabilidad
--comment: HU-14 Criterios 3 y 4. Cada versión guarda quién hizo el cambio (creado_por) y una copia de los
--comment: datos de la propuesta en ese momento (título, resumen, palabras clave, línea temática), para que las
--comment: versiones anteriores sigan siendo evidencia aunque la propuesta se modifique después.
--comment: creado_por admite NULL para no romper filas que existan antes de este changeset.
ALTER TABLE "versiones_propuesta" ADD COLUMN IF NOT EXISTS "creado_por" BIGINT;
ALTER TABLE "versiones_propuesta" ADD COLUMN IF NOT EXISTS "titulo" VARCHAR(200);
ALTER TABLE "versiones_propuesta" ADD COLUMN IF NOT EXISTS "resumen" TEXT;
ALTER TABLE "versiones_propuesta" ADD COLUMN IF NOT EXISTS "palabras_clave" VARCHAR(255);
ALTER TABLE "versiones_propuesta" ADD COLUMN IF NOT EXISTS "linea_tematica_id" INT;
ALTER TABLE "versiones_propuesta" DROP CONSTRAINT IF EXISTS "fk_versiones_propuesta_creado_por";
ALTER TABLE "versiones_propuesta" ADD CONSTRAINT "fk_versiones_propuesta_creado_por"
    FOREIGN KEY ("creado_por") REFERENCES "usuarios" ("usuario_id");
ALTER TABLE "versiones_propuesta" DROP CONSTRAINT IF EXISTS "fk_versiones_propuesta_linea_tematica";
ALTER TABLE "versiones_propuesta" ADD CONSTRAINT "fk_versiones_propuesta_linea_tematica"
    FOREIGN KEY ("linea_tematica_id") REFERENCES "lineas_tematicas" ("linea_tematica_id");
CREATE INDEX IF NOT EXISTS "idx_versiones_propuesta_propuesta" ON "versiones_propuesta" ("propuesta_id", "numero_version");
--rollback DROP INDEX IF EXISTS "idx_versiones_propuesta_propuesta";
--rollback ALTER TABLE "versiones_propuesta" DROP CONSTRAINT IF EXISTS "fk_versiones_propuesta_linea_tematica";
--rollback ALTER TABLE "versiones_propuesta" DROP CONSTRAINT IF EXISTS "fk_versiones_propuesta_creado_por";
--rollback ALTER TABLE "versiones_propuesta" DROP COLUMN IF EXISTS "linea_tematica_id";
--rollback ALTER TABLE "versiones_propuesta" DROP COLUMN IF EXISTS "palabras_clave";
--rollback ALTER TABLE "versiones_propuesta" DROP COLUMN IF EXISTS "resumen";
--rollback ALTER TABLE "versiones_propuesta" DROP COLUMN IF EXISTS "titulo";
--rollback ALTER TABLE "versiones_propuesta" DROP COLUMN IF EXISTS "creado_por";

--changeset SIGEA:hu14-002-version-definitiva-y-autorizacion
--comment: HU-14 Criterio 5. es_definitiva marca la versión aprobada como definitiva (la marca la pone RF29 / CU-16).
--comment: Solo puede haber una versión definitiva por propuesta (índice único parcial).
--comment: propuestas.edicion_autorizada es la autorización explícita, de un solo uso, que el administrador
--comment: concede para modificar una propuesta bloqueada; se consume al crear la siguiente versión.
ALTER TABLE "versiones_propuesta" ADD COLUMN IF NOT EXISTS "es_definitiva" BOOLEAN NOT NULL DEFAULT false;
CREATE UNIQUE INDEX IF NOT EXISTS "uq_versiones_propuesta_una_definitiva"
    ON "versiones_propuesta" ("propuesta_id") WHERE "es_definitiva" = true;
ALTER TABLE "propuestas" ADD COLUMN IF NOT EXISTS "edicion_autorizada" BOOLEAN NOT NULL DEFAULT false;
--rollback ALTER TABLE "propuestas" DROP COLUMN IF EXISTS "edicion_autorizada";
--rollback DROP INDEX IF EXISTS "uq_versiones_propuesta_una_definitiva";
--rollback ALTER TABLE "versiones_propuesta" DROP COLUMN IF EXISTS "es_definitiva";

--changeset SIGEA:hu14-003-tabla-documentos-propuesta
--comment: HU-14 Criterio 1 y RF20. Una versión puede tener uno o varios documentos adjuntos.
--comment: versiones_propuesta.documento_url conserva la ruta del documento principal (columna NOT NULL existente);
--comment: esta tabla guarda todos los documentos de la versión con su nombre original, tipo y tamaño.
--comment: Cuando una versión nueva conserva un documento de la anterior, se crea otra fila que apunta al mismo
--comment: archivo almacenado (ruta_almacenamiento), así cada versión queda completa por sí sola.
CREATE TABLE IF NOT EXISTS "documentos_propuesta" (
  "documento_id" SERIAL PRIMARY KEY,
  "version_id" INT NOT NULL,
  "nombre_original" VARCHAR(255) NOT NULL,
  "ruta_almacenamiento" VARCHAR(255) NOT NULL,
  "tipo_contenido" VARCHAR(100),
  "tamano_bytes" BIGINT NOT NULL,
  "principal" BOOLEAN NOT NULL DEFAULT false,
  "fecha_carga" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT "ck_documentos_propuesta_tamano" CHECK ("tamano_bytes" > 0),
  CONSTRAINT "fk_documentos_propuesta_version" FOREIGN KEY ("version_id")
      REFERENCES "versiones_propuesta" ("version_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE
);
CREATE INDEX IF NOT EXISTS "idx_documentos_propuesta_version" ON "documentos_propuesta" ("version_id");
--rollback DROP TABLE IF EXISTS "documentos_propuesta";
