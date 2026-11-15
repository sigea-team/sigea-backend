package com.sigea.demosigea_backend.exception;

/**
 * Excepción lanzada cuando una actividad o evento ha alcanzado su capacidad máxima o aforo disponible (HU-23, Criterio 2 y 4).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class CupoAgotadoException extends OperacionNoPermitidaException {

    public CupoAgotadoException(String mensaje) {
        super(mensaje);
    }
}
