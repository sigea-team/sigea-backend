package com.sigea.demosigea_backend.dto.memoria;

import com.sigea.demosigea_backend.model.ModalidadEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Evento de la memoria histórica en el listado (HU-28, Criterio 2). */
@Schema(description = "Evento cerrado formalmente, en el listado de la memoria histórica")
public record MemoriaHistoricaResumenResponse(
        Long memoriaId,
        Long eventoId,
        String nombre,
        String tipoEvento,
        ModalidadEvento modalidad,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String semestre,
        @Schema(description = "Evento base de la familia de ediciones (null si es el evento base)") Long eventoBaseId,
        LocalDateTime fechaConsolidacion,
        String cerradaPor,
        @Schema(description = "Indicadores principales (inscritos, asistentes, actividades, propuestas)")
        List<IndicadorResponse> indicadores
) {
}
