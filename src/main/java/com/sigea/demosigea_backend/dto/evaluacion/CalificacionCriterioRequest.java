package com.sigea.demosigea_backend.dto.evaluacion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO que representa la calificación asignada a un criterio específico de la rúbrica (HU-17).
 * <p>
 * Forma parte de la solicitud {@link RegistrarEvaluacionRequest}. La calificación debe
 * estar en el rango permitido de 0.0 a 10.0.
 * </p>
 *
 * @param criterioId    ID del criterio de la rúbrica que se está calificando.
 * @param calificacion  Calificación numérica otorgada (de 0.0 a 10.0).
 * @param observaciones Comentarios u observaciones del evaluador sobre el criterio (opcional).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record CalificacionCriterioRequest(

        @NotNull(message = "El ID del criterio es obligatorio.")
        Long criterioId,

        @NotNull(message = "La calificación es obligatoria.")
        @DecimalMin(value = "0.0", message = "La calificación mínima permitida es 0.0.")
        @DecimalMax(value = "10.0", message = "La calificación máxima permitida es 10.0.")
        BigDecimal calificacion,

        String observaciones
) {
}
