package com.sigea.demosigea_backend.exception;

import lombok.Getter;

@Getter
public class CorreoNoVerificadoException extends RuntimeException {

    private final String correo;

    public CorreoNoVerificadoException(String message, String correo) {
        super(message);
        this.correo = correo;
    }
}
