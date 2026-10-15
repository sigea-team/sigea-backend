package com.sigea.demosigea_backend.dto.presupuesto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Indicador de ejecución presupuestal del evento (HU-09, RF12).
 * <p>
 * {@code porcentajeEjecucion = totalEjecutado / valorTotalAprobado × 100}, con 2 decimales.
 * Sin gastos (o sin presupuesto aprobado) el porcentaje es 0 y no se genera error (Criterio 4).
 * </p>
 *
 * @param eventoId                  ID del evento.
 * @param eventoNombre              Nombre del evento.
 * @param tienePresupuestoAprobado  {@code true} si existe presupuesto aprobado vigente (HU-08).
 * @param valorTotalAprobado        Monto del presupuesto aprobado vigente (0 si no hay).
 * @param totalPresupuestadoRubros  Suma de los subtotales de los rubros vigentes.
 * @param totalEjecutado            Suma de todos los gastos del evento.
 * @param saldoDisponible           {@code valorTotalAprobado − totalEjecutado}.
 * @param porcentajeEjecucion       Porcentaje de ejecución del presupuesto aprobado.
 * @param cantidadGastos            Número de gastos registrados.
 * @param rubros                    Ejecución de cada rubro vigente.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Ejecución presupuestal del evento")
public record EjecucionPresupuestalResponse(
        @Schema(example = "3") Long eventoId,
        @Schema(example = "Congreso de Ingeniería de Sistemas") String eventoNombre,
        @Schema(example = "true") boolean tienePresupuestoAprobado,
        @Schema(example = "5000000.00") BigDecimal valorTotalAprobado,
        @Schema(example = "4800000.00") BigDecimal totalPresupuestadoRubros,
        @Schema(example = "1250000.00") BigDecimal totalEjecutado,
        @Schema(example = "3750000.00") BigDecimal saldoDisponible,
        @Schema(example = "25.00") BigDecimal porcentajeEjecucion,
        @Schema(example = "4") long cantidadGastos,
        List<EjecucionRubroResponse> rubros
) {}
