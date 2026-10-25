--liquibase formatted sql

-- =====================================================================
-- HU-12 (RF16 + RF17) - Registrar y administrar información de conferencistas
-- Los datos personales y de contacto viven en "personas" (documento y correo ya son UNIQUE, lo que
-- evita duplicados - Criterio 3). La trayectoria es UNA por persona (perfil del conferencista) y la
-- asociación con cada evento queda en "participaciones_conferencista" (historial RF17).
-- =====================================================================

--changeset SIGEA:hu12-001-perfiles-conferencista
--comment: HU-12 Criterio 1. Trayectoria académica y profesional del conferencista (una por persona).
--comment: Al ser única por persona, al actualizarla el cambio se ve en todos los eventos donde participa (Criterio 4).
CREATE TABLE IF NOT EXISTS "perfiles_conferencista" (
  "perfil_id" SERIAL PRIMARY KEY,
  "persona_id" INT NOT NULL,
  "biografia" TEXT NOT NULL,
  "cargo_actual" VARCHAR(150),
  "institucion" VARCHAR(150),
  "enlace_perfil" VARCHAR(300),
  "fecha_registro" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "fecha_actualizacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  CONSTRAINT "ux_perfiles_conferencista_persona" UNIQUE ("persona_id"),
  CONSTRAINT "fk_perfiles_conferencista_persona" FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id")
);
--rollback DROP TABLE IF EXISTS "perfiles_conferencista";

--changeset SIGEA:hu12-002-estudios-conferencista
--comment: HU-12 Criterio 1. Estudios (formación académica) que forman parte de la trayectoria.
CREATE TABLE IF NOT EXISTS "estudios_conferencista" (
  "estudio_id" SERIAL PRIMARY KEY,
  "perfil_id" INT NOT NULL,
  "nivel" VARCHAR(20) NOT NULL,
  "titulo" VARCHAR(200) NOT NULL,
  "institucion" VARCHAR(150) NOT NULL,
  "anio" INT,
  "orden" INT NOT NULL DEFAULT 1,
  CONSTRAINT "estudios_conferencista_nivel_check"
    CHECK ("nivel" IN ('pregrado', 'especializacion', 'maestria', 'doctorado', 'posdoctorado', 'otro')),
  CONSTRAINT "estudios_conferencista_anio_check" CHECK ("anio" IS NULL OR "anio" BETWEEN 1900 AND 2100),
  CONSTRAINT "fk_estudios_conferencista_perfil" FOREIGN KEY ("perfil_id")
    REFERENCES "perfiles_conferencista" ("perfil_id") ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS "idx_estudios_conferencista_perfil" ON "estudios_conferencista" ("perfil_id", "orden");
--rollback DROP TABLE IF EXISTS "estudios_conferencista";

--changeset SIGEA:hu12-003-publicaciones-conferencista
--comment: HU-12 Criterio 1. Publicaciones que forman parte de la trayectoria.
CREATE TABLE IF NOT EXISTS "publicaciones_conferencista" (
  "publicacion_id" SERIAL PRIMARY KEY,
  "perfil_id" INT NOT NULL,
  "tipo" VARCHAR(20) NOT NULL,
  "titulo" VARCHAR(300) NOT NULL,
  "medio" VARCHAR(200),
  "anio" INT,
  "enlace" VARCHAR(300),
  "orden" INT NOT NULL DEFAULT 1,
  CONSTRAINT "publicaciones_conferencista_tipo_check"
    CHECK ("tipo" IN ('articulo', 'libro', 'capitulo', 'ponencia', 'otro')),
  CONSTRAINT "publicaciones_conferencista_anio_check" CHECK ("anio" IS NULL OR "anio" BETWEEN 1900 AND 2100),
  CONSTRAINT "fk_publicaciones_conferencista_perfil" FOREIGN KEY ("perfil_id")
    REFERENCES "perfiles_conferencista" ("perfil_id") ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS "idx_publicaciones_conferencista_perfil" ON "publicaciones_conferencista" ("perfil_id", "orden");
--rollback DROP TABLE IF EXISTS "publicaciones_conferencista";

--changeset SIGEA:hu12-004-participacion-unica-por-evento
--comment: HU-12 Criterio 3. Un conferencista se asocia UNA sola vez a cada evento.
--comment: El índice original (persona_id, evento_id, tipo_participacion, tema) no lo impedía: con tema NULL o con
--comment: otro tema la misma persona quedaba repetida en el evento.
DROP INDEX IF EXISTS "participaciones_conferencista_persona_id_evento_id_tipo_par_idx";
CREATE UNIQUE INDEX IF NOT EXISTS "ux_participaciones_conferencista_evento_persona"
    ON "participaciones_conferencista" ("evento_id", "persona_id");
--rollback DROP INDEX IF EXISTS "ux_participaciones_conferencista_evento_persona";
--rollback CREATE UNIQUE INDEX IF NOT EXISTS "participaciones_conferencista_persona_id_evento_id_tipo_par_idx" ON "participaciones_conferencista" ("persona_id", "evento_id", "tipo_participacion", "tema");
