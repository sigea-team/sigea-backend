package com.sigea.demosigea_backend.dto.sala;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de petición para registrar una franja horaria de disponibilidad — HU-Salas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Franja horaria de disponibilidad (fecha, hora de inicio y hora de fin)")
public record DisponibilidadHorariaRequest(
        @NotNull(message = "La fecha es obligatoria")
        @Schema(description = "Fecha de disponibilidad", example = "2026-11-15")
        LocalDate fecha,

        @NotNull(message = "La hora de inicio es obligatoria")
        @Schema(description = "Hora de inicio (HH:mm)", example = "08:00")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        @Schema(description = "Hora de fin (HH:mm)", example = "12:00")
        LocalTime horaFin
) {
}
