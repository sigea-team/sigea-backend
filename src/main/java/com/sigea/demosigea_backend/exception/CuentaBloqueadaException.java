package com.sigea.demosigea_backend.exception;

import java.time.LocalDateTime;

/**
 * Excepción de regla de negocio lanzada cuando un usuario intenta iniciar sesión
 * mientras su cuenta se encuentra temporalmente bloqueada por haber superado el número
 * máximo de intentos fallidos consecutivos configurado en {@code app.security.max-intentos-fallidos}.
 * <p>
 * Transporta la fecha y hora en que finaliza el bloqueo para que el manejador global
 * pueda informarla al cliente (frontend) de forma estructurada.
 * </p>
 * <p>
 * Corresponde al Criterio 3 de la historia de usuario HU-01 (RF01 - Autenticar usuario).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.1
 * @since 2026
 */
public class CuentaBloqueadaException extends RuntimeException {

    /** Fecha y hora hasta la cual la cuenta permanece bloqueada. */
    private final LocalDateTime bloqueadoHasta;

    /**
     * Construye una nueva excepción con un mensaje descriptivo y el fin del bloqueo.
     *
     * @param message        Mensaje explicativo sobre el bloqueo.
     * @param bloqueadoHasta Fecha y hora en que la cuenta vuelve a estar disponible.
     */
    public CuentaBloqueadaException(String message, LocalDateTime bloqueadoHasta) {
        super(message);
        this.bloqueadoHasta = bloqueadoHasta;
    }

    /**
     * Obtiene la fecha y hora en que finaliza el bloqueo temporal de la cuenta.
     *
     * @return fecha y hora de desbloqueo
     */
    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}