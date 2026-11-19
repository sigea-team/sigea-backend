package com.sigea.demosigea_backend.dto.encuesta;

import com.sigea.demosigea_backend.model.EstadoEncuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultados consolidados de una encuesta (HU-25, Criterio 4).
 *
 * @param tasaRespuesta porcentaje de habilitados que respondieron (null si no hay habilitados).
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resultados consolidados de la encuesta")
public record ResultadosEncuestaResponse(
        @Schema(example = "7") Long encuestaId,
        @Schema(example = "¿Cómo te pareció el taller?") String titulo,
        @Schema(example = "1") Long eventoId,
        @Schema(example = "Semana de Ingeniería de Sistemas") String eventoNombre,
        @Schema(example = "25") Long actividadId,
        @Schema(example = "Taller de Spring Boot") String actividadNombre,
        @Schema(example = "actividad") String ambito,
        @Schema(example = "true") boolean anonima,
        @Schema(example = "publicada") EstadoEncuesta estado,
        @Schema(example = "48") long totalHabilitados,
        @Schema(example = "31") long totalRespuestas,
        @Schema(example = "64.6") BigDecimal tasaRespuesta,
        LocalDateTime fechaConsulta,
        List<ResultadoPreguntaResponse> preguntas
) {
}
