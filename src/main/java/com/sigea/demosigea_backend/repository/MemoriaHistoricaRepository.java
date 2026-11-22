package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.MemoriaHistorica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la memoria histórica de los eventos (HU-28, RF55, CU-32).
 * <p>
 * La consolidación se hace con consultas nativas de PostgreSQL que leen todas las tablas del evento en una sola
 * pasada: así el contenido guardado corresponde exactamente a la base de datos en el momento del cierre, sin cargar
 * cada entidad en memoria. Las consultas no usan el operador {@code ::} (se usa {@code CAST}) para no confundir el
 * análisis de parámetros con nombre.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public interface MemoriaHistoricaRepository extends JpaRepository<MemoriaHistorica, Long> {

    /** Nombre de la variable de sesión que habilita la eliminación con autorización especial (changeset hu28-004). */
    String VARIABLE_ELIMINACION_HISTORICA = "sigea.eliminacion_historica";

    /** Memoria del evento, con quien la cerró. */
    @Query("SELECT m FROM MemoriaHistorica m LEFT JOIN FETCH m.cerradaPor WHERE m.evento.id = :eventoId")
    Optional<MemoriaHistorica> findByEventoId(@Param("eventoId") Long eventoId);

    boolean existsByEvento_Id(Long eventoId);

    /**
     * Eventos de la memoria histórica, del más reciente al más antiguo (Criterio 2).
     *
     * @param semestre  semestre exacto, o {@code null}.
     * @param familiaId evento base: devuelve el evento base y todas sus ediciones; o {@code null}.
     * @param texto     patrón en minúsculas para el nombre ({@code %texto%}), o {@code null}.
     */
    @Query("""
            SELECT m FROM MemoriaHistorica m
            JOIN FETCH m.evento e
            LEFT JOIN FETCH e.tipoEvento
            LEFT JOIN FETCH e.eventoBase
            LEFT JOIN FETCH m.cerradaPor
            WHERE (:semestre IS NULL OR e.semestre = :semestre)
              AND (:familiaId IS NULL OR e.id = :familiaId OR e.eventoBase.id = :familiaId)
              AND (:texto IS NULL OR LOWER(e.nombre) LIKE :texto)
            ORDER BY e.fechaInicio DESC, e.id DESC
            """)
    List<MemoriaHistorica> buscar(@Param("semestre") String semestre,
                                  @Param("familiaId") Long familiaId,
                                  @Param("texto") String texto);

    /**
     * Procesos pendientes del evento al cierre (CU-32, flujo alterno 1.1).
     */
    @Query(value = """
            SELECT
                (SELECT COUNT(*) FROM asignaciones_evaluacion a
                 JOIN propuestas p ON p.propuesta_id = a.propuesta_id
                 JOIN convocatorias c ON c.convocatoria_id = p.convocatoria_id
                 WHERE c.evento_id = :eventoId AND a.estado IN ('pendiente', 'en_proceso')) AS "evaluacionesSinFinalizar",
                (SELECT COUNT(*) FROM propuestas p
                 JOIN convocatorias c ON c.convocatoria_id = p.convocatoria_id
                 WHERE c.evento_id = :eventoId AND p.estado IN ('recibida', 'en_evaluacion')) AS "propuestasSinDecision",
                (SELECT COUNT(*) FROM propuestas p
                 JOIN convocatorias c ON c.convocatoria_id = p.convocatoria_id
                 WHERE c.evento_id = :eventoId AND p.estado = 'ajustes') AS "propuestasConAjustesPendientes",
                (SELECT COUNT(*) FROM actividades a
                 WHERE a.evento_id = :eventoId AND a.estado = 'por_programar') AS "actividadesPorProgramar",
                (SELECT COUNT(*) FROM encuestas e
                 WHERE e.evento_id = :eventoId AND e.estado = 'publicada') AS "encuestasAbiertas",
                (SELECT COUNT(DISTINCT i.persona_id) FROM asistencias a
                 JOIN inscripciones_evento i ON i.inscripcion_id = a.inscripcion_id
                 WHERE i.evento_id = :eventoId
                   AND NOT EXISTS (SELECT 1 FROM certificados ce
                                   WHERE ce.evento_id = :eventoId AND ce.persona_id = i.persona_id
                                     AND ce.tipo_certificado = 'asistente')) AS "certificadosSinGenerar"
            """, nativeQuery = true)
    ConteoPendientes contarPendientes(@Param("eventoId") Long eventoId);

    /** IDs de las encuestas publicadas del evento, que se cierran con el cierre formal. */
    @Query(value = """
            SELECT CAST(e.encuesta_id AS BIGINT) FROM encuestas e
            WHERE e.evento_id = :eventoId AND e.estado = 'publicada'
            ORDER BY e.encuesta_id
            """, nativeQuery = true)
    List<Long> idsEncuestasPublicadas(@Param("eventoId") Long eventoId);

    /**
     * Calcula y guarda los indicadores del evento al cierre (los que no tienen dato, como la tasa de asistencia sin
     * inscritos, no se guardan).
     *
     * @return cantidad de indicadores guardados.
     */
    @Modifying
    @Query(value = """
            INSERT INTO indicadores (evento_id, codigo, nombre, valor, unidad, fecha_calculo)
            SELECT :eventoId, x.codigo, x.nombre, x.valor, x.unidad, :fechaCalculo
            FROM (
                SELECT b.*,
                       (SELECT COALESCE(SUM(g.valor), 0) FROM gastos_ejecutados g
                        JOIN rubros_presupuestales r ON r.rubro_id = g.rubro_id WHERE r.evento_id = :eventoId) AS ejecutado,
                       (SELECT pa.valor_total_aprobado FROM presupuestos_aprobados pa
                        WHERE pa.evento_id = :eventoId AND pa.estado = 'aprobado'
                        ORDER BY pa.fecha_aprobacion DESC LIMIT 1) AS aprobado
                FROM (
                    SELECT
                        (SELECT COUNT(*) FROM inscripciones_evento i WHERE i.evento_id = :eventoId) AS inscritos,
                        (SELECT COUNT(DISTINCT i.persona_id) FROM asistencias a
                         JOIN inscripciones_evento i ON i.inscripcion_id = a.inscripcion_id WHERE i.evento_id = :eventoId) AS asistentes,
                        (SELECT COUNT(*) FROM asistencias a
                         JOIN inscripciones_evento i ON i.inscripcion_id = a.inscripcion_id WHERE i.evento_id = :eventoId) AS registros,
                        (SELECT COUNT(*) FROM actividades a
                         WHERE a.evento_id = :eventoId AND a.estado NOT IN ('cancelada', 'por_programar')) AS realizadas,
                        (SELECT COUNT(*) FROM actividades a WHERE a.evento_id = :eventoId AND a.estado = 'cancelada') AS canceladas,
                        (SELECT COUNT(*) FROM propuestas p
                         JOIN convocatorias c ON c.convocatoria_id = p.convocatoria_id WHERE c.evento_id = :eventoId) AS propuestas,
                        (SELECT COUNT(*) FROM propuestas p
                         JOIN convocatorias c ON c.convocatoria_id = p.convocatoria_id
                         WHERE c.evento_id = :eventoId AND p.estado IN ('aprobada', 'ajustes')) AS aprobadas,
                        (SELECT COUNT(*) FROM propuestas p
                         JOIN convocatorias c ON c.convocatoria_id = p.convocatoria_id
                         WHERE c.evento_id = :eventoId AND p.estado = 'rechazada') AS rechazadas,
                        (SELECT COUNT(DISTINCT pc.persona_id) FROM participaciones_conferencista pc
                         WHERE pc.evento_id = :eventoId) AS conferencistas,
                        (SELECT COUNT(*) FROM participaciones_encuesta pe
                         JOIN encuestas e ON e.encuesta_id = pe.encuesta_id WHERE e.evento_id = :eventoId) AS respuestas_encuesta,
                        (SELECT ROUND(AVG(100.0 * (r.valor_escala - q.escala_minima) / (q.escala_maxima - q.escala_minima)), 2)
                         FROM respuestas_encuesta r
                         JOIN preguntas_encuesta q ON q.pregunta_id = r.pregunta_id
                         JOIN encuestas e ON e.encuesta_id = q.encuesta_id
                         WHERE e.evento_id = :eventoId AND r.valor_escala IS NOT NULL
                           AND q.escala_maxima > q.escala_minima) AS satisfaccion
                ) b
            ) t
            CROSS JOIN LATERAL (VALUES
                ('INSCRITOS', 'Participantes inscritos', CAST(t.inscritos AS NUMERIC), 'personas'),
                ('ASISTENTES', 'Participantes con asistencia registrada', CAST(t.asistentes AS NUMERIC), 'personas'),
                ('TASA_ASISTENCIA', 'Tasa de asistencia de los inscritos',
                    CASE WHEN t.inscritos = 0 THEN NULL ELSE ROUND(100.0 * t.asistentes / t.inscritos, 2) END, '%'),
                ('ASISTENCIAS_REGISTRADAS', 'Registros de asistencia a actividades', CAST(t.registros AS NUMERIC), 'registros'),
                ('ACTIVIDADES_REALIZADAS', 'Actividades programadas (no canceladas)', CAST(t.realizadas AS NUMERIC), 'actividades'),
                ('ACTIVIDADES_CANCELADAS', 'Actividades canceladas', CAST(t.canceladas AS NUMERIC), 'actividades'),
                ('PROPUESTAS_RECIBIDAS', 'Propuestas recibidas', CAST(t.propuestas AS NUMERIC), 'propuestas'),
                ('PROPUESTAS_APROBADAS', 'Propuestas aprobadas (incluye con ajustes)', CAST(t.aprobadas AS NUMERIC), 'propuestas'),
                ('PROPUESTAS_RECHAZADAS', 'Propuestas rechazadas', CAST(t.rechazadas AS NUMERIC), 'propuestas'),
                ('TASA_APROBACION', 'Tasa de aprobación de propuestas',
                    CASE WHEN t.propuestas = 0 THEN NULL ELSE ROUND(100.0 * t.aprobadas / t.propuestas, 2) END, '%'),
                ('CONFERENCISTAS', 'Conferencistas y ponentes', CAST(t.conferencistas AS NUMERIC), 'personas'),
                ('RESPUESTAS_ENCUESTAS', 'Respuestas a encuestas de satisfacción', CAST(t.respuestas_encuesta AS NUMERIC), 'respuestas'),
                ('SATISFACCION_PROMEDIO', 'Satisfacción promedio (preguntas de escala)', t.satisfaccion, '%'),
                ('PRESUPUESTO_APROBADO', 'Presupuesto aprobado', t.aprobado, 'COP'),
                ('PRESUPUESTO_EJECUTADO', 'Presupuesto ejecutado', CAST(t.ejecutado AS NUMERIC), 'COP'),
                ('EJECUCION_PRESUPUESTAL', 'Porcentaje de ejecución presupuestal',
                    CASE WHEN t.aprobado IS NULL OR t.aprobado = 0 THEN NULL ELSE ROUND(100.0 * t.ejecutado / t.aprobado, 2) END, '%')
            ) AS x(codigo, nombre, valor, unidad)
            WHERE x.valor IS NOT NULL
            """, nativeQuery = true)
    int insertarIndicadores(@Param("eventoId") Long eventoId, @Param("fechaCalculo") LocalDateTime fechaCalculo);

    /**
     * Información consolidada del evento, en JSON (Criterio 1). Incluye los indicadores ya guardados.
     *
     * @param pendientes JSON (arreglo) con los procesos pendientes confirmados al cierre.
     */
    @Query(value = """
            SELECT CAST(jsonb_build_object(
                'version', 1,
                'evento', (
                    SELECT jsonb_build_object(
                        'id', e.evento_id, 'nombre', e.nombre, 'objetivo', e.objetivo, 'descripcion', e.descripcion,
                        'tipoEvento', te.nombre, 'modalidad', e.modalidad, 'fechaInicio', e.fecha_inicio, 'fechaFin', e.fecha_fin,
                        'semestre', e.semestre, 'estado', e.estado, 'eventoBaseId', e.evento_base_id, 'eventoBaseNombre', b.nombre)
                    FROM eventos e
                    LEFT JOIN tipos_evento te ON te.tipo_evento_id = e.tipo_evento_id
                    LEFT JOIN eventos b ON b.evento_id = e.evento_base_id
                    WHERE e.evento_id = :eventoId),
                'comiteOrganizador', COALESCE((
                    SELECT jsonb_agg(jsonb_build_object(
                        'personaId', p.persona_id, 'nombre', p.nombres || ' ' || p.apellidos, 'correo', p.correo,
                        'rol', co.rol_comite, 'activo', co.activo, 'fechaAsignacion', co.fecha_asignacion,
                        'fechaRetiro', co.fecha_retiro) ORDER BY co.activo DESC, co.fecha_asignacion, co.comite_id)
                    FROM comite_organizador co JOIN personas p ON p.persona_id = co.persona_id
                    WHERE co.evento_id = :eventoId), CAST('[]' AS jsonb)),
                'lineasTematicas', COALESCE((
                    SELECT jsonb_agg(jsonb_build_object('id', l.linea_tematica_id, 'nombre', l.nombre, 'descripcion', l.descripcion)
                        ORDER BY l.nombre, l.linea_tematica_id)
                    FROM lineas_tematicas l WHERE l.evento_id = :eventoId), CAST('[]' AS jsonb)),
                'tiposActividad', COALESCE((
                    SELECT jsonb_agg(jsonb_build_object('id', t.tipo_actividad_id, 'nombre', t.nombre, 'descripcion', t.descripcion)
                        ORDER BY t.nombre, t.tipo_actividad_id)
                    FROM tipos_actividad t WHERE t.evento_id = :eventoId), CAST('[]' AS jsonb)),
                'presupuesto', jsonb_build_object(
                    'rubros', COALESCE((
                        SELECT jsonb_agg(jsonb_build_object(
                            'id', r.rubro_id, 'nombre', r.nombre, 'cantidad', r.cantidad,
                            'valorUnitarioProyectado', r.valor_unitario_proyectado,
                            'totalProyectado', r.cantidad * r.valor_unitario_proyectado, 'activo', r.activo,
                            'ejecutado', COALESCE((SELECT SUM(g.valor) FROM gastos_ejecutados g WHERE g.rubro_id = r.rubro_id), 0))
                            ORDER BY r.nombre, r.rubro_id)
                        FROM rubros_presupuestales r WHERE r.evento_id = :eventoId), CAST('[]' AS jsonb)),
                    'totalProyectado', COALESCE((
                        SELECT SUM(r.cantidad * r.valor_unitario_proyectado) FROM rubros_presupuestales r
                        WHERE r.evento_id = :eventoId AND r.activo), 0),
                    'aprobado', (
                        SELECT jsonb_build_object('valor', pa.valor_total_aprobado, 'fechaAprobacion', pa.fecha_aprobacion,
                                                  'evidenciaUrl', pa.evidencia_url)
                        FROM presupuestos_aprobados pa
                        WHERE pa.evento_id = :eventoId AND pa.estado = 'aprobado'
                        ORDER BY pa.fecha_aprobacion DESC LIMIT 1),
                    'totalEjecutado', COALESCE((
                        SELECT SUM(g.valor) FROM gastos_ejecutados g
                        JOIN rubros_presupuestales r ON r.rubro_id = g.rubro_id WHERE r.evento_id = :eventoId), 0)),
                'convocatorias', COALESCE((
                    SELECT jsonb_agg(jsonb_build_object(
                        'id', c.convocatoria_id, 'titulo', c.titulo, 'estado', c.estado,
                        'fechaApertura', c.fecha_apertura, 'fechaCierre', c.fecha_cierre,
                        'umbralAceptacion', c.umbral_aceptacion, 'umbralAjustes', c.umbral_ajustes,
                        'propuestas', COALESCE((
                            SELECT jsonb_agg(jsonb_build_object(
                                'id', pr.propuesta_id, 'titulo', pr.titulo, 'estado', pr.estado, 'fechaEnvio', pr.fecha_envio,
                                'lineaTematica', lt.nombre,
                                'autores', COALESCE((
                                    SELECT jsonb_agg(jsonb_build_object(
                                        'personaId', pe.persona_id, 'nombre', pe.nombres || ' ' || pe.apellidos,
                                        'rol', pa.rol, 'orden', pa.orden) ORDER BY pa.orden, pe.persona_id)
                                    FROM propuestas_autores pa JOIN personas pe ON pe.persona_id = pa.persona_id
                                    WHERE pa.propuesta_id = pr.propuesta_id), CAST('[]' AS jsonb)),
                                'puntuacionPonderada', re.puntuacion_ponderada,
                                'clasificacion', re.clasificacion,
                                'versionDefinitiva', (
                                    SELECT v.numero_version FROM versiones_propuesta v
                                    WHERE v.propuesta_id = pr.propuesta_id AND v.es_definitiva
                                    ORDER BY v.numero_version DESC LIMIT 1),
                                'evaluacionesCompletadas', (
                                    SELECT COUNT(*) FROM asignaciones_evaluacion a
                                    WHERE a.propuesta_id = pr.propuesta_id AND a.estado = 'completada'),
                                'evaluacionesAsignadas', (
                                    SELECT COUNT(*) FROM asignaciones_evaluacion a WHERE a.propuesta_id = pr.propuesta_id))
                                ORDER BY pr.fecha_envio, pr.propuesta_id)
                            FROM propuestas pr
                            LEFT JOIN lineas_tematicas lt ON lt.linea_tematica_id = pr.linea_tematica_id
                            LEFT JOIN resultados_evaluacion re ON re.propuesta_id = pr.propuesta_id
                            WHERE pr.convocatoria_id = c.convocatoria_id), CAST('[]' AS jsonb)))
                        ORDER BY c.fecha_apertura, c.convocatoria_id)
                    FROM convocatorias c WHERE c.evento_id = :eventoId), CAST('[]' AS jsonb)),
                'conferencistas', COALESCE((
                    SELECT jsonb_agg(jsonb_build_object(
                        'personaId', p.persona_id, 'nombre', p.nombres || ' ' || p.apellidos, 'correo', p.correo,
                        'tipoParticipacion', pc.tipo_participacion, 'tema', pc.tema) ORDER BY p.apellidos, p.nombres, pc.participacion_id)
                    FROM participaciones_conferencista pc JOIN personas p ON p.persona_id = pc.persona_id
                    WHERE pc.evento_id = :eventoId), CAST('[]' AS jsonb)),
                'agenda', jsonb_build_object(
                    'diasPublicados', COALESCE((
                        SELECT jsonb_agg(ap.fecha_dia ORDER BY ap.fecha_dia)
                        FROM agendas_publicadas ap WHERE ap.evento_id = :eventoId AND ap.estado = 'publicada'), CAST('[]' AS jsonb)),
                    'actividades', COALESCE((
                        SELECT jsonb_agg(jsonb_build_object(
                            'id', a.actividad_id, 'nombre', a.nombre, 'tipoActividad', ta.nombre, 'lineaTematica', lt.nombre,
                            'fecha', a.fecha, 'horaInicio', a.hora_inicio, 'horaFin', a.hora_fin,
                            'sala', s.nombre, 'tipoSala', s.tipo, 'modalidad', a.modalidad,
                            'ponente', CASE WHEN po.persona_id IS NULL THEN NULL ELSE po.nombres || ' ' || po.apellidos END,
                            'estado', a.estado, 'propuestaId', a.propuesta_id,
                            'inscritos', (SELECT COUNT(*) FROM inscripciones_actividad ia WHERE ia.actividad_id = a.actividad_id),
                            'asistentes', (SELECT COUNT(*) FROM asistencias asi WHERE asi.actividad_id = a.actividad_id))
                            ORDER BY a.fecha NULLS LAST, a.hora_inicio NULLS LAST, a.actividad_id)
                        FROM actividades a
                        LEFT JOIN tipos_actividad ta ON ta.tipo_actividad_id = a.tipo_actividad_id
                        LEFT JOIN lineas_tematicas lt ON lt.linea_tematica_id = a.linea_tematica_id
                        LEFT JOIN salas s ON s.sala_id = a.sala_id
                        LEFT JOIN personas po ON po.persona_id = a.ponente_id
                        WHERE a.evento_id = :eventoId), CAST('[]' AS jsonb))),
                'participantes', COALESCE((
                    SELECT jsonb_agg(jsonb_build_object(
                        'inscripcionId', i.inscripcion_id, 'personaId', p.persona_id,
                        'nombre', p.nombres || ' ' || p.apellidos, 'tipoDocumento', p.tipo_documento,
                        'numeroDocumento', p.numero_documento, 'correo', p.correo,
                        'categoria', i.categoria_participante, 'fechaInscripcion', i.fecha_inscripcion,
                        'actividadesInscritas', (SELECT COUNT(*) FROM inscripciones_actividad ia WHERE ia.inscripcion_id = i.inscripcion_id),
                        'actividadesAsistidas', (SELECT COUNT(*) FROM asistencias asi WHERE asi.inscripcion_id = i.inscripcion_id))
                        ORDER BY p.apellidos, p.nombres, i.inscripcion_id)
                    FROM inscripciones_evento i JOIN personas p ON p.persona_id = i.persona_id
                    WHERE i.evento_id = :eventoId), CAST('[]' AS jsonb)),
                'asistencia', (
                    SELECT jsonb_build_object(
                        'registros', COUNT(asi.asistencia_id),
                        'personasConAsistencia', COUNT(DISTINCT i.persona_id),
                        'porQr', COUNT(asi.asistencia_id) FILTER (WHERE asi.metodo_registro = 'qr'),
                        'manuales', COUNT(asi.asistencia_id) FILTER (WHERE asi.metodo_registro = 'manual'))
                    FROM asistencias asi JOIN inscripciones_evento i ON i.inscripcion_id = asi.inscripcion_id
                    WHERE i.evento_id = :eventoId),
                'encuestas', COALESCE((
                    SELECT jsonb_agg(jsonb_build_object(
                        'id', en.encuesta_id, 'titulo', en.titulo, 'actividad', act.nombre, 'anonima', en.anonima,
                        'estado', en.estado, 'fechaPublicacion', en.fecha_publicacion, 'fechaCierre', en.fecha_cierre,
                        'respuestas', (SELECT COUNT(*) FROM participaciones_encuesta pe WHERE pe.encuesta_id = en.encuesta_id),
                        'preguntas', COALESCE((
                            SELECT jsonb_agg(jsonb_build_object(
                                'texto', q.texto, 'tipo', q.tipo_respuesta, 'escalaMinima', q.escala_minima,
                                'escalaMaxima', q.escala_maxima,
                                'respuestas', (SELECT COUNT(*) FROM respuestas_encuesta r WHERE r.pregunta_id = q.pregunta_id),
                                'promedio', (SELECT ROUND(AVG(r.valor_escala), 2) FROM respuestas_encuesta r
                                             WHERE r.pregunta_id = q.pregunta_id AND r.valor_escala IS NOT NULL),
                                'opciones', COALESCE((
                                    SELECT jsonb_agg(jsonb_build_object(
                                        'texto', o.texto,
                                        'respuestas', (SELECT COUNT(*) FROM respuestas_encuesta r WHERE r.opcion_id = o.opcion_id))
                                        ORDER BY o.orden, o.opcion_id)
                                    FROM opciones_pregunta_encuesta o WHERE o.pregunta_id = q.pregunta_id), CAST('[]' AS jsonb)))
                                ORDER BY q.orden, q.pregunta_id)
                            FROM preguntas_encuesta q WHERE q.encuesta_id = en.encuesta_id), CAST('[]' AS jsonb)))
                        ORDER BY en.fecha_creacion, en.encuesta_id)
                    FROM encuestas en LEFT JOIN actividades act ON act.actividad_id = en.actividad_id
                    WHERE en.evento_id = :eventoId), CAST('[]' AS jsonb)),
                'certificados', COALESCE((
                    SELECT jsonb_object_agg(x.tipo_certificado, x.cantidad)
                    FROM (SELECT ce.tipo_certificado, COUNT(*) AS cantidad FROM certificados ce
                          WHERE ce.evento_id = :eventoId GROUP BY ce.tipo_certificado) x), CAST('{}' AS jsonb)),
                'indicadores', COALESCE((
                    SELECT jsonb_agg(jsonb_build_object('codigo', ind.codigo, 'nombre', ind.nombre, 'valor', ind.valor,
                                                        'unidad', ind.unidad, 'fechaCalculo', ind.fecha_calculo)
                        ORDER BY ind.indicador_id)
                    FROM indicadores ind WHERE ind.evento_id = :eventoId), CAST('[]' AS jsonb)),
                'pendientesAlCierre', CAST(:pendientes AS jsonb)
            ) AS text)
            """, nativeQuery = true)
    String construirContenido(@Param("eventoId") Long eventoId, @Param("pendientes") String pendientes);

    /**
     * Habilita la eliminación de la información histórica de un solo evento (Criterio 3, con autorización especial).
     * Vale solo dentro de la transacción actual ({@code set_config(..., true)}): los triggers dejan pasar únicamente
     * los DELETE de filas de ese evento; las de otros eventos de la memoria histórica siguen protegidas.
     */
    @Query(value = "SELECT set_config('" + VARIABLE_ELIMINACION_HISTORICA + "', CAST(:eventoId AS TEXT), true)",
            nativeQuery = true)
    String autorizarEliminacionHistorica(@Param("eventoId") Long eventoId);

    /** Quita la autorización de eliminación antes de terminar la transacción (no depende solo del fin de esta). */
    @Query(value = "SELECT set_config('" + VARIABLE_ELIMINACION_HISTORICA + "', '', true)", nativeQuery = true)
    String revocarEliminacionHistorica();

    // Eliminación con autorización especial, en orden: algunas claves foráneas no tienen ON DELETE CASCADE
    // (asistencias, certificados y encuestas → actividades; actividades → propuestas; asignaciones → comité evaluador),
    // así que borrar solo el evento fallaría.

    @Modifying
    @Query(value = """
            DELETE FROM asistencias
            WHERE inscripcion_id IN (SELECT i.inscripcion_id FROM inscripciones_evento i WHERE i.evento_id = :eventoId)
               OR actividad_id IN (SELECT a.actividad_id FROM actividades a WHERE a.evento_id = :eventoId)
            """, nativeQuery = true)
    int eliminarAsistencias(@Param("eventoId") Long eventoId);

    @Modifying
    @Query(value = """
            DELETE FROM certificados
            WHERE evento_id = :eventoId
               OR actividad_id IN (SELECT a.actividad_id FROM actividades a WHERE a.evento_id = :eventoId)
            """, nativeQuery = true)
    int eliminarCertificados(@Param("eventoId") Long eventoId);

    @Modifying
    @Query(value = """
            DELETE FROM encuestas
            WHERE evento_id = :eventoId
               OR actividad_id IN (SELECT a.actividad_id FROM actividades a WHERE a.evento_id = :eventoId)
            """, nativeQuery = true)
    int eliminarEncuestas(@Param("eventoId") Long eventoId);

    @Modifying
    @Query(value = "DELETE FROM actividades WHERE evento_id = :eventoId", nativeQuery = true)
    int eliminarActividades(@Param("eventoId") Long eventoId);

    @Modifying
    @Query(value = """
            DELETE FROM propuestas
            WHERE convocatoria_id IN (SELECT c.convocatoria_id FROM convocatorias c WHERE c.evento_id = :eventoId)
            """, nativeQuery = true)
    int eliminarPropuestas(@Param("eventoId") Long eventoId);

    /** Borra el evento; el resto de su información se borra en cascada (incluida la memoria histórica). */
    @Modifying
    @Query(value = "DELETE FROM eventos WHERE evento_id = :eventoId", nativeQuery = true)
    int eliminarEvento(@Param("eventoId") Long eventoId);

    /**
     * Conteos de procesos pendientes al cierre. Los alias de la consulta van entre comillas para conservar las
     * mayúsculas.
     */
    interface ConteoPendientes {
        Long getEvaluacionesSinFinalizar();

        Long getPropuestasSinDecision();

        Long getPropuestasConAjustesPendientes();

        Long getActividadesPorProgramar();

        Long getEncuestasAbiertas();

        Long getCertificadosSinGenerar();
    }
}
