package com.sigea.demosigea_backend.dto.convocatoria;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO para modificar la fecha límite de recepción de propuestas de una convocatoria ya publicada
 * (HU-11, Criterio 2).
 *
 * @param fechaCierre Nueva fecha y hora límite (obligatoria, futura y posterior a la fecha de apertura).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Nueva fecha límite de recepción de propuestas de una convocatoria publicada")
@JsonIgnoreProperties(ignoreUnknown = true)
public record FechaCierreRequest(

        @Schema(description = "Nueva fecha y hora límite", example = "2026-11-30T23:59:59")
        @NotNull(message = "La nueva fecha límite de recepción de propuestas es obligatoria.")
        LocalDateTime fechaCierre
) {}
