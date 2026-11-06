package com.sigea.demosigea_backend.exception;

import lombok.Getter;

/**
 * HU-15, Criterio 4: se intenta modificar una rúbrica que ya tiene calificaciones registradas.
 * <p>
 * Informa el impacto (calificaciones, asignaciones y propuestas afectadas) para que el organizador decida.
 * El {@code GlobalExceptionHandler} responde 409 con el código {@code RUBRICA_EN_USO}; el frontend
 * reintenta con {@code ?confirmar=true} si el organizador acepta reemplazar la rúbrica.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class RubricaEnUsoException extends ConfirmacionRequeridaException {

    /** Calificaciones ya registradas con los criterios de la rúbrica. */
    private final long calificacionesRegistradas;

    /** Asignaciones de evaluación que tendrían que volver a calificarse. */
    private final long asignacionesAfectadas;

    /** Propuestas distintas afectadas. */
    private final long propuestasAfectadas;

    /**
     * @param mensaje                   explicación del impacto.
     * @param calificacionesRegistradas calificaciones registradas con la rúbrica.
     * @param asignacionesAfectadas     asignaciones que se reabrirían.
     * @param propuestasAfectadas       propuestas afectadas.
     */
    public RubricaEnUsoException(String mensaje, long calificacionesRegistradas, long asignacionesAfectadas,
                                 long propuestasAfectadas) {
        super(mensaje);
        this.calificacionesRegistradas = calificacionesRegistradas;
        this.asignacionesAfectadas = asignacionesAfectadas;
        this.propuestasAfectadas = propuestasAfectadas;
    }
}
