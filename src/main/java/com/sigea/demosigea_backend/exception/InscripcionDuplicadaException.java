package com.sigea.demosigea_backend.exception;

/**
 * Excepción lanzada cuando un participante intenta inscribirse a un evento o actividad en los que ya está registrado previamente (HU-23, Criterio 4).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class InscripcionDuplicadaException extends RecursoDuplicadoException {

    public InscripcionDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
