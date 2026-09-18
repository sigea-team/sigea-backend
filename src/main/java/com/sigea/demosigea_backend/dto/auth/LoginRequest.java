package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Objeto de Transferencia de Datos (DTO) que encapsula las credenciales
 * requeridas para autenticar un usuario en el sistema SIGEA.
 * <p>
 * Permite la identificación flexible mediante correo electrónico o nombre
 * de usuario, acompañado de la contraseña en texto plano para su posterior
 * verificación contra el hash almacenado en la base de datos.
 * </p>
 *
 * @param identificador Correo electrónico o nombre de usuario registrado del participante/administrador. No puede estar vacío.
 * @param contrasena Contraseña asociada a la cuenta para validar la autenticación. No puede estar vacía.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
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