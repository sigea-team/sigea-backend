package com.sigea.demosigea_backend.exception;

import lombok.Getter;

/**
 * HU-28, Criterios 2 y 3: se intentó modificar o eliminar información de un evento cerrado formalmente, cuya
 * información está en la memoria histórica (solo consulta). Responde 409 {@value #CODIGO} y el intento queda en la
 * auditoría.
 * <p>
 * La lanza el backend al revisar el evento ({@code MemoriaHistoricaReglas}) o se construye a partir del error SQLSTATE
 * {@value #SQLSTATE} de los triggers de la base de datos (changeset hu28-004). Extiende
 * {@link OperacionNoPermitidaException} para que el código que ya esperaba un 409 de operación no permitida siga
 * funcionando.
 * </p>
 */
@Getter
public class EventoArchivadoException extends OperacionNoPermitidaException {

    /** Código de negocio de la respuesta. */
    public static final String CODIGO = "EVENTO_ARCHIVADO";

    /** SQLSTATE que lanzan los triggers de inmutabilidad de la memoria histórica. */
    public static final String SQLSTATE = "SG028";

    /** ID del evento, si se conoce. */
    private final Long eventoId;

    /** {@code true} si el intento era una eliminación (Criterio 3). */
    private final boolean eliminacion;

    public EventoArchivadoException(String mensaje, Long eventoId, boolean eliminacion) {
        super(mensaje);
        this.eventoId = eventoId;
        this.eliminacion = eliminacion;
    }
}
