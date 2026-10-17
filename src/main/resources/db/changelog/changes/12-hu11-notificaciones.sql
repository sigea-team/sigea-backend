--liquibase formatted sql

-- =====================================================================
-- HU-11 (RF14) - Publicar convocatorias: notificaciones dentro del sistema
-- =====================================================================

--changeset SIGEA:hu11-001-tabla-notificaciones
--comment: HU-11 Criterio 2. Notificaciones que cada usuario ve en la campana del frontend (p. ej. el cambio de
--comment: fecha límite de una convocatoria publicada). Una fila por usuario destinatario, para saber qué leyó cada uno.
--comment: "tipo" no lleva CHECK a propósito: los valores los define el enum TipoNotificacion y así otras historias
--comment: pueden agregar tipos sin una migración nueva.
CREATE TABLE IF NOT EXISTS "notificaciones" (
  "notificacion_id" SERIAL PRIMARY KEY,
  "usuario_id" INT NOT NULL,
  "tipo" VARCHAR(50) NOT NULL,
  "titulo" VARCHAR(150) NOT NULL,
  "mensaje" TEXT NOT NULL,
  "entidad" VARCHAR(50),
  "entidad_id" INT,
  "leida" BOOLEAN NOT NULL DEFAULT false,
  "fecha_creacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "fecha_lectura" TIMESTAMP,
  CONSTRAINT "fk_notificaciones_usuario" FOREIGN KEY ("usuario_id")
      REFERENCES "usuarios" ("usuario_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE
);
CREATE INDEX IF NOT EXISTS "idx_notificaciones_usuario_fecha" ON "notificaciones" ("usuario_id", "fecha_creacion" DESC);
CREATE INDEX IF NOT EXISTS "idx_notificaciones_usuario_no_leidas" ON "notificaciones" ("usuario_id") WHERE "leida" = false;
--rollback DROP TABLE IF EXISTS "notificaciones";
