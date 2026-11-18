package com.sigea.demosigea_backend.dto.asistencia;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Respuesta de error cuando no se registra la asistencia (HU-24, Criterio 3). Tiene los mismos campos que
 * {@code ErrorResponse} y, si la asistencia ya existía, la asistencia registrada antes.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Registro de asistencia rechazado")
public record AsistenciaRechazadaResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,

        @Schema(description = "QR_INVALIDO, QR_NO_VIGENTE, QR_OTRO_EVENTO, NO_INSCRITO_ACTIVIDAD, "
                + "ACTIVIDAD_NO_DISPONIBLE, EVENTO_NO_DISPONIBLE o ASISTENCIA_DUPLICADA", example = "ASISTENCIA_DUPLICADA")
        String codigo,

        String path,

        @Schema(description = "Asistencia registrada antes (solo con ASISTENCIA_DUPLICADA)")
        AsistenciaResponse asistenciaExistente
) {}
