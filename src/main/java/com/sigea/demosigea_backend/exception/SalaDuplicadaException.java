package com.sigea.demosigea_backend.exception;

/**
 * Excepción lanzada cuando se intenta crear o actualizar una sala con un nombre que ya existe
 * en el mismo evento (sin distinguir mayúsculas) — HU-Salas, Criterio 3.
 * <p>
 * Es procesada por el {@code GlobalExceptionHandler} para retornar HTTP 409 con el código
 * {@code SALA_DUPLICADA}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class SalaDuplicadaException extends RecursoDuplicadoException {

    /**
     * Construye la excepción con el mensaje descriptivo del duplicado.
     *
     * @param message Mensaje con el nombre duplicado y el evento.
     */
    public SalaDuplicadaException(String message) {
        super(message);
    }
}
