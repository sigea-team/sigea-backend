--liquibase formatted sql

--changeset DF:002-eliminar-afiliacion-institucional
--validCheckSum: ANY
ALTER TABLE personas DROP COLUMN IF EXISTS afiliacion_institucional;