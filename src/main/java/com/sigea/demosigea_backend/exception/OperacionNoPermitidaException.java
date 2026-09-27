package com.sigea.demosigea_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción de negocio lanzada cuando una operación solicitada es rechazada
 * debido a restricciones de integridad referencial controlada o políticas de dominio del sistema.
 * <p>
 * Un ejemplo típico es el intento de eliminación de un rol que posee usuarios con estado activo
 * asignados (Criterio 3 de la HU-02), donde se requiere una reasignación previa de dichos usuarios.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class OperacionNoPermitidaException extends RuntimeException {

    /**
     * Construye una nueva excepción con el mensaje descriptivo del motivo del rechazo.
     *
     * @param mensaje Explicación legible para el usuario o cliente API.
     */
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
