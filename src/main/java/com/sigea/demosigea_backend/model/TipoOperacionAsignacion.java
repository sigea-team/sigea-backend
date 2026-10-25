package com.sigea.demosigea_backend.model;

/**
 * Tipo de movimiento registrado en el historial de asignaciones de evaluación (HU-16, Criterio 3).
 * <p>
 * Mapea la restricción CHECK de la columna {@code tipo_operacion} en la tabla {@code historial_asignaciones}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum TipoOperacionAsignacion {
    /** La propuesta se asignó por primera vez al evaluador. */
    asignacion,
    /** La asignación pasó de un evaluador a otro. */
    reasignacion
}
