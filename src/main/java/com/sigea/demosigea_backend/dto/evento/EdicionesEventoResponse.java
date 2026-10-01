package com.sigea.demosigea_backend.dto.evento;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Listado de ediciones de un evento ordenado cronológicamente (HU-04, Criterio 4).
 *
 * @param eventoBaseId     ID del evento raíz de la familia.
 * @param nombreEventoBase Nombre del evento raíz.
 * @param totalEdiciones   Número total de registros (evento base + ediciones derivadas).
 * @param ediciones        Evento base y ediciones, del más antiguo al más reciente, con su estado.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Ediciones de un evento ordenadas cronológicamente")
public record EdicionesEventoResponse(
        @Schema(example = "1") Long eventoBaseId,
        @Schema(example = "Congreso de Ingeniería de Sistemas") String nombreEventoBase,
        @Schema(example = "3") int totalEdiciones,
        List<EventoResponse> ediciones
) {
}
