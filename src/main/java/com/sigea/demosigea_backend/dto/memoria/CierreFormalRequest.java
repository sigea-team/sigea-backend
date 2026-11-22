package com.sigea.demosigea_backend.dto.memoria;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Cierre formal de un evento y consolidación de su memoria histórica (HU-28, CU-32).
 *
 * @param confirmarPendientes confirmación explícita para cerrar aunque haya procesos pendientes (CU-32, flujo alterno).
 * @param resumen             resumen o acta de cierre (opcional).
 */
@Schema(description = "Cierre formal del evento (HU-28)")
public record CierreFormalRequest(
        @Schema(description = "Confirma el cierre aunque el evento tenga procesos pendientes", example = "false")
        Boolean confirmarPendientes,

        @Schema(description = "Resumen o acta de cierre del evento (opcional)", example = "Se realizaron 24 actividades...")
        @Size(max = 4000, message = "El resumen no puede superar 4000 caracteres")
        String resumen
) {

    /** @return {@code true} solo si se confirmó explícitamente. */
    public boolean pendientesConfirmados() {
        return Boolean.TRUE.equals(confirmarPendientes);
    }
}
