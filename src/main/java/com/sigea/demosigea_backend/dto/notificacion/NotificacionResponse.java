package com.sigea.demosigea_backend.dto.notificacion;

import com.sigea.demosigea_backend.model.Notificacion;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Notificación tal como la muestra la campana del frontend.
 *
 * @param id            ID de la notificación.
 * @param tipo          Tipo de notificación.
 * @param titulo        Título corto.
 * @param mensaje       Texto completo.
 * @param entidad       Recurso relacionado (ej. {@code convocatorias}), para navegar a su detalle.
 * @param entidadId     ID del recurso relacionado.
 * @param leida         Si el usuario ya la leyó.
 * @param fechaCreacion Fecha y hora en que se generó.
 * @param fechaLectura  Fecha y hora en que se marcó como leída.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Notificación del usuario autenticado")
public record NotificacionResponse(
        @Schema(example = "15") Long id,
        @Schema(example = "CONVOCATORIA_FECHA_MODIFICADA") TipoNotificacion tipo,
        @Schema(example = "Nueva fecha límite: Convocatoria de Ponencias 2026") String titulo,
        @Schema(example = "La fecha límite ... cambió del 15/11/2026 23:59 al 30/11/2026 23:59. Aún estás a tiempo de enviar tu propuesta.") String mensaje,
        @Schema(example = "convocatorias") String entidad,
        @Schema(example = "10") Long entidadId,
        @Schema(example = "false") boolean leida,
        @Schema(example = "2026-10-17T12:00:00") LocalDateTime fechaCreacion,
        @Schema(example = "2026-10-17T12:30:00") LocalDateTime fechaLectura
) {
    public static NotificacionResponse fromEntity(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getTipo(), n.getTitulo(), n.getMensaje(), n.getEntidad(),
                n.getEntidadId(), Boolean.TRUE.equals(n.getLeida()), n.getFechaCreacion(), n.getFechaLectura());
    }
}
