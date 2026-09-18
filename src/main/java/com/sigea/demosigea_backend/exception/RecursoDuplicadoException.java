package com.sigea.demosigea_backend.exception;

/**
 * Excepción de regla de negocio lanzada cuando se intenta registrar o crear
 * un recurso cuyo identificador o campo único ya existe en la base de datos
 * (ej. correo electrónico, número de documento o nombre de usuario en uso).
 * <p>
 * Es procesada por el {@code GlobalExceptionHandler} para retornar un estado
 * HTTP 409 (CONFLICT) al cliente.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class RecursoDuplicadoException extends RuntimeException {

    /**
     * Construye una nueva excepción con un mensaje detallado sobre el recurso duplicado.
     *
     * @param message Descripción específica de la duplicidad encontrada.
     */
    public RecursoDuplicadoException(String message) {
        super(message);
    }
}