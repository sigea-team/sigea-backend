package com.sigea.demosigea_backend.dto.clasificacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Situación de clasificación de una propuesta (HU-18).
 *
 * @param propuestaId              ID de la propuesta.
 * @param titulo                   Título de la propuesta.
 * @param estadoPropuesta          Estado actual de la propuesta.
 * @param evaluacionCompleta       {@code true} si tiene evaluadores, todos completaron y hay puntuación calculada.
 * @param asignacionesPendientes   Evaluaciones que faltan por completar.
 * @param puntuacionPonderada      Puntuación ponderada (HU-17), {@code null} si no se ha calculado.
 * @param clasificacionPorUmbrales Clasificación que corresponde con los umbrales vigentes ({@code null} sin puntuación).
 * @param clasificacion            Clasificación vigente del resultado (automática o ajustada manualmente).
 * @param etiqueta                 Nombre legible de {@code clasificacion}: Aceptada, Aceptada con modificaciones, Rechazada.
 * @param ajusteManual             {@code true} si la clasificación la fijó el administrador con un motivo.
 * @param aplicada                 {@code true} si la clasificación vigente ya se aplicó al estado de la propuesta.
 * @param fechaClasificacion       Fecha en que se aplicó por última vez la clasificación.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Situación de clasificación de una propuesta")
public record PropuestaClasificacionResponse(
        @Schema(example = "40") Long propuestaId,
        @Schema(example = "Analítica educativa") String titulo,
        @Schema(example = "aprobada") String estadoPropuesta,
        @Schema(example = "true") boolean evaluacionCompleta,
        @Schema(example = "0") long asignacionesPendientes,
        @Schema(example = "7.60") BigDecimal puntuacionPonderada,
        @Schema(example = "aprobada") String clasificacionPorUmbrales,
        @Schema(example = "aprobada") String clasificacion,
        @Schema(example = "Aceptada") String etiqueta,
        @Schema(example = "false") boolean ajusteManual,
        @Schema(example = "true") boolean aplicada,
        LocalDateTime fechaClasificacion
) {
}
