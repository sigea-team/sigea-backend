package com.sigea.demosigea_backend.dto.propuesta;

import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.Propuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * DTO de respuesta con la información completa de una propuesta académica postulada junto con sus autores.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Detalle de una propuesta académica postulada junto con sus autores")
public record PropuestaResponse(

        @Schema(description = "ID único de la propuesta", example = "1")
        Long id,

        @Schema(description = "ID de la convocatoria", example = "1")
        Long convocatoriaId,

        @Schema(description = "Título de la convocatoria", example = "Convocatoria Anual de Ponencias 2026")
        String convocatoriaTitulo,

        @Schema(description = "Título de la propuesta", example = "Modelos de Deep Learning para Diagnóstico Temprano")
        String titulo,

        @Schema(description = "Resumen de la propuesta")
        String resumen,

        @Schema(description = "Palabras clave")
        String palabrasClave,

        @Schema(description = "ID de la línea temática")
        Long lineaTematicaId,

        @Schema(description = "Nombre de la línea temática")
        String lineaTematicaNombre,

        @Schema(description = "Estado actual de la propuesta", example = "recibida")
        EstadoPropuesta estado,

        @Schema(description = "Fecha y hora de envío de la propuesta")
        LocalDateTime fechaEnvio,

        @Schema(description = "Lista ordenada de autores y ponentes asociados a la propuesta")
        List<AutorResponse> autores
) {
    /**
     * Mapea una entidad {@link Propuesta} y su lista de {@link AutorResponse} al DTO {@link PropuestaResponse}.
     *
     * @param p       Entidad de la propuesta.
     * @param autores Lista de autores y ponentes vinculados.
     * @return DTO de respuesta.
     */
    public static PropuestaResponse fromEntity(Propuesta p, List<AutorResponse> autores) {
        return new PropuestaResponse(
                p.getId(),
                p.getConvocatoria() != null ? p.getConvocatoria().getId() : p.getConvocatoriaId(),
                p.getConvocatoria() != null ? p.getConvocatoria().getTitulo() : null,
                p.getTitulo(),
                p.getResumen(),
                p.getPalabrasClave(),
                p.getLineaTematica() != null ? p.getLineaTematica().getId() : null,
                p.getLineaTematica() != null ? p.getLineaTematica().getNombre() : null,
                p.getEstado(),
                p.getFechaEnvio(),
                autores != null ? autores : Collections.emptyList()
        );
    }
}
