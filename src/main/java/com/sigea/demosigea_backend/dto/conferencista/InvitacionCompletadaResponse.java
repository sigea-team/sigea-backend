package com.sigea.demosigea_backend.dto.conferencista;

/**
 * Confirmación para el conferencista que completó su registro desde la invitación (HU-12).
 *
 * @param vistaPublicaDisponible {@code true} si el evento ya está publicado y su perfil se ve en la página pública.
 */
public record InvitacionCompletadaResponse(
        Long eventoId,
        String eventoNombre,
        String nombreCompleto,
        boolean vistaPublicaDisponible
) {
}
