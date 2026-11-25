--liquibase formatted sql

-- =====================================================================
-- HU-26 (RF47) - Habilitar los certificados del evento
-- El organizador habilita los certificados cuando el evento terminó y la plantilla está completa (fondo y al menos
-- una firma). En ese momento la plantilla queda fija y se generan los certificados de quienes cumplen; antes nadie
-- puede generarlos. Quién lo hizo queda en la auditoría (CERTIFICADOS_HABILITADOS).
-- =====================================================================

--changeset SIGEA:hu26-009-fecha-habilitacion-certificados
--comment: Fecha en que se habilitaron los certificados del evento; null = aún no habilitados. La tabla ya está
--comment: protegida por los triggers de la memoria histórica (hu26-008).
ALTER TABLE "plantillas_certificado" ADD COLUMN IF NOT EXISTS "fecha_habilitacion" TIMESTAMP;
--rollback ALTER TABLE "plantillas_certificado" DROP COLUMN IF EXISTS "fecha_habilitacion";
