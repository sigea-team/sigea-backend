package com.sigea.demosigea_backend.dto.actividad;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo de la respuesta 409 cuando no se guarda una actividad (HU-21, Criterios 2 y 5).
 *
 * @param timestamp    Fecha y hora de la respuesta.
 * @param status       Código HTTP (409).
 * @param error        Nombre del error HTTP.
 * @param message      Explicación.
 * @param codigo       CONFLICTO_PROGRAMACION (hay cruces: corrige los datos o marca como simultáneas) o
 *                     CONFIRMACION_REQUERIDA (solo advertencias: reenvía con {@code confirmarAdvertencias = true}).
 * @param path         URI de la petición.
 * @param conflictos   Cruces con otras actividades.
 * @param advertencias Advertencias de disponibilidad.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Actividad no guardada por conflictos o advertencias sin confirmar")
public record ProgramacionRechazadaResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        @Schema(example = "CONFLICTO_PROGRAMACION") String codigo,
        String path,
        List<ConflictoActividadResponse> conflictos,
        List<AdvertenciaActividadResponse> advertencias
) {
}
