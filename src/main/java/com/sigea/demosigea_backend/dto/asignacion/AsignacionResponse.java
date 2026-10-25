package com.sigea.demosigea_backend.dto.asignacion;

import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.ComiteEvaluador;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Asignación de una propuesta a un evaluador, tal como la ven el organizador y la bandeja del evaluador (HU-16).
 *
 * @param asignacionId       ID de la asignación.
 * @param propuestaId        ID de la propuesta.
 * @param propuestaTitulo    Título de la propuesta.
 * @param estadoPropuesta    Estado de la propuesta.
 * @param convocatoriaId     ID de la convocatoria.
 * @param convocatoriaTitulo Título de la convocatoria.
 * @param comiteEvaluadorId  ID del miembro del comité evaluador.
 * @param evaluadorPersonaId ID de la persona evaluadora.
 * @param evaluadorNombre    Nombre completo del evaluador.
 * @param evaluadorCorreo    Correo del evaluador.
 * @param areaExperticia     Área de experticia del evaluador.
 * @param fechaAsignacion    Fecha y hora de la asignación (o de la última reasignación).
 * @param estado             Estado de la asignación (pendiente, en_proceso, completada).
 * @param advertencias       Advertencias de conflicto de interés confirmadas en esta operación (vacía si no hay).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Asignación de una propuesta a un evaluador del comité")
public record AsignacionResponse(
        @Schema(example = "12") Long asignacionId,
        @Schema(example = "40") Long propuestaId,
        @Schema(example = "Analítica de datos en la educación superior") String propuestaTitulo,
        @Schema(example = "en_evaluacion") String estadoPropuesta,
        @Schema(example = "10") Long convocatoriaId,
        @Schema(example = "Convocatoria de Ponencias 2026") String convocatoriaTitulo,
        @Schema(example = "3") Long comiteEvaluadorId,
        @Schema(example = "21") Long evaluadorPersonaId,
        @Schema(example = "Ana Pérez") String evaluadorNombre,
        @Schema(example = "ana.perez@ufps.edu.co") String evaluadorCorreo,
        @Schema(example = "Ingeniería de software") String areaExperticia,
        LocalDateTime fechaAsignacion,
        @Schema(example = "pendiente") String estado,
        List<String> advertencias
) {

    /** Construye la respuesta sin advertencias (listados y bandeja). */
    public static AsignacionResponse fromEntity(AsignacionEvaluacion a) {
        return fromEntity(a, List.of());
    }

    /** Construye la respuesta con las advertencias de conflicto confirmadas en la operación. */
    public static AsignacionResponse fromEntity(AsignacionEvaluacion a, List<String> advertencias) {
        Propuesta p = a.getPropuesta();
        ComiteEvaluador ce = a.getComiteEvaluador();
        Persona persona = ce.getPersona();
        return new AsignacionResponse(
                a.getId(),
                p.getId(),
                p.getTitulo(),
                p.getEstado() != null ? p.getEstado().name() : null,
                p.getConvocatoria() != null ? p.getConvocatoria().getId() : null,
                p.getConvocatoria() != null ? p.getConvocatoria().getTitulo() : null,
                ce.getId(),
                persona.getId(),
                nombreCompleto(persona),
                persona.getCorreo(),
                ce.getAreaExperticia(),
                a.getFechaAsignacion(),
                a.getEstado() != null ? a.getEstado().name() : null,
                advertencias == null ? List.of() : List.copyOf(advertencias)
        );
    }

    static String nombreCompleto(Persona persona) {
        if (persona == null) {
            return null;
        }
        return (persona.getNombres() + " " + persona.getApellidos()).trim();
    }
}
