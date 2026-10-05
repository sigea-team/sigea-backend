package com.sigea.demosigea_backend.exception;

/**
 * Se lanza cuando se intenta registrar o modificar una línea temática con un nombre que ya existe
 * en el mismo evento (Criterio 2).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class LineaTematicaDuplicadaException extends RecursoDuplicadoException {

    public LineaTematicaDuplicadaException(String message) {
        super(message);
    }
}
