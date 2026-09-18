package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Objeto de Transferencia de Datos (DTO) que representa la respuesta emitida por
 * el sistema tras procesar exitosamente la solicitud de reenvío de un token de verificación.
 * <p>
 * Confirma al usuario que se ha generado un nuevo enlace de activación y especifica
 * la dirección de correo a la cual fue despachado el mensaje.
 * </p>
 *
 * @param mensaje Confirmación textual del envío exitoso de la notificación.
 * @param correo Dirección de correo electrónico a la que se envió el enlace de verificación.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Confirmación de reenvío de correo de verificación")
public record ReenviarVerificacionResponse(
        @Schema(description = "Mensaje descriptivo", example = "Se ha enviado un nuevo enlace de verificación a su correo electrónico.")
        String mensaje,

        @Schema(description = "Correo destinatario del enlace", example = "carlos.gomez@universidad.edu.co")
        String correo
) {
}