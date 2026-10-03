package com.sigea.demosigea_backend.dto.evento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO para crear una nueva edición a partir de un evento existente (HU-04, Criterio 3).
 * <p>
 * La configuración general (objetivo, descripción, tipo y modalidad) se hereda del evento origen;
 * solo se piden los datos propios del nuevo periodo. La edición resultante es un registro
 * independiente vinculado al evento base, y puede ajustarse después con la actualización del evento.
 * </p>
 *
 * @param nombre      Nombre de la edición (opcional; por defecto el del evento origen).
 * @param fechaInicio Fecha de inicio de la edición.
 * @param fechaFin    Fecha de fin de la edición.
 * @param semestre    Semestre (opcional; se deriva de la fecha de inicio).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos para crear una nueva edición de un evento")
public record NuevaEdicionRequest(
        @Schema(description = "Nombre de la edición (por defecto se usa el del evento origen)",
                example = "Congreso de Ingeniería de Sistemas 2027")
        @Size(max = 200, message = "El nombre no puede superar los 200 caracteres.")
        String nombre,

        @Schema(description = "Fecha de inicio de la edición", example = "2027-10-19")
        @NotNull(message = "La fecha de inicio es obligatoria.")
        LocalDate fechaInicio,

        @Schema(description = "Fecha de fin de la edición", example = "2027-10-21")
        @NotNull(message = "La fecha de fin es obligatoria.")
        LocalDate fechaFin,

        @Schema(description = "Semestre académico", example = "2027-2")
        @Pattern(regexp = "^\\d{4}-[12]$", message = "El semestre debe tener el formato AAAA-1 o AAAA-2.")
        String semestre
) {
    /**
     * La fecha de fin no puede ser anterior a la de inicio.
     *
     * @return {@code true} si el rango es válido o si alguna fecha es nula (lo reporta @NotNull).
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "La fecha de fin no puede ser anterior a la fecha de inicio.")
    public boolean isRangoFechasValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
