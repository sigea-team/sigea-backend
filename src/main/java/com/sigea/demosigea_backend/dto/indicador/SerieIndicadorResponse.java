package com.sigea.demosigea_backend.dto.indicador;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Serie de datos para una gráfica del dashboard (HU-29, Criterio 1). Si el evento no tiene datos para ella, llega
 * con {@code hayDatos = false}, el mensaje y sin puntos (Criterio 2).
 *
 * @param codigo   código estable de la serie.
 * @param titulo   título visible de la gráfica.
 * @param grafico  tipo de gráfica sugerido: {@code barras}, {@code barras_agrupadas}, {@code circular} o
 *                 {@code linea}.
 * @param hayDatos {@code false} si no hay datos para graficar.
 * @param mensaje  aviso de ausencia de datos; {@code null} si hay datos.
 * @param medidas  medidas de cada punto.
 * @param puntos   puntos de la serie, en el orden en que se grafican.
 */
@Schema(description = "Serie de datos para una gráfica del dashboard")
public record SerieIndicadorResponse(
        @Schema(example = "ASISTENCIA_POR_ACTIVIDAD") String codigo,
        @Schema(example = "Inscritos y asistentes por actividad") String titulo,
        @Schema(example = "barras_agrupadas") String grafico,
        @Schema(example = "true") boolean hayDatos,
        @Schema(nullable = true) String mensaje,
        List<MedidaSerieResponse> medidas,
        List<PuntoSerieResponse> puntos
) {
}
