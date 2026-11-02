--liquibase formatted sql

--changeset sigea-dev:hu-salas-001
--comment: HU-Salas Criterio 1. Crea la tabla salas con índice único (evento_id, nombre) y FK hacia eventos.
--comment: El tipo se restringe a 'fisica' o 'virtual' y el aforo debe ser mayor que 0.
CREATE TABLE IF NOT EXISTS "salas" (
    "sala_id"      SERIAL       NOT NULL,
    "evento_id"    INT          NOT NULL,
    "nombre"       VARCHAR(100) NOT NULL,
    "ubicacion"    VARCHAR(150),
    "tipo"         VARCHAR(20)  NOT NULL,
    "aforo_maximo" INT          NOT NULL,
    CONSTRAINT "salas_pkey"             PRIMARY KEY ("sala_id"),
    CONSTRAINT "salas_evento_fk"        FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE,
    CONSTRAINT "salas_tipo_check"       CHECK ("tipo" IN ('FISICA', 'VIRTUAL')),
    CONSTRAINT "salas_aforo_check"      CHECK ("aforo_maximo" > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS "salas_evento_id_nombre_idx"
    ON "salas" ("evento_id", LOWER("nombre"));

CREATE INDEX IF NOT EXISTS "idx_salas_evento_id"
    ON "salas" ("evento_id");

--rollback DROP INDEX IF EXISTS "idx_salas_evento_id";
--rollback DROP INDEX IF EXISTS "salas_evento_id_nombre_idx";
--rollback DROP TABLE IF EXISTS "salas";

--changeset sigea-dev:hu-salas-002
--comment: HU-Salas Criterio 1. Crea la tabla disponibilidad_salas con CHECK hora_fin > hora_inicio y FK CASCADE hacia salas.
CREATE TABLE IF NOT EXISTS "disponibilidad_salas" (
    "disponibilidad_id" SERIAL NOT NULL,
    "sala_id"           INT    NOT NULL,
    "fecha"             DATE   NOT NULL,
    "hora_inicio"       TIME   NOT NULL,
    "hora_fin"          TIME   NOT NULL,
    CONSTRAINT "disponibilidad_salas_pkey"        PRIMARY KEY ("disponibilidad_id"),
    CONSTRAINT "disponibilidad_salas_sala_fk"     FOREIGN KEY ("sala_id") REFERENCES "salas" ("sala_id") ON DELETE CASCADE,
    CONSTRAINT "disponibilidad_salas_hora_check"  CHECK ("hora_fin" > "hora_inicio")
);

CREATE INDEX IF NOT EXISTS "idx_disponibilidad_salas_sala_id"
    ON "disponibilidad_salas" ("sala_id");

CREATE INDEX IF NOT EXISTS "idx_disponibilidad_salas_fecha"
    ON "disponibilidad_salas" ("sala_id", "fecha");

--rollback DROP INDEX IF EXISTS "idx_disponibilidad_salas_fecha";
--rollback DROP INDEX IF EXISTS "idx_disponibilidad_salas_sala_id";
--rollback DROP TABLE IF EXISTS "disponibilidad_salas";

--changeset sigea-dev:hu-salas-003
--comment: HU-Salas Criterio 2. Crea la tabla disponibilidad_ponentes con CHECK hora_fin > hora_inicio.
--comment: FK de evento con ON DELETE CASCADE; FK de persona sin cascade (una persona es permanente).
CREATE TABLE IF NOT EXISTS "disponibilidad_ponentes" (
    "disponibilidad_id" SERIAL NOT NULL,
    "persona_id"        INT    NOT NULL,
    "evento_id"         INT    NOT NULL,
    "fecha"             DATE   NOT NULL,
    "hora_inicio"       TIME   NOT NULL,
    "hora_fin"          TIME   NOT NULL,
    CONSTRAINT "disponibilidad_ponentes_pkey"         PRIMARY KEY ("disponibilidad_id"),
    CONSTRAINT "disponibilidad_ponentes_persona_fk"   FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id"),
    CONSTRAINT "disponibilidad_ponentes_evento_fk"    FOREIGN KEY ("evento_id")  REFERENCES "eventos"  ("evento_id") ON DELETE CASCADE,
    CONSTRAINT "disponibilidad_ponentes_hora_check"   CHECK ("hora_fin" > "hora_inicio")
);

CREATE INDEX IF NOT EXISTS "idx_disponibilidad_ponentes_persona_evento"
    ON "disponibilidad_ponentes" ("persona_id", "evento_id");

CREATE INDEX IF NOT EXISTS "idx_disponibilidad_ponentes_evento_id"
    ON "disponibilidad_ponentes" ("evento_id");

--rollback DROP INDEX IF EXISTS "idx_disponibilidad_ponentes_evento_id";
--rollback DROP INDEX IF EXISTS "idx_disponibilidad_ponentes_persona_evento";
--rollback DROP TABLE IF EXISTS "disponibilidad_ponentes";
