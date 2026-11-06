package com.sigea.demosigea_backend.model;

/**
 * Origen de una clasificación registrada en el historial (HU-18).
 * <p>
 * Mapea la restricción CHECK de la columna {@code tipo} en la tabla {@code historial_clasificaciones}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum TipoClasificacion {
    /** Aplicada por el sistema según los umbrales de la convocatoria (Criterio 1). */
    automatica,
    /** Ajustada por el administrador con un motivo (Criterio 2). */
    manual
}
