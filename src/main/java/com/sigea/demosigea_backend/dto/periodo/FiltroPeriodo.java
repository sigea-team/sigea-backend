package com.sigea.demosigea_backend.dto.periodo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Filtros de consulta por periodo académico (HU-27, Criterio 3). Todos son opcionales y se combinan con AND.
 *
 * @param semestre      Semestre académico con formato AAAA-1 o AAAA-2.
 * @param fechaDesde    Inicio del rango: incluye los eventos que terminan en esta fecha o después.
 * @param fechaHasta    Fin del rango: incluye los eventos que empiezan en esta fecha o antes.
 * @param tipoEventoId  Tipo de evento del catálogo global {@code tipos_evento}.
 * @param tipoActividad Nombre del tipo de actividad (los tipos son por evento, por eso se filtra por nombre,
 *                      sin distinguir mayúsculas).
 */
@Schema(description = "Filtros aplicados a la consulta")
public record FiltroPeriodo(
        String semestre,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Long tipoEventoId,
        String tipoActividad
) {

    /** Filtro sin restricciones. */
    public static FiltroPeriodo vacio() {
        return new FiltroPeriodo(null, null, null, null, null);
    }

    /** @return copia con los textos recortados y los vacíos convertidos en null. */
    public FiltroPeriodo normalizado() {
        return new FiltroPeriodo(limpiar(semestre), fechaDesde, fechaHasta, tipoEventoId, limpiar(tipoActividad));
    }

    /** @return copia con otro semestre y los demás filtros iguales. */
    public FiltroPeriodo conSemestre(String otroSemestre) {
        return new FiltroPeriodo(otroSemestre, fechaDesde, fechaHasta, tipoEventoId, tipoActividad);
    }

    /** @return true si no se aplicó ningún filtro. */
    public boolean sinFiltros() {
        return semestre == null && fechaDesde == null && fechaHasta == null && tipoEventoId == null
                && tipoActividad == null;
    }

    private static String limpiar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
