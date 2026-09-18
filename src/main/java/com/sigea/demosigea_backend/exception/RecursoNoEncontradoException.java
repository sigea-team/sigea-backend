package com.sigea.demosigea_backend.exception;

/**
 * Excepción de regla de negocio lanzada cuando una entidad o recurso solicitado
 * no es localizado en la base de datos de la plataforma SIGEA.
 * <p>
 * Es procesada centralizadamente por el {@code GlobalExceptionHandler} para emitir
 * una respuesta con código de estado HTTP 404 (NOT_FOUND).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class RecursoNoEncontradoException extends RuntimeException {

    /**
     * Construye una nueva excepción indicando el recurso o la entidad que no pudo ser hallada.
     *
     * @param message Mensaje descriptivo con los criterios de búsqueda que fallaron.
     */
    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}