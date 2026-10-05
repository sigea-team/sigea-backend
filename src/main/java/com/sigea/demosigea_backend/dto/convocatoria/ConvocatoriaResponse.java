package com.sigea.demosigea_backend.dto.convocatoria;

import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO inmutable de respuesta con la información detallada de una convocatoria.
 *
 * @param id            ID de la convocatoria.
 * @param eventoId      ID del evento asociado.
 * @param eventoNombre  Nombre del evento asociado.
 * @param titulo        Título de la convocatoria.
 * @param descripcion   Descripción general.
 * @param requisitos    Requisitos exigidos.
 * @param fechaApertura Fecha/hora de apertura.
 * @param fechaCierre   Fecha/hora de cierre.
 * @param estado        Estado actual ('borrador', 'publicada', 'cerrada').
 * @param estaAbierta   Indica si actualmente se aceptan envíos de propuestas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Respuesta detallada de una convocatoria de evento")
public record ConvocatoriaResponse(

        @Schema(description = "Identificador único de la convocatoria", example = "1")
        Long id,

        @Schema(description = "ID del evento asociado", example = "1")
        Long eventoId,

        @Schema(description = "Nombre del evento asociado", example = "Congreso de Ingeniería de Sistemas")
        String eventoNombre,

        @Schema(description = "Título de la convocatoria", example = "Convocatoria de Ponencias Congreso 2026")
        String titulo,

        @Schema(description = "Descripción de la convocatoria")
        String descripcion,

        @Schema(description = "Requisitos de participación")
        String requisitos,

        @Schema(description = "Fecha y hora de apertura", example = "2026-10-10T08:00:00")
        LocalDateTime fechaApertura,

        @Schema(description = "Fecha y hora de cierre", example = "2026-11-15T23:59:59")
        LocalDateTime fechaCierre,

        @Schema(description = "Estado de la convocatoria", example = "borrador")
        EstadoConvocatoria estado,

        @Schema(description = "Indica si la convocatoria admite el envío de propuestas en este momento", example = "false")
        boolean estaAbierta
) {
    public static ConvocatoriaResponse fromEntity(Convocatoria c) {
        return new ConvocatoriaResponse(
                c.getId(),
                c.getEvento() != null ? c.getEvento().getId() : null,
                c.getEvento() != null ? c.getEvento().getNombre() : null,
                c.getTitulo(),
                c.getDescripcion(),
                c.getRequisitos(),
                c.getFechaApertura(),
                c.getFechaCierre(),
                c.getEstado(),
                c.estaAbiertaParaPropuestas(LocalDateTime.now())
        );
    }
}
