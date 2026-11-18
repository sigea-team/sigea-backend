package com.sigea.demosigea_backend.dto.historial;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Historial de participación de una persona en los eventos del programa (HU-27, Criterios 1 a 3).
 *
 * @param personaId            Identificador de la persona.
 * @param nombreCompleto       Nombres y apellidos.
 * @param numeroDocumento      Número de documento.
 * @param correo               Correo de contacto.
 * @param tieneHistorial       true si hay al menos un evento que cumple los filtros.
 * @param primeraParticipacion true si la persona no tiene ninguna participación registrada (Criterio 2).
 * @param mensaje              Mensaje informativo cuando no hay historial (null en caso contrario).
 * @param totalEventos         Número de eventos del historial.
 * @param eventosPorRol        Número de eventos en que la persona tuvo cada rol.
 * @param eventos              Eventos, del más reciente al más antiguo.
 */
@Schema(description = "Historial de participación de una persona")
public record HistorialParticipacionResponse(
        Long personaId,
        String nombreCompleto,
        String numeroDocumento,
        String correo,
        boolean tieneHistorial,
        boolean primeraParticipacion,
        String mensaje,
        int totalEventos,
        Map<RolParticipacion, Long> eventosPorRol,
        List<EventoHistorialResponse> eventos
) {
}
