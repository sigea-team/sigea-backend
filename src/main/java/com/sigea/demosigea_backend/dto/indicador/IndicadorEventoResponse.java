package com.sigea.demosigea_backend.dto.indicador;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Indicador del evento para la tabla del dashboard (HU-29, Criterio 1).
 *
 * @param codigo     código estable del indicador (los mismos de la memoria histórica de HU-28).
 * @param nombre     nombre visible.
 * @param grupo      código del grupo al que pertenece (ver {@link GrupoIndicadoresResponse}).
 * @param valor      valor calculado; {@code null} si no hay datos suficientes.
 * @param unidad     unidad del valor ({@code personas}, {@code %}, {@code COP}...).
 * @param disponible {@code false} si no hay datos suficientes para calcularlo (Criterio 2).
 * @param motivo     por qué no está disponible; {@code null} si está disponible.
 */
@Schema(description = "Indicador del evento calculado con la información registrada")
public record IndicadorEventoResponse(
        @Schema(example = "TASA_ASISTENCIA") String codigo,
        @Schema(example = "Tasa de asistencia de los inscritos") String nombre,
        @Schema(example = "ASISTENCIA") String grupo,
        @Schema(example = "82.50", nullable = true) BigDecimal valor,
        @Schema(example = "%") String unidad,
        @Schema(example = "true") boolean disponible,
        @Schema(example = "No hay asistencia registrada en las actividades del evento.", nullable = true) String motivo
) {
}
