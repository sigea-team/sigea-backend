package com.sigea.demosigea_backend.dto.memoria;

import com.sigea.demosigea_backend.model.Indicador;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Indicador calculado al cierre formal del evento (HU-28). */
@Schema(description = "Indicador de gestión del evento")
public record IndicadorResponse(
        @Schema(example = "TASA_ASISTENCIA") String codigo,
        @Schema(example = "Tasa de asistencia de los inscritos") String nombre,
        @Schema(example = "82.50") BigDecimal valor,
        @Schema(example = "%") String unidad,
        LocalDateTime fechaCalculo
) {

    public static IndicadorResponse fromEntity(Indicador indicador) {
        return new IndicadorResponse(indicador.getCodigo(), indicador.getNombre(), indicador.getValor(),
                indicador.getUnidad(), indicador.getFechaCalculo());
    }
}
