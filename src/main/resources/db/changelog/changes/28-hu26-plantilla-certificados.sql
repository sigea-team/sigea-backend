--liquibase formatted sql

-- =====================================================================
-- HU-26 (RF47) - Plantilla de los certificados: fondo y firmantes por evento
-- Cada evento puede tener una imagen de fondo (plantillas_certificado) y de 1 a 4 firmantes con nombre, cargo e
-- imagen de la firma (firmantes_certificado). Todos los certificados del evento usan la misma plantilla: el backend
-- no deja cambiarla después de emitir el primero (todos los certificados deben ser iguales).
--
-- NOTA (HU-28): hu26-008 vuelve a definir fn_evento_de_fila con las dos tablas nuevas. Si se modifica hu28-004
-- (runOnChange), agregue allí 'plantillas_certificado' y 'firmantes_certificado' a la primera lista.
-- =====================================================================

--changeset SIGEA:hu26-006-plantillas-certificado
--comment: Fondo del certificado del evento (ruta en el almacenamiento de archivos, JPEG ya normalizado). Sin fila o sin
--comment: fondo, el certificado usa el diseño predeterminado.
CREATE TABLE IF NOT EXISTS "plantillas_certificado" (
    "plantilla_id" SERIAL PRIMARY KEY,
    "evento_id" INT NOT NULL UNIQUE REFERENCES "eventos" ("evento_id") ON DELETE CASCADE,
    "fondo_url" VARCHAR(255),
    "fecha_actualizacion" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE IF EXISTS "plantillas_certificado";

--changeset SIGEA:hu26-007-firmantes-certificado
--comment: Firmantes del certificado del evento, en el orden en que se imprimen (1 a 4; el máximo lo controla el
--comment: backend). firma_url es la ruta de la imagen PNG en el almacenamiento de archivos.
CREATE TABLE IF NOT EXISTS "firmantes_certificado" (
    "firmante_id" SERIAL PRIMARY KEY,
    "evento_id" INT NOT NULL REFERENCES "eventos" ("evento_id") ON DELETE CASCADE,
    "nombre" VARCHAR(150) NOT NULL,
    "cargo" VARCHAR(150) NOT NULL,
    "firma_url" VARCHAR(255) NOT NULL,
    "orden" INT NOT NULL CHECK ("orden" >= 1),
    "fecha_registro" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS "idx_firmantes_certificado_evento" ON "firmantes_certificado" ("evento_id", "orden");
--rollback DROP TABLE IF EXISTS "firmantes_certificado";

--changeset SIGEA:hu26-008-plantilla-memoria-historica splitStatements:false runOnChange:true
--comment: HU-28: con el evento cerrado formalmente, su plantilla y sus firmantes tampoco se modifican. Se agrega a
--comment: fn_evento_de_fila (igual a la de hu28-004 más las dos tablas) y se crean sus triggers.
CREATE OR REPLACE FUNCTION "fn_evento_de_fila"(p_tabla TEXT, p_fila JSONB) RETURNS INT AS $$
DECLARE
    v_evento INT;
BEGIN
    IF p_tabla IN ('eventos', 'memorias_historicas', 'indicadores', 'actividades', 'agendas_publicadas', 'certificados',
                   'comite_organizador', 'convocatorias', 'disponibilidad_ponentes', 'encuestas', 'inscripciones_evento',
                   'lineas_tematicas', 'participaciones_conferencista', 'presupuestos_aprobados',
                   'rubros_presupuestales', 'historial_rubros', 'tipos_actividad', 'invitaciones_conferencista',
                   'evento_salas', 'plantillas_certificado', 'firmantes_certificado') THEN
        v_evento := (p_fila ->> 'evento_id')::INT;
    ELSIF p_tabla IN ('propuestas', 'rubricas_evaluacion', 'comites_evaluadores') THEN
        SELECT c."evento_id" INTO v_evento FROM "convocatorias" c
        WHERE c."convocatoria_id" = (p_fila ->> 'convocatoria_id')::INT;
    ELSIF p_tabla IN ('asignaciones_evaluacion', 'propuestas_autores', 'resultados_evaluacion', 'versiones_propuesta',
                      'historial_asignaciones', 'historial_clasificaciones') THEN
        SELECT c."evento_id" INTO v_evento FROM "propuestas" p
        JOIN "convocatorias" c ON c."convocatoria_id" = p."convocatoria_id"
        WHERE p."propuesta_id" = (p_fila ->> 'propuesta_id')::INT;
    ELSIF p_tabla IN ('documentos_propuesta', 'observaciones_propuesta') THEN
        SELECT c."evento_id" INTO v_evento FROM "versiones_propuesta" v
        JOIN "propuestas" p ON p."propuesta_id" = v."propuesta_id"
        JOIN "convocatorias" c ON c."convocatoria_id" = p."convocatoria_id"
        WHERE v."version_id" = (p_fila ->> 'version_id')::INT;
    ELSIF p_tabla = 'criterios_rubrica' THEN
        SELECT c."evento_id" INTO v_evento FROM "rubricas_evaluacion" r
        JOIN "convocatorias" c ON c."convocatoria_id" = r."convocatoria_id"
        WHERE r."rubrica_id" = (p_fila ->> 'rubrica_id')::INT;
    ELSIF p_tabla = 'evaluaciones' THEN
        SELECT c."evento_id" INTO v_evento FROM "asignaciones_evaluacion" a
        JOIN "propuestas" p ON p."propuesta_id" = a."propuesta_id"
        JOIN "convocatorias" c ON c."convocatoria_id" = p."convocatoria_id"
        WHERE a."asignacion_id" = (p_fila ->> 'asignacion_id')::INT;
    ELSIF p_tabla IN ('inscripciones_actividad', 'asistencias', 'codigos_qr') THEN
        SELECT i."evento_id" INTO v_evento FROM "inscripciones_evento" i
        WHERE i."inscripcion_id" = (p_fila ->> 'inscripcion_id')::INT;
    ELSIF p_tabla IN ('preguntas_encuesta', 'participaciones_encuesta') THEN
        SELECT e."evento_id" INTO v_evento FROM "encuestas" e
        WHERE e."encuesta_id" = (p_fila ->> 'encuesta_id')::INT;
    ELSIF p_tabla IN ('opciones_pregunta_encuesta', 'respuestas_encuesta') THEN
        SELECT e."evento_id" INTO v_evento FROM "preguntas_encuesta" q
        JOIN "encuestas" e ON e."encuesta_id" = q."encuesta_id"
        WHERE q."pregunta_id" = (p_fila ->> 'pregunta_id')::INT;
    ELSIF p_tabla = 'gastos_ejecutados' THEN
        SELECT r."evento_id" INTO v_evento FROM "rubros_presupuestales" r
        WHERE r."rubro_id" = (p_fila ->> 'rubro_id')::INT;
    END IF;
    RETURN v_evento;
END;
$$ LANGUAGE plpgsql STABLE;

DROP TRIGGER IF EXISTS "trg_memoria_historica_plantillas_certificado" ON "plantillas_certificado";
CREATE TRIGGER "trg_memoria_historica_plantillas_certificado" BEFORE INSERT OR UPDATE OR DELETE ON "plantillas_certificado"
    FOR EACH ROW EXECUTE FUNCTION "fn_memoria_historica_inmutable"();
DROP TRIGGER IF EXISTS "trg_memoria_historica_firmantes_certificado" ON "firmantes_certificado";
CREATE TRIGGER "trg_memoria_historica_firmantes_certificado" BEFORE INSERT OR UPDATE OR DELETE ON "firmantes_certificado"
    FOR EACH ROW EXECUTE FUNCTION "fn_memoria_historica_inmutable"();
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_firmantes_certificado" ON "firmantes_certificado";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_plantillas_certificado" ON "plantillas_certificado";
