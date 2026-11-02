package com.sigea.demosigea_backend.dto.sala;

import com.sigea.demosigea_backend.model.TipoSala;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de petición para crear o actualizar una sala — HU-Salas, Criterios 1 y 3.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos para crear o actualizar una sala del evento")
public record SalaRequest(
        @NotBlank(message = "El nombre de la sala es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        @Schema(description = "Nombre de la sala (único por evento)", example = "Aula Magna")
        String nombre,

        @Size(max = 150, message = "La ubicación no puede superar los 150 caracteres")
        @Schema(description = "Ubicación física o URL de la sala virtual", example = "Edificio A, piso 2")
        String ubicacion,

        @NotNull(message = "El tipo de sala es obligatorio")
        @Schema(description = "Tipo de sala: fisica o virtual (como en el CHECK de la tabla salas)", example = "fisica")
        TipoSala tipo,

        @NotNull(message = "El aforo máximo es obligatorio")
        @Min(value = 1, message = "El aforo máximo debe ser mayor que 0")
        @Schema(description = "Capacidad máxima de la sala", example = "200")
        Integer aforoMaximo,

        @Valid
        @Schema(description = "Franjas de disponibilidad horaria (opcional al crear/actualizar)")
        List<DisponibilidadHorariaRequest> disponibilidades,

        @Schema(description = "HU-21: la sala admite actividades simultáneas. Opcional: si no se envía, al crear es "
                + "false y al actualizar se conserva el valor actual.", example = "false")
        Boolean permiteSimultaneidad
) {
}
