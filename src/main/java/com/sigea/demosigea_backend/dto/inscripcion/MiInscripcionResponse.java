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
        List<InscripcionActividadResponse> actividades,

        @Schema(description = "HU-24: código QR único de la inscripción (el mismo para todas sus actividades)",
                example = "SIGEA-9f1c2a7be04d4c6b8a3e51d7c2f09b44")
        String codigoQr
) {
    /** Constructor previo a HU-24, sin código QR. */
    public MiInscripcionResponse(Long inscripcionId, Long eventoId, String eventoNombre, Long personaId,
                                 String personaNombre, String personaCorreo, CategoriaParticipante categoria,
                                 LocalDateTime fechaInscripcion, List<InscripcionActividadResponse> actividades) {
        this(inscripcionId, eventoId, eventoNombre, personaId, personaNombre, personaCorreo, categoria,
                fechaInscripcion, actividades, null);
    }

    /** HU-24: detalle de la inscripción con su código QR. */
    public static MiInscripcionResponse fromEntity(InscripcionEvento ie, List<InscripcionActividadResponse> actividades,
                                                   String codigoQr) {
        MiInscripcionResponse base = fromEntity(ie, actividades);
        return new MiInscripcionResponse(base.inscripcionId(), base.eventoId(), base.eventoNombre(), base.personaId(),
                base.personaNombre(), base.personaCorreo(), base.categoria(), base.fechaInscripcion(),
                base.actividades(), codigoQr);
    }

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
