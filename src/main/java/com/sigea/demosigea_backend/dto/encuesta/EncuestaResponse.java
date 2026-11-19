package com.sigea.demosigea_backend.dto.encuesta;

import com.sigea.demosigea_backend.model.EstadoEncuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Encuesta vista por el administrador u organizador (HU-25).
 *
 * @param totalHabilitados personas que asistieron al evento o a la actividad y por eso pueden responder.
 * @param totalRespuestas  personas que ya respondieron.
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Encuesta de satisfacción (vista del organizador)")
public record EncuestaResponse(
        @Schema(example = "7") Long encuestaId,
        @Schema(example = "1") Long eventoId,
        @Schema(example = "Semana de Ingeniería de Sistemas") String eventoNombre,
        @Schema(description = "null si es la encuesta general del evento", example = "25") Long actividadId,
        @Schema(example = "Taller de Spring Boot") String actividadNombre,
        @Schema(description = "evento o actividad", example = "actividad") String ambito,
        @Schema(example = "¿Cómo te pareció el taller?") String titulo,
        String descripcion,
        @Schema(example = "true") boolean anonima,
        @Schema(example = "publicada") EstadoEncuesta estado,
        @Schema(description = "true si está publicada y no ha pasado la fecha de cierre", example = "true")
        boolean recibeRespuestas,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaPublicacion,
        LocalDateTime fechaCierre,
        @Schema(example = "48") long totalHabilitados,
        @Schema(example = "31") long totalRespuestas,
        List<PreguntaEncuestaResponse> preguntas
) {
}
