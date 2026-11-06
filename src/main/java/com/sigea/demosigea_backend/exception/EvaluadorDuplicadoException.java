package com.sigea.demosigea_backend.exception;

/**
 * HU-15, Criterio 4: ya existe un evaluador con ese correo en el comité de la convocatoria.
 * <p>
 * Al ser subclase de {@link RecursoDuplicadoException}, el {@code GlobalExceptionHandler} la atiende con su
 * manejador específico y responde 409 con el código {@code EVALUADOR_DUPLICADO}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class EvaluadorDuplicadoException extends RecursoDuplicadoException {

    /**
     * @param message descripción del duplicado.
     */
    public EvaluadorDuplicadoException(String message) {
        super(message);
    }
}
