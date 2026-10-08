package com.sigea.demosigea_backend.dto.presupuesto;

import com.sigea.demosigea_backend.model.EstadoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO con el presupuesto preliminar completo de un evento: sus rubros y el total (HU-07, Criterio 1).
 *
 * @param eventoId            ID del evento.
 * @param eventoNombre        Nombre del evento.
 * @param estadoEvento        Estado actual del evento.
 * @param rubros              Rubros del presupuesto (solo vigentes, o también eliminados si se pidió).
 * @param cantidadRubros      Número de rubros vigentes.
 * @param total               Suma de los subtotales de los rubros vigentes.
 * @param presupuestoAprobado {@code true} si el evento ya tiene presupuesto aprobado (HU-08).
 * @param editable            {@code true} si todavía se pueden agregar, editar o eliminar rubros.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Presupuesto preliminar de un evento con sus rubros y el total calculado")
public record PresupuestoPreliminarResponse(
        @Schema(example = "3") Long eventoId,
        @Schema(example = "Congreso Internacional de Ingeniería de Sistemas") String eventoNombre,
        @Schema(example = "en_configuracion") EstadoEvento estadoEvento,
        List<RubroResponse> rubros,
        @Schema(description = "Número de rubros vigentes", example = "4") int cantidadRubros,
        @Schema(description = "Total del presupuesto preliminar (solo rubros vigentes)", example = "5250000.00")
        BigDecimal total,
        @Schema(description = "El evento ya tiene presupuesto aprobado", example = "false") boolean presupuestoAprobado,
        @Schema(description = "Se pueden agregar, editar o eliminar rubros", example = "true") boolean editable
) {
}
