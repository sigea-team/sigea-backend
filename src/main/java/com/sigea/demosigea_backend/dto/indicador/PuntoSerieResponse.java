package com.sigea.demosigea_backend.dto.indicador;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Punto (categoría) de una serie del dashboard.
 *
 * @param clave    identificador estable del punto (código de categoría o estado, ID de actividad, fecha ISO...).
 * @param etiqueta texto visible en el eje o en la leyenda.
 * @param valores  valor de cada medida de la serie, en el orden de {@link SerieIndicadorResponse#medidas()};
 *                 un valor es {@code null} si no se puede calcular para este punto.
 */
@Schema(description = "Punto de una serie del dashboard")
public record PuntoSerieResponse(
        @Schema(example = "estudiante") String clave,
        @Schema(example = "Estudiante") String etiqueta,
        Map<String, BigDecimal> valores
) {
}
