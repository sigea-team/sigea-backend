package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Objeto de Transferencia de Datos (DTO) que captura la solicitud de un usuario
 * para volver a generar y enviar un enlace/token de verificación de correo electrónico.
 * <p>
 * Se utiliza principalmente cuando el mensaje original ha expirado, no fue recibido o fue
 * extraviado por el usuario durante el proceso inicial de registro en la plataforma SIGEA.
 * </p>
 *
 * @param correo Correo electrónico registrado asociado a la cuenta que requiere una nueva verificación. No puede estar en blanco.
 *
 * @author SIGEA Development Team
 * @version 1.1
 * @since 2026
 */
@Schema(description = "Petición para reenviar el enlace de verificación de correo")
public record ReenviarVerificacionRequest(
        @Schema(description = "Correo electrónico de la cuenta pendiente de verificación", example = "carlos.gomez@universidad.edu.co")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato de correo electrónico no es válido")
        String correo
) {
}