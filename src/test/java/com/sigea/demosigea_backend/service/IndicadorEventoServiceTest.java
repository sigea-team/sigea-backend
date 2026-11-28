package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.indicador.DashboardIndicadoresResponse;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.IndicadorEventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link IndicadorEventoService} (HU-29).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IndicadorEventoServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Mock private EventoRepository eventoRepository;
    @Mock private IndicadorEventoRepository indicadorEventoRepository;

    private IndicadorEventoService servicio;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-26T06:00:00Z"), BOGOTA);
        servicio = new IndicadorEventoService(eventoRepository, indicadorEventoRepository, clock);
        Evento evento = Evento.builder().id(5L).nombre("Congreso").estado(EstadoEvento.en_ejecucion).build();
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));
        when(indicadorEventoRepository.calcular(5L)).thenReturn(filasIndicadores(4));
        when(indicadorEventoRepository.inscritosPorCategoria(5L)).thenReturn(List.<Object[]>of(new Object[]{"estudiante", 4L}));
    }

    private static List<Object[]> filasIndicadores(long inscritos) {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{"INSCRITOS", "Participantes inscritos", BigDecimal.valueOf(inscritos), "personas"});
        filas.add(new Object[]{"TASA_ASISTENCIA", "Tasa de asistencia de los inscritos", null, "%"});
        return filas;
    }

    @Test
    @DisplayName("Criterio 1: calcula indicadores y series del evento en el momento de la solicitud")
    void generar() {
        DashboardIndicadoresResponse r = servicio.generar(5L);

        assertEquals(5L, r.eventoId());
        assertEquals("Congreso", r.nombreEvento());
        assertEquals(EstadoEvento.en_ejecucion, r.estadoEvento());
        assertFalse(r.archivado());
        assertEquals(LocalDateTime.of(2026, 11, 26, 1, 0), r.fechaCalculo());
        assertTrue(r.hayDatos());
        assertEquals(2, r.indicadores().size());
        assertEquals(7, r.series().size());
        assertTrue(r.series().get(0).hayDatos());
        verify(indicadorEventoRepository).asistenciaPorActividad(5L);
        verify(indicadorEventoRepository).presupuestoPorRubro(5L);
    }

    @Test
    @DisplayName("Criterio 3: cada solicitud vuelve a consultar la base de datos")
    void recalculaEnCadaSolicitud() {
        assertEquals(0, new BigDecimal("4").compareTo(servicio.generar(5L).indicadores().get(0).valor()));

        when(indicadorEventoRepository.calcular(5L)).thenReturn(filasIndicadores(6));

        assertEquals(0, new BigDecimal("6").compareTo(servicio.generar(5L).indicadores().get(0).valor()));
        verify(indicadorEventoRepository, times(2)).calcular(5L);
    }

    @Test
    @DisplayName("Criterio 2: sin datos responde sin error")
    void sinDatos() {
        when(indicadorEventoRepository.calcular(5L)).thenReturn(filasIndicadores(0));
        when(indicadorEventoRepository.inscritosPorCategoria(5L)).thenReturn(List.of());

        DashboardIndicadoresResponse r = servicio.generar(5L);

        assertFalse(r.hayDatos());
        assertEquals(ReglasIndicadoresEvento.MENSAJE_SIN_DATOS, r.mensaje());
    }

    @Test
    @DisplayName("Evento archivado: se marca en la respuesta")
    void archivado() {
        Evento evento = Evento.builder().id(5L).nombre("Congreso").estado(EstadoEvento.cerrado)
                .memoriasHistoricas(1L).build();
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        DashboardIndicadoresResponse r = servicio.generar(5L);

        assertTrue(r.archivado());
        assertEquals(ReglasIndicadoresEvento.MENSAJE_ARCHIVADO, r.mensaje());
    }

    @Test
    @DisplayName("Criterio 3: transacción de solo lectura REPEATABLE READ (todas las consultas ven la misma foto)")
    void transaccionRepeatableRead() throws NoSuchMethodException {
        Transactional transaccion = IndicadorEventoService.class.getMethod("generar", Long.class)
                .getAnnotation(Transactional.class);

        assertTrue(transaccion.readOnly());
        assertEquals(Isolation.REPEATABLE_READ, transaccion.isolation());
    }

    @Test
    @DisplayName("Evento inexistente: 404 sin consultar indicadores")
    void eventoInexistente() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.generar(99L));
        verify(indicadorEventoRepository, never()).calcular(anyLong());
    }
}
