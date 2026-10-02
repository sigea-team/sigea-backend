package com.sigea.demosigea_backend.exception;

/**
 * Excepción lanzada cuando se intenta modificar o eliminar un registro de auditoría
 * (HU-03, Criterio 3). Se traduce a HTTP 405 METHOD NOT ALLOWED.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class RegistroAuditoriaInmutableException extends RuntimeException {

    public static final String MENSAJE =
            "Los registros de auditoría no pueden modificarse ni eliminarse, para preservar la integridad del historial.";

    public RegistroAuditoriaInmutableException() {
        super(MENSAJE);
    }
}
