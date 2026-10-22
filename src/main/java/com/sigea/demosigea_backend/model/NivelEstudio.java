package com.sigea.demosigea_backend.model;

/**
 * Nivel de formación de un estudio registrado en la trayectoria del conferencista (HU-12, Criterio 1).
 * <p>
 * Los valores coinciden con el CHECK {@code estudios_conferencista_nivel_check}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum NivelEstudio {
    pregrado,
    especializacion,
    maestria,
    doctorado,
    posdoctorado,
    otro
}
