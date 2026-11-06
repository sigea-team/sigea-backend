package com.sigea.demosigea_backend.dto.comiteevaluador;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio del área de experticia de un evaluador del comité (HU-15).
 *
 * @param areaExperticia nueva área de experticia.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record ActualizarEvaluadorRequest(
        @Schema(description = "Área de experticia", example = "Analítica de datos")
        @NotBlank(message = "El área de experticia es obligatoria.")
        @Size(max = 150, message = "El área de experticia no puede superar los 150 caracteres.")
        String areaExperticia
) {
}
