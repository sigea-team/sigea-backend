package com.sigea.demosigea_backend.dto.presupuesto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Respuesta de crear, editar o eliminar un rubro: el rubro afectado y el total actualizado del
 * presupuesto preliminar (HU-07, Criterios 1 y 2). Así el frontend refresca el total sin otra petición.
 *
 * @param rubro            Rubro creado, editado o eliminado ({@code activo = false}).
 * @param totalPresupuesto Total del presupuesto preliminar después de la operación.
 * @param cantidadRubros   Número de rubros vigentes después de la operación.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Rubro afectado y total actualizado del presupuesto preliminar")
public record RubroOperacionResponse(
        RubroResponse rubro,
        @Schema(example = "5250000.00") BigDecimal totalPresupuesto,
        @Schema(example = "4") int cantidadRubros
) {
}
