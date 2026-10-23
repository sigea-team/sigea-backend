package com.sigea.demosigea_backend.dto.evaluacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de solicitud para registrar la evaluación completa de una propuesta (HU-17).
 * <p>
 * Requiere la asignación identificadora y la lista de calificaciones por criterio.
 * Todos los criterios de la rúbrica activa deben estar presentes (Criterio 2).
 * </p>
 *
 * @param asignacionId  ID de la asignación de evaluación que se está completando.
 * @param calificaciones Lista de calificaciones por criterio. Debe incluir todos los criterios de la rúbrica.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record RegistrarEvaluacionRequest(

        @NotNull(message = "El ID de la asignación es obligatorio.")
        Long asignacionId,

        @NotNull(message = "La lista de calificaciones es obligatoria.")
        @Size(min = 1, message = "Debe incluir al menos una calificación por criterio.")
        List<@Valid CalificacionCriterioRequest> calificaciones
) {
}
