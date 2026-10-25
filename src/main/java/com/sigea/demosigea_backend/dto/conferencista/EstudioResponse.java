package com.sigea.demosigea_backend.dto.conferencista;

import com.sigea.demosigea_backend.model.EstudioConferencista;
import com.sigea.demosigea_backend.model.NivelEstudio;

/**
 * Estudio de la trayectoria del conferencista (HU-12).
 */
public record EstudioResponse(Long id, NivelEstudio nivel, String titulo, String institucion, Integer anio) {

    public static EstudioResponse de(EstudioConferencista estudio) {
        return new EstudioResponse(estudio.getId(), estudio.getNivel(), estudio.getTitulo(),
                estudio.getInstitucion(), estudio.getAnio());
    }
}
