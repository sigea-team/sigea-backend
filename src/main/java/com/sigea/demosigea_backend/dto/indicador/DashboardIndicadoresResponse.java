package com.sigea.demosigea_backend.dto.indicador;

import com.sigea.demosigea_backend.model.EstadoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Indicadores y dashboard de un evento (HU-29).
 * <p>
 * Se calculan en el momento de la solicitud con la información registrada (Criterio 3); no se guardan. Los
 * indicadores sirven para la vista en tabla y las series para las gráficas (Criterio 1). Lo que no tiene datos
 * suficientes llega marcado, con su mensaje, sin error (Criterio 2).
 * </p>
 *
 * @param eventoId     evento.
 * @param nombreEvento nombre del evento.
 * @param estadoEvento estado actual del evento.
 * @param archivado    {@code true} si el evento está en la memoria histórica (HU-28): su información ya no cambia
 *                     y los indicadores son definitivos.
 * @param fechaCalculo momento del cálculo.
 * @param hayDatos     {@code false} si el evento no tiene ninguna información para los indicadores.
 * @param mensaje      aviso general (sin datos, o datos definitivos del evento archivado); puede ser {@code null}.
 * @param grupos       grupos de indicadores con su disponibilidad.
 * @param indicadores  indicadores para la tabla, en orden.
 * @param series       series para las gráficas, en orden.
 */
@Schema(description = "Indicadores y dashboard de un evento")
public record DashboardIndicadoresResponse(
        Long eventoId,
        String nombreEvento,
        EstadoEvento estadoEvento,
        boolean archivado,
        LocalDateTime fechaCalculo,
        boolean hayDatos,
        @Schema(nullable = true) String mensaje,
        List<GrupoIndicadoresResponse> grupos,
        List<IndicadorEventoResponse> indicadores,
        List<SerieIndicadorResponse> series
) {
}
