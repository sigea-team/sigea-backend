package com.sigea.demosigea_backend.dto.tipoevento;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Uso de un tipo de evento: cuántos eventos lo tienen asignado. Permite advertir del impacto
 * antes de eliminarlo; un tipo en uso no puede eliminarse.
 *
 * @param id         ID del tipo de evento.
 * @param nombre     Nombre del tipo.
 * @param eventos    Eventos y ediciones que lo usan.
 * @param eliminable {@code true} si ningún evento lo usa.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Uso de un tipo de evento")
public record UsoTipoEventoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Congreso") String nombre,
        @Schema(description = "Eventos y ediciones que usan el tipo", example = "3") long eventos,
        @Schema(description = "true si puede eliminarse", example = "false") boolean eliminable
) {
    /**
     * @param id      ID del tipo.
     * @param nombre  Nombre del tipo.
     * @param eventos Número de eventos que lo usan.
     * @return Uso calculado; es eliminable solo si no lo usa ningún evento.
     */
    public static UsoTipoEventoResponse de(Long id, String nombre, long eventos) {
        return new UsoTipoEventoResponse(id, nombre, eventos, eventos == 0);
    }
}
