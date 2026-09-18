package com.sigea.demosigea_backend.exception;

import lombok.Getter;

/**
 * Excepción de regla de negocio lanzada cuando un usuario intenta realizar una acción
 * protegida (como iniciar sesión) sin haber verificado previamente su dirección de correo electrónico.
 * <p>
 * Transporta la dirección de correo involucrada para que el manejador global de excepciones
 * ({@code GlobalExceptionHandler}) pueda construir una respuesta estructurada que permita
 * al cliente del frontend ofrecer directamente la opción de reenvío de activación.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class CorreoNoVerificadoException extends RuntimeException {

    /**
     * Dirección de correo electrónico asociada al usuario sin verificar.
     */
    private final String correo;

    /**
     * Construye una nueva excepción con un mensaje explicativo y el correo involucrado.
     *
     * @param message Mensaje que describe la causa específica de la excepción.
     * @param correo  Dirección de correo electrónico asociada a la cuenta pendiente de activación.
     */
    public CorreoNoVerificadoException(String message, String correo) {
        super(message);
        this.correo = correo;
    }
}