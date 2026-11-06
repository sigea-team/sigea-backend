package com.sigea.demosigea_backend.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Excepción lanzada cuando la rúbrica enviada no cumple las reglas de negocio (HU-15, Criterio 2): los pesos
 * no suman 100 %, hay criterios repetidos o una escala es inválida.
 * <p>
 * El {@code GlobalExceptionHandler} la traduce a HTTP 400 con el código {@code RUBRICA_INVALIDA} y el detalle
 * por campo en {@code erroresValidacion}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class RubricaInvalidaException extends RuntimeException {

    /** Campo en error (p. ej. {@code pesoPorcentual} o {@code criterios[1].escalaMaxima}) y su mensaje. */
    private final Map<String, String> campos;

    /**
     * @param mensaje mensaje general del rechazo.
     * @param campos  detalle por campo.
     */
    public RubricaInvalidaException(String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.campos = campos;
    }
}
