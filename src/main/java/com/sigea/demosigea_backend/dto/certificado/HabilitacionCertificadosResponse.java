package com.sigea.demosigea_backend.dto.certificado;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de habilitar los certificados del evento (HU-26): la plantilla queda fija y se generan los certificados de
 * quienes cumplen los requisitos.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resultado de habilitar los certificados del evento")
public record HabilitacionCertificadosResponse(
        @Schema(description = "ID del evento", example = "3") Long eventoId,
        @Schema(description = "Fecha y hora de habilitación", example = "2026-12-04T10:15:00") LocalDateTime fechaHabilitacion,
        @Schema(description = "Certificados generados") List<CertificadoResponse> generados,
        @Schema(description = "Certificados que ya existían", example = "0") int yaEmitidos,
        @Schema(description = "Participantes que no cumplen los requisitos, con el motivo")
        List<ExclusionCertificadoResponse> excluidos
) {
}
