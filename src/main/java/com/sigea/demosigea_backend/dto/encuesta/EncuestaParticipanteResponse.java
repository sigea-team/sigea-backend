package com.sigea.demosigea_backend.dto.encuesta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sigea.demosigea_backend.model.EstadoEncuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Encuesta habilitada para el participante (HU-25, Criterios 1 y 3). En el listado no trae las preguntas; en el
 * formulario sí.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Encuesta de satisfacción habilitada para el participante")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EncuestaParticipanteResponse(
        @Schema(example = "7") Long encuestaId,
        @Schema(example = "1") Long eventoId,
        @Schema(example = "Semana de Ingeniería de Sistemas") String eventoNombre,
        @Schema(example = "25") Long actividadId,
        @Schema(example = "Taller de Spring Boot") String actividadNombre,
        @Schema(description = "evento o actividad", example = "actividad") String ambito,
        @Schema(example = "¿Cómo te pareció el taller?") String titulo,
        String descripcion,
        @Schema(description = "Si es true, las respuestas no quedan asociadas al participante", example = "true")
        boolean anonima,
        @Schema(example = "publicada") EstadoEncuesta estado,
        LocalDateTime fechaCierre,
        @Schema(description = "true si todavía se puede responder", example = "true") boolean recibeRespuestas,
        @Schema(description = "true si el participante ya la respondió", example = "false") boolean respondida,
        LocalDateTime fechaRespuesta,
        @Schema(example = "6") int totalPreguntas,
        @Schema(description = "Solo en el formulario") List<PreguntaEncuestaResponse> preguntas
) {
}
