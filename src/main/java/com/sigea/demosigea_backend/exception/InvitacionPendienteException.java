package com.sigea.demosigea_backend.exception;

/**
 * Ya existe una invitación vigente para ese correo en el evento (HU-12). Se responde 409
 * {@code INVITACION_PENDIENTE}; el administrador puede reenviarla.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class InvitacionPendienteException extends RecursoDuplicadoException {

    public InvitacionPendienteException(String message) {
        super(message);
    }
}
