package com.sigea.demosigea_backend.model;

/**
 * Clasificación del resultado final de la evaluación ponderada de una propuesta (HU-17).
 * <p>
 * Mapea la restricción CHECK de la columna {@code clasificacion} en la tabla {@code resultados_evaluacion}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum ClasificacionResultado {
    aprobada,
    aprobada_con_ajustes,
    rechazada
}
