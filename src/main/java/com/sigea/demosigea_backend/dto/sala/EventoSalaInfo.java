package com.sigea.demosigea_backend.dto.sala;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumen de un evento al que está vinculada una sala institucional — HU-Salas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resumen del evento vinculado a la sala")
public record EventoSalaInfo(
        @Schema(description = "ID del evento", example = "1")
        Long id,

        @Schema(description = "Nombre del evento", example = "Seminario de Computación 2026")
        String nombre,

        @Schema(description = "Semestre o edición", example = "2026-1")
        String semestre,

        @Schema(description = "Estado del evento", example = "habilitado")
        String estado
) {
}
