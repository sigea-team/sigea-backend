package com.sigea.demosigea_backend.dto.clasificacion;

import com.sigea.demosigea_backend.model.ClasificacionResultado;
import com.sigea.demosigea_backend.model.HistorialClasificacion;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.ReglasClasificacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento del historial de clasificaciones de una propuesta (HU-18, Criterio 2).
 *
 * @param historialId           ID del registro.
 * @param propuestaId           ID de la propuesta.
 * @param tipo                  {@code automatica} o {@code manual}.
 * @param clasificacionAnterior Clasificación previa ({@code null} si es la primera).
 * @param clasificacionNueva    Clasificación aplicada.
 * @param etiquetaNueva         Nombre legible de la clasificación aplicada.
 * @param puntuacionPonderada   Puntuación en el momento del movimiento.
 * @param umbralAceptacion      Umbral de aprobación vigente.
 * @param umbralAjustes         Umbral de aprobación con ajustes vigente.
 * @param motivo                Motivo (obligatorio en ajustes manuales).
 * @param realizadoPor          Nombre del usuario que hizo el movimiento.
 * @param fechaHora             Fecha y hora del movimiento.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Movimiento del historial de clasificaciones de una propuesta")
public record HistorialClasificacionResponse(
        @Schema(example = "5") Long historialId,
        @Schema(example = "40") Long propuestaId,
        @Schema(example = "manual") String tipo,
        @Schema(example = "aprobada") String clasificacionAnterior,
        @Schema(example = "aprobada_con_ajustes") String clasificacionNueva,
        @Schema(example = "Aceptada con modificaciones") String etiquetaNueva,
        @Schema(example = "7.10") BigDecimal puntuacionPonderada,
        @Schema(example = "7.00") BigDecimal umbralAceptacion,
        @Schema(example = "5.00") BigDecimal umbralAjustes,
        @Schema(example = "El comité acordó solicitar ajustes de forma.") String motivo,
        @Schema(example = "María Rodríguez") String realizadoPor,
        LocalDateTime fechaHora
) {

    public static HistorialClasificacionResponse fromEntity(HistorialClasificacion h) {
        Persona persona = h.getUsuario() != null ? h.getUsuario().getPersona() : null;
        return new HistorialClasificacionResponse(
                h.getId(),
                h.getPropuesta() != null ? h.getPropuesta().getId() : null,
                h.getTipo() != null ? h.getTipo().name() : null,
                nombre(h.getClasificacionAnterior()),
                nombre(h.getClasificacionNueva()),
                ReglasClasificacion.etiqueta(h.getClasificacionNueva()),
                h.getPuntuacionPonderada(),
                h.getUmbralAceptacion(),
                h.getUmbralAjustes(),
                h.getMotivo(),
                persona != null ? (persona.getNombres() + " " + persona.getApellidos()).trim() : null,
                h.getFechaHora()
        );
    }

    private static String nombre(ClasificacionResultado clasificacion) {
        return clasificacion != null ? clasificacion.name() : null;
    }
}
