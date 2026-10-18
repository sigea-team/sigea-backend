--liquibase formatted sql

-- =====================================================================
-- HU-13 (Criterios 1, 2 y 3) - Registrar autores y ponentes en propuestas
-- =====================================================================

--changeset DF:hu13-001-rol-propuestas-autores
--comment: HU-13 Criterio 2. Rol específico del autor en la propuesta (autor_principal, coautor, ponente).
ALTER TABLE "propuestas_autores" ADD COLUMN IF NOT EXISTS "rol" VARCHAR(20) NOT NULL DEFAULT 'autor_principal';
ALTER TABLE "propuestas_autores" DROP CONSTRAINT IF EXISTS "propuestas_autores_rol_check";
ALTER TABLE "propuestas_autores" ADD CONSTRAINT "propuestas_autores_rol_check" 
    CHECK ("rol" IN ('autor_principal', 'coautor', 'ponente'));
--rollback ALTER TABLE "propuestas_autores" DROP CONSTRAINT IF EXISTS "propuestas_autores_rol_check";
--rollback ALTER TABLE "propuestas_autores" DROP COLUMN IF EXISTS "rol";

--changeset DF:hu13-002-indices-propuestas-autores
--comment: HU-13 Criterio 1. Optimizar consultas de autores por propuesta y por persona.
CREATE INDEX IF NOT EXISTS "idx_propuestas_autores_propuesta" ON "propuestas_autores" ("propuesta_id", "orden");
CREATE INDEX IF NOT EXISTS "idx_propuestas_autores_persona" ON "propuestas_autores" ("persona_id");
--rollback DROP INDEX IF EXISTS "idx_propuestas_autores_persona";
--rollback DROP INDEX IF EXISTS "idx_propuestas_autores_propuesta";
