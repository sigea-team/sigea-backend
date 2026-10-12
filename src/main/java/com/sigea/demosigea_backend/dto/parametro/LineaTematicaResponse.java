package com.sigea.demosigea_backend.dto.parametro;

import com.sigea.demosigea_backend.model.LineaTematica;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta con una línea temática del evento (HU-05).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Línea temática configurada en un evento")
public record LineaTematicaResponse(
        @Schema(description = "ID de la línea temática", example = "7") Long id,
        @Schema(example = "1") Long eventoId,
        @Schema(example = "Inteligencia artificial y ciencia de datos") String nombre,
        @Schema(example = "Aprendizaje automático, analítica y visualización de datos") String descripcion
) {
    /**
     * @param linea Entidad JPA.
     * @return DTO inmutable.
     */
    public static LineaTematicaResponse fromEntity(LineaTematica linea) {
        return new LineaTematicaResponse(linea.getId(), linea.getEvento().getId(), linea.getNombre(), linea.getDescripcion());
    }
}
