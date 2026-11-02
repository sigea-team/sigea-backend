package com.sigea.demosigea_backend.dto.propuesta;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Confirmación del envío o modificación de una propuesta (HU-14, Criterios 1 y 3).
 *
 * @param mensaje   Mensaje de confirmación para el autor.
 * @param propuesta Estado actual de la propuesta con sus autores.
 * @param version   Versión creada con sus documentos.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Confirmación de recepción de la propuesta y de la versión creada")
public record EnvioPropuestaResponse(

        @Schema(description = "Mensaje de confirmación", example = "La propuesta fue recibida correctamente (versión 1).")
        String mensaje,

        @Schema(description = "Propuesta con sus autores")
        PropuestaResponse propuesta,

        @Schema(description = "Versión creada con sus documentos")
        VersionPropuestaResponse version
) {
}
