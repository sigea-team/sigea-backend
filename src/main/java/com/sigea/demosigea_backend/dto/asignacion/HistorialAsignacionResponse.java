package com.sigea.demosigea_backend.dto.asignacion;

import com.sigea.demosigea_backend.model.ComiteEvaluador;
import com.sigea.demosigea_backend.model.HistorialAsignacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Movimiento del historial de asignaciones de una propuesta (HU-16, Criterio 3).
 *
 * @param historialId             ID del registro de historial.
 * @param asignacionId            ID de la asignación afectada.
 * @param propuestaId             ID de la propuesta.
 * @param tipoOperacion           {@code asignacion} o {@code reasignacion}.
 * @param evaluadorAnteriorId     ID del evaluador anterior ({@code null} en una asignación inicial).
 * @param evaluadorAnteriorNombre Nombre del evaluador anterior.
 * @param evaluadorNuevoId        ID del evaluador que queda con la asignación.
 * @param evaluadorNuevoNombre    Nombre del evaluador que queda con la asignación.
 * @param conflictoAdvertido      Si se confirmó pese a una advertencia de conflicto de interés.
 * @param motivo                  Motivo informado (opcional).
 * @param realizadoPor            Nombre del usuario que hizo el movimiento.
 * @param fechaHora               Fecha y hora del movimiento.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Movimiento del historial de asignaciones de una propuesta")
public record HistorialAsignacionResponse(
        @Schema(example = "8") Long historialId,
        @Schema(example = "12") Long asignacionId,
        @Schema(example = "40") Long propuestaId,
        @Schema(example = "reasignacion") String tipoOperacion,
        @Schema(example = "3") Long evaluadorAnteriorId,
        @Schema(example = "Ana Pérez") String evaluadorAnteriorNombre,
        @Schema(example = "7") Long evaluadorNuevoId,
        @Schema(example = "Luis Gómez") String evaluadorNuevoNombre,
        @Schema(example = "false") boolean conflictoAdvertido,
        @Schema(example = "El evaluador inicial no tiene disponibilidad") String motivo,
        @Schema(example = "María Rodríguez") String realizadoPor,
        LocalDateTime fechaHora
) {

    public static HistorialAsignacionResponse fromEntity(HistorialAsignacion h) {
        ComiteEvaluador anterior = h.getEvaluadorAnterior();
        ComiteEvaluador nuevo = h.getEvaluadorNuevo();
        return new HistorialAsignacionResponse(
                h.getId(),
                h.getAsignacion() != null ? h.getAsignacion().getId() : null,
                h.getPropuesta() != null ? h.getPropuesta().getId() : null,
                h.getTipoOperacion() != null ? h.getTipoOperacion().name() : null,
                anterior != null ? anterior.getId() : null,
                anterior != null ? AsignacionResponse.nombreCompleto(anterior.getPersona()) : null,
                nuevo != null ? nuevo.getId() : null,
                nuevo != null ? AsignacionResponse.nombreCompleto(nuevo.getPersona()) : null,
                Boolean.TRUE.equals(h.getConflictoAdvertido()),
                h.getMotivo(),
                h.getUsuario() != null ? AsignacionResponse.nombreCompleto(h.getUsuario().getPersona()) : null,
                h.getFechaHora()
        );
    }
}
