package com.sigea.demosigea_backend.dto.presupuesto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen presupuestal comparativo para el evento (HU-08).
 * Permite visualizar el presupuesto preliminar proyectado, la versión oficial vigente
 * y el historial completo de cambios / versiones anteriores.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resumen presupuestal comparativo del evento (preliminar vs oficial vs historial)")
public record PresupuestoResumenResponse(

        @Schema(description = "ID del evento", example = "10")
        Long eventoId,

        @Schema(description = "Nombre del evento", example = "Congreso de Ingeniería de Sistemas")
        String eventoNombre,

        @Schema(description = "Total del presupuesto preliminar proyectado a partir de los rubros", example = "48500000.00")
        BigDecimal totalPreliminarProyectado,

        @Schema(description = "Cantidad de rubros presupuestales activos que conforman el preliminar", example = "8")
        int cantidadRubrosPreliminares,

        @Schema(description = "Versión oficial vigente del presupuesto aprobado (null si aún no ha sido aprobado)")
        PresupuestoAprobadoResponse presupuestoOficial,

        @Schema(description = "Historial completo de versiones registradas (incluye oficiales e históricas)")
        List<PresupuestoAprobadoResponse> historialVersiones
) {}
