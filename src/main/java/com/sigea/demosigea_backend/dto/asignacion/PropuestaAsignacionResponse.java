package com.sigea.demosigea_backend.dto.asignacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Propuesta de una convocatoria con el resumen de sus asignaciones, para que el organizador vea qué propuestas
 * le falta asignar (HU-16, Criterio 1).
 *
 * @param propuestaId             ID de la propuesta.
 * @param titulo                  Título de la propuesta.
 * @param estado                  Estado de la propuesta.
 * @param fechaEnvio              Fecha y hora de envío.
 * @param autores                 Nombres de los autores, en orden de autoría.
 * @param evaluadores             Nombres de los evaluadores que la tienen asignada actualmente.
 * @param asignacionesTotales     Número de evaluadores asignados.
 * @param asignacionesCompletadas Número de evaluaciones ya completadas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Propuesta de la convocatoria con el resumen de sus asignaciones")
public record PropuestaAsignacionResponse(
        @Schema(example = "40") Long propuestaId,
        @Schema(example = "Analítica de datos en la educación superior") String titulo,
        @Schema(example = "en_evaluacion") String estado,
        LocalDateTime fechaEnvio,
        List<String> autores,
        List<String> evaluadores,
        @Schema(example = "2") int asignacionesTotales,
        @Schema(example = "1") int asignacionesCompletadas
) {
}
