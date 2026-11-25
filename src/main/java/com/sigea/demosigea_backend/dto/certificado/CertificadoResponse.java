package com.sigea.demosigea_backend.dto.certificado;

import com.sigea.demosigea_backend.model.Certificado;
import com.sigea.demosigea_backend.model.TipoCertificado;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Certificado emitido (HU-26). Los datos son los que quedaron impresos al emitirlo (Criterio 3).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Certificado de participación emitido")
public record CertificadoResponse(
        @Schema(description = "ID del certificado", example = "21")
        Long certificadoId,

        @Schema(description = "ID del evento", example = "3")
        Long eventoId,

        @Schema(description = "Nombre del evento impreso en el certificado", example = "Semana de Ingeniería de Sistemas")
        String eventoNombre,

        @Schema(description = "Fecha de inicio del evento", example = "2026-11-10")
        LocalDate fechaInicioEvento,

        @Schema(description = "Fecha de fin del evento", example = "2026-11-12")
        LocalDate fechaFinEvento,

        @Schema(description = "Rol certificado", example = "asistente")
        TipoCertificado tipo,

        @Schema(description = "Rol para mostrar", example = "Asistente")
        String tipoDescripcion,

        @Schema(description = "ID de la persona certificada", example = "12")
        Long personaId,

        @Schema(description = "Nombre impreso", example = "María Pérez")
        String nombreParticipante,

        @Schema(description = "Tipo de documento impreso", example = "CC")
        String tipoDocumento,

        @Schema(description = "Número de documento impreso", example = "1090123456")
        String numeroDocumento,

        @Schema(description = "Porcentaje de asistencia con el que se emitió (solo asistente)", example = "85.71")
        BigDecimal porcentajeAsistencia,

        @Schema(description = "Detalle impreso de la participación",
                example = "Asistió a 6 de las 7 actividades en las que se inscribió (85,71 %).")
        String detalle,

        @Schema(description = "Fecha y hora de emisión", example = "2026-11-24T18:30:00")
        LocalDateTime fechaGeneracion,

        @Schema(description = "Código para comprobar la autenticidad", example = "CERT-3F9A1C07B2D44E5A9C61B0D2")
        String codigoVerificacion,

        @Schema(description = "Ruta para descargar el PDF (requiere sesión)", example = "/api/v1/certificados/21/pdf")
        String urlDescarga
) {

    /** Ruta de descarga del PDF. */
    public static String rutaDescarga(Long certificadoId) {
        return "/api/v1/certificados/" + certificadoId + "/pdf";
    }

    public static CertificadoResponse fromEntity(Certificado c) {
        return new CertificadoResponse(
                c.getId(),
                c.getEvento() != null ? c.getEvento().getId() : null,
                c.getNombreEvento() != null ? c.getNombreEvento() : (c.getEvento() != null ? c.getEvento().getNombre() : null),
                c.getFechaInicioEvento(),
                c.getFechaFinEvento(),
                c.getTipo(),
                c.getTipo() != null ? c.getTipo().getDescripcion() : null,
                c.getPersona() != null ? c.getPersona().getId() : null,
                c.getNombreParticipante(),
                c.getTipoDocumento(),
                c.getNumeroDocumento(),
                c.getPorcentajeAsistencia(),
                c.getDetalle(),
                c.getFechaGeneracion(),
                c.getCodigoVerificacion(),
                rutaDescarga(c.getId())
        );
    }
}
