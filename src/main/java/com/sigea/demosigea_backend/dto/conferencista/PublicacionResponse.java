package com.sigea.demosigea_backend.dto.conferencista;

import com.sigea.demosigea_backend.model.PublicacionConferencista;
import com.sigea.demosigea_backend.model.TipoPublicacion;

/**
 * Publicación de la trayectoria del conferencista (HU-12).
 */
public record PublicacionResponse(Long id, TipoPublicacion tipo, String titulo, String medio, Integer anio,
                                  String enlace) {

    public static PublicacionResponse de(PublicacionConferencista publicacion) {
        return new PublicacionResponse(publicacion.getId(), publicacion.getTipo(), publicacion.getTitulo(),
                publicacion.getMedio(), publicacion.getAnio(), publicacion.getEnlace());
    }
}
