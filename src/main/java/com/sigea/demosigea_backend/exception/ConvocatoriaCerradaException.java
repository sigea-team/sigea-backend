package com.sigea.demosigea_backend.exception;

/**
 * Excepción lanzada cuando se intenta realizar una acción sobre una convocatoria
 * cuyo periodo de recepción de propuestas ha finalizado o no está abierta.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class ConvocatoriaCerradaException extends RuntimeException {

    public ConvocatoriaCerradaException(String message) {
        super(message);
    }
}
