package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que el conferencista registra desde el enlace de la invitación (HU-12, Criterios 1 y 2).
 * El correo no se envía: es el de la invitación.
 *
 * @param tipoDocumento   Tipo de documento.
 * @param numeroDocumento Número de documento. Obligatorio salvo que la persona ya lo tenga registrado:
 *                        en ese caso, vacío conserva el guardado.
 * @param nombres         Nombres.
 * @param apellidos       Apellidos.
 * @param telefono        Teléfono (opcional). Si la persona ya tiene uno registrado, vacío lo conserva.
 * @param tema            Tema de su conferencia en el evento (opcional).
 * @param trayectoria     Biografía, estudios y publicaciones.
 * @param contrasena      Contraseña de su nueva cuenta en SIGEA. Obligatoria solo si la persona aún no tiene
 *                        cuenta (ver {@code InvitacionPublicaResponse.requiereContrasena}); misma política
 *                        que el registro de usuarios. Si ya tiene cuenta, se ignora.
 */
@Schema(description = "Datos personales, de contacto y trayectoria que registra el conferencista invitado")
public record CompletarInvitacionRequest(
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Size(max = 20, message = "El tipo de documento no puede exceder 20 caracteres")
        String tipoDocumento,

        @Size(max = 30, message = "El número de documento no puede exceder 30 caracteres")
        String numeroDocumento,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
        String apellidos,

        @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
        String telefono,

        @Size(max = 200, message = "El tema no puede exceder 200 caracteres")
        String tema,

        @NotNull(message = "La trayectoria del conferencista es obligatoria")
        @Valid
        TrayectoriaRequest trayectoria,

        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._\\-#])[A-Za-z\\d@$!%*?&._\\-#]{8,64}$",
                message = "La contraseña no cumple con la política mínima de seguridad. Requisitos: mínimo 8 caracteres, "
                        + "al menos una letra mayúscula, una letra minúscula, un número y un carácter especial (@$!%*?&._-#)"
        )
        String contrasena
) {
}
