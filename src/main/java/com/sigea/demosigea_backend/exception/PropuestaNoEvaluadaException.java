package com.sigea.demosigea_backend.exception;

/**
 * HU-18, Criterio 3: la propuesta no ha sido evaluada completamente (sin evaluadores asignados, con evaluaciones
 * pendientes o sin puntuación ponderada calculada) y por eso no se puede clasificar.
 * <p>
 * El {@code GlobalExceptionHandler} responde 409 con el código {@code PROPUESTA_NO_EVALUADA}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class PropuestaNoEvaluadaException extends RuntimeException {

    /**
     * @param message motivo por el que no se puede clasificar.
     */
    public PropuestaNoEvaluadaException(String message) {
        super(message);
    }
}
