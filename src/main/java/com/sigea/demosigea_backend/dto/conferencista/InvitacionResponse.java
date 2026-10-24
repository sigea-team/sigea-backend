package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Invitación pendiente de un evento, para el administrador (HU-12).
 *
 * @param estado {@code pendiente} (enlace vigente) o {@code vencida} (debe reenviarse).
 * @param enlace Enlace que recibió el invitado; el administrador puede copiarlo y enviarlo por otro medio.
 */
@Schema(description = "Invitación enviada a un conferencista nuevo")
public record InvitacionResponse(
        @Schema(example = "4") Long id,
        @Schema(example = "3") Long eventoId,
        @Schema(example = "Ana María Pérez Gómez") String nombre,
        @Schema(example = "ana.perez@unal.edu.co") String correo,
        @Schema(example = "pendiente") String estado,
        LocalDateTime fechaEnvio,
        LocalDateTime fechaExpiracion,
        @Schema(example = "https://sigea-team.github.io/sigea-front/#/conferencista/invitacion?token=...") String enlace
) {
}
