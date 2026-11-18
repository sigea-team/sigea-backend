package com.sigea.demosigea_backend.dto.periodo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Indicadores del programa calculados sobre los eventos que cumplen los filtros (HU-27, Criterios 3 a 5).
 * <p>
 * Las actividades canceladas no se cuentan. Si hay filtro de tipo de actividad, los indicadores de actividades
 * solo cuentan las de ese tipo; los de inscripciones, conferencistas y propuestas son de los eventos encontrados.
 * </p>
 *
 * @param filtros                    Filtros aplicados.
 * @param hayInformacion             false si ningún evento cumple los filtros (Criterio 4).
 * @param mensaje                    Mensaje informativo cuando no hay información.
 * @param totalEventos               Eventos encontrados.
 * @param eventosPorTipo             Eventos por nombre de tipo de evento.
 * @param eventosPorEstado           Eventos por estado.
 * @param totalActividades           Actividades no canceladas.
 * @param actividadesPorTipo         Actividades por nombre de tipo de actividad.
 * @param totalInscripciones         Inscripciones a los eventos.
 * @param participantesUnicos        Personas distintas inscritas.
 * @param inscripcionesPorCategoria  Inscripciones por categoría de participante.
 * @param inscripcionesActividades   Inscripciones a las actividades contadas.
 * @param promedioInscritosPorEvento Inscripciones / eventos, con dos decimales.
 * @param totalConferencistas        Participaciones como conferencista.
 * @param totalPonentes              Participaciones como ponente.
 * @param propuestasRecibidas        Propuestas postuladas a las convocatorias de los eventos.
 * @param propuestasAprobadas        Propuestas en estado {@code aprobada} (incluye las aprobadas con ajustes cuya
 *                                   versión definitiva ya se aprobó, HU-19).
 * @param propuestasConAjustes       Propuestas en estado {@code ajustes}: clasificadas como aprobadas con ajustes
 *                                   (HU-18) y pendientes de la versión definitiva.
 * @param tasaAprobacionPropuestas   Porcentaje de propuestas con resultado positivo
 *                                   ({@code aprobada} + {@code ajustes}) sobre las recibidas (null si no hay propuestas).
 */
@Schema(description = "Indicadores del periodo consultado")
public record IndicadoresPeriodoResponse(
        FiltroPeriodo filtros,
        boolean hayInformacion,
        String mensaje,
        long totalEventos,
        Map<String, Long> eventosPorTipo,
        Map<String, Long> eventosPorEstado,
        long totalActividades,
        Map<String, Long> actividadesPorTipo,
        long totalInscripciones,
        long participantesUnicos,
        Map<String, Long> inscripcionesPorCategoria,
        long inscripcionesActividades,
        BigDecimal promedioInscritosPorEvento,
        long totalConferencistas,
        long totalPonentes,
        long propuestasRecibidas,
        long propuestasAprobadas,
        long propuestasConAjustes,
        BigDecimal tasaAprobacionPropuestas
) {
}
