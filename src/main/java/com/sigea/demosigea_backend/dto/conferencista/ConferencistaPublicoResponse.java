package com.sigea.demosigea_backend.dto.conferencista;

import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Conferencista tal como se muestra en la vista pública del evento (HU-12, Criterio 4).
 * No incluye documento, correo ni teléfono.
 */
@Schema(description = "Conferencista en la vista pública del evento (sin datos de contacto)")
public record ConferencistaPublicoResponse(
        Long personaId,
        String nombreCompleto,
        TipoParticipacionConferencista tipoParticipacion,
        String tema,
        String cargoActual,
        String institucion,
        String biografia,
        String enlacePerfil,
        List<EstudioResponse> estudios,
        List<PublicacionResponse> publicaciones,
        LocalDateTime fechaActualizacion
) {
}
