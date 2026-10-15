package com.sigea.demosigea_backend.dto.tipoevento;

import com.sigea.demosigea_backend.model.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta con un tipo de evento del catálogo.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Tipo de evento del catálogo")
public record TipoEventoResponse(
        @Schema(description = "ID del tipo de evento", example = "1") Long id,
        @Schema(example = "Congreso") String nombre,
        @Schema(example = "Evento de varios días con conferencias y ponencias evaluadas") String descripcion
) {
    /**
     * @param tipo Entidad JPA.
     * @return DTO inmutable.
     */
    public static TipoEventoResponse fromEntity(TipoEvento tipo) {
        return new TipoEventoResponse(tipo.getId(), tipo.getNombre(), tipo.getDescripcion());
    }
}
