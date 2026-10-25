package com.sigea.demosigea_backend.dto.conferencista;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Respuesta 409 {@code CONFERENCISTA_EXISTENTE} (HU-12, Criterio 3).
 * <p>
 * Tiene los mismos campos que {@code ErrorResponse} y, además, la persona encontrada para que el
 * frontend ofrezca asociarla directamente al evento en lugar de crear un duplicado.
 * </p>
 */
@Schema(description = "Error 409: el conferencista ya existe; incluye la persona para asociarla")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConflictoConferencistaResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        @Schema(example = "CONFERENCISTA_EXISTENTE") String codigo,
        String path,
        CoincidenciaConferencistaResponse coincidencia
) {
}
