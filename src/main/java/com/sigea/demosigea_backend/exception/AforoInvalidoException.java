package com.sigea.demosigea_backend.exception;

/**
 * Excepción lanzada cuando el nuevo aforo máximo de una sala quedaría por debajo del número
 * de inscripciones activas en alguna de sus actividades — HU-Salas, Criterio 4.
 * <p>
 * Es procesada por el {@code GlobalExceptionHandler} para retornar HTTP 409 con el código
 * {@code AFORO_INVALIDO}. Se advierte al usuario del conflicto sin bloquear la operación
 * permanentemente, permitiendo que el administrador confirme si desea proceder.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class AforoInvalidoException extends OperacionNoPermitidaException {

    /**
     * Construye la excepción con el mensaje descriptivo del conflicto de aforo.
     *
     * @param mensaje Descripción del conflicto (aforo nuevo vs inscripciones existentes).
     */
    public AforoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
