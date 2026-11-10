package com.sigea.demosigea_backend.dto.observacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Propuesta con versión definitiva aprobada, disponible para programar en la agenda del evento (HU-19, Criterio 3).
 *
 * @param propuestaId          ID de la propuesta (se envía como {@code propuestaId} al programar la actividad).
 * @param titulo               título.
 * @param convocatoriaId       convocatoria de la propuesta.
 * @param convocatoriaTitulo   título de la convocatoria.
 * @param lineaTematicaId      línea temática (para sugerirla en la actividad).
 * @param lineaTematicaNombre  nombre de la línea temática.
 * @param numeroVersion        número de la versión definitiva.
 * @param fechaDefinitiva      fecha de aprobación.
 * @param autores              autores en orden de autoría.
 * @param ponenteSugeridoId    persona con rol ponente (o el autor principal) para sugerirla como ponente.
 * @param programada           si ya hay una actividad (no cancelada) vinculada a la propuesta.
 * @param actividadIds         actividades no canceladas vinculadas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Propuesta aprobada en versión definitiva, disponible para la agenda")
public record PropuestaProgramableResponse(
        Long propuestaId,
        String titulo,
        Long convocatoriaId,
        String convocatoriaTitulo,
        Long lineaTematicaId,
        String lineaTematicaNombre,
        Integer numeroVersion,
        LocalDateTime fechaDefinitiva,
        List<String> autores,
        Long ponenteSugeridoId,
        boolean programada,
        List<Long> actividadIds
) {
}
