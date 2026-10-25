package com.sigea.demosigea_backend.dto.asignacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para reasignar una asignación de evaluación a otro evaluador del comité (HU-16, Criterio 3).
 *
 * @param comiteEvaluadorId  ID del nuevo evaluador.
 * @param motivo             Motivo de la reasignación (opcional), queda en el historial.
 * @param confirmarConflicto {@code true} para confirmar pese a una advertencia de conflicto de interés.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Nuevo evaluador de la asignación")
public record ReasignarEvaluadorRequest(
        @Schema(example = "7")
        @NotNull(message = "Debe indicar el nuevo evaluador.")
        Long comiteEvaluadorId,

        @Schema(example = "El evaluador inicial no tiene disponibilidad")
        @Size(max = 255, message = "El motivo no puede superar 255 caracteres.")
        String motivo,

        @Schema(example = "false", description = "Confirma la reasignación pese a advertencias de conflicto de interés")
        Boolean confirmarConflicto
) {

    /** @return {@code true} solo si el organizador confirmó explícitamente. */
    public boolean conflictoConfirmado() {
        return Boolean.TRUE.equals(confirmarConflicto);
    }
}
