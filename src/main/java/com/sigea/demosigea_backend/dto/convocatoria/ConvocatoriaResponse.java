package com.sigea.demosigea_backend.dto.convocatoria;

import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa los datos de una convocatoria.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Respuesta detallada con los datos de una convocatoria pública")
public record ConvocatoriaResponse(

        @Schema(description = "Identificador único de la convocatoria", example = "1")
        Long id,

        @Schema(description = "ID del evento asociado", example = "1")
        Long eventoId,

        @Schema(description = "Nombre del evento asociado", example = "Congreso Internacional de Ingeniería 2026")
        String eventoNombre,

        @Schema(description = "Título de la convocatoria", example = "Convocatoria Abierta de Ponencias 2026")
        String titulo,

        @Schema(description = "Descripción general", example = "Presentación de proyectos de investigación.")
        String descripcion,

        @Schema(description = "Requisitos y términos", example = "Formato IEEE, máximo 6 páginas.")
        String requisitos,

        @Schema(description = "Fecha de apertura", example = "2026-03-01T08:00:00")
        LocalDateTime fechaApertura,

        @Schema(description = "Fecha de cierre", example = "2026-05-31T23:59:59")
        LocalDateTime fechaCierre,

        @Schema(description = "Estado actual de la convocatoria", example = "borrador")
        EstadoConvocatoria estado
) {
    /**
     * Mapea una entidad JPA {@link Convocatoria} a su DTO de respuesta.
     */
    public static ConvocatoriaResponse fromEntity(Convocatoria entity) {
        return new ConvocatoriaResponse(
                entity.getId(),
                entity.getEvento() != null ? entity.getEvento().getId() : null,
                entity.getEvento() != null ? entity.getEvento().getNombre() : null,
                entity.getTitulo(),
                entity.getDescripcion(),
                entity.getRequisitos(),
                entity.getFechaApertura(),
                entity.getFechaCierre(),
                entity.getEstado()
        );
    }
}
