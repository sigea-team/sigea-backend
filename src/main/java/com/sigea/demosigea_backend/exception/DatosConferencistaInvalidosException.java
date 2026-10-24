package com.sigea.demosigea_backend.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Reglas de la HU-12 que no cubre Bean Validation (Criterio 2): años posteriores al actual,
 * trayectoria faltante al asociar a una persona que no la tiene, o cambio del correo de una
 * persona con cuenta de usuario. Se responde 400 {@code VALIDACION_FALLIDA} con el detalle por
 * campo, igual que los errores de validación del formulario.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class DatosConferencistaInvalidosException extends RuntimeException {

    /** Mensaje por campo del formulario. */
    private final transient Map<String, String> campos;

    public DatosConferencistaInvalidosException(String message, Map<String, String> campos) {
        super(message);
        this.campos = campos;
    }
}
