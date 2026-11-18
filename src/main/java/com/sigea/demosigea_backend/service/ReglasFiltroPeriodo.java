package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.periodo.FiltroPeriodo;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Evento;

import java.util.regex.Pattern;

/**
 * Reglas comunes de los filtros por periodo académico (HU-27, Criterio 3), compartidas por el historial de
 * participación ({@link HistorialParticipacionService}) y la consulta de eventos e indicadores
 * ({@link PeriodoAcademicoService}).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
final class ReglasFiltroPeriodo {

    /** Mismo formato que exige el registro de eventos (AAAA-1 o AAAA-2). */
    private static final Pattern SEMESTRE = Pattern.compile("^\\d{4}-[12]$");

    private ReglasFiltroPeriodo() {
    }

    /**
     * Normaliza y valida el filtro.
     *
     * @param filtro Filtro recibido (puede ser null).
     * @return Filtro normalizado.
     * @throws SolicitudInvalidaException si el semestre no tiene el formato esperado o el rango está invertido.
     */
    static FiltroPeriodo validar(FiltroPeriodo filtro) {
        FiltroPeriodo f = filtro == null ? FiltroPeriodo.vacio() : filtro.normalizado();
        validarSemestre(f.semestre());
        if (f.fechaDesde() != null && f.fechaHasta() != null && f.fechaDesde().isAfter(f.fechaHasta())) {
            throw new SolicitudInvalidaException("La fecha inicial del rango no puede ser posterior a la fecha final.");
        }
        return f;
    }

    /**
     * @param semestre Semestre a validar (null se acepta).
     * @throws SolicitudInvalidaException si no tiene el formato AAAA-1 o AAAA-2.
     */
    static void validarSemestre(String semestre) {
        if (semestre != null && !SEMESTRE.matcher(semestre).matches()) {
            throw new SolicitudInvalidaException("El semestre debe tener el formato AAAA-1 o AAAA-2.");
        }
    }

    /**
     * Indica si el evento cumple los filtros de semestre, tipo de evento y rango de fechas.
     * Un evento está en el rango si se cruza con él (empieza antes del fin y termina después del inicio).
     * <p>
     * Se comporta igual que {@link PeriodoAcademicoService#especificacion}: en SQL una comparación con NULL no se
     * cumple, así que un evento sin la fecha necesaria queda fuera cuando hay rango (las columnas son NOT NULL,
     * pero se mantiene la coherencia).
     * </p>
     */
    static boolean eventoCumple(Evento evento, FiltroPeriodo f) {
        if (f.semestre() != null && !f.semestre().equals(evento.getSemestre())) {
            return false;
        }
        if (f.tipoEventoId() != null
                && (evento.getTipoEvento() == null || !f.tipoEventoId().equals(evento.getTipoEvento().getId()))) {
            return false;
        }
        if (f.fechaHasta() != null
                && (evento.getFechaInicio() == null || evento.getFechaInicio().isAfter(f.fechaHasta()))) {
            return false;
        }
        return f.fechaDesde() == null
                || (evento.getFechaFin() != null && !evento.getFechaFin().isBefore(f.fechaDesde()));
    }

    /**
     * Indica si la actividad cumple el filtro de tipo de actividad (por nombre) y el rango de fechas.
     * Una actividad sin fecha (por programar) no se descarta por el rango.
     */
    static boolean actividadCumple(Actividad actividad, FiltroPeriodo f) {
        if (f.tipoActividad() != null) {
            String tipo = actividad.getTipoActividad() == null ? null : actividad.getTipoActividad().getNombre();
            if (tipo == null || !tipo.trim().equalsIgnoreCase(f.tipoActividad())) {
                return false;
            }
        }
        if (actividad.getFecha() == null) {
            return true;
        }
        if (f.fechaDesde() != null && actividad.getFecha().isBefore(f.fechaDesde())) {
            return false;
        }
        return f.fechaHasta() == null || !actividad.getFecha().isAfter(f.fechaHasta());
    }

    /** Mensaje del Criterio 4: el periodo o los filtros no tienen información. */
    static String mensajeSinInformacion(FiltroPeriodo f) {
        boolean soloSemestre = f.semestre() != null && f.fechaDesde() == null && f.fechaHasta() == null
                && f.tipoEventoId() == null && f.tipoActividad() == null;
        if (soloSemestre) {
            return "No hay información disponible para el semestre " + f.semestre() + ".";
        }
        if (f.sinFiltros()) {
            return "No hay eventos registrados.";
        }
        return "No hay información disponible para los filtros aplicados.";
    }
}
