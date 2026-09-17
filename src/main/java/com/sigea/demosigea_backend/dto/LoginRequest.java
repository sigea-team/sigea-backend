package com.sigea.demosigea_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para inicio de sesión en la plataforma")
public record LoginRequest(
        @Schema(description = "Correo electrónico o nombre de usuario registrado", example = "carlos.gomez@universidad.edu.co")
        @NotBlank(message = "El correo electrónico o nombre de usuario es obligatorio")
        String identificador,

        @Schema(description = "Contraseña de la cuenta", example = "Segura123*")
        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena
) {
}
