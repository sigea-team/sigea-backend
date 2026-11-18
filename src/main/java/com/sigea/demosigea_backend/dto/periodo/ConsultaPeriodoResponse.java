package com.sigea.demosigea_backend.dto.periodo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resultado de la consulta de eventos por periodo académico (HU-27, Criterios 3 y 4).
 *
 * @param filtros        Filtros aplicados.
 * @param hayInformacion false si ningún evento cumple los filtros (Criterio 4).
 * @param mensaje        Mensaje informativo cuando no hay información (null en caso contrario).
 * @param totalEventos   Número de eventos encontrados.
 * @param eventos        Eventos, del más reciente al más antiguo.
 */
@Schema(description = "Eventos del periodo consultado")
public record ConsultaPeriodoResponse(
        FiltroPeriodo filtros,
        boolean hayInformacion,
        String mensaje,
        int totalEventos,
        List<EventoPeriodoResponse> eventos
) {
}
