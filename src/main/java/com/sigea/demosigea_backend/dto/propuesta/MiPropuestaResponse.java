package com.sigea.demosigea_backend.dto.propuesta;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Propuesta del usuario autenticado con el resumen de sus versiones (HU-14).
 * Le permite al frontend mostrar en qué versión va y si puede modificarla.
 *
 * @param propuesta              Datos de la propuesta con sus autores.
 * @param versionActual          Número de la última versión (0 si la propuesta no tiene versiones).
 * @param totalVersiones         Cantidad de versiones registradas.
 * @param tieneVersionDefinitiva Si ya tiene versión definitiva aprobada (Criterio 5).
 * @param edicionAutorizada      Si el administrador concedió una autorización de edición pendiente de usar.
 * @param convocatoriaEstado     Estado de la convocatoria (borrador, publicada, cerrada).
 * @param convocatoriaFechaCierre Fecha límite de la convocatoria.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Propuesta del usuario con el resumen de sus versiones")
public record MiPropuestaResponse(

        @Schema(description = "Propuesta con sus autores")
        PropuestaResponse propuesta,

        @Schema(description = "Número de la última versión", example = "2")
        Integer versionActual,

        @Schema(description = "Cantidad de versiones registradas", example = "2")
        Integer totalVersiones,

        @Schema(description = "Indica si la propuesta tiene versión definitiva aprobada (RF29)")
        Boolean tieneVersionDefinitiva,

        @Schema(description = "Indica si hay una autorización de edición pendiente de usar")
        Boolean edicionAutorizada,

        @Schema(description = "Estado de la convocatoria", example = "publicada")
        String convocatoriaEstado,

        @Schema(description = "Fecha límite de la convocatoria")
        LocalDateTime convocatoriaFechaCierre
) {
}
