package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Trayectoria académica y profesional del conferencista (HU-12, Criterio 1).
 * <p>
 * La biografía es obligatoria porque es lo que se publica en la agenda y el material de difusión
 * (Criterio 2). Los estudios y publicaciones son opcionales, pero cada elemento enviado debe estar
 * completo. Al actualizar, las listas reemplazan a las anteriores en el orden recibido.
 * </p>
 *
 * @param biografia     Reseña biográfica (obligatoria).
 * @param cargoActual   Cargo actual (opcional).
 * @param institucion   Institución o empresa actual (opcional).
 * @param enlacePerfil  CvLAC, ORCID, Google Scholar o LinkedIn (opcional).
 * @param estudios      Formación académica (opcional).
 * @param publicaciones Publicaciones (opcional).
 */
@Schema(description = "Trayectoria académica y profesional del conferencista")
public record TrayectoriaRequest(
        @Schema(example = "Ingeniera de sistemas con 15 años de experiencia en arquitectura de software...")
        @NotBlank(message = "La biografía del conferencista es obligatoria")
        @Size(max = 4000, message = "La biografía no puede exceder 4000 caracteres")
        String biografia,

        @Schema(example = "Profesora titular")
        @Size(max = 150, message = "El cargo no puede exceder 150 caracteres")
        String cargoActual,

        @Schema(example = "Universidad Francisco de Paula Santander")
        @Size(max = 150, message = "La institución no puede exceder 150 caracteres")
        String institucion,

        @Schema(example = "https://orcid.org/0000-0002-1825-0097")
        @Size(max = 300, message = "El enlace no puede exceder 300 caracteres")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "El enlace del perfil debe iniciar con http:// o https://")
        String enlacePerfil,

        @Size(max = 20, message = "Se permiten máximo 20 estudios")
        List<@Valid @NotNull EstudioRequest> estudios,

        @Size(max = 50, message = "Se permiten máximo 50 publicaciones")
        List<@Valid @NotNull PublicacionRequest> publicaciones
) {
}
