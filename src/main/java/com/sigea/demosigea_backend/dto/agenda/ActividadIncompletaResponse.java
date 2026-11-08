package com.sigea.demosigea_backend.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Actividad sin sala u horario asignado que impide publicar la agenda (HU-22, Criterio 4).
 *
 * @param actividadId ID de la actividad.
 * @param nombre      Nombre de la actividad.
 * @param faltantes   Datos que faltan: fecha, horario y/o sala.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Actividad a la que le falta sala u horario")
public record ActividadIncompletaResponse(
        Long actividadId,
        String nombre,
        @Schema(example = "[\"fecha\", \"horario\", \"sala\"]") List<String> faltantes
) {
}
