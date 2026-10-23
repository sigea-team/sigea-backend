package com.sigea.demosigea_backend.exception;

/**
 * HU-17, Criterio 4: se intenta modificar una evaluación ya enviada cuando el periodo de
 * evaluación de la convocatoria ya está cerrado.
 * <p>
 * Se responde 409 CONFLICT con el código {@code EVALUACION_CERRADA}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class EvaluacionCerradaException extends RuntimeException {

    /**
     * Crea una nueva excepción indicando que la evaluación está cerrada.
     *
     * @param mensaje descripción del motivo por el que no se permite la modificación.
     */
    public EvaluacionCerradaException(String mensaje) {
        super(mensaje);
    }
}
