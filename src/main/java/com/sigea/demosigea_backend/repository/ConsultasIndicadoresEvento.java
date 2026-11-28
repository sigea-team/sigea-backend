package com.sigea.demosigea_backend.repository;

/**
 * SQL común del cálculo de los indicadores de un evento.
 * <p>
 * Lo usan el cierre formal (HU-28, {@link MemoriaHistoricaRepository#insertarIndicadores}), que guarda los
 * indicadores en {@code indicadores}, y el dashboard (HU-29, {@link IndicadorEventoRepository#calcular}), que los
 * calcula al vuelo sin guardarlos. Así los dos muestran siempre las mismas cifras con las mismas reglas.
 * </p>
 * <p>
 * {@link #CALCULO} es una consulta con el parámetro {@code :eventoId} que devuelve una fila por indicador con las
 * columnas {@code orden, codigo, nombre, valor, unidad}. {@code valor} es {@code NULL} cuando el indicador no se
 * puede calcular (por ejemplo, la tasa de asistencia sin inscritos o la ejecución presupuestal sin presupuesto
 * aprobado). Es una constante de compilación para poder usarla dentro de {@code @Query}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public final class ConsultasIndicadoresEvento {

    /** Indicadores del evento: {@code orden, codigo, nombre, valor, unidad}. */
    public static final String CALCULO = """
            SELECT x.orden, x.codigo, x.nombre, x.valor, x.unidad
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
                (1, 'INSCRITOS', 'Participantes inscritos', CAST(t.inscritos AS NUMERIC), 'personas'),
                (2, 'ASISTENTES', 'Participantes con asistencia registrada', CAST(t.asistentes AS NUMERIC), 'personas'),
                (3, 'TASA_ASISTENCIA', 'Tasa de asistencia de los inscritos',
                    CASE WHEN t.inscritos = 0 THEN NULL ELSE ROUND(100.0 * t.asistentes / t.inscritos, 2) END, '%'),
                (4, 'ASISTENCIAS_REGISTRADAS', 'Registros de asistencia a actividades', CAST(t.registros AS NUMERIC), 'registros'),
                (5, 'ACTIVIDADES_REALIZADAS', 'Actividades programadas (no canceladas)', CAST(t.realizadas AS NUMERIC), 'actividades'),
                (6, 'ACTIVIDADES_CANCELADAS', 'Actividades canceladas', CAST(t.canceladas AS NUMERIC), 'actividades'),
                (7, 'PROPUESTAS_RECIBIDAS', 'Propuestas recibidas', CAST(t.propuestas AS NUMERIC), 'propuestas'),
                (8, 'PROPUESTAS_APROBADAS', 'Propuestas aprobadas (incluye con ajustes)', CAST(t.aprobadas AS NUMERIC), 'propuestas'),
                (9, 'PROPUESTAS_RECHAZADAS', 'Propuestas rechazadas', CAST(t.rechazadas AS NUMERIC), 'propuestas'),
                (10, 'TASA_APROBACION', 'Tasa de aprobación de propuestas',
                    CASE WHEN t.propuestas = 0 THEN NULL ELSE ROUND(100.0 * t.aprobadas / t.propuestas, 2) END, '%'),
                (11, 'CONFERENCISTAS', 'Conferencistas y ponentes', CAST(t.conferencistas AS NUMERIC), 'personas'),
                (12, 'RESPUESTAS_ENCUESTAS', 'Respuestas a encuestas de satisfacción', CAST(t.respuestas_encuesta AS NUMERIC), 'respuestas'),
                (13, 'SATISFACCION_PROMEDIO', 'Satisfacción promedio (preguntas de escala)', t.satisfaccion, '%'),
                (14, 'PRESUPUESTO_APROBADO', 'Presupuesto aprobado', t.aprobado, 'COP'),
                (15, 'PRESUPUESTO_EJECUTADO', 'Presupuesto ejecutado', CAST(t.ejecutado AS NUMERIC), 'COP'),
                (16, 'EJECUCION_PRESUPUESTAL', 'Porcentaje de ejecución presupuestal',
                    CASE WHEN t.aprobado IS NULL OR t.aprobado = 0 THEN NULL ELSE ROUND(100.0 * t.ejecutado / t.aprobado, 2) END, '%')
            ) AS x(orden, codigo, nombre, valor, unidad)
            """;

    private ConsultasIndicadoresEvento() {
    }
}
