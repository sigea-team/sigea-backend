package com.sigea.demosigea_backend.dto.conferencista;

import com.sigea.demosigea_backend.model.NivelEstudio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Estudio de la trayectoria del conferencista (HU-12, Criterio 1).
 * El año no puede ser posterior al actual; esa regla se valida en el servicio.
 *
 * @param nivel       Nivel de formación (obligatorio).
 * @param titulo      Título obtenido (obligatorio).
 * @param institucion Institución que otorgó el título (obligatoria).
 * @param anio        Año de grado (opcional).
 */
@Schema(description = "Estudio de la trayectoria académica del conferencista")
public record EstudioRequest(
        @Schema(example = "doctorado")
        @NotNull(message = "El nivel del estudio es obligatorio")
        NivelEstudio nivel,

        @Schema(example = "Doctor en Ingeniería")
        @NotBlank(message = "El título del estudio es obligatorio")
        @Size(max = 200, message = "El título del estudio no puede exceder 200 caracteres")
        String titulo,

        @Schema(example = "Universidad Nacional de Colombia")
        @NotBlank(message = "La institución del estudio es obligatoria")
        @Size(max = 150, message = "La institución no puede exceder 150 caracteres")
        String institucion,

        @Schema(example = "2018")
        @Min(value = 1900, message = "El año del estudio no es válido")
        @Max(value = 2100, message = "El año del estudio no es válido")
        Integer anio
) {
}
