package com.sigea.demosigea_backend.dto.certificado;

import com.sigea.demosigea_backend.model.TipoCertificado;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estado de un rol del participante en el evento: si cumple los requisitos y si ya tiene el certificado (HU-26).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Certificado que le corresponde al participante por un rol y si cumple los requisitos")
public record OpcionCertificadoResponse(
        @Schema(description = "Rol", example = "asistente")
        TipoCertificado tipo,

        @Schema(description = "Rol para mostrar", example = "Asistente")
        String tipoDescripcion,

        @Schema(description = "Si cumple los requisitos de asistencia o participación del rol", example = "true")
        boolean cumpleRequisitos,

        @Schema(description = "Si puede generarlo ahora (cumple, el evento terminó, no está archivado y aún no lo tiene)",
                example = "true")
        boolean puedeGenerar,

        @Schema(description = "Por qué no cumple o no puede generarlo; null si puede",
                example = "Asististe a 3 de las 7 actividades en las que te inscribiste (42,85 %); el mínimo es 80 %.")
        String motivo,

        @Schema(description = "Asistente: actividades inscritas que cuentan (no canceladas ni por programar)", example = "7")
        Long actividadesConsideradas,

        @Schema(description = "Asistente: actividades con asistencia registrada", example = "6")
        Long actividadesAsistidas,

        @Schema(description = "Asistente: porcentaje de asistencia (truncado a 2 decimales)", example = "85.71")
        BigDecimal porcentajeAsistencia,

        @Schema(description = "Ponente o conferencista: actividades realizadas a su cargo")
        List<String> actividadesACargo,

        @Schema(description = "Certificado ya emitido para este rol, o null")
        CertificadoResponse certificado
) {
}
