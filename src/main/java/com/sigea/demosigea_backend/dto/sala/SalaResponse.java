package com.sigea.demosigea_backend.dto.sala;

import com.sigea.demosigea_backend.model.Sala;
import com.sigea.demosigea_backend.model.TipoSala;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO de respuesta con los datos de una sala y sus franjas de disponibilidad — HU-Salas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Sala del evento con su disponibilidad horaria")
public record SalaResponse(
        @Schema(description = "ID de la sala", example = "3") Long id,
        @Schema(description = "ID del evento", example = "1") Long eventoId,
        @Schema(description = "Nombre de la sala", example = "Aula Magna") String nombre,
        @Schema(description = "Ubicación física o URL", example = "Edificio A, piso 2") String ubicacion,
        @Schema(description = "Tipo de sala: fisica o virtual") TipoSala tipo,
        @Schema(description = "Aforo máximo", example = "200") Integer aforoMaximo,
        @Schema(description = "Franjas de disponibilidad horaria registradas") List<DisponibilidadSalaResponse> disponibilidades
) {
    /**
     * Construye el DTO desde la entidad JPA con sus disponibilidades ya cargadas.
     *
     * @param sala           Entidad {@link Sala}.
     * @param disponibilidades Lista de franjas de disponibilidad.
     * @return DTO inmutable.
     */
    public static SalaResponse fromEntity(Sala sala, List<DisponibilidadSalaResponse> disponibilidades) {
        return new SalaResponse(
                sala.getId(),
                sala.getEvento().getId(),
                sala.getNombre(),
                sala.getUbicacion(),
                sala.getTipo(),
                sala.getAforoMaximo(),
                disponibilidades
        );
    }
}
