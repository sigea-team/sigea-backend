--liquibase formatted sql

-- =====================================================================
-- HU-26 (RF47, CU-23) - Generar certificados de participación
-- La tabla certificados ya existía (sin uso). Se completa para emitir certificados de asistente, ponente y
-- conferencista sin duplicarlos y para poder entregar siempre la misma versión (Criterio 3).
-- =====================================================================

--changeset SIGEA:hu26-001-porcentaje-minimo-asistencia
--comment: RN-01: la condición de asistencia es configurable por evento. Porcentaje mínimo de las actividades
--comment: inscritas (no canceladas ni por programar) a las que el participante debe asistir para su certificado
--comment: de asistente. Valor inicial 80 % (decisión del equipo, ajustable por evento).
ALTER TABLE "eventos" ADD COLUMN IF NOT EXISTS "porcentaje_minimo_asistencia" NUMERIC(5,2) NOT NULL DEFAULT 80;
ALTER TABLE "eventos" ADD CONSTRAINT "eventos_porcentaje_minimo_asistencia_check"
    CHECK ("porcentaje_minimo_asistencia" > 0 AND "porcentaje_minimo_asistencia" <= 100);
--rollback ALTER TABLE "eventos" DROP CONSTRAINT IF EXISTS "eventos_porcentaje_minimo_asistencia_check";
--rollback ALTER TABLE "eventos" DROP COLUMN IF EXISTS "porcentaje_minimo_asistencia";

--changeset SIGEA:hu26-002-tipo-certificado-conferencista
--comment: Criterio 1: el certificado lleva el rol correspondiente (asistente, ponente o conferencista). El CHECK
--comment: original no tenía 'conferencista'; se conservan 'evaluador' y 'organizador' (RF47) para más adelante.
ALTER TABLE "certificados" DROP CONSTRAINT IF EXISTS "certificados_tipo_certificado_check";
ALTER TABLE "certificados" ADD CONSTRAINT "certificados_tipo_certificado_check"
    CHECK ("tipo_certificado" IN ('asistente', 'ponente', 'conferencista', 'evaluador', 'organizador'));
--rollback ALTER TABLE "certificados" DROP CONSTRAINT IF EXISTS "certificados_tipo_certificado_check";
--rollback ALTER TABLE "certificados" ADD CONSTRAINT "certificados_tipo_certificado_check" CHECK ("tipo_certificado" IN ('asistente', 'ponente', 'evaluador', 'organizador'));

--changeset SIGEA:hu26-003-datos-emitidos-y-codigo-verificacion
--comment: Criterio 3: los datos impresos se guardan al emitir (nombre, documento, evento, fechas y detalle), de modo que
--comment: el certificado se vuelve a entregar igual aunque después cambien la persona o el evento, y el PDF se puede
--comment: reconstruir si el archivo se pierde del almacenamiento. codigo_verificacion permite comprobar su autenticidad
--comment: en la consulta pública. Las filas anteriores (la tabla no se usaba) reciben un código aleatorio.
ALTER TABLE "certificados" ADD COLUMN IF NOT EXISTS "codigo_verificacion" VARCHAR(40);
ALTER TABLE "certificados" ADD COLUMN IF NOT EXISTS "nombre_participante" VARCHAR(201);
ALTER TABLE "certificados" ADD COLUMN IF NOT EXISTS "tipo_documento" VARCHAR(20);
ALTER TABLE "certificados" ADD COLUMN IF NOT EXISTS "numero_documento" VARCHAR(30);
ALTER TABLE "certificados" ADD COLUMN IF NOT EXISTS "nombre_evento" VARCHAR(200);
ALTER TABLE "certificados" ADD COLUMN IF NOT EXISTS "fecha_inicio_evento" DATE;
ALTER TABLE "certificados" ADD COLUMN IF NOT EXISTS "fecha_fin_evento" DATE;
ALTER TABLE "certificados" ADD COLUMN IF NOT EXISTS "detalle" TEXT;
UPDATE "certificados"
SET "codigo_verificacion" = 'CERT-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 24))
WHERE "codigo_verificacion" IS NULL;
ALTER TABLE "certificados" ALTER COLUMN "codigo_verificacion" SET NOT NULL;
ALTER TABLE "certificados" ADD CONSTRAINT "certificados_codigo_verificacion_key" UNIQUE ("codigo_verificacion");
--rollback ALTER TABLE "certificados" DROP CONSTRAINT IF EXISTS "certificados_codigo_verificacion_key";
--rollback ALTER TABLE "certificados" DROP COLUMN IF EXISTS "detalle";
--rollback ALTER TABLE "certificados" DROP COLUMN IF EXISTS "fecha_fin_evento";
--rollback ALTER TABLE "certificados" DROP COLUMN IF EXISTS "fecha_inicio_evento";
--rollback ALTER TABLE "certificados" DROP COLUMN IF EXISTS "nombre_evento";
--rollback ALTER TABLE "certificados" DROP COLUMN IF EXISTS "numero_documento";
--rollback ALTER TABLE "certificados" DROP COLUMN IF EXISTS "tipo_documento";
--rollback ALTER TABLE "certificados" DROP COLUMN IF EXISTS "nombre_participante";
--rollback ALTER TABLE "certificados" DROP COLUMN IF EXISTS "codigo_verificacion";

--changeset SIGEA:hu26-004-indices-certificados
--comment: Criterio 3: un solo certificado por persona, evento y tipo (los de todo el evento, sin actividad). Si en el
--comment: futuro se emiten certificados por actividad, el segundo índice evita duplicarlos por actividad.
--comment: Índices de consulta por evento y por persona (perfil del participante).
CREATE UNIQUE INDEX IF NOT EXISTS "ux_certificados_persona_evento_tipo"
    ON "certificados" ("persona_id", "evento_id", "tipo_certificado") WHERE "actividad_id" IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS "ux_certificados_persona_actividad_tipo"
    ON "certificados" ("persona_id", "actividad_id", "tipo_certificado") WHERE "actividad_id" IS NOT NULL;
CREATE INDEX IF NOT EXISTS "idx_certificados_evento" ON "certificados" ("evento_id");
CREATE INDEX IF NOT EXISTS "idx_certificados_persona" ON "certificados" ("persona_id");
--rollback DROP INDEX IF EXISTS "idx_certificados_persona";
--rollback DROP INDEX IF EXISTS "idx_certificados_evento";
--rollback DROP INDEX IF EXISTS "ux_certificados_persona_actividad_tipo";
--rollback DROP INDEX IF EXISTS "ux_certificados_persona_evento_tipo";

--changeset SIGEA:hu26-005-asignar-permiso-emitir-certificados
--comment: El permiso CERTIFICADOS_EMITIR ya existía (changeset 3) sin uso. Asignación inicial: quien edita eventos
--comment: (organizador/administrador) genera los certificados del evento y configura el porcentaje mínimo. Se ajusta
--comment: por rol en HU-02. El participante no necesita permiso para pedir y descargar los suyos.
INSERT INTO "roles_permisos" ("rol_id", "permiso_id")
SELECT DISTINCT rp."rol_id", p."permiso_id"
FROM "roles_permisos" rp
JOIN "permisos" origen ON origen."permiso_id" = rp."permiso_id" AND origen."codigo" = 'EVENTOS_EDITAR'
JOIN "permisos" p ON p."codigo" = 'CERTIFICADOS_EMITIR'
ON CONFLICT DO NOTHING;
--rollback empty
