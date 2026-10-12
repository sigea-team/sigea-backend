package com.sigea.demosigea_backend.dto.presupuesto;

import com.sigea.demosigea_backend.model.RubroPresupuestal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO de respuesta con un rubro del presupuesto preliminar y su subtotal calculado (HU-07).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Rubro del presupuesto preliminar")
public record RubroResponse(
        @Schema(description = "ID del rubro", example = "7") Long id,
        @Schema(example = "3") Long eventoId,
        @Schema(example = "Transporte de conferencistas") String nombre,
        @Schema(example = "2.00") BigDecimal cantidad,
        @Schema(example = "350000.00") BigDecimal valorUnitarioProyectado,
        @Schema(description = "cantidad × valor unitario", example = "700000.00") BigDecimal subtotal,
        @Schema(description = "true = vigente; false = eliminado (historial)", example = "true") boolean activo
) {
    /**
     * Construye el DTO a partir de la entidad.
     *
     * @param rubro Entidad JPA.
     * @return DTO inmutable.
     */
    public static RubroResponse fromEntity(RubroPresupuestal rubro) {
        return new RubroResponse(
                rubro.getId(),
                rubro.getEvento() != null ? rubro.getEvento().getId() : null,
                rubro.getNombre(),
                rubro.getCantidad(),
                rubro.getValorUnitarioProyectado(),
                rubro.getSubtotal(),
                rubro.isActivo()
        );
    }
}
