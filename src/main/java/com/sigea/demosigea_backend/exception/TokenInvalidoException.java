package com.sigea.demosigea_backend.exception;

/**
 * Excepción de regla de negocio lanzada cuando un token de acceso (JWT) o de
 * verificación de correo electrónico resulta ser inválido, malformado, alterado o ha expirado.
 * <p>
 * Es procesada centralizadamente por el {@code GlobalExceptionHandler} para devolver
 * una respuesta con código de estado HTTP 400 (BAD_REQUEST) o 401 (UNAUTHORIZED)
 * según el flujo correspondiente.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class TokenInvalidoException extends RuntimeException {

    /**
     * Construye una nueva excepción especificando el motivo por el cual el token no es válido.
     *
     * @param message Mensaje descriptivo con el detalle de la falla de validación del token.
     */
    public TokenInvalidoException(String message) {
        super(message);
    }
}