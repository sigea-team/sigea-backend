package com.sigea.demosigea_backend.dto.lineatematica;

import com.sigea.demosigea_backend.model.LineaTematica;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta que representa la información de una línea temática registrada.
 *
 * @param id          Identificador de la línea temática.
 * @param eventoId    ID del evento al que pertenece.
 * @param nombre      Nombre de la línea temática.
 * @param descripcion Descripción de la línea temática.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Respuesta con los datos de una línea temática de un evento")
public record LineaTematicaResponse(

        @Schema(description = "Identificador único de la línea temática", example = "1")
        Long id,

        @Schema(description = "ID del evento asociado", example = "1")
        Long eventoId,

        @Schema(description = "Nombre del eje temático", example = "Inteligencia Artificial y Aprendizaje Automático")
        String nombre,

        @Schema(description = "Descripción o alcances", example = "Investigaciones aplicadas en IA.")
        String descripcion
) {
    /**
     * Mapea una entidad JPA {@link LineaTematica} a su DTO inmutable.
     */
    public static LineaTematicaResponse fromEntity(LineaTematica entity) {
        return new LineaTematicaResponse(
                entity.getId(),
                entity.getEvento() != null ? entity.getEvento().getId() : null,
                entity.getNombre(),
                entity.getDescripcion()
        );
    }
}
