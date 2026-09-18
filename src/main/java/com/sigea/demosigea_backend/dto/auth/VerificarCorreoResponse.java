package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Objeto de Transferencia de Datos (DTO) que representa la respuesta emitida tras
 * el procesamiento de una solicitud de confirmación de correo electrónico.
 * <p>
 * Notifica al cliente el resultado de la validación del token y confirma si la
 * cuenta ha sido activada correctamente para proceder con el inicio de sesión.
 * </p>
 *
 * @param mensaje Confirmación o detalle explicativo del resultado de la operación.
 * @param verificado Estado final del proceso; {@code true} si el token fue válido y la cuenta activada exitosamente.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resultado de la verificación de correo electrónico")
public record VerificarCorreoResponse(
        @Schema(description = "Mensaje informativo del resultado", example = "Su correo electrónico ha sido verificado con éxito. Ya puede iniciar sesión.")
        String mensaje,

        @Schema(description = "Indica si la verificación fue exitosa", example = "true")
        boolean verificado
) {
}