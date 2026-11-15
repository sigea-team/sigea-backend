package com.sigea.demosigea_backend.dto.evento;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO para modificar las fechas de un evento ya publicado (habilitado) o en ejecución.
 * <p>
 * En un evento en ejecución solo puede cambiar la fecha de fin; la de inicio debe enviarse igual a la actual.
 * </p>
 *
 * @param fechaInicio Nueva fecha de inicio.
 * @param fechaFin    Nueva fecha de fin.
 */
@Schema(description = "Nuevas fechas de un evento publicado o en ejecución")
public record AmpliacionFechasRequest(
        @Schema(description = "Fecha de inicio", example = "2026-10-20")
        @NotNull(message = "La fecha de inicio es obligatoria.")
        LocalDate fechaInicio,

        @Schema(description = "Fecha de fin", example = "2026-10-24")
        @NotNull(message = "La fecha de fin es obligatoria.")
        LocalDate fechaFin
) {
}
