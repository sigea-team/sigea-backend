package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * Vista pública de los conferencistas de un evento publicado (HU-12, Criterio 4).
 */
@Schema(description = "Conferencistas de un evento publicado")
public record ConferencistasPublicosEventoResponse(
        Long eventoId,
        String eventoNombre,
        String semestre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        List<ConferencistaPublicoResponse> conferencistas
) {
}
