package com.sigea.demosigea_backend.exception;

/**
 * Se lanza al intentar eliminar un tipo de actividad o una línea temática que está en uso en la
 * agenda o en propuestas (HU-05, Criterio 2).
 * <p>
 * Extiende {@link OperacionNoPermitidaException} (409 CONFLICT) pero tiene manejador propio en
 * {@code GlobalExceptionHandler}, que responde con el código {@code PARAMETRO_EN_USO}. El mensaje
 * informa el impacto (cuántas actividades / propuestas lo usan).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class ParametroEventoEnUsoException extends OperacionNoPermitidaException {

    /**
     * @param mensaje Descripción del uso que impide la eliminación.
     */
    public ParametroEventoEnUsoException(String mensaje) {
        super(mensaje);
    }
}
