package com.sigea.demosigea_backend.dto.certificado;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Certificados que le corresponden al usuario autenticado en un evento (HU-26).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Certificados del participante en un evento")
public record MisCertificadosEventoResponse(
        @Schema(description = "ID del evento", example = "3")
        Long eventoId,

        @Schema(description = "Nombre del evento", example = "Semana de Ingeniería de Sistemas")
        String eventoNombre,

        @Schema(description = "Fecha de fin del evento", example = "2026-11-12")
        LocalDate fechaFinEvento,

        @Schema(description = "Si el evento terminó (los certificados se generan al terminar)", example = "true")
        boolean eventoFinalizado,

        @Schema(description = "Si el evento fue cerrado formalmente (solo se entregan los certificados ya emitidos)",
                example = "false")
        boolean eventoArchivado,

        @Schema(description = "Porcentaje mínimo de asistencia del evento para el certificado de asistente", example = "80")
        BigDecimal porcentajeMinimoAsistencia,

        @Schema(description = "Un elemento por rol del participante; vacío si no participó en el evento")
        List<OpcionCertificadoResponse> opciones
) {
}
