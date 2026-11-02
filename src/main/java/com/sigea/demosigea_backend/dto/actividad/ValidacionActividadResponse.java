package com.sigea.demosigea_backend.dto.actividad;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resultado de revisar una actividad contra la agenda sin guardarla (HU-21, Criterio 2: notificar antes de guardar).
 *
 * @param puedeGuardar          true si no hay conflictos (las advertencias se pueden confirmar).
 * @param requiereConfirmacion  true si hay advertencias que se deben confirmar al guardar.
 * @param conflictos            Cruces que impiden guardar.
 * @param advertencias          Advertencias de disponibilidad.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Conflictos y advertencias de una actividad antes de guardarla")
public record ValidacionActividadResponse(
        boolean puedeGuardar,
        boolean requiereConfirmacion,
        List<ConflictoActividadResponse> conflictos,
        List<AdvertenciaActividadResponse> advertencias
) {

    /** Construye el resultado a partir de los conflictos y advertencias encontrados. */
    public static ValidacionActividadResponse de(List<ConflictoActividadResponse> conflictos,
                                                 List<AdvertenciaActividadResponse> advertencias) {
        return new ValidacionActividadResponse(conflictos.isEmpty(), !advertencias.isEmpty(),
                List.copyOf(conflictos), List.copyOf(advertencias));
    }
}
