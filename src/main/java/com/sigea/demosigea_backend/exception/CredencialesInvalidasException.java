package com.sigea.demosigea_backend.exception;

/**
 * Excepción de regla de negocio lanzada cuando el proceso de autenticación falla
 * debido a credenciales incorrectas (identificador no encontrado o contraseña errónea).
 * <p>
 * Su propósito es abstraer el motivo exacto del fallo en las respuestas HTTP para evitar
 * la fuga de información sensible que pueda comprometer la seguridad del sistema.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class CredencialesInvalidasException extends RuntimeException {

    /**
     * Construye una nueva excepción con un mensaje descriptivo del error de autenticación.
     *
     * @param message Mensaje explicativo sobre la falla en la validación de credenciales.
     */
    public CredencialesInvalidasException(String message) {
        super(message);
    }
}