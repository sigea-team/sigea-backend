package com.sigea.demosigea_backend.dto.encuesta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Respuestas del participante a la encuesta (HU-25, Criterio 2). Las preguntas opcionales se pueden omitir.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Respuestas a la encuesta")
public record ResponderEncuestaRequest(
        @NotNull(message = "Las respuestas son obligatorias")
        @Size(max = 50, message = "Hay más respuestas que preguntas")
        List<@Valid @NotNull RespuestaPreguntaRequest> respuestas
) {
}
