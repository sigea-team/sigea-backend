package com.sigea.demosigea_backend.dto.memoria;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** 409 CIERRE_CON_PENDIENTES: el evento tiene procesos pendientes y no se confirmó el cierre (CU-32, flujo 1.2). */
@Schema(description = "Cierre formal rechazado por procesos pendientes sin confirmar")
public record CierreConPendientesResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String codigo,
        String path,
        List<PendienteCierreResponse> pendientes
) {
}
