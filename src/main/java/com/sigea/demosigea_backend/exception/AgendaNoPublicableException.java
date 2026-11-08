package com.sigea.demosigea_backend.exception;

import com.sigea.demosigea_backend.dto.agenda.ActividadIncompletaResponse;
import com.sigea.demosigea_backend.dto.agenda.ConflictoAgendaResponse;
import lombok.Getter;

import java.util.List;

/**
 * La agenda no se publica (HU-22, Criterios 3 y 4): hay actividades sin sala u horario, cruces entre actividades o
 * no hay actividades. Se responde 409 con las listas para que el organizador las corrija.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class AgendaNoPublicableException extends RuntimeException {

    /** Criterio 4: hay actividades sin sala u horario asignado. */
    public static final String AGENDA_INCOMPLETA = "AGENDA_INCOMPLETA";

    /** Criterio 3: hay actividades que se cruzan. */
    public static final String AGENDA_CON_CONFLICTOS = "AGENDA_CON_CONFLICTOS";

    /** No hay actividades programadas que publicar. */
    public static final String AGENDA_VACIA = "AGENDA_VACIA";

    private final String codigo;
    private final transient List<ActividadIncompletaResponse> incompletas;
    private final transient List<ConflictoAgendaResponse> conflictos;

    public AgendaNoPublicableException(String codigo, String mensaje, List<ActividadIncompletaResponse> incompletas,
                                       List<ConflictoAgendaResponse> conflictos) {
        super(mensaje);
        this.codigo = codigo;
        this.incompletas = List.copyOf(incompletas);
        this.conflictos = List.copyOf(conflictos);
    }
}
