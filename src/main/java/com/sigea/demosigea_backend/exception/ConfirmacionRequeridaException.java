package com.sigea.demosigea_backend.exception;

/**
 * Excepción de negocio lanzada cuando una operación sensible (p. ej. eliminar un evento o edición
 * con información ya registrada) requiere que el usuario confirme explícitamente la acción
 * (HU-04, Criterio 5).
 * <p>
 * El {@code GlobalExceptionHandler} la traduce a HTTP 409 con el código {@code CONFIRMACION_REQUERIDA},
 * para que el frontend muestre un diálogo de confirmación y reintente con {@code ?confirmar=true}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class ConfirmacionRequeridaException extends RuntimeException {

    /**
     * @param message Explicación de por qué se requiere la confirmación.
     */
    public ConfirmacionRequeridaException(String message) {
        super(message);
    }
}
