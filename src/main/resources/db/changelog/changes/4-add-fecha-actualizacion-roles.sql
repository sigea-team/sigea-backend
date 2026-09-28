--liquibase formatted sql

--changeset SIGEA:004-add-fecha-actualizacion-roles
--validCheckSum: ANY
ALTER TABLE "roles"
ADD COLUMN IF NOT EXISTS "fecha_actualizacion" TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
