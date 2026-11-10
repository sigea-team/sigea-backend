package com.sigea.demosigea_backend.dto.observacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Situación de las observaciones de una propuesta y de su versión definitiva (HU-19).
 *
 * @param propuestaId              ID de la propuesta.
 * @param titulo                   título.
 * @param estadoPropuesta          estado de la propuesta.
 * @param versionActual            número de la versión más reciente.
 * @param versionDefinitiva        número de la versión definitiva; {@code null} si aún no se aprueba.
 * @param fechaDefinitiva          fecha de aprobación de la versión definitiva.
 * @param ajustesPendientes        ajustes requeridos sin resolver (bloquean la aprobación, Criterio 4).
 * @param ajustesPorAtender        de los pendientes, cuántos aún no tienen una versión del autor que los atienda.
 * @param puedeAprobarse           si hoy se puede aprobar la versión actual como definitiva.
 * @param motivoNoAprobable        por qué no se puede aprobar (si {@code puedeAprobarse} es {@code false}).
 * @param observaciones            observaciones de todas las versiones, en orden de registro.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Observaciones de una propuesta y situación de su versión definitiva")
public record ObservacionesPropuestaResponse(
        Long propuestaId,
        String titulo,
        String estadoPropuesta,
        Integer versionActual,
        Integer versionDefinitiva,
        LocalDateTime fechaDefinitiva,
        long ajustesPendientes,
        long ajustesPorAtender,
        boolean puedeAprobarse,
        String motivoNoAprobable,
        List<ObservacionResponse> observaciones
) {
}
