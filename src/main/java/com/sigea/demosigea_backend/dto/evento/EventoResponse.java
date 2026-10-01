package com.sigea.demosigea_backend.dto.evento;

import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.ModalidadEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * DTO de respuesta con la información de un evento o edición.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Información de un evento o de una de sus ediciones")
public record EventoResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "Congreso de Ingeniería de Sistemas 2026") String nombre,
        String objetivo,
        String descripcion,
        @Schema(example = "Congreso") String tipo,
        @Schema(example = "presencial") ModalidadEvento modalidad,
        @Schema(example = "2026-10-20") LocalDate fechaInicio,
        @Schema(example = "2026-10-22") LocalDate fechaFin,
        @Schema(example = "2026-2") String semestre,
        @Schema(example = "en_configuracion") EstadoEvento estado,
        @Schema(description = "ID del evento base (null si este registro es el evento base)", example = "1") Long eventoBaseId,
        @Schema(description = "Indica si el registro es una edición derivada", example = "true") boolean esEdicion
) {
    /**
     * Construye el DTO a partir de la entidad.
     *
     * @param evento Entidad JPA.
     * @return DTO inmutable.
     */
    public static EventoResponse fromEntity(Evento evento) {
        Long baseId = evento.getEventoBase() != null ? evento.getEventoBase().getId() : null;
        return new EventoResponse(
                evento.getId(),
                evento.getNombre(),
                evento.getObjetivo(),
                evento.getDescripcion(),
                evento.getTipo(),
                evento.getModalidad(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.getSemestre(),
                evento.getEstado(),
                baseId,
                baseId != null
        );
    }
}
