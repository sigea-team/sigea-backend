package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Indicador;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Consultas de solo lectura del dashboard de indicadores de un evento (HU-29, RF50, RF51, CU-25).
 * <p>
 * Todo se calcula al vuelo con consultas agregadas sobre las tablas del evento; nada se guarda. Así, cada vez que se
 * piden los indicadores o se refresca el dashboard, reflejan la información más reciente (Criterio 3). Los
 * indicadores generales usan el mismo SQL del cierre formal de HU-28 ({@link ConsultasIndicadoresEvento}).
 * </p>
 * <p>
 * Las consultas devuelven filas {@code Object[]} (el servicio convierte los tipos) y no usan el operador {@code ::}
 * para no confundir el análisis de parámetros con nombre.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public interface IndicadorEventoRepository extends Repository<Indicador, Long> {

    /**
     * Indicadores generales del evento, en orden: {@code [codigo, nombre, valor, unidad]}. {@code valor} es
     * {@code null} si no se puede calcular.
     */
    @Query(value = "SELECT c.codigo, c.nombre, c.valor, c.unidad FROM ("
            + ConsultasIndicadoresEvento.CALCULO
            + ") c ORDER BY c.orden", nativeQuery = true)
    List<Object[]> calcular(@Param("eventoId") Long eventoId);

    /** Inscripciones al evento por categoría de participante: {@code [categoria, cantidad]}. */
    @Query(value = """
            SELECT i.categoria_participante, COUNT(*)
            FROM inscripciones_evento i
            WHERE i.evento_id = :eventoId
            GROUP BY i.categoria_participante
            ORDER BY COUNT(*) DESC, i.categoria_participante
            """, nativeQuery = true)
    List<Object[]> inscritosPorCategoria(@Param("eventoId") Long eventoId);

    /** Inscripciones al evento por día en que se hicieron: {@code [fecha, cantidad]}. */
    @Query(value = """
            SELECT CAST(i.fecha_inscripcion AS DATE) AS dia, COUNT(*)
            FROM inscripciones_evento i
            WHERE i.evento_id = :eventoId
            GROUP BY CAST(i.fecha_inscripcion AS DATE)
            ORDER BY dia
            """, nativeQuery = true)
    List<Object[]> inscripcionesPorFecha(@Param("eventoId") Long eventoId);

    /**
     * Inscritos y asistentes de cada actividad programada (no cancelada ni por programar):
     * {@code [actividadId, nombre, fecha, horaInicio, inscritos, asistentes]}.
     * <p>
     * Solo cuenta inscripciones y asistencias cuya inscripción al evento es de este mismo evento (igual que los
     * indicadores generales), aunque la base de datos no impide por sí sola mezclar eventos. Por el índice único
     * {@code (inscripcion_id, actividad_id)} de cada tabla, cada registro es una persona distinta en la actividad.
     * </p>
     */
    @Query(value = """
            SELECT a.actividad_id, a.nombre, a.fecha, a.hora_inicio,
                   (SELECT COUNT(*) FROM inscripciones_actividad ia
                    JOIN inscripciones_evento ie ON ie.inscripcion_id = ia.inscripcion_id AND ie.evento_id = a.evento_id
                    WHERE ia.actividad_id = a.actividad_id) AS inscritos,
                   (SELECT COUNT(*) FROM asistencias s
                    JOIN inscripciones_evento ie ON ie.inscripcion_id = s.inscripcion_id AND ie.evento_id = a.evento_id
                    WHERE s.actividad_id = a.actividad_id) AS asistentes
            FROM actividades a
            WHERE a.evento_id = :eventoId AND a.estado NOT IN ('cancelada', 'por_programar')
            ORDER BY a.fecha, a.hora_inicio, a.actividad_id
            """, nativeQuery = true)
    List<Object[]> asistenciaPorActividad(@Param("eventoId") Long eventoId);

    /** Propuestas recibidas en las convocatorias del evento por estado: {@code [estado, cantidad]}. */
    @Query(value = """
            SELECT p.estado, COUNT(*)
            FROM propuestas p
            JOIN convocatorias c ON c.convocatoria_id = p.convocatoria_id
            WHERE c.evento_id = :eventoId
            GROUP BY p.estado
            """, nativeQuery = true)
    List<Object[]> propuestasPorEstado(@Param("eventoId") Long eventoId);

    /**
     * Encuestas de satisfacción publicadas o cerradas del evento, con sus respuestas y la satisfacción promedio de sus
     * preguntas de escala en porcentaje (misma fórmula del indicador SATISFACCION_PROMEDIO):
     * {@code [encuestaId, titulo, estado, nombreActividad, respuestas, satisfaccion]}.
     */
    @Query(value = """
            SELECT e.encuesta_id, e.titulo, e.estado, act.nombre,
                   (SELECT COUNT(*) FROM participaciones_encuesta pe WHERE pe.encuesta_id = e.encuesta_id) AS respuestas,
                   (SELECT ROUND(AVG(100.0 * (r.valor_escala - q.escala_minima) / (q.escala_maxima - q.escala_minima)), 2)
                    FROM respuestas_encuesta r
                    JOIN preguntas_encuesta q ON q.pregunta_id = r.pregunta_id
                    WHERE q.encuesta_id = e.encuesta_id AND r.valor_escala IS NOT NULL
                      AND q.escala_maxima > q.escala_minima) AS satisfaccion
            FROM encuestas e
            LEFT JOIN actividades act ON act.actividad_id = e.actividad_id
            WHERE e.evento_id = :eventoId AND e.estado <> 'borrador'
            ORDER BY COALESCE(e.fecha_publicacion, e.fecha_creacion), e.encuesta_id
            """, nativeQuery = true)
    List<Object[]> satisfaccionPorEncuesta(@Param("eventoId") Long eventoId);

    /**
     * Presupuesto proyectado (cantidad × valor unitario) y ejecutado (gastos registrados) de cada rubro activo:
     * {@code [rubroId, nombre, proyectado, ejecutado]}.
     */
    @Query(value = """
            SELECT r.rubro_id, r.nombre, r.cantidad * r.valor_unitario_proyectado AS proyectado,
                   (SELECT COALESCE(SUM(g.valor), 0) FROM gastos_ejecutados g WHERE g.rubro_id = r.rubro_id) AS ejecutado
            FROM rubros_presupuestales r
            WHERE r.evento_id = :eventoId AND r.activo = TRUE
            ORDER BY r.rubro_id
            """, nativeQuery = true)
    List<Object[]> presupuestoPorRubro(@Param("eventoId") Long eventoId);
}
