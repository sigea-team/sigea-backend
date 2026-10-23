package com.sigea.demosigea_backend.dto.evaluacion;

import com.sigea.demosigea_backend.model.Evaluacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta con el detalle de la calificación de un criterio dentro de una evaluación (HU-17).
 *
 * @param evaluacionId    ID único de la evaluación.
 * @param asignacionId    ID de la asignación a la que pertenece esta calificación.
 * @param criterioId      ID del criterio de la rúbrica calificado.
 * @param nombreCriterio  Nombre del criterio calificado.
 * @param pesoPorcentual  Peso porcentual del criterio en la puntuación final.
 * @param calificacion    Calificación numérica otorgada (0.0 – 10.0).
 * @param observaciones   Observaciones del evaluador sobre el criterio.
 * @param fechaEvaluacion Fecha y hora del registro de la calificación.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record EvaluacionResponse(
        Long evaluacionId,
        Long asignacionId,
        Long criterioId,
        String nombreCriterio,
        BigDecimal pesoPorcentual,
        BigDecimal calificacion,
        String observaciones,
        LocalDateTime fechaEvaluacion
) {

    /**
     * Fábrica estática que construye el DTO a partir de la entidad {@link Evaluacion}.
     *
     * @param e entidad de evaluación persistida.
     * @return DTO con los datos de la calificación y el criterio asociado.
     */
    public static EvaluacionResponse fromEntity(Evaluacion e) {
        return new EvaluacionResponse(
                e.getId(),
                e.getAsignacion().getId(),
                e.getCriterio().getId(),
                e.getCriterio().getNombre(),
                e.getCriterio().getPesoPorcentual(),
                e.getCalificacion(),
                e.getObservaciones(),
                e.getFechaEvaluacion()
        );
    }
}
