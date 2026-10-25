package com.sigea.demosigea_backend.dto.conferencista;

import com.sigea.demosigea_backend.model.PerfilConferencista;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Trayectoria académica y profesional del conferencista (HU-12).
 *
 * @param fechaActualizacion Última modificación; la vista pública la muestra (Criterio 4).
 */
@Schema(description = "Trayectoria académica y profesional del conferencista")
public record TrayectoriaResponse(
        String biografia,
        String cargoActual,
        String institucion,
        String enlacePerfil,
        List<EstudioResponse> estudios,
        List<PublicacionResponse> publicaciones,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacion
) {

    /**
     * @param perfil Perfil del conferencista; {@code null} si la persona aún no tiene trayectoria.
     * @return DTO o {@code null}.
     */
    public static TrayectoriaResponse de(PerfilConferencista perfil) {
        if (perfil == null) {
            return null;
        }
        return new TrayectoriaResponse(
                perfil.getBiografia(),
                perfil.getCargoActual(),
                perfil.getInstitucion(),
                perfil.getEnlacePerfil(),
                perfil.getEstudios().stream().map(EstudioResponse::de).toList(),
                perfil.getPublicaciones().stream().map(PublicacionResponse::de).toList(),
                perfil.getFechaRegistro(),
                perfil.getFechaActualizacion());
    }
}
