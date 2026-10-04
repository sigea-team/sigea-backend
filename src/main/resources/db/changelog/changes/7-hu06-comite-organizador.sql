--liquibase formatted sql

--changeset SIGEA:hu06-001-comite-fecha-retiro
--comment: HU-06 Criterio 3. Fecha en que el miembro fue retirado del comité vigente (NULL mientras esté activo).
--comment: Junto con activo = false conserva el historial de participación sin borrar el registro.
ALTER TABLE "comite_organizador" ADD COLUMN IF NOT EXISTS "fecha_retiro" TIMESTAMP;
--rollback ALTER TABLE "comite_organizador" DROP COLUMN IF EXISTS "fecha_retiro";

--changeset SIGEA:hu06-002-comite-unico-miembro-activo
--comment: HU-06 Criterio 2. Una persona solo puede tener UNA participación vigente por evento.
--comment: El índice original (evento_id, persona_id, rol_comite) incluía los registros retirados, lo que impedía
--comment: volver a vincular a alguien con el mismo rol después de retirarlo. El índice parcial solo aplica a activo = true,
--comment: así cada participación (incluidas las anteriores) queda como un registro independiente del historial.
DROP INDEX IF EXISTS "comite_organizador_evento_id_persona_id_rol_comite_idx";
CREATE UNIQUE INDEX IF NOT EXISTS "ux_comite_organizador_miembro_activo"
    ON "comite_organizador" ("evento_id", "persona_id")
    WHERE "activo" = true;
CREATE INDEX IF NOT EXISTS "idx_comite_organizador_evento" ON "comite_organizador" ("evento_id");
--rollback DROP INDEX IF EXISTS "idx_comite_organizador_evento";
--rollback DROP INDEX IF EXISTS "ux_comite_organizador_miembro_activo";
--rollback CREATE UNIQUE INDEX IF NOT EXISTS "comite_organizador_evento_id_persona_id_rol_comite_idx" ON "comite_organizador" ("evento_id", "persona_id", "rol_comite");
