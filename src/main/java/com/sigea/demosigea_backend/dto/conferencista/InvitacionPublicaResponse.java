package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo que ve el invitado al abrir el enlace del correo (HU-12): el evento, el nombre y correo con los
 * que fue invitado y, si ya existe en SIGEA, sus datos para precargar el formulario.
 */
@Schema(description = "Invitación vista por el conferencista desde el enlace del correo")
public record InvitacionPublicaResponse(
        Long eventoId,
        String eventoNombre,
        String semestre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String nombre,
        String correo,
        LocalDateTime fechaExpiracion,
        DatosRegistradosResponse datosRegistrados
) {
}
