--liquibase formatted sql

--changeset HU01:001-agregar-bloqueo-login
ALTER TABLE "usuarios" ADD COLUMN "intentos_fallidos" INT NOT NULL DEFAULT 0;
ALTER TABLE "usuarios" ADD COLUMN "bloqueado_hasta" TIMESTAMP;
