package com.sigea.demosigea_backend.dto.encuesta;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Respuesta de texto libre (HU-25, Criterio 4). En encuestas anónimas no trae al participante.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Respuesta de texto libre")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaTextoResponse(
        @Schema(example = "Faltó tiempo para las preguntas.") String texto,
        @Schema(example = "2026-11-21") LocalDate fecha,
        @Schema(description = "Solo en encuestas identificadas", example = "María Pérez") String participanteNombre
) {
}
