package com.sigea.demosigea_backend.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HU-11, Criterio 3: se intenta publicar una convocatoria a la que le faltan datos obligatorios
 * (por ejemplo, la fecha límite de recepción de propuestas).
 * <p>
 * Lleva el detalle de los campos que el organizador debe completar, para que el frontend pueda
 * señalarlos en el formulario. Se responde 400 con el código {@code CONVOCATORIA_INCOMPLETA}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class ConvocatoriaIncompletaException extends RuntimeException {

    /** Campo faltante → mensaje que solicita completarlo. */
    private final Map<String, String> camposFaltantes;

    public ConvocatoriaIncompletaException(String mensaje, Map<String, String> camposFaltantes) {
        super(mensaje);
        this.camposFaltantes = camposFaltantes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(camposFaltantes));
    }
}
