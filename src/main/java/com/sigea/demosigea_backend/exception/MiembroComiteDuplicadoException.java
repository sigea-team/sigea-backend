package com.sigea.demosigea_backend.exception;

/**
 * Se lanza cuando se intenta agregar al comité organizador a una persona que ya tiene una
 * participación vigente en el mismo evento (HU-06, Criterio 2).
 * <p>
 * Extiende {@link RecursoDuplicadoException} (409 CONFLICT), pero tiene su propio manejador en
 * {@code GlobalExceptionHandler} para responder con el código {@code MIEMBRO_COMITE_DUPLICADO}
 * en lugar de {@code CUENTA_YA_EXISTE}, que es específico del registro de usuarios.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class MiembroComiteDuplicadoException extends RecursoDuplicadoException {

    /**
     * @param message Descripción del duplicado (persona y evento).
     */
    public MiembroComiteDuplicadoException(String message) {
        super(message);
    }
}
