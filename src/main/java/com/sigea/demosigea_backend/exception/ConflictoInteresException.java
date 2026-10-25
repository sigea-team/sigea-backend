package com.sigea.demosigea_backend.exception;

/**
 * Excepción de negocio lanzada cuando se intenta asignar una propuesta a un evaluador que tiene un conflicto de
 * interés que impide la asignación, por ejemplo porque es autor, coautor o ponente de la propuesta
 * (HU-16, Criterio 2).
 * <p>
 * El {@code GlobalExceptionHandler} la traduce a HTTP 409 con el código {@code CONFLICTO_INTERES}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class ConflictoInteresException extends RuntimeException {

    /**
     * @param message Descripción del conflicto detectado.
     */
    public ConflictoInteresException(String message) {
        super(message);
    }
}
