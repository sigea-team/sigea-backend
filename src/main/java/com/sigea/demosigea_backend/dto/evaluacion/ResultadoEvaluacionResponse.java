package com.sigea.demosigea_backend.dto.evaluacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta con el resultado de evaluación ponderada de una propuesta (HU-17).
 *
 * @param resultadoId         ID del resultado (null si aún no está calculado).
 * @param propuestaId         ID de la propuesta evaluada.
 * @param propuestaTitulo     Título de la propuesta evaluada.
 * @param puntuacionPonderada Puntuación ponderada calculada (null si incompleto).
 * @param clasificacion       Clasificación final: aprobada, aprobada_con_ajustes o rechazada (null si incompleto).
 * @param fechaCalculo        Fecha y hora del cálculo (null si incompleto).
 * @param completo            {@code true} si todas las asignaciones están completadas y el resultado fue calculado.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record ResultadoEvaluacionResponse(
        Long resultadoId,
        Long propuestaId,
        String propuestaTitulo,
        BigDecimal puntuacionPonderada,
        String clasificacion,
        LocalDateTime fechaCalculo,
        boolean completo
) {
}
