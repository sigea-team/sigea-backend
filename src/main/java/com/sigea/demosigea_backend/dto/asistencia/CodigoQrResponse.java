package com.sigea.demosigea_backend.dto.asistencia;

import com.sigea.demosigea_backend.model.CodigoQr;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.Persona;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Código QR de una inscripción al evento (HU-24, Criterio 1). El frontend dibuja el QR con {@code codigo}
 * o pide la imagen PNG a {@code /inscripciones/mi-inscripcion/qr/imagen}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Código QR único de una inscripción al evento")
public record CodigoQrResponse(
        @Schema(description = "ID del código QR", example = "7")
        Long qrId,

        @Schema(description = "ID de la inscripción al evento", example = "15")
        Long inscripcionId,

        @Schema(description = "ID del evento", example = "3")
        Long eventoId,

        @Schema(description = "Nombre del evento", example = "Semana de Ingeniería de Sistemas")
        String eventoNombre,

        @Schema(description = "ID de la persona inscrita", example = "12")
        Long personaId,

        @Schema(description = "Nombre completo del participante", example = "María Pérez")
        String participanteNombre,

        @Schema(description = "Correo del participante", example = "maria.perez@ufps.edu.co")
        String participanteCorreo,

        @Schema(description = "Valor codificado en el QR", example = "SIGEA-9f1c2a7be04d4c6b8a3e51d7c2f09b44")
        String codigo,

        @Schema(description = "Fecha y hora de generación", example = "2026-11-15T10:00:00")
        LocalDateTime fechaGeneracion,

        @Schema(description = "Si el código se acepta para registrar asistencia", example = "true")
        boolean vigente
) {
    public static CodigoQrResponse fromEntity(CodigoQr qr) {
        InscripcionEvento ie = qr.getInscripcion();
        Persona p = ie != null ? ie.getPersona() : null;
        return new CodigoQrResponse(
                qr.getId(),
                ie != null ? ie.getId() : null,
                ie != null && ie.getEvento() != null ? ie.getEvento().getId() : null,
                ie != null && ie.getEvento() != null ? ie.getEvento().getNombre() : null,
                p != null ? p.getId() : null,
                nombreCompleto(p),
                p != null ? p.getCorreo() : null,
                qr.getCodigo(),
                qr.getFechaGeneracion(),
                qr.estaVigente()
        );
    }

    static String nombreCompleto(Persona p) {
        if (p == null) {
            return null;
        }
        return ((p.getNombres() != null ? p.getNombres() : "") + " "
                + (p.getApellidos() != null ? p.getApellidos() : "")).trim();
    }
}
