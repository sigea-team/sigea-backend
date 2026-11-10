package com.sigea.demosigea_backend.dto.observacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Observaciones y ajustes requeridos que se registran juntos sobre la versión actual de una propuesta
 * (HU-19, Criterio 1). El autor recibe una sola notificación.
 *
 * @param observaciones una o varias observaciones.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Observaciones y ajustes requeridos a una propuesta")
public record RegistrarObservacionesRequest(
        @NotEmpty(message = "Debe registrar al menos una observación.")
        @Size(max = 30, message = "Se pueden registrar como máximo 30 observaciones a la vez.")
        List<@Valid ObservacionRequest> observaciones
) {
}
