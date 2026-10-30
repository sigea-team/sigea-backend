package com.sigea.demosigea_backend.dto.propuesta;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de conceder la autorización explícita de edición (HU-14, Criterio 5).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resultado de la autorización de edición de una propuesta")
public record AutorizacionEdicionResponse(

        @Schema(description = "ID de la propuesta", example = "15")
        Long propuestaId,

        @Schema(description = "Indica si la propuesta tiene una autorización de edición pendiente de usar")
        Boolean edicionAutorizada,

        @Schema(description = "Motivo registrado")
        String motivo,

        @Schema(description = "Mensaje de confirmación")
        String mensaje
) {
}
