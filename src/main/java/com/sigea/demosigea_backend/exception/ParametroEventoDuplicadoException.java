package com.sigea.demosigea_backend.exception;

/**
 * Se lanza al crear o renombrar un tipo de actividad o una línea temática con un nombre que ya
 * existe en el mismo evento (HU-05, Criterio 3).
 * <p>
 * Extiende {@link RecursoDuplicadoException} (409 CONFLICT) pero tiene manejador propio en
 * {@code GlobalExceptionHandler}, que responde con el código {@code PARAMETRO_DUPLICADO}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class ParametroEventoDuplicadoException extends RecursoDuplicadoException {

    /**
     * @param message Descripción del duplicado (nombre y evento).
     */
    public ParametroEventoDuplicadoException(String message) {
        super(message);
    }
}
