package com.sigea.demosigea_backend.dto.asistencia;

import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Asistencia;
import com.sigea.demosigea_backend.model.CategoriaParticipante;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.MetodoRegistroAsistencia;
import com.sigea.demosigea_backend.model.Persona;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Asistencia registrada de un participante a una actividad (HU-24, Criterio 2).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Asistencia registrada a una actividad")
public record AsistenciaResponse(
        @Schema(description = "ID de la asistencia", example = "40")
        Long asistenciaId,

        @Schema(description = "ID de la inscripción al evento", example = "15")
        Long inscripcionId,

        @Schema(description = "ID de la actividad", example = "25")
        Long actividadId,

        @Schema(description = "Nombre de la actividad", example = "Taller de Inteligencia Artificial")
        String actividadNombre,

        @Schema(description = "ID de la persona", example = "12")
        Long personaId,

        @Schema(description = "Nombre completo del participante", example = "María Pérez")
        String participanteNombre,

        @Schema(description = "Número de documento del participante", example = "1090123456")
        String numeroDocumento,

        @Schema(description = "Categoría de la inscripción", example = "estudiante")
        CategoriaParticipante categoria,

        @Schema(description = "Fecha y hora del registro", example = "2026-11-20T14:03:12")
        LocalDateTime fechaHoraRegistro,

        @Schema(description = "Método de registro", example = "qr")
        MetodoRegistroAsistencia metodoRegistro
) {
    public static AsistenciaResponse fromEntity(Asistencia a) {
        InscripcionEvento ie = a.getInscripcion();
        Persona p = ie != null ? ie.getPersona() : null;
        Actividad act = a.getActividad();
        return new AsistenciaResponse(
                a.getId(),
                ie != null ? ie.getId() : null,
                act != null ? act.getId() : null,
                act != null ? act.getNombre() : null,
                p != null ? p.getId() : null,
                CodigoQrResponse.nombreCompleto(p),
                p != null ? p.getNumeroDocumento() : null,
                ie != null ? ie.getCategoriaParticipante() : null,
                a.getFechaHoraRegistro(),
                a.getMetodoRegistro()
        );
    }
}
