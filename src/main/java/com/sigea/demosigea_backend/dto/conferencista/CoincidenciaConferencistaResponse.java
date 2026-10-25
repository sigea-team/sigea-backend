package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Persona ya registrada que coincide por documento o correo con un conferencista que se intenta
 * registrar (HU-12, Criterio 3). El frontend la usa para ofrecer asociarla al evento.
 *
 * @param coincidePor        {@code documento}, {@code correo} o {@code documento_y_correo}.
 * @param tienePerfil        {@code true} si ya tiene trayectoria (no hace falta pedirla al asociar).
 * @param yaAsociadoAlEvento {@code true} si ya está en el evento consultado; {@code null} si no se indicó evento.
 * @param eventosPrevios     Cantidad de eventos en los que ha participado como conferencista.
 */
@Schema(description = "Persona existente que coincide por documento o correo")
public record CoincidenciaConferencistaResponse(
        @Schema(example = "12") Long personaId,
        @Schema(example = "Ana María Pérez Gómez") String nombreCompleto,
        @Schema(example = "CC") String tipoDocumento,
        @Schema(example = "1090123456") String numeroDocumento,
        @Schema(example = "ana.perez@ufps.edu.co") String correo,
        @Schema(example = "documento") String coincidePor,
        @Schema(example = "true") boolean tienePerfil,
        @Schema(example = "false") Boolean yaAsociadoAlEvento,
        @Schema(example = "2") long eventosPrevios
) {
}
