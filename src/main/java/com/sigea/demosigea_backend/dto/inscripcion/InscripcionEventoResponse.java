package com.sigea.demosigea_backend.dto.inscripcion;

import com.sigea.demosigea_backend.model.CategoriaParticipante;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.Persona;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Respuesta con los datos de una inscripción a un evento (HU-23).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Detalle de la inscripción a un evento")
public record InscripcionEventoResponse(
        @Schema(description = "Identificador de la inscripción", example = "1")
        Long inscripcionId,

        @Schema(description = "ID del evento", example = "5")
        Long eventoId,

        @Schema(description = "Nombre del evento", example = "Congreso Internacional de Ingeniería")
        String eventoNombre,

        @Schema(description = "ID de la persona inscrita", example = "12")
        Long personaId,

        @Schema(description = "Nombre completo del participante", example = "María Pérez")
        String personaNombre,

        @Schema(description = "Correo del participante", example = "maria.perez@ufps.edu.co")
        String personaCorreo,

        @Schema(description = "Categoría del participante", example = "estudiante")
        CategoriaParticipante categoria,

        @Schema(description = "Fecha y hora del registro de inscripción", example = "2026-11-15T10:00:00")
        LocalDateTime fechaInscripcion,

        @Schema(description = "Cantidad de actividades inscritas por el participante", example = "2")
        int totalActividadesInscritas
) {
    public static InscripcionEventoResponse fromEntity(InscripcionEvento ie) {
        Persona p = ie.getPersona();
        String nombre = p != null ? ((p.getNombres() != null ? p.getNombres() : "") + " "
                + (p.getApellidos() != null ? p.getApellidos() : "")).trim() : null;
        String correo = p != null ? p.getCorreo() : null;
        int total = ie.getActividadesInscritas() != null ? ie.getActividadesInscritas().size() : 0;

        return new InscripcionEventoResponse(
                ie.getId(),
                ie.getEvento() != null ? ie.getEvento().getId() : null,
                ie.getEvento() != null ? ie.getEvento().getNombre() : null,
                p != null ? p.getId() : null,
                nombre,
                correo,
                ie.getCategoriaParticipante(),
                ie.getFechaInscripcion(),
                total
        );
    }
}
