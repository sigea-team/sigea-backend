package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Invitación a un conferencista nuevo (HU-12): el administrador solo indica nombre y correo; el resto
 * de los datos los registra el propio conferencista desde el enlace que recibe.
 *
 * @param nombre Nombre del conferencista, como lo conoce el organizador.
 * @param correo Correo al que se envía el enlace.
 */
@Schema(description = "Nombre y correo del conferencista que se invita a registrarse")
public record InvitarConferencistaRequest(
        @Schema(example = "Ana María Pérez Gómez")
        @NotBlank(message = "El nombre del conferencista es obligatorio")
        @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
        String nombre,

        @Schema(example = "ana.perez@unal.edu.co")
        @NotBlank(message = "El correo del conferencista es obligatorio")
        @Email(message = "El formato del correo no es válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String correo
) {
}
