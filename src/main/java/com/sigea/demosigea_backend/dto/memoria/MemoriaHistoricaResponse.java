package com.sigea.demosigea_backend.dto.memoria;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Memoria histórica de un evento, solo consulta (HU-28, Criterio 2).
 *
 * @param contenido información consolidada al cierre, en JSON (se envía tal cual, como objeto).
 */
@Schema(description = "Memoria histórica de un evento cerrado formalmente (solo consulta)")
public record MemoriaHistoricaResponse(
        Long memoriaId,
        Long eventoId,
        String nombre,
        String semestre,
        LocalDateTime fechaConsolidacion,
        @Schema(description = "Usuario que hizo el cierre formal (null si ya no existe)") Long cerradaPorUsuarioId,
        String cerradaPor,
        String resumen,
        String archivoUrl,
        @Schema(description = "Siempre false: la memoria histórica no admite cambios") boolean editable,
        List<IndicadorResponse> indicadores,
        @Schema(description = "Información consolidada al cierre: evento, comiteOrganizador, lineasTematicas, "
                + "tiposActividad, presupuesto, convocatorias (con propuestas), conferencistas, agenda, participantes, "
                + "asistencia, encuestas, certificados, indicadores y pendientesAlCierre",
                type = "object")
        @JsonRawValue String contenido
) {
}
