package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Objeto de Transferencia de Datos (DTO) que encapsula las credenciales
 * requeridas para autenticar un usuario en el sistema SIGEA.
 * <p>
 * Requiere la dirección de correo electrónico del usuario y su contraseña
 * en texto plano para su posterior verificación contra el hash almacenado.
 * </p>
 *
 * @param correo     Correo electrónico registrado del usuario. No puede estar vacío y debe ser un email válido.
 * @param contrasena Contraseña asociada a la cuenta para validar la autenticación. No puede estar vacía.
 *
 * @author SIGEA Development Team
 * @version 1.1
 * @since 2026
 */
@Schema(description = "Credenciales para inicio de sesión en la plataforma")
public record LoginRequest(
        @Schema(description = "Correo electrónico registrado de la cuenta", example = "carlos.gomez@universidad.edu.co")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato de correo electrónico no es válido")
        String correo,

        @Schema(description = "Contraseña de la cuenta", example = "Segura123*")
        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena
) {
}