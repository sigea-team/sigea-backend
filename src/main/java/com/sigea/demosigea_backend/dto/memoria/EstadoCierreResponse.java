package com.sigea.demosigea_backend.dto.memoria;

import com.sigea.demosigea_backend.model.EstadoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado del evento frente al cierre formal: si ya tiene memoria histórica, si se puede cerrar y qué procesos quedan
 * pendientes (HU-28, CU-32).
 */
@Schema(description = "Revisión previa al cierre formal del evento (HU-28)")
public record EstadoCierreResponse(
        Long eventoId,
        String nombre,
        EstadoEvento estado,
        LocalDate fechaFin,
        @Schema(description = "El evento ya fue cerrado formalmente") boolean consolidada,
        @Schema(description = "Fecha del cierre formal, si ya se hizo") LocalDateTime fechaConsolidacion,
        @Schema(description = "Se puede cerrar formalmente ahora") boolean puedeCerrarse,
        @Schema(description = "Por qué no se puede cerrar todavía") String motivoNoCerrable,
        @Schema(description = "Procesos pendientes; para cerrar con pendientes se envía confirmarPendientes=true")
        List<PendienteCierreResponse> pendientes
) {
}
