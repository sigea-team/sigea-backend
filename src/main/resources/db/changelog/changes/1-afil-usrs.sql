--liquibase formatted sql

--changeset DF:001-eliminar-nombre-usuario
ALTER TABLE "usuarios" DROP COLUMN IF EXISTS "nombre_usuario";

--changeset DF:002-crear-tabla-dom-afiliaciones
CREATE TABLE "afiliaciones" (
  "id" SERIAL PRIMARY KEY,
  "nombre_afiliacion" VARCHAR(100) NOT NULL UNIQUE
);

--changeset DF:003-insertar-opciones-combobox
INSERT INTO "afiliaciones" ("nombre_afiliacion") VALUES
  ('Estudiante UFPS'),
  ('Particular'),
  ('Estudiante Internacional'),
  ('Estudiante Externo UFPS');

--changeset DF:004-vincular-afiliacion-con-persona
ALTER TABLE "personas" ADD COLUMN "afiliacion_id" INT;
ALTER TABLE "personas" ADD CONSTRAINT "fk_persona_afiliacion" 
  FOREIGN KEY ("afiliacion_id") REFERENCES "afiliaciones" ("id");