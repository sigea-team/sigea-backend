package com.sigea.demosigea_backend.exception;

/**
 * Excepción lanzada cuando los parámetros de una consulta son incoherentes
 * (por ejemplo, un rango de fechas cuyo inicio es posterior al fin). Se traduce a HTTP 400.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
