package com.sigea.demosigea_backend.dto.sala;

import com.sigea.demosigea_backend.model.DisponibilidadPonente;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de respuesta con una franja de disponibilidad de ponente — HU-Salas, Criterio 2.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Franja de disponibilidad horaria de un ponente")
public record DisponibilidadPonenteResponse(
        @Schema(description = "ID de la franja", example = "15") Long id,
        @Schema(description = "ID del ponente (persona)", example = "7") Long personaId,
        @Schema(description = "ID del evento", example = "1") Long eventoId,
        @Schema(description = "Fecha de disponibilidad", example = "2026-11-15") LocalDate fecha,
        @Schema(description = "Hora de inicio", example = "09:00") LocalTime horaInicio,
        @Schema(description = "Hora de fin", example = "13:00") LocalTime horaFin
) {
    /**
     * Construye el DTO desde la entidad JPA.
     *
     * @param dp Entidad {@link DisponibilidadPonente}.
     * @return DTO inmutable.
     */
    public static DisponibilidadPonenteResponse fromEntity(DisponibilidadPonente dp) {
        return new DisponibilidadPonenteResponse(
                dp.getId(),
                dp.getPersona().getId(),
                dp.getEvento().getId(),
                dp.getFecha(),
                dp.getHoraInicio(),
                dp.getHoraFin()
        );
    }
}
