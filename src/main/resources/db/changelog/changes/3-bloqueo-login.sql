--liquibase formatted sql

--changeset HU01:001-agregar-bloqueo-login
--validCheckSum: ANY
ALTER TABLE "usuarios" ADD COLUMN IF NOT EXISTS "intentos_fallidos" INT NOT NULL DEFAULT 0;
ALTER TABLE "usuarios" ADD COLUMN IF NOT EXISTS "bloqueado_hasta" TIMESTAMP;
