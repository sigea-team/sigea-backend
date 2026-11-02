package com.sigea.demosigea_backend.exception;

/**
 * HU-14, Criterio 5: se intenta modificar una propuesta cuya versión definitiva ya fue aprobada (RF29)
 * sin contar con la autorización explícita del administrador.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class PropuestaBloqueadaException extends RuntimeException {

    public PropuestaBloqueadaException(String mensaje) {
        super(mensaje);
    }
}
