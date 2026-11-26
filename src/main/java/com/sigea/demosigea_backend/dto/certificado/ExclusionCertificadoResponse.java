package com.sigea.demosigea_backend.dto.certificado;

import com.sigea.demosigea_backend.model.TipoCertificado;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Participante al que no se le generó un certificado en la generación del evento y el motivo (CU-23, flujo alterno).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Participante excluido de la generación y su motivo")
public record ExclusionCertificadoResponse(
        @Schema(description = "ID de la persona", example = "12") Long personaId,
        @Schema(description = "Nombre", example = "María Pérez") String nombre,
        @Schema(description = "Número de documento", example = "1090123456") String numeroDocumento,
        @Schema(description = "Rol", example = "asistente") TipoCertificado tipo,
        @Schema(description = "Motivo", example = "Asistió a 3 de las 7 actividades en las que se inscribió (42,85 %); el mínimo es 80 %.")
        String motivo
) {
}
