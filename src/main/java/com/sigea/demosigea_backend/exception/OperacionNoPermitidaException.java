package com.sigea.demosigea_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando una operación sobre un recurso es rechazada
 * debido a restricciones de integridad o lógica de negocio (ej. eliminar un rol con usuarios activos).
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
