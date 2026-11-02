package com.sigea.demosigea_backend.dto.evaluacion;

import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.PropuestaAutor;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * DTO de respuesta con el resumen de una asignación de evaluación y sus calificaciones (HU-17).
 *
 * @param asignacionId       ID único de la asignación.
 * @param propuestaId        ID de la propuesta asignada para evaluación.
 * @param propuestaTitulo    Título de la propuesta.
 * @param propuestaResumen   Resumen de la propuesta.
 * @param autores            Nombres de los autores de la propuesta, en orden de autoría.
 * @param convocatoriaId     ID de la convocatoria de la propuesta.
 * @param convocatoriaTitulo Título de la convocatoria.
 * @param fechaCierre        Fecha de cierre de la convocatoria; pasada esta fecha una evaluación enviada ya no se puede modificar.
 * @param comiteEvaluadorId  ID del miembro del comité evaluador.
 * @param evaluadorNombre    Nombre completo del evaluador.
 * @param fechaAsignacion    Fecha y hora en que se realizó la asignación.
 * @param estado             Estado actual de la asignación (pendiente, en_proceso, completada).
 * @param criterios          Criterios de la rúbrica activa de la convocatoria (vacía si no hay rúbrica activa).
 * @param evaluaciones       Lista de calificaciones registradas por criterio.
 *
 * @author SIGEA Development Team
 * @version 1.1
 * @since 2026
 */
public record AsignacionEvaluacionResponse(
        Long asignacionId,
        Long propuestaId,
        String propuestaTitulo,
        String propuestaResumen,
        List<String> autores,
        Long convocatoriaId,
        String convocatoriaTitulo,
        LocalDateTime fechaCierre,
        Long comiteEvaluadorId,
        String evaluadorNombre,
        LocalDateTime fechaAsignacion,
        String estado,
        List<CriterioRubricaResponse> criterios,
        List<EvaluacionResponse> evaluaciones
) {

    /**
     * Fábrica estática que construye el DTO a partir de la entidad {@link AsignacionEvaluacion},
     * los criterios de la rúbrica y la lista de respuestas de evaluación ya mapeadas.
     *
     * @param a            entidad de asignación de evaluación.
     * @param criterios    criterios de la rúbrica activa de la convocatoria.
     * @param evaluaciones lista de DTOs de calificaciones por criterio.
     * @return DTO de respuesta con todos los datos de la asignación.
     */
    public static AsignacionEvaluacionResponse fromEntity(AsignacionEvaluacion a,
                                                          List<CriterioRubricaResponse> criterios,
                                                          List<EvaluacionResponse> evaluaciones) {
        Propuesta p = a.getPropuesta();
        Convocatoria conv = a.getComiteEvaluador().getConvocatoria();
        String nombreEvaluador = nombreCompleto(a.getComiteEvaluador().getPersona());
        return new AsignacionEvaluacionResponse(
                a.getId(),
                p.getId(),
                p.getTitulo(),
                p.getResumen(),
                nombresAutores(p),
                conv != null ? conv.getId() : null,
                conv != null ? conv.getTitulo() : null,
                conv != null ? conv.getFechaCierre() : null,
                a.getComiteEvaluador().getId(),
                nombreEvaluador,
                a.getFechaAsignacion(),
                a.getEstado().name(),
                criterios == null ? List.of() : List.copyOf(criterios),
                evaluaciones
        );
    }

    private static List<String> nombresAutores(Propuesta p) {
        if (p.getAutores() == null) {
            return List.of();
        }
        return p.getAutores().stream()
                .sorted(Comparator.comparing(PropuestaAutor::getOrden,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(PropuestaAutor::getPersona)
                .map(AsignacionEvaluacionResponse::nombreCompleto)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String nombreCompleto(Persona persona) {
        if (persona == null) {
            return null;
        }
        return ((persona.getNombres() == null ? "" : persona.getNombres()) + " "
                + (persona.getApellidos() == null ? "" : persona.getApellidos())).trim();
    }
}
