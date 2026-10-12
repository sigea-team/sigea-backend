package com.sigea.demosigea_backend.dto.parametro;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Impacto de eliminar un tipo de actividad o una línea temática (HU-05, Criterio 2).
 * <p>
 * Permite al frontend <b>advertir del impacto</b> antes de intentar la eliminación: si
 * {@code eliminable} es {@code false}, el backend rechazará el DELETE con 409 PARAMETRO_EN_USO.
 * </p>
 *
 * @param id          ID del elemento consultado.
 * @param nombre      Nombre del elemento.
 * @param actividades Actividades de la agenda que lo usan.
 * @param propuestas  Propuestas que lo usan (siempre 0 para tipos de actividad: las propuestas no se clasifican por tipo).
 * @param eliminable  {@code true} si no está en uso y puede eliminarse.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Uso de un parámetro del evento en la agenda y en las propuestas")
public record UsoParametroResponse(
        @Schema(example = "4") Long id,
        @Schema(example = "Taller") String nombre,
        @Schema(description = "Actividades de la agenda que lo usan", example = "3") long actividades,
        @Schema(description = "Propuestas que lo usan (0 para tipos de actividad)", example = "0") long propuestas,
        @Schema(description = "true si puede eliminarse", example = "false") boolean eliminable
) {
    /**
     * @return Construye la respuesta calculando {@code eliminable}.
     */
    public static UsoParametroResponse de(Long id, String nombre, long actividades, long propuestas) {
        return new UsoParametroResponse(id, nombre, actividades, propuestas, actividades == 0 && propuestas == 0);
    }

    /**
     * @return Descripción legible del uso, ej. "3 actividad(es) de la agenda y 2 propuesta(s)".
     */
    public String describirUso() {
        StringBuilder sb = new StringBuilder();
        if (actividades > 0) {
            sb.append(actividades).append(" actividad(es) de la agenda");
        }
        if (propuestas > 0) {
            if (!sb.isEmpty()) {
                sb.append(" y ");
            }
            sb.append(propuestas).append(" propuesta(s)");
        }
        return sb.toString();
    }
}
