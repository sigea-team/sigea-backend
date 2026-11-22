--liquibase formatted sql

-- =====================================================================
-- HU-28 (RF55, CU-32) - Mantener memoria histórica de los eventos
-- Las tablas memorias_historicas e indicadores ya existían (sin uso). Se completan para:
--   * el cierre formal del evento: una fila de memorias_historicas por evento (índice único existente) con la
--     información consolidada en ese momento (contenido, JSON) y quién lo cerró (Criterio 1);
--   * los indicadores calculados al cierre, con un código estable para consultarlos;
--   * la inmutabilidad: con la memoria consolidada, la información del evento ya no se modifica ni se elimina
--     (Criterios 2 y 3). La regla está en la base de datos (triggers), así que también la respetan los borrados
--     en cascada y las consultas nativas.
--
-- ATENCIÓN (rollback): deshacer hu28-004 vuelve editable la información de los eventos cerrados formalmente.
-- (La nota va en la cabecera y no dentro del changeset para no cambiar su checksum.)
-- =====================================================================

--changeset SIGEA:hu28-001-memorias-historicas-contenido
--comment: Criterio 1: contenido guarda en JSON la información consolidada del evento al cierre formal (configuración,
--comment: comité, presupuesto, convocatorias y propuestas, conferencistas, agenda, participantes, asistencia, encuestas
--comment: e indicadores). cerrada_por queda en NULL si se elimina el usuario. resumen y archivo_url ya existían.
ALTER TABLE "memorias_historicas" ADD COLUMN IF NOT EXISTS "contenido" TEXT;
ALTER TABLE "memorias_historicas" ADD COLUMN IF NOT EXISTS "cerrada_por" INT;
ALTER TABLE "memorias_historicas" ADD CONSTRAINT "memorias_historicas_cerrada_por_fkey"
    FOREIGN KEY ("cerrada_por") REFERENCES "usuarios" ("usuario_id") ON DELETE SET NULL;
--rollback ALTER TABLE "memorias_historicas" DROP CONSTRAINT IF EXISTS "memorias_historicas_cerrada_por_fkey";
--rollback ALTER TABLE "memorias_historicas" DROP COLUMN IF EXISTS "cerrada_por";
--rollback ALTER TABLE "memorias_historicas" DROP COLUMN IF EXISTS "contenido";

--changeset SIGEA:hu28-002-indicadores-codigo
--comment: Código estable del indicador (INSCRITOS, TASA_ASISTENCIA...) para consultarlo sin depender del nombre visible.
ALTER TABLE "indicadores" ADD COLUMN IF NOT EXISTS "codigo" VARCHAR(50);
CREATE INDEX IF NOT EXISTS "idx_indicadores_evento" ON "indicadores" ("evento_id");
--rollback DROP INDEX IF EXISTS "idx_indicadores_evento";
--rollback ALTER TABLE "indicadores" DROP COLUMN IF EXISTS "codigo";

--changeset SIGEA:hu28-003-permisos-memoria-historica
--comment: MEMORIA_HISTORICA_GESTIONAR: cierre formal del evento; se asigna a quien edita eventos (organizador o
--comment: administrador). MEMORIA_HISTORICA_ELIMINAR: autorización especial para eliminar un evento cerrado
--comment: formalmente (Criterio 3); no se asigna a ningún rol: un administrador debe otorgarla a propósito en HU-02.
INSERT INTO "permisos" ("codigo", "modulo", "descripcion") VALUES
  ('MEMORIA_HISTORICA_GESTIONAR', 'MEMORIA_HISTORICA', 'Cerrar formalmente un evento y consolidar su memoria histórica'),
  ('MEMORIA_HISTORICA_ELIMINAR', 'MEMORIA_HISTORICA', 'Autorización especial para eliminar un evento cerrado formalmente y su memoria histórica')
ON CONFLICT ("codigo") DO NOTHING;
INSERT INTO "roles_permisos" ("rol_id", "permiso_id")
SELECT DISTINCT rp."rol_id", p."permiso_id"
FROM "roles_permisos" rp
JOIN "permisos" origen ON origen."permiso_id" = rp."permiso_id" AND origen."codigo" = 'EVENTOS_EDITAR'
JOIN "permisos" p ON p."codigo" = 'MEMORIA_HISTORICA_GESTIONAR'
ON CONFLICT DO NOTHING;
--rollback DELETE FROM "roles_permisos" WHERE "permiso_id" IN (SELECT "permiso_id" FROM "permisos" WHERE "codigo" IN ('MEMORIA_HISTORICA_GESTIONAR', 'MEMORIA_HISTORICA_ELIMINAR'));
--rollback DELETE FROM "permisos" WHERE "codigo" IN ('MEMORIA_HISTORICA_GESTIONAR', 'MEMORIA_HISTORICA_ELIMINAR');

--changeset SIGEA:hu28-004-memoria-historica-inmutable splitStatements:false
--comment: Criterios 2 y 3: con la memoria consolidada, ninguna fila del evento se inserta, modifica ni elimina. Cada
--comment: tabla del evento tiene un trigger que averigua a qué evento pertenece la fila (directo o a través de la
--comment: convocatoria, la propuesta, la inscripción o la encuesta) y rechaza la operación con SQLSTATE SG028; el
--comment: backend la traduce a 409 EVENTO_ARCHIVADO y la registra en la auditoría.
--comment: Excepciones: (1) poner en NULL una columna que apunta a usuarios, que es lo que hace ON DELETE SET NULL al
--comment: eliminar un usuario; (2) la eliminación con autorización especial, que el backend habilita solo dentro de su
--comment: transacción con set_config('sigea.eliminacion_historica', 'autorizada', true).
--comment: Las salas y su disponibilidad no se incluyen: son recursos reutilizables entre eventos; la memoria guarda el
--comment: nombre de la sala de cada actividad.
CREATE OR REPLACE FUNCTION "fn_evento_de_fila"(p_tabla TEXT, p_fila JSONB) RETURNS INT AS $$
DECLARE
    v_evento INT;
BEGIN
    IF p_tabla IN ('eventos', 'memorias_historicas', 'indicadores', 'actividades', 'agendas_publicadas', 'certificados',
                   'comite_organizador', 'convocatorias', 'disponibilidad_ponentes', 'encuestas', 'inscripciones_evento',
                   'lineas_tematicas', 'participaciones_conferencista', 'presupuestos_aprobados',
                   'rubros_presupuestales', 'historial_rubros', 'tipos_actividad', 'invitaciones_conferencista',
                   'evento_salas') THEN
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

CREATE OR REPLACE FUNCTION "fn_evento_archivado"(p_evento_id INT) RETURNS BOOLEAN AS $$
    SELECT p_evento_id IS NOT NULL
       AND EXISTS (SELECT 1 FROM "memorias_historicas" m WHERE m."evento_id" = p_evento_id);
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION "fn_memoria_historica_inmutable"() RETURNS trigger AS $$
DECLARE
    v_evento INT;
    v_cambios TEXT[];
BEGIN
    IF TG_OP = 'DELETE' AND current_setting('sigea.eliminacion_historica', true) = 'autorizada' THEN
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        SELECT array_agg(n.key) INTO v_cambios
        FROM jsonb_each(to_jsonb(NEW)) n
        WHERE n.value IS DISTINCT FROM (to_jsonb(OLD) -> n.key);
        -- UPDATE sin cambios reales: no altera la memoria.
        IF v_cambios IS NULL THEN
            RETURN NEW;
        END IF;
        -- ON DELETE SET NULL al eliminar un usuario: solo pasan a NULL columnas que apuntan a usuarios.
        IF v_cambios <@ ARRAY['usuario_id', 'creada_por', 'cerrada_por', 'resuelto_por', 'aprobada_por',
                              'aprobado_por', 'registrado_por', 'invitado_por', 'creado_por']
           AND NOT EXISTS (SELECT 1 FROM jsonb_each(to_jsonb(NEW)) n
                           WHERE n.key = ANY (v_cambios) AND n.value <> 'null'::jsonb) THEN
            RETURN NEW;
        END IF;
    END IF;

    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        v_evento := "fn_evento_de_fila"(TG_TABLE_NAME, to_jsonb(OLD));
        IF "fn_evento_archivado"(v_evento) THEN
            RAISE EXCEPTION 'La información del evento % está consolidada en la memoria histórica y no se puede % (% sobre %).',
                v_evento, CASE WHEN TG_OP = 'DELETE' THEN 'eliminar' ELSE 'modificar' END, TG_OP, TG_TABLE_NAME
                USING ERRCODE = 'SG028';
        END IF;
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        v_evento := "fn_evento_de_fila"(TG_TABLE_NAME, to_jsonb(NEW));
        IF "fn_evento_archivado"(v_evento) THEN
            RAISE EXCEPTION 'La información del evento % está consolidada en la memoria histórica y no se puede modificar (% sobre %).',
                v_evento, TG_OP, TG_TABLE_NAME
                USING ERRCODE = 'SG028';
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['eventos', 'memorias_historicas', 'indicadores', 'actividades', 'agendas_publicadas',
        'certificados', 'comite_organizador', 'convocatorias', 'disponibilidad_ponentes', 'encuestas',
        'inscripciones_evento', 'lineas_tematicas', 'participaciones_conferencista', 'presupuestos_aprobados',
        'rubros_presupuestales', 'historial_rubros', 'tipos_actividad', 'invitaciones_conferencista', 'evento_salas',
        'propuestas', 'rubricas_evaluacion', 'comites_evaluadores', 'asignaciones_evaluacion', 'propuestas_autores',
        'resultados_evaluacion', 'versiones_propuesta', 'historial_asignaciones', 'historial_clasificaciones',
        'documentos_propuesta', 'observaciones_propuesta', 'criterios_rubrica', 'evaluaciones',
        'inscripciones_actividad', 'asistencias', 'codigos_qr', 'preguntas_encuesta', 'participaciones_encuesta',
        'opciones_pregunta_encuesta', 'respuestas_encuesta', 'gastos_ejecutados']
    LOOP
        IF to_regclass(format('%I', t)) IS NOT NULL THEN
            EXECUTE format('DROP TRIGGER IF EXISTS %I ON %I', 'trg_memoria_historica_' || t, t);
            EXECUTE format('CREATE TRIGGER %I BEFORE INSERT OR UPDATE OR DELETE ON %I '
                           || 'FOR EACH ROW EXECUTE FUNCTION "fn_memoria_historica_inmutable"()',
                           'trg_memoria_historica_' || t, t);
        END IF;
    END LOOP;
END;
$$;
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_eventos" ON "eventos";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_memorias_historicas" ON "memorias_historicas";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_indicadores" ON "indicadores";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_actividades" ON "actividades";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_agendas_publicadas" ON "agendas_publicadas";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_certificados" ON "certificados";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_comite_organizador" ON "comite_organizador";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_convocatorias" ON "convocatorias";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_disponibilidad_ponentes" ON "disponibilidad_ponentes";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_encuestas" ON "encuestas";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_inscripciones_evento" ON "inscripciones_evento";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_lineas_tematicas" ON "lineas_tematicas";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_participaciones_conferencista" ON "participaciones_conferencista";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_presupuestos_aprobados" ON "presupuestos_aprobados";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_rubros_presupuestales" ON "rubros_presupuestales";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_historial_rubros" ON "historial_rubros";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_tipos_actividad" ON "tipos_actividad";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_invitaciones_conferencista" ON "invitaciones_conferencista";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_evento_salas" ON "evento_salas";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_propuestas" ON "propuestas";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_rubricas_evaluacion" ON "rubricas_evaluacion";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_comites_evaluadores" ON "comites_evaluadores";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_asignaciones_evaluacion" ON "asignaciones_evaluacion";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_propuestas_autores" ON "propuestas_autores";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_resultados_evaluacion" ON "resultados_evaluacion";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_versiones_propuesta" ON "versiones_propuesta";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_historial_asignaciones" ON "historial_asignaciones";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_historial_clasificaciones" ON "historial_clasificaciones";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_documentos_propuesta" ON "documentos_propuesta";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_observaciones_propuesta" ON "observaciones_propuesta";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_criterios_rubrica" ON "criterios_rubrica";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_evaluaciones" ON "evaluaciones";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_inscripciones_actividad" ON "inscripciones_actividad";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_asistencias" ON "asistencias";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_codigos_qr" ON "codigos_qr";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_preguntas_encuesta" ON "preguntas_encuesta";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_participaciones_encuesta" ON "participaciones_encuesta";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_opciones_pregunta_encuesta" ON "opciones_pregunta_encuesta";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_respuestas_encuesta" ON "respuestas_encuesta";
--rollback DROP TRIGGER IF EXISTS "trg_memoria_historica_gastos_ejecutados" ON "gastos_ejecutados";
--rollback DROP FUNCTION IF EXISTS "fn_memoria_historica_inmutable"();
--rollback DROP FUNCTION IF EXISTS "fn_evento_archivado"(INT);
--rollback DROP FUNCTION IF EXISTS "fn_evento_de_fila"(TEXT, JSONB);
