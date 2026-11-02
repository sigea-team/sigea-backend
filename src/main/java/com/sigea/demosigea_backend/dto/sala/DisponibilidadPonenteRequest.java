package com.sigea.demosigea_backend.dto.sala;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO de petición para registrar o reemplazar la disponibilidad horaria de un ponente — HU-Salas, Criterio 2.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Disponibilidad horaria de un ponente en un evento")
public record DisponibilidadPonenteRequest(
        @NotNull(message = "El ID del ponente es obligatorio")
        @Schema(description = "ID de la persona (ponente)", example = "7")
        Long personaId,

        @Valid
        @Schema(description = "Lista de franjas horarias a guardar (reemplaza las existentes)")
        List<DisponibilidadHorariaRequest> franjas
) {
}
