--liquibase formatted sql

-- =====================================================================
-- HU-24 (RF44 + RF45) - Generar código QR y registrar asistencia
-- Las tablas codigos_qr y asistencias ya existían (sin uso). No se cambia su estructura.
-- =====================================================================

--changeset SIGEA:hu24-001-codigos-qr-inscripciones-existentes
--comment: Criterio 1: cada inscripción al evento tiene un código QR único. Las inscripciones registradas antes de
--comment: HU-24 reciben su código aquí (aleatorio de 128 bits, mismo formato que CodigoQrService: SIGEA- + 32 hex).
--comment: No se envían correos; el participante lo ve en su inscripción o pide el reenvío (Criterio 4).
INSERT INTO "codigos_qr" ("inscripcion_id", "codigo", "fecha_generacion", "vigente")
SELECT ie."inscripcion_id", 'SIGEA-' || replace(gen_random_uuid()::text, '-', ''), CURRENT_TIMESTAMP, true
FROM "inscripciones_evento" ie
WHERE NOT EXISTS (SELECT 1 FROM "codigos_qr" q WHERE q."inscripcion_id" = ie."inscripcion_id");
--rollback empty

--changeset SIGEA:hu24-002-indices-asistencias
--comment: Criterio 3: el índice único (inscripcion_id, actividad_id) impide duplicar una asistencia (ya existe en el
--comment: esquema; se asegura con IF NOT EXISTS). Índice por actividad para listar las asistencias de una actividad.
CREATE UNIQUE INDEX IF NOT EXISTS "asistencias_inscripcion_id_actividad_id_idx" ON "asistencias" ("inscripcion_id", "actividad_id");
CREATE INDEX IF NOT EXISTS "idx_asistencias_actividad" ON "asistencias" ("actividad_id");
--rollback DROP INDEX IF EXISTS "idx_asistencias_actividad";

--changeset SIGEA:hu24-003-asignar-permiso-registrar-asistencia
--comment: El permiso ASISTENCIA_REGISTRAR ya existía (changeset 3) pero ningún endpoint lo usaba. Asignación inicial:
--comment: quien edita eventos (organizador/administrador) registra asistencia. Se ajusta por rol en HU-02.
INSERT INTO "roles_permisos" ("rol_id", "permiso_id")
SELECT DISTINCT rp."rol_id", p."permiso_id"
FROM "roles_permisos" rp
JOIN "permisos" origen ON origen."permiso_id" = rp."permiso_id" AND origen."codigo" = 'EVENTOS_EDITAR'
JOIN "permisos" p ON p."codigo" = 'ASISTENCIA_REGISTRAR'
ON CONFLICT DO NOTHING;
--rollback empty
