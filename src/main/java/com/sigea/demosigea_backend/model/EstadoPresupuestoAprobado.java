package com.sigea.demosigea_backend.model;

/**
 * Estados del registro de presupuesto aprobado formalmente para un evento (HU-08).
 * <ul>
 *   <li>{@code aprobado}: Versión oficial vigente autorizada para el evento.</li>
 *   <li>{@code historico}: Versión anterior conservada tras una actualización para mantener la trazabilidad.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum EstadoPresupuestoAprobado {
    aprobado,
    historico
}
