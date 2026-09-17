package com.sigea.demosigea_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta tras un registro exitoso en el sistema")
public record RegistroResponse(
        @Schema(description = "Identificador único asignado al usuario", example = "1")
        Long usuarioId,

        @Schema(description = "Nombre de usuario asignado en el sistema", example = "cgomez")
        String nombreUsuario,

        @Schema(description = "Correo electrónico al cual se envió el enlace de verificación", example = "carlos.gomez@universidad.edu.co")
        String correo,

        @Schema(description = "Mensaje informativo sobre el siguiente paso", example = "Cuenta creada exitosamente. Se ha enviado un enlace de verificación a su correo electrónico.")
        String mensaje,

        @Schema(description = "Indica si la cuenta requiere verificar el correo antes de habilitar el acceso", example = "true")
        boolean requiereVerificacion
) {
}
