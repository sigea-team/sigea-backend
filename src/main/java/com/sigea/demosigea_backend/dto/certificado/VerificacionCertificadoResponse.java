package com.sigea.demosigea_backend.dto.certificado;

import com.sigea.demosigea_backend.model.TipoCertificado;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Consulta pública de autenticidad de un certificado por su código (HU-26). El documento se muestra parcialmente.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos de un certificado auténtico emitido por SIGEA")
public record VerificacionCertificadoResponse(
        @Schema(description = "Código consultado", example = "CERT-3F9A1C07B2D44E5A9C61B0D2") String codigoVerificacion,
        @Schema(description = "Nombre impreso", example = "María Pérez") String nombreParticipante,
        @Schema(description = "Documento parcialmente oculto", example = "CC ******3456") String documento,
        @Schema(description = "Rol", example = "asistente") TipoCertificado tipo,
        @Schema(description = "Rol para mostrar", example = "Asistente") String tipoDescripcion,
        @Schema(description = "Evento", example = "Semana de Ingeniería de Sistemas") String eventoNombre,
        @Schema(description = "Fecha de inicio del evento", example = "2026-11-10") LocalDate fechaInicioEvento,
        @Schema(description = "Fecha de fin del evento", example = "2026-11-12") LocalDate fechaFinEvento,
        @Schema(description = "Fecha y hora de emisión", example = "2026-11-24T18:30:00") LocalDateTime fechaGeneracion
) {
}
