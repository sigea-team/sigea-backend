package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Objeto de Transferencia de Datos (DTO) que captura la solicitud de un usuario
 * para volver a generar y enviar un enlace/token de verificación de correo electrónico.
 * <p>
 * Se utiliza principalmente cuando el mensaje original ha expirado, no fue recibido o fue
 * extraviado por el usuario durante el proceso inicial de registro en la plataforma SIGEA.
 * </p>
 *
 * @param identificador Correo electrónico registrado o nombre de usuario asociado a la cuenta que requiere una nueva verificación. No puede estar en blanco.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Petición para reenviar el enlace de verificación de correo")
public record ReenviarVerificacionRequest(
        @Schema(description = "Correo electrónico o nombre de usuario de la cuenta pendiente de verificación", example = "carlos.gomez@universidad.edu.co")
        @NotBlank(message = "Debe indicar su correo electrónico o nombre de usuario")
        String identificador
) {
}