package com.sigea.demosigea_backend.exception;

/**
 * Excepción lanzada cuando ya existe un rubro vigente con el mismo nombre en el presupuesto
 * preliminar del evento (HU-07). Se traduce a HTTP 409 con código {@code RUBRO_DUPLICADO}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class RubroDuplicadoException extends RecursoDuplicadoException {

    /**
     * @param message Descripción del duplicado (nombre del rubro y evento).
     */
    public RubroDuplicadoException(String message) {
        super(message);
    }
}
