package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.exception.EventoArchivadoException;
import com.sigea.demosigea_backend.model.Evento;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reglas de la memoria histórica que usan los demás servicios (HU-28, Criterios 2 y 3).
 * <p>
 * Un evento cerrado formalmente ({@link Evento#isArchivado()}) es solo consulta. Los servicios que modifican la
 * información de un evento llaman a {@link #verificarEditable(Evento)} para responder con un mensaje claro; la garantía
 * final está en la base de datos (triggers del changeset hu28-004), que rechaza con SQLSTATE
 * {@value EventoArchivadoException#SQLSTATE} cualquier cambio que se escape, también en tablas sin revisión en Java.
 * {@link #desdeErrorBaseDatos(Throwable)} traduce ese error a {@link EventoArchivadoException}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public final class MemoriaHistoricaReglas {

    private static final Pattern EVENTO = Pattern.compile("evento (\\d+)");
    private static final Pattern OPERACION = Pattern.compile("\\((INSERT|UPDATE|DELETE) sobre ([a-z_]+)\\)");

    private MemoriaHistoricaReglas() {
    }

    /**
     * Rechaza la modificación si el evento fue cerrado formalmente.
     *
     * @param evento evento a revisar (puede ser {@code null}: no se revisa).
     * @throws EventoArchivadoException si el evento está en la memoria histórica.
     */
    public static void verificarEditable(Evento evento) {
        verificarEditable(evento, false);
    }

    /**
     * Rechaza la modificación o eliminación si el evento fue cerrado formalmente.
     *
     * @param evento      evento a revisar (puede ser {@code null}: no se revisa).
     * @param eliminacion {@code true} si la operación es una eliminación (Criterio 3, queda así en la auditoría).
     * @throws EventoArchivadoException si el evento está en la memoria histórica.
     */
    public static void verificarEditable(Evento evento, boolean eliminacion) {
        if (evento != null && evento.isArchivado()) {
            throw new EventoArchivadoException(mensaje(evento.getNombre(), eliminacion), evento.getId(), eliminacion);
        }
    }

    /**
     * Mensaje para el usuario.
     *
     * @param nombreEvento nombre del evento, o {@code null} si no se conoce.
     * @param eliminacion  si se intentó eliminar.
     */
    static String mensaje(String nombreEvento, boolean eliminacion) {
        String evento = nombreEvento == null ? "Este evento" : "El evento «" + nombreEvento + "»";
        return evento + " fue cerrado formalmente y su información está en la memoria histórica: solo se puede "
                + "consultar, no " + (eliminacion ? "eliminar" : "modificar") + ".";
    }

    /**
     * Busca en la cadena de causas el error de los triggers de la memoria histórica (SQLSTATE
     * {@value EventoArchivadoException#SQLSTATE}) y lo convierte en {@link EventoArchivadoException}. Se basa en el
     * SQLSTATE, no en el texto; del texto solo toma el ID del evento y si era un DELETE.
     *
     * @param ex excepción recibida (de Hibernate, Spring o el driver).
     * @return la excepción de negocio, o vacío si el error es otro.
     */
    public static Optional<EventoArchivadoException> desdeErrorBaseDatos(Throwable ex) {
        Throwable causa = ex;
        for (int i = 0; causa != null && i < 20; i++) {
            if (causa instanceof EventoArchivadoException archivado) {
                return Optional.of(archivado);
            }
            if (causa instanceof SQLException sql && EventoArchivadoException.SQLSTATE.equals(sql.getSQLState())) {
                String texto = sql.getMessage() == null ? "" : sql.getMessage();
                Long eventoId = null;
                Matcher evento = EVENTO.matcher(texto);
                if (evento.find()) {
                    eventoId = Long.valueOf(evento.group(1));
                }
                Matcher operacion = OPERACION.matcher(texto);
                boolean eliminacion = operacion.find() && "DELETE".equals(operacion.group(1));
                return Optional.of(new EventoArchivadoException(mensaje(null, eliminacion), eventoId, eliminacion));
            }
            causa = causa.getCause() == causa ? null : causa.getCause();
        }
        return Optional.empty();
    }
}
