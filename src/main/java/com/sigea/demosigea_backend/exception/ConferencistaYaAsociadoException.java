package com.sigea.demosigea_backend.exception;

/**
 * Se lanza al asociar a un evento un conferencista que ya está asociado a ese evento
 * (HU-12, Criterio 3). Se responde 409 {@code CONFERENCISTA_YA_ASOCIADO}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class ConferencistaYaAsociadoException extends RecursoDuplicadoException {

    public ConferencistaYaAsociadoException(String message) {
        super(message);
    }
}
