package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Objeto de Transferencia de Datos (DTO) que confirma la creación exitosa de un
 * nuevo usuario en la plataforma SIGEA.
 * <p>
 * Devuelve el identificador asignado, los datos básicos de la cuenta recién generada
 * e indica el estado del flujo de verificación por correo electrónico para guiarlo hacia
 * el siguiente paso de autenticación.
 * </p>
 *
 * @param usuarioId Identificador único asignado al usuario recién creado en la base de datos.
 * @param correo Dirección de correo electrónico asociada a la cuenta a la cual se remitió la notificación de activación.
 * @param mensaje Confirmación textual con instrucciones informativas para el usuario final.
 * @param requiereVerificacion Bandera lógica que indica si la cuenta se encuentra bloqueada hasta completar la validación de la dirección de correo.
 *
 * @author SIGEA Development Team
 * @version 1.1
 * @since 2026
 */
@Schema(description = "Respuesta tras un registro exitoso en el sistema")
public record RegistroResponse(
        @Schema(description = "Identificador único asignado al usuario", example = "1")
        Long usuarioId,

        @Schema(description = "Correo electrónico al cual se envió el enlace de verificación", example = "carlos.gomez@universidad.edu.co")
        String correo,

        @Schema(description = "Mensaje informativo sobre el siguiente paso", example = "Cuenta creada exitosamente. Se ha enviado un enlace de verificación a su correo electrónico.")
        String mensaje,

        @Schema(description = "Indica si la cuenta requiere verificar el correo antes de habilitar el acceso", example = "true")
        boolean requiereVerificacion
) {
}