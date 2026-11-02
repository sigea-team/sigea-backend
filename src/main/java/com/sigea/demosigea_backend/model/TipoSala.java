package com.sigea.demosigea_backend.model;

/**
 * Tipo de sala disponible para las actividades de un evento.
 * <ul>
 *   <li>{@link #FISICA} — sala presencial con ubicación física.</li>
 *   <li>{@link #VIRTUAL} — sala en línea (videoconferencia, streaming, etc.).</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum TipoSala {
    /** Sala con presencia física en un lugar específico. */
    FISICA,
    /** Sala virtual (en línea). */
    VIRTUAL
}
