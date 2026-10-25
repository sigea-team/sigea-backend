package com.sigea.demosigea_backend.dto.asignacion;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Evaluador del comité de la convocatoria, visto desde una propuesta concreta, para que el organizador elija a
 * quién asignarla (HU-16, Criterios 1 y 2).
 *
 * @param comiteEvaluadorId    ID del miembro del comité evaluador.
 * @param personaId            ID de la persona evaluadora.
 * @param nombre               Nombre completo.
 * @param correo               Correo.
 * @param areaExperticia       Área de experticia.
 * @param asignacionesAbiertas Asignaciones aún no completadas del evaluador (carga de trabajo).
 * @param yaAsignado           Si ya tiene asignada esta propuesta.
 * @param conflicto            {@code NINGUNO}, {@code ADVERTENCIA} (requiere confirmar) o {@code BLOQUEANTE}.
 * @param detalleConflicto     Explicación del conflicto, o {@code null} si no hay.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Evaluador del comité disponible para la propuesta")
public record EvaluadorDisponibleResponse(
        @Schema(example = "3") Long comiteEvaluadorId,
        @Schema(example = "21") Long personaId,
        @Schema(example = "Ana Pérez") String nombre,
        @Schema(example = "ana.perez@ufps.edu.co") String correo,
        @Schema(example = "Ingeniería de software") String areaExperticia,
        @Schema(example = "2") long asignacionesAbiertas,
        @Schema(example = "false") boolean yaAsignado,
        @Schema(example = "NINGUNO", allowableValues = {"NINGUNO", "ADVERTENCIA", "BLOQUEANTE"}) String conflicto,
        @Schema(example = "Comparte la afiliación 'UFPS' con un autor de la propuesta.") String detalleConflicto
) {
}
