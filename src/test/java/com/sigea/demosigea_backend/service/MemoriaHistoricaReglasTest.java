package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.exception.EventoArchivadoException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link MemoriaHistoricaReglas} (HU-28, Criterios 2 y 3).
 */
class MemoriaHistoricaReglasTest {

    @Test
    @DisplayName("Un evento cerrado formalmente no se modifica: 409 EVENTO_ARCHIVADO")
    void eventoArchivado() {
        Evento evento = Evento.builder().id(4L).nombre("Congreso").estado(EstadoEvento.cerrado)
                .memoriasHistoricas(1L).build();

        EventoArchivadoException ex = assertThrows(EventoArchivadoException.class,
                () -> MemoriaHistoricaReglas.verificarEditable(evento, true));

        assertEquals(4L, ex.getEventoId());
        assertTrue(ex.isEliminacion());
        assertTrue(ex.getMessage().contains("«Congreso»"));
        assertTrue(ex.getMessage().contains("no eliminar"));
        assertInstanceOf(OperacionNoPermitidaException.class, ex);
    }

    @Test
    @DisplayName("Un evento sin memoria histórica (o sin el dato cargado) sí se modifica")
    void eventoEditable() {
        Evento cerradoSinMemoria = Evento.builder().id(4L).estado(EstadoEvento.cerrado).memoriasHistoricas(0L).build();
        Evento sinDato = Evento.builder().id(5L).estado(EstadoEvento.cerrado).build();

        assertFalse(cerradoSinMemoria.isArchivado());
        assertFalse(sinDato.isArchivado());
        assertDoesNotThrow(() -> MemoriaHistoricaReglas.verificarEditable(cerradoSinMemoria));
        assertDoesNotThrow(() -> MemoriaHistoricaReglas.verificarEditable(sinDato));
        assertDoesNotThrow(() -> MemoriaHistoricaReglas.verificarEditable(null));
    }

    @Test
    @DisplayName("Traduce el error SG028 de los triggers, aunque llegue envuelto, a EVENTO_ARCHIVADO")
    void traduceErrorDeBaseDeDatos() {
        SQLException sql = new SQLException("ERROR: La información del evento 12 está consolidada en la memoria "
                + "histórica y no se puede eliminar (DELETE sobre propuestas).", "SG028");
        RuntimeException envuelta = new RuntimeException("could not execute statement",
                new IllegalStateException("flush", sql));

        Optional<EventoArchivadoException> ex = MemoriaHistoricaReglas.desdeErrorBaseDatos(envuelta);

        assertTrue(ex.isPresent());
        assertEquals(12L, ex.get().getEventoId());
        assertTrue(ex.get().isEliminacion());
    }

    @Test
    @DisplayName("Una modificación rechazada por los triggers no se marca como eliminación")
    void modificacionDeBaseDeDatos() {
        SQLException sql = new SQLException("La información del evento 3 está consolidada en la memoria histórica y "
                + "no se puede modificar (UPDATE sobre actividades).", "SG028");

        Optional<EventoArchivadoException> ex = MemoriaHistoricaReglas.desdeErrorBaseDatos(sql);

        assertTrue(ex.isPresent());
        assertEquals(3L, ex.get().getEventoId());
        assertFalse(ex.get().isEliminacion());
    }

    @Test
    @DisplayName("Otros errores de base de datos no se traducen")
    void otrosErrores() {
        SQLException unicidad = new SQLException("duplicate key value", "23505");

        assertTrue(MemoriaHistoricaReglas.desdeErrorBaseDatos(new RuntimeException(unicidad)).isEmpty());
        assertTrue(MemoriaHistoricaReglas.desdeErrorBaseDatos(new IllegalStateException("x")).isEmpty());
        assertTrue(MemoriaHistoricaReglas.desdeErrorBaseDatos(null).isEmpty());
    }
}
