package com.sigea.demosigea_backend.exception;

/**
 * Excepción lanzada cuando un participante intenta inscribirse a una actividad que se traslapa
 * en horario con otra actividad a la que ya se encuentra inscrito (HU-23, Criterio 3).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class ConflictoHorarioInscripcionException extends OperacionNoPermitidaException {

    public ConflictoHorarioInscripcionException(String mensaje) {
        super(mensaje);
    }
}
