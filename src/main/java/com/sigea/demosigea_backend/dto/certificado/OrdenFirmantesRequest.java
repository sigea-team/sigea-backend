package com.sigea.demosigea_backend.dto.certificado;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Nuevo orden de los firmantes, de izquierda a derecha (HU-26). Debe incluir a todos los firmantes del evento.
 *
 * @param firmanteIds IDs de los firmantes en el orden deseado.
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Orden de los firmantes de izquierda a derecha")
public record OrdenFirmantesRequest(
        @Schema(description = "IDs de todos los firmantes del evento, en orden", example = "[5, 4]")
        @NotEmpty(message = "Indique el orden de los firmantes.")
        List<@NotNull Long> firmanteIds
) {
}
