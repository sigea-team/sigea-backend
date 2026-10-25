package com.sigea.demosigea_backend.dto.conferencista;

import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Participación del conferencista en un evento, como parte de su historial (HU-12, RF17).
 *
 * @param visiblePublicamente {@code true} si el evento ya está publicado: allí se ve la trayectoria (Criterio 4).
 */
@Schema(description = "Participación del conferencista en un evento del Programa")
public record ParticipacionHistorialResponse(
        Long participacionId,
        Long eventoId,
        String eventoNombre,
        String semestre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoEvento estadoEvento,
        TipoParticipacionConferencista tipoParticipacion,
        String tema,
        boolean visiblePublicamente
) {

    public static ParticipacionHistorialResponse de(ParticipacionConferencista participacion, boolean visible) {
        Evento evento = participacion.getEvento();
        return new ParticipacionHistorialResponse(
                participacion.getId(),
                evento.getId(),
                evento.getNombre(),
                evento.getSemestre(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.getEstado(),
                participacion.getTipoParticipacion(),
                participacion.getTema(),
                visible);
    }
}
