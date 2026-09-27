package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Objeto de Transferencia de Datos (DTO) para completar el restablecimiento
 * de contraseña con el token recibido por correo (HU-32, Criterios 2 y 3).
 * <p>
 * La política de contraseña aplicada es <b>la misma</b> que en el registro
 * ({@link RegistroRequest#contrasena()}): 8 a 64 caracteres, con al menos una
 * mayúscula, una minúscula, un número y un carácter especial. Se reutiliza el
 * mismo patrón para no tener dos políticas de seguridad distintas en el sistema.
 * </p>
 *
 * @param token Token de recuperación (UUID) recibido por correo electrónico.
 * @param nuevaContrasena Nueva contraseña que reemplazará a la actual.
 *
 * @author SIGEA Development Team
 * @since 2026
 */
@Schema(description = "Petición para restablecer la contraseña usando el token de recuperación")
public record RestablecerContrasenaRequest(
        @Schema(description = "Token de recuperación recibido por correo electrónico", example = "4c522da4-7d52-4467-bc18-2ad16a690d79")
        @NotBlank(message = "El token de recuperación es obligatorio")
        String token,

        @Schema(
                description = "Nueva contraseña segura. Mínimo 8 caracteres, al menos una mayúscula, una minúscula, un dígito y un carácter especial",
                example = "NuevaSegura123*"
        )
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._\\-#])[A-Za-z\\d@$!%*?&._\\-#]{8,64}$",
                message = "La contraseña no cumple con la política mínima de seguridad. Requisitos: mínimo 8 caracteres, al menos una letra mayúscula, una letra minúscula, un número y un carácter especial (@$!%*?&._-#)"
        )
        String nuevaContrasena
) {
}
