package com.sigea.demosigea_backend.dto.asignacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solicitud para asignar una propuesta a uno o más evaluadores del comité (HU-16, Criterio 1).
 *
 * @param comiteEvaluadorIds IDs de los miembros del comité evaluador de la convocatoria de la propuesta.
 * @param confirmarConflicto {@code true} para confirmar la asignación pese a una advertencia de conflicto de
 *                           interés (Criterio 2). Los conflictos que impiden la asignación no se pueden confirmar.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Evaluadores a los que se asigna la propuesta")
public record AsignarEvaluadoresRequest(
        @Schema(example = "[3, 5]")
        @NotEmpty(message = "Debe indicar al menos un evaluador.")
        @Size(max = 20, message = "Puede asignar como máximo 20 evaluadores a la vez.")
        List<@NotNull(message = "El ID del evaluador no puede ser nulo.") Long> comiteEvaluadorIds,

        @Schema(example = "false", description = "Confirma la asignación pese a advertencias de conflicto de interés")
        Boolean confirmarConflicto
) {

    /** @return {@code true} solo si el organizador confirmó explícitamente. */
    public boolean conflictoConfirmado() {
        return Boolean.TRUE.equals(confirmarConflicto);
    }
}
