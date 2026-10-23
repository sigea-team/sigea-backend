package com.sigea.demosigea_backend.dto.evaluacion;

import com.sigea.demosigea_backend.model.AsignacionEvaluacion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta con el resumen de una asignación de evaluación y sus calificaciones (HU-17).
 *
 * @param asignacionId      ID único de la asignación.
 * @param propuestaId       ID de la propuesta asignada para evaluación.
 * @param propuestaTitulo   Título de la propuesta.
 * @param comiteEvaluadorId ID del miembro del comité evaluador.
 * @param evaluadorNombre   Nombre completo del evaluador.
 * @param fechaAsignacion   Fecha y hora en que se realizó la asignación.
 * @param estado            Estado actual de la asignación (pendiente, en_proceso, completada).
 * @param evaluaciones      Lista de calificaciones registradas por criterio.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record AsignacionEvaluacionResponse(
        Long asignacionId,
        Long propuestaId,
        String propuestaTitulo,
        Long comiteEvaluadorId,
        String evaluadorNombre,
        LocalDateTime fechaAsignacion,
        String estado,
        List<EvaluacionResponse> evaluaciones
) {

    /**
     * Fábrica estática que construye el DTO a partir de la entidad {@link AsignacionEvaluacion}
     * y la lista de respuestas de evaluación ya mapeadas.
     *
     * @param a           entidad de asignación de evaluación.
     * @param evaluaciones lista de DTOs de calificaciones por criterio.
     * @return DTO de respuesta con todos los datos de la asignación.
     */
    public static AsignacionEvaluacionResponse fromEntity(AsignacionEvaluacion a,
                                                          List<EvaluacionResponse> evaluaciones) {
        String nombreEvaluador = a.getComiteEvaluador().getPersona().getNombres()
                + " " + a.getComiteEvaluador().getPersona().getApellidos();
        return new AsignacionEvaluacionResponse(
                a.getId(),
                a.getPropuesta().getId(),
                a.getPropuesta().getTitulo(),
                a.getComiteEvaluador().getId(),
                nombreEvaluador,
                a.getFechaAsignacion(),
                a.getEstado().name(),
                evaluaciones
        );
    }
}
