--liquibase formatted sql

--changeset SIGEA:006-eventos-semestre-unico-familia
--comment: HU-04. Un semestre solo puede aparecer una vez por familia de eventos (evento base + sus ediciones).
--comment: COALESCE agrupa al evento base (evento_base_id NULL) con sus ediciones bajo el mismo identificador.
--comment: Complementa la validación de EventoService y evita duplicados por peticiones simultáneas.
CREATE UNIQUE INDEX IF NOT EXISTS "ux_eventos_familia_semestre"
    ON "eventos" ((COALESCE("evento_base_id", "evento_id")), "semestre");
--rollback DROP INDEX IF EXISTS "ux_eventos_familia_semestre";
