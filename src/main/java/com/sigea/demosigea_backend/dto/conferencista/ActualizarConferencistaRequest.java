package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Actualización de los datos y la trayectoria de un conferencista (HU-12, Criterio 4).
 * Reemplaza todos los campos: lo que se omite en los opcionales queda vacío.
 *
 * @param tipoDocumento   Tipo de documento.
 * @param numeroDocumento Número de documento (no puede pertenecer a otra persona).
 * @param nombres         Nombres.
 * @param apellidos       Apellidos.
 * @param correo          Correo (no puede pertenecer a otra persona ni cambiarse si la persona tiene cuenta).
 * @param telefono        Teléfono (opcional).
 * @param trayectoria     Trayectoria completa.
 */
@Schema(description = "Datos personales, de contacto y trayectoria actualizados del conferencista")
public record ActualizarConferencistaRequest(
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Size(max = 20, message = "El tipo de documento no puede exceder 20 caracteres")
        String tipoDocumento,

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 30, message = "El número de documento no puede exceder 30 caracteres")
        String numeroDocumento,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato del correo no es válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String correo,

        @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
        String telefono,

        @NotNull(message = "La trayectoria del conferencista es obligatoria")
        @Valid
        TrayectoriaRequest trayectoria
) {
}
