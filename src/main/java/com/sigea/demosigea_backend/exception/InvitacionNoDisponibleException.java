package com.sigea.demosigea_backend.exception;

import lombok.Getter;

/**
 * El enlace de invitación ya no se puede usar (HU-12): venció, ya se completó, fue cancelado o el
 * evento ya no recibe conferencistas. Se responde 410 GONE con el código correspondiente.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class InvitacionNoDisponibleException extends RuntimeException {

    public static final String VENCIDA = "INVITACION_VENCIDA";
    public static final String COMPLETADA = "INVITACION_COMPLETADA";
    public static final String CANCELADA = "INVITACION_CANCELADA";
    public static final String EVENTO_CERRADO = "INVITACION_EVENTO_CERRADO";

    /** Código de negocio que recibe el frontend. */
    private final String codigo;

    public InvitacionNoDisponibleException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }
}
