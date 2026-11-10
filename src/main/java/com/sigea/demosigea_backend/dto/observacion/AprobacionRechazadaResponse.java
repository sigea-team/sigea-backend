package com.sigea.demosigea_backend.dto.observacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo de la respuesta 409 cuando no se aprueba la versión definitiva (HU-19, Criterio 4): indica los ajustes
 * pendientes.
 *
 * @param timestamp  fecha y hora de la respuesta.
 * @param status     código HTTP (409).
 * @param error      nombre del error HTTP.
 * @param message    explicación.
 * @param codigo     {@code OBSERVACIONES_PENDIENTES} o {@code AJUSTES_SIN_REGISTRAR}.
 * @param path       URI de la petición.
 * @param pendientes ajustes requeridos sin resolver.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Versión definitiva no aprobada por tener ajustes pendientes")
public record AprobacionRechazadaResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        @Schema(example = "OBSERVACIONES_PENDIENTES") String codigo,
        String path,
        List<ObservacionResponse> pendientes
) {
}
