--liquibase formatted sql

--changeset DF:hu-salas-reutilizables-001
--comment: Permite que las salas se creen desvinculadas de eventos y se vinculen a múltiples eventos (reutilizables)
ALTER TABLE "salas" ALTER COLUMN "evento_id" DROP NOT NULL;

CREATE TABLE IF NOT EXISTS "evento_salas" (
    "evento_id" INT NOT NULL,
    "sala_id"   INT NOT NULL,
    "creado_en" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "evento_salas_pkey" PRIMARY KEY ("evento_id", "sala_id"),
    CONSTRAINT "evento_salas_evento_fk" FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE,
    CONSTRAINT "evento_salas_sala_fk"   FOREIGN KEY ("sala_id") REFERENCES "salas" ("sala_id") ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS "idx_evento_salas_sala_id" ON "evento_salas" ("sala_id");

-- Migrar salas existentes que tenían evento_id a la tabla de unión evento_salas
INSERT INTO "evento_salas" ("evento_id", "sala_id")
SELECT "evento_id", "sala_id" FROM "salas"
WHERE "evento_id" IS NOT NULL
ON CONFLICT DO NOTHING;

CREATE UNIQUE INDEX IF NOT EXISTS "salas_nombre_global_idx"
    ON "salas" (LOWER("nombre"))
    WHERE "evento_id" IS NULL;

--rollback DROP INDEX IF EXISTS "salas_nombre_global_idx";
--rollback DROP TABLE IF EXISTS "evento_salas";
--rollback ALTER TABLE "salas" ALTER COLUMN "evento_id" SET NOT NULL;
