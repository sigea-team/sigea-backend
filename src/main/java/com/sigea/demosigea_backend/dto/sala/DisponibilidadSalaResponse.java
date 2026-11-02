package com.sigea.demosigea_backend.dto.sala;

import com.sigea.demosigea_backend.model.DisponibilidadSala;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de respuesta con una franja de disponibilidad de sala — HU-Salas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Franja de disponibilidad horaria de una sala")
public record DisponibilidadSalaResponse(
        @Schema(description = "ID de la franja", example = "10") Long id,
        @Schema(description = "ID de la sala", example = "3") Long salaId,
        @Schema(description = "Fecha de disponibilidad", example = "2026-11-15") LocalDate fecha,
        @Schema(description = "Hora de inicio", example = "08:00") LocalTime horaInicio,
        @Schema(description = "Hora de fin", example = "12:00") LocalTime horaFin
) {
    /**
     * Construye el DTO desde la entidad JPA.
     *
     * @param ds Entidad {@link DisponibilidadSala}.
     * @return DTO inmutable.
     */
    public static DisponibilidadSalaResponse fromEntity(DisponibilidadSala ds) {
        return new DisponibilidadSalaResponse(
                ds.getId(),
                ds.getSala().getId(),
                ds.getFecha(),
                ds.getHoraInicio(),
                ds.getHoraFin()
        );
    }
}
