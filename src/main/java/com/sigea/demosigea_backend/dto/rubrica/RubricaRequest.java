package com.sigea.demosigea_backend.dto.rubrica;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos para crear o reemplazar la rúbrica activa de una convocatoria (HU-15, Criterios 1 y 2).
 *
 * @param nombre    nombre de la rúbrica.
 * @param criterios criterios con sus pesos y escalas; los pesos deben sumar 100.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record RubricaRequest(

        @Schema(description = "Nombre de la rúbrica", example = "Rúbrica de ponencias 2026")
        @NotBlank(message = "El nombre de la rúbrica es obligatorio.")
        @Size(max = 100, message = "El nombre de la rúbrica no puede superar los 100 caracteres.")
        String nombre,

        @Schema(description = "Criterios de evaluación; la suma de los pesos debe ser 100")
        @NotEmpty(message = "La rúbrica debe tener al menos un criterio.")
        @Size(max = 30, message = "La rúbrica no puede tener más de 30 criterios.")
        List<@Valid CriterioRubricaRequest> criterios
) {
}
