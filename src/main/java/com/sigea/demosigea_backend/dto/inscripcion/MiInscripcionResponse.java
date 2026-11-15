package com.sigea.demosigea_backend.dto.inscripcion;

import com.sigea.demosigea_backend.model.CategoriaParticipante;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.Persona;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta completa de la inscripción del participante con sus actividades asociadas (HU-23).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resumen de la inscripción del participante y sus actividades inscritas")
public record MiInscripcionResponse(
        @Schema(description = "Identificador de la inscripción", example = "1")
        Long inscripcionId,

        @Schema(description = "ID del evento", example = "5")
        Long eventoId,

        @Schema(description = "Nombre del evento", example = "Congreso Internacional de Ingeniería")
        String eventoNombre,

        @Schema(description = "ID de la persona", example = "12")
        Long personaId,

        @Schema(description = "Nombre completo del participante", example = "María Pérez")
        String personaNombre,

        @Schema(description = "Correo del participante", example = "maria.perez@ufps.edu.co")
        String personaCorreo,

        @Schema(description = "Categoría en la que se inscribió", example = "estudiante")
        CategoriaParticipante categoria,

        @Schema(description = "Fecha de inscripción al evento", example = "2026-11-15T10:00:00")
        LocalDateTime fechaInscripcion,

        @Schema(description = "Lista de actividades en las que se encuentra inscrito")
        List<InscripcionActividadResponse> actividades
) {
    public static MiInscripcionResponse fromEntity(InscripcionEvento ie, List<InscripcionActividadResponse> actividades) {
        Persona p = ie.getPersona();
        String nombre = p != null ? ((p.getNombres() != null ? p.getNombres() : "") + " "
                + (p.getApellidos() != null ? p.getApellidos() : "")).trim() : null;
        String correo = p != null ? p.getCorreo() : null;

        return new MiInscripcionResponse(
                ie.getId(),
                ie.getEvento() != null ? ie.getEvento().getId() : null,
                ie.getEvento() != null ? ie.getEvento().getNombre() : null,
                p != null ? p.getId() : null,
                nombre,
                correo,
                ie.getCategoriaParticipante(),
                ie.getFechaInscripcion(),
                actividades
        );
    }
}
