package com.sigea.demosigea_backend.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * HU-17, Criterios 2 y 3: se intenta registrar una evaluación con criterios sin calificar,
 * o se intenta calcular la puntuación final de una propuesta con asignaciones pendientes.
 * <p>
 * Lleva la lista de nombres de criterios faltantes para que el frontend pueda señalarlos
 * en el formulario. Se responde 400 BAD REQUEST con el código {@code EVALUACION_INCOMPLETA}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class EvaluacionIncompletaException extends RuntimeException {

    /** Nombres de los criterios que no fueron calificados en la solicitud. */
    private final List<String> criteriosFaltantes;

    /**
     * Crea una nueva excepción indicando los criterios que faltan por calificar.
     *
     * @param mensaje           descripción del problema de completitud.
     * @param criteriosFaltantes lista de nombres de criterios sin calificación.
     */
    public EvaluacionIncompletaException(String mensaje, List<String> criteriosFaltantes) {
        super(mensaje);
        this.criteriosFaltantes = criteriosFaltantes == null
                ? List.of()
                : Collections.unmodifiableList(criteriosFaltantes);
    }
}
