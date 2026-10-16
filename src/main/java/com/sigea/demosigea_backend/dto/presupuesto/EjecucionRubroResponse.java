package com.sigea.demosigea_backend.dto.presupuesto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Ejecución de un rubro: presupuestado, acumulado ejecutado, disponible y porcentaje (HU-09).
 *
 * @param rubroId             ID del rubro.
 * @param nombre              Nombre del rubro.
 * @param presupuestado       Subtotal del rubro ({@code cantidad × valor unitario proyectado}).
 * @param ejecutado           Acumulado de los gastos del rubro.
 * @param disponible          {@code presupuestado − ejecutado}; negativo si el rubro se pasó (se permite).
 * @param porcentajeEjecucion {@code ejecutado / presupuestado × 100} con 2 decimales (0 si presupuestado = 0).
 * @param excedido            {@code true} si lo ejecutado supera lo presupuestado.
 * @param cantidadGastos      Número de gastos registrados en el rubro.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Ejecución presupuestal de un rubro")
public record EjecucionRubroResponse(
        @Schema(example = "7") Long rubroId,
        @Schema(example = "Transporte de conferencistas") String nombre,
        @Schema(example = "700000.00") BigDecimal presupuestado,
        @Schema(example = "350000.00") BigDecimal ejecutado,
        @Schema(example = "350000.00") BigDecimal disponible,
        @Schema(example = "50.00") BigDecimal porcentajeEjecucion,
        @Schema(example = "false") boolean excedido,
        @Schema(example = "1") long cantidadGastos
) {}
