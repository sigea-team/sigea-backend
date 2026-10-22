--liquibase formatted sql

-- =====================================================================
-- HU-12 - Invitación de conferencistas por correo
-- El administrador vincula a los conferencistas ya registrados o invita a uno nuevo solo con nombre
-- y correo. El invitado recibe un enlace (vigente 3 días) y él mismo registra su documento, teléfono
-- y trayectoria. La persona se crea al completar la invitación, porque personas exige documento.
-- =====================================================================

--changeset SIGEA:hu12-005-invitaciones-conferencista
--comment: HU-12. Invitaciones enviadas a conferencistas nuevos. Estado vencida no se guarda: es pendiente con fecha_expiracion pasada.
CREATE TABLE IF NOT EXISTS "invitaciones_conferencista" (
  "invitacion_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "nombre" VARCHAR(200) NOT NULL,
  "correo" VARCHAR(150) NOT NULL,
  "tipo_participacion" VARCHAR(20) NOT NULL DEFAULT 'conferencista',
  "token" VARCHAR(64) NOT NULL,
  "estado" VARCHAR(20) NOT NULL DEFAULT 'pendiente',
  "fecha_envio" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "fecha_expiracion" TIMESTAMP NOT NULL,
  "fecha_completada" TIMESTAMP,
  "persona_id" INT,
  "invitado_por" INT,
  CONSTRAINT "ux_invitaciones_conferencista_token" UNIQUE ("token"),
  CONSTRAINT "invitaciones_conferencista_estado_check" CHECK ("estado" IN ('pendiente', 'completada', 'cancelada')),
  CONSTRAINT "invitaciones_conferencista_tipo_check" CHECK ("tipo_participacion" IN ('conferencista', 'ponente')),
  CONSTRAINT "invitaciones_conferencista_fechas_check" CHECK ("fecha_expiracion" > "fecha_envio"),
  CONSTRAINT "fk_invitaciones_conferencista_evento" FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE,
  CONSTRAINT "fk_invitaciones_conferencista_persona" FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id"),
  CONSTRAINT "fk_invitaciones_conferencista_usuario" FOREIGN KEY ("invitado_por") REFERENCES "usuarios" ("usuario_id")
);
--comment: Una sola invitación pendiente por correo en cada evento (si venció, se renueva la misma).
CREATE UNIQUE INDEX IF NOT EXISTS "ux_invitaciones_conferencista_pendiente"
    ON "invitaciones_conferencista" ("evento_id", LOWER("correo"))
    WHERE "estado" = 'pendiente';
CREATE INDEX IF NOT EXISTS "idx_invitaciones_conferencista_evento" ON "invitaciones_conferencista" ("evento_id", "estado");
--rollback DROP TABLE IF EXISTS "invitaciones_conferencista";
