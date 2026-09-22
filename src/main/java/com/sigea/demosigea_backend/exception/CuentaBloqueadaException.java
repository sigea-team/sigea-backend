package com.sigea.demosigea_backend.exception;

/**
 * Excepción de regla de negocio lanzada cuando un usuario intenta iniciar sesión
 * mientras su cuenta se encuentra temporalmente bloqueada por haber superado el número
 * máximo de intentos fallidos consecutivos configurado en {@code app.security.max-intentos-fallidos}.
 * <p>
 * Corresponde al Criterio 3 de la historia de usuario HU-01 (RF01 - Autenticar usuario).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class CuentaBloqueadaException extends RuntimeException {

    /**
     * Construye una nueva excepción con un mensaje descriptivo, indicando normalmente
     * hasta qué hora permanecerá bloqueada la cuenta.
     *
     * @param message Mensaje explicativo sobre el bloqueo.
     */
    public CuentaBloqueadaException(String message) {
        super(message);
    }
}
