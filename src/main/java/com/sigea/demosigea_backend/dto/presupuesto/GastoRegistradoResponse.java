package com.sigea.demosigea_backend.dto.presupuesto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Resultado de registrar un gasto: el gasto guardado, el acumulado actualizado de su rubro y el
 * porcentaje de ejecución recalculado del evento (HU-09, Criterio 1).
 *
 * @param gasto                Gasto registrado.
 * @param rubro                Ejecución del rubro después del registro.
 * @param valorTotalAprobado   Presupuesto aprobado vigente del evento.
 * @param totalEjecutado       Total ejecutado del evento después del registro.
 * @param porcentajeEjecucion  Porcentaje de ejecución del evento después del registro.
 * @param excedioDisponible    {@code true} si el gasto superó el disponible del rubro. Es solo
 *                             informativo: el gasto se registra igual.
 * @param advertencia          Mensaje informativo cuando hubo exceso; {@code null} en otro caso.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Gasto registrado con la ejecución recalculada")
public record GastoRegistradoResponse(
        GastoEjecutadoResponse gasto,
        EjecucionRubroResponse rubro,
        @Schema(example = "5000000.00") BigDecimal valorTotalAprobado,
        @Schema(example = "1250000.00") BigDecimal totalEjecutado,
        @Schema(example = "25.00") BigDecimal porcentajeEjecucion,
        @Schema(example = "false") boolean excedioDisponible,
        @Schema(example = "null") String advertencia
) {}
