package com.sigea.demosigea_backend.model;

/**
 * Estados posibles de una asignación de evaluación de propuesta (HU-17).
 * <p>
 * Mapea la restricción CHECK de la columna {@code estado} en la tabla {@code asignaciones_evaluacion}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum EstadoAsignacion {
    pendiente,
    en_proceso,
    completada
}
