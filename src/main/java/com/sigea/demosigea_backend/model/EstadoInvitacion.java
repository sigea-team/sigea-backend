package com.sigea.demosigea_backend.model;

/**
 * Estado de una invitación enviada a un conferencista nuevo (HU-12).
 * <p>
 * Coincide con el CHECK {@code invitaciones_conferencista_estado_check}. "Vencida" no se guarda: es
 * una invitación {@link #pendiente} cuya {@code fecha_expiracion} ya pasó.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum EstadoInvitacion {
    pendiente,
    completada,
    cancelada
}
