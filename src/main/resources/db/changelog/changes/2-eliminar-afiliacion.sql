--liquibase formatted sql

--changeset DF:002-eliminar-afiliacion-institucional
ALTER TABLE personas DROP COLUMN afiliacion_institucional;