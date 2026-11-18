package com.sigea.demosigea_backend.dto.encuesta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Respuesta a una pregunta (HU-25, Criterio 2). Según el tipo de la pregunta se usa {@code valorEscala},
 * {@code opcionId} o {@code texto}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Respuesta a una pregunta")
public record RespuestaPreguntaRequest(
        @Schema(example = "40")
        @NotNull(message = "La pregunta es obligatoria")
        Long preguntaId,

        @Schema(description = "Solo escala", example = "4")
        Integer valorEscala,

        @Schema(description = "Solo opcion", example = "12")
        Long opcionId,

        @Schema(description = "Solo texto", example = "Faltó tiempo para las preguntas.")
        @Size(max = 2000, message = "La respuesta no puede superar 2000 caracteres")
        String texto
) {
}
