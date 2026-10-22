package com.sigea.demosigea_backend.model;

/**
 * Tipo de participación de una persona en un evento como conferencista (HU-12, RF16).
 * <p>
 * Los valores coinciden con el CHECK de la columna {@code participaciones_conferencista.tipo_participacion}.
 * </p>
 * <ul>
 *   <li>{@link #conferencista}: invitado a dictar una conferencia (central, magistral, etc.).</li>
 *   <li>{@link #ponente}: presenta una ponencia dentro del evento.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum TipoParticipacionConferencista {
    conferencista,
    ponente
}
