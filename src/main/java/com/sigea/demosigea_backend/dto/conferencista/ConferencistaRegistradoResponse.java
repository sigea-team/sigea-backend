package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Conferencista ya registrado en SIGEA (con trayectoria), para elegirlo de la lista y vincularlo a un
 * evento sin volver a pedir sus datos (HU-12, Criterio 3).
 *
 * @param yaAsociadoAlEvento {@code true} si ya está en el evento consultado; {@code null} si no se indicó evento.
 */
@Schema(description = "Conferencista registrado que se puede vincular a un evento")
public record ConferencistaRegistradoResponse(
        Long personaId,
        String nombreCompleto,
        String tipoDocumento,
        String numeroDocumento,
        String correo,
        String cargoActual,
        String institucion,
        long eventosPrevios,
        Boolean yaAsociadoAlEvento,
        LocalDateTime fechaActualizacion
) {
}
