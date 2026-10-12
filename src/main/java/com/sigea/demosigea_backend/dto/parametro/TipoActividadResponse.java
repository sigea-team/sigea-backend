package com.sigea.demosigea_backend.dto.parametro;

import com.sigea.demosigea_backend.model.TipoActividad;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta con un tipo de actividad del evento (HU-05).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Tipo de actividad configurado en un evento")
public record TipoActividadResponse(
        @Schema(description = "ID del tipo de actividad", example = "4") Long id,
        @Schema(example = "1") Long eventoId,
        @Schema(example = "Taller") String nombre,
        @Schema(example = "Sesión práctica de 2 a 4 horas con cupo limitado") String descripcion
) {
    /**
     * @param tipo Entidad JPA.
     * @return DTO inmutable.
     */
    public static TipoActividadResponse fromEntity(TipoActividad tipo) {
        return new TipoActividadResponse(tipo.getId(), tipo.getEvento().getId(), tipo.getNombre(), tipo.getDescripcion());
    }
}
