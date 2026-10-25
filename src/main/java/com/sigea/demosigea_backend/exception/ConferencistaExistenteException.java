package com.sigea.demosigea_backend.exception;

import com.sigea.demosigea_backend.dto.conferencista.CoincidenciaConferencistaResponse;
import lombok.Getter;

/**
 * Se lanza al registrar un conferencista cuyo documento o correo ya pertenece a una persona del
 * sistema (HU-12, Criterio 3). En lugar de crear un duplicado, la respuesta 409
 * {@code CONFERENCISTA_EXISTENTE} incluye la persona encontrada para asociarla al evento.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class ConferencistaExistenteException extends RecursoDuplicadoException {

    /** Persona que ya existe; el frontend la ofrece para asociarla al evento. */
    private final transient CoincidenciaConferencistaResponse coincidencia;

    public ConferencistaExistenteException(String message, CoincidenciaConferencistaResponse coincidencia) {
        super(message);
        this.coincidencia = coincidencia;
    }
}
