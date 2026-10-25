package com.sigea.demosigea_backend.exception;

/**
 * Excepción de negocio lanzada cuando la propuesta ya está asignada al evaluador indicado (HU-16).
 * <p>
 * El {@code GlobalExceptionHandler} la traduce a HTTP 409 con el código {@code ASIGNACION_DUPLICADA}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class AsignacionDuplicadaException extends RuntimeException {

    /**
     * @param message Descripción de la asignación repetida.
     */
    public AsignacionDuplicadaException(String message) {
        super(message);
    }
}
