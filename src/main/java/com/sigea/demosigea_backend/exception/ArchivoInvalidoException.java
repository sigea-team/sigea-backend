package com.sigea.demosigea_backend.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HU-14, Criterio 2: uno o varios documentos de la propuesta no cumplen las reglas de carga
 * (formato no permitido, tamaño excedido, archivo vacío o documentos obligatorios faltantes).
 * <p>
 * {@code errores} indica, por cada archivo (o por el campo {@code documentos}), el motivo del rechazo,
 * para que el frontend pueda mostrarlo al autor.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class ArchivoInvalidoException extends RuntimeException {

    private final Map<String, String> errores;

    public ArchivoInvalidoException(String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.errores = errores == null ? Collections.emptyMap() : new LinkedHashMap<>(errores);
    }

    public Map<String, String> getErrores() {
        return Collections.unmodifiableMap(errores);
    }
}
