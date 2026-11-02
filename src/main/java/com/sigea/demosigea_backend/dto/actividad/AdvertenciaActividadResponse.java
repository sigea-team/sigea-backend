package com.sigea.demosigea_backend.dto.actividad;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Advertencia al programar una actividad (HU-21 con la disponibilidad de HU-20). No impide guardar, pero exige
 * confirmación explícita ({@code confirmarAdvertencias = true}).
 *
 * @param tipo    FUERA_DISPONIBILIDAD_SALA o FUERA_DISPONIBILIDAD_PONENTE.
 * @param mensaje Explicación de la advertencia.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Advertencia que requiere confirmación")
public record AdvertenciaActividadResponse(
        @Schema(example = "FUERA_DISPONIBILIDAD_SALA") TipoAdvertencia tipo,
        String mensaje
) {

    /** Tipos de advertencia. */
    public enum TipoAdvertencia {
        /** El horario no está dentro de la disponibilidad registrada de la sala (o la sala no tiene). */
        FUERA_DISPONIBILIDAD_SALA,
        /** El horario no está dentro de la disponibilidad registrada del ponente (o el ponente no tiene). */
        FUERA_DISPONIBILIDAD_PONENTE
    }
}
