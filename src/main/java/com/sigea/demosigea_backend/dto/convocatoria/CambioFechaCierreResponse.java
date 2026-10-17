package com.sigea.demosigea_backend.dto.convocatoria;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resultado de modificar la fecha límite de una convocatoria publicada (HU-11, Criterio 2).
 *
 * @param convocatoria          Convocatoria con la fecha límite ya actualizada.
 * @param fechaCierreAnterior   Fecha límite que tenía antes del cambio.
 * @param usuariosNotificados   Número de usuarios activos que recibieron la notificación del cambio (campana).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resultado del cambio de fecha límite de una convocatoria publicada")
public record CambioFechaCierreResponse(

        @Schema(description = "Convocatoria actualizada")
        ConvocatoriaResponse convocatoria,

        @Schema(description = "Fecha límite anterior", example = "2026-11-15T23:59:59")
        LocalDateTime fechaCierreAnterior,

        @Schema(description = "Cantidad de usuarios notificados en el sistema", example = "120")
        int usuariosNotificados
) {}
