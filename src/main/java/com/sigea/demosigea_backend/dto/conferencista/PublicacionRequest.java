package com.sigea.demosigea_backend.dto.conferencista;

import com.sigea.demosigea_backend.model.TipoPublicacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Publicación de la trayectoria del conferencista (HU-12, Criterio 1).
 * El año no puede ser posterior al actual; esa regla se valida en el servicio.
 *
 * @param tipo   Tipo de publicación (obligatorio).
 * @param titulo Título de la publicación (obligatorio).
 * @param medio  Revista, editorial o evento donde se publicó (opcional).
 * @param anio   Año de publicación (opcional).
 * @param enlace DOI o URL (opcional; debe iniciar con http:// o https://).
 */
@Schema(description = "Publicación de la trayectoria del conferencista")
public record PublicacionRequest(
        @Schema(example = "articulo")
        @NotNull(message = "El tipo de publicación es obligatorio")
        TipoPublicacion tipo,

        @Schema(example = "Arquitecturas de microservicios en sistemas académicos")
        @NotBlank(message = "El título de la publicación es obligatorio")
        @Size(max = 300, message = "El título de la publicación no puede exceder 300 caracteres")
        String titulo,

        @Schema(example = "Revista Colombiana de Computación")
        @Size(max = 200, message = "El medio de publicación no puede exceder 200 caracteres")
        String medio,

        @Schema(example = "2024")
        @Min(value = 1900, message = "El año de la publicación no es válido")
        @Max(value = 2100, message = "El año de la publicación no es válido")
        Integer anio,

        @Schema(example = "https://doi.org/10.1000/xyz123")
        @Size(max = 300, message = "El enlace no puede exceder 300 caracteres")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "El enlace debe iniciar con http:// o https://")
        String enlace
) {
}
