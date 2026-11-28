package com.sigea.demosigea_backend.dto.indicador;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Medida (eje o barra) de una serie del dashboard; es la clave de {@link PuntoSerieResponse#valores()}.
 *
 * @param codigo clave del valor en cada punto.
 * @param nombre nombre visible para la leyenda.
 * @param unidad unidad del valor.
 */
@Schema(description = "Medida de una serie del dashboard")
public record MedidaSerieResponse(
        @Schema(example = "asistentes") String codigo,
        @Schema(example = "Asistentes") String nombre,
        @Schema(example = "personas") String unidad
) {
}
