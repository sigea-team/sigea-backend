package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Objeto de Transferencia de Datos (DTO) de respuesta tras restablecer la
 * contraseña exitosamente (HU-32, Criterio 2).
 *
 * @param mensaje Mensaje de confirmación del cambio de contraseña.
 * @param correo Correo electrónico de la cuenta cuya contraseña fue actualizada.
 *
 * @author SIGEA Development Team
 * @since 2026
 */
@Schema(description = "Confirmación de restablecimiento exitoso de contraseña")
public record RestablecerContrasenaResponse(
        @Schema(description = "Mensaje de confirmación", example = "Tu contraseña fue actualizada correctamente. Te notificamos el cambio por correo electrónico.")
        String mensaje,

        @Schema(description = "Correo de la cuenta actualizada", example = "carlos.gomez@universidad.edu.co")
        String correo
) {
}
