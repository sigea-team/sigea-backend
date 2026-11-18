package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.periodo.ComparacionSemestresResponse;
import com.sigea.demosigea_backend.dto.periodo.ConsultaPeriodoResponse;
import com.sigea.demosigea_backend.dto.periodo.FiltroPeriodo;
import com.sigea.demosigea_backend.dto.periodo.IndicadoresPeriodoResponse;
import com.sigea.demosigea_backend.dto.periodo.VariacionIndicadorResponse;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.CategoriaParticipante;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.TipoActividad;
import com.sigea.demosigea_backend.model.TipoEvento;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.InscripcionActividadRepository;
import com.sigea.demosigea_backend.repository.InscripcionEventoRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PeriodoAcademicoServiceTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private InscripcionEventoRepository inscripcionEventoRepository;
    @Mock private InscripcionActividadRepository inscripcionActividadRepository;
    @Mock private ParticipacionConferencistaRepository participacionRepository;
    @Mock private PropuestaRepository propuestaRepository;

    @InjectMocks
    private PeriodoAcademicoService service;

    private static Evento evento(Long id) {
        return Evento.builder().id(id).nombre("Evento " + id).semestre("2026-1")
                .fechaInicio(LocalDate.of(2026, 3, 1)).fechaFin(LocalDate.of(2026, 3, 3))
                .estado(EstadoEvento.cerrado)
                .tipoEvento(TipoEvento.builder().id(1L).nombre("Congreso").build()).build();
    }

    private static Actividad actividad(Long id, Evento evento, String tipo, String estado) {
        return Actividad.builder().id(id).evento(evento).nombre("Act " + id).estado(estado)
                .tipoActividad(TipoActividad.builder().id(id).nombre(tipo).build())
                .fecha(LocalDate.of(2026, 3, 2)).build();
    }

    private static List<Object[]> filas(Object[]... filas) {
        return new ArrayList<>(List.of(filas));
    }

    @SuppressWarnings("unchecked")
    private void eventosEncontrados(List<Evento> eventos) {
        when(eventoRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(eventos);
    }

    private void datosDeUnEvento() {
        Evento e = evento(1L);
        eventosEncontrados(List.of(e));
        when(actividadRepository.findDeEventos(anyCollection())).thenReturn(List.of(
                actividad(10L, e, "Taller", Actividad.ESTADO_PROGRAMADA),
                actividad(11L, e, "Conferencia", Actividad.ESTADO_PROGRAMADA),
                actividad(12L, e, "Taller", Actividad.ESTADO_CANCELADA)));
        when(inscripcionEventoRepository.contarPorEvento(anyCollection())).thenReturn(filas(new Object[]{1L, 4L}));
        when(inscripcionEventoRepository.contarPersonasDistintas(anyCollection())).thenReturn(4L);
        when(inscripcionEventoRepository.contarPorCategoria(anyCollection())).thenReturn(filas(
                new Object[]{CategoriaParticipante.estudiante, 3L}, new Object[]{CategoriaParticipante.docente, 1L}));
        when(inscripcionActividadRepository.contarPorActividad(anyCollection())).thenReturn(filas(
                new Object[]{10L, 2L}, new Object[]{11L, 3L}));
        when(participacionRepository.contarPorTipo(anyCollection())).thenReturn(filas(
                new Object[]{TipoParticipacionConferencista.conferencista, 1L},
                new Object[]{TipoParticipacionConferencista.ponente, 2L}));
        when(propuestaRepository.contarPorEstado(anyCollection())).thenReturn(filas(
                new Object[]{EstadoPropuesta.aprobada, 2L}, new Object[]{EstadoPropuesta.ajustes, 1L},
                new Object[]{EstadoPropuesta.rechazada, 1L}));
    }

    @Test
    @DisplayName("Criterio 3: indicadores del semestre, sin contar actividades canceladas")
    void calcularIndicadores_conEventos() {
        datosDeUnEvento();

        IndicadoresPeriodoResponse r = service.calcularIndicadores(new FiltroPeriodo("2026-1", null, null, null, null));

        assertTrue(r.hayInformacion());
        assertNull(r.mensaje());
        assertEquals(1, r.totalEventos());
        assertEquals(1L, r.eventosPorTipo().get("Congreso"));
        assertEquals(1L, r.eventosPorEstado().get("cerrado"));
        assertEquals(2, r.totalActividades());
        assertEquals(1L, r.actividadesPorTipo().get("Taller"));
        assertEquals(4, r.totalInscripciones());
        assertEquals(4, r.participantesUnicos());
        assertEquals(3L, r.inscripcionesPorCategoria().get("estudiante"));
        assertEquals(5, r.inscripcionesActividades());
        assertEquals(new BigDecimal("4.00"), r.promedioInscritosPorEvento());
        assertEquals(1, r.totalConferencistas());
        assertEquals(2, r.totalPonentes());
        assertEquals(4, r.propuestasRecibidas());
        assertEquals(2, r.propuestasAprobadas());
        assertEquals(1, r.propuestasConAjustes());
        // Tasa = (aprobada + ajustes) / recibidas: "aprobada con ajustes" también es un resultado positivo.
        assertEquals(new BigDecimal("75.00"), r.tasaAprobacionPropuestas());
    }

    @Test
    @DisplayName("Criterio 3: el filtro de tipo de actividad deja solo las actividades de ese tipo")
    void consultarEventos_filtroTipoActividad() {
        datosDeUnEvento();

        ConsultaPeriodoResponse r = service.consultarEventos(new FiltroPeriodo(null, null, null, null, "taller"));

        assertTrue(r.hayInformacion());
        assertEquals(1, r.totalEventos());
        assertEquals(4, r.eventos().get(0).inscritos());
        assertEquals(2, r.eventos().get(0).totalActividades());
        assertTrue(r.eventos().get(0).actividades().stream().allMatch(a -> a.tipoActividad().equals("Taller")));
        assertEquals(2, r.eventos().get(0).actividades().get(0).inscritos());
    }

    @Test
    @DisplayName("Criterio 4: semestre sin eventos informa que no hay información disponible")
    void consultarEventos_semestreSinEventos() {
        eventosEncontrados(List.of());

        ConsultaPeriodoResponse r = service.consultarEventos(new FiltroPeriodo("2019-1", null, null, null, null));

        assertFalse(r.hayInformacion());
        assertEquals("No hay información disponible para el semestre 2019-1.", r.mensaje());
        assertTrue(r.eventos().isEmpty());
        verify(actividadRepository, never()).findDeEventos(anyCollection());
    }

    @Test
    @DisplayName("Criterio 4: indicadores en cero y mensaje si no hay eventos")
    void calcularIndicadores_sinEventos() {
        eventosEncontrados(List.of());

        IndicadoresPeriodoResponse r = service.calcularIndicadores(
                new FiltroPeriodo("2019-1", null, null, 3L, null));

        assertFalse(r.hayInformacion());
        assertEquals("No hay información disponible para los filtros aplicados.", r.mensaje());
        assertEquals(0, r.totalEventos());
        assertNull(r.tasaAprobacionPropuestas());
        verify(inscripcionEventoRepository, never()).contarPorEvento(anyCollection());
    }

    @Test
    @DisplayName("Criterio 3: filtros inválidos → SolicitudInvalidaException")
    void filtrosInvalidos() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarEventos(new FiltroPeriodo("2026", null, null, null, null)));
        assertThrows(SolicitudInvalidaException.class, () -> service.calcularIndicadores(
                new FiltroPeriodo(null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), null, null)));
    }

    @Test
    @DisplayName("Criterio 5: compara los indicadores de dos semestres")
    void comparar_dosSemestres() {
        datosDeUnEvento();

        ComparacionSemestresResponse r = service.comparar("2025-2", "2026-1", null, null);

        assertEquals("2025-2", r.semestreA());
        assertEquals("2025-2", r.periodoA().filtros().semestre());
        assertEquals("2026-1", r.periodoB().filtros().semestre());
        assertNull(r.mensaje());
        VariacionIndicadorResponse eventos = r.comparativo().stream()
                .filter(v -> v.codigo().equals("totalEventos")).findFirst().orElseThrow();
        assertEquals("SIN_CAMBIO", eventos.tendencia());
        assertEquals(12, r.comparativo().size());
    }

    @Test
    @DisplayName("Criterio 5: avisa cuando uno de los semestres no tiene información")
    @SuppressWarnings("unchecked")
    void comparar_unSemestreSinInformacion() {
        datosDeUnEvento();
        Evento e = evento(1L);
        when(eventoRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(e))
                .thenReturn(List.of());

        ComparacionSemestresResponse r = service.comparar("2025-2", "2026-1", null, null);

        assertTrue(r.periodoA().hayInformacion());
        assertFalse(r.periodoB().hayInformacion());
        assertEquals("No hay información disponible para el semestre 2026-1.", r.mensaje());
    }

    @Test
    @DisplayName("Criterio 5: faltan semestres, formato inválido o semestres iguales → SolicitudInvalidaException")
    void comparar_solicitudInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> service.comparar(null, "2026-1", null, null));
        assertThrows(SolicitudInvalidaException.class, () -> service.comparar("2026-1", "2026-9", null, null));
        assertThrows(SolicitudInvalidaException.class, () -> service.comparar("2026-1", " 2026-1 ", null, null));
    }

    @Test
    @DisplayName("Criterio 5: cálculo de variación, porcentaje y tendencia")
    void variacion() {
        VariacionIndicadorResponse aumento = PeriodoAcademicoService.variacion("x", "X",
                BigDecimal.valueOf(4), BigDecimal.valueOf(6));
        assertEquals(new BigDecimal("2"), aumento.diferencia());
        assertEquals(new BigDecimal("50.00"), aumento.variacionPorcentual());
        assertEquals("AUMENTO", aumento.tendencia());

        VariacionIndicadorResponse desdeCero = PeriodoAcademicoService.variacion("x", "X",
                BigDecimal.ZERO, BigDecimal.valueOf(3));
        assertNull(desdeCero.variacionPorcentual());
        assertEquals("AUMENTO", desdeCero.tendencia());

        assertEquals("DISMINUCION", PeriodoAcademicoService.variacion("x", "X",
                BigDecimal.TEN, BigDecimal.ONE).tendencia());
        assertEquals("NO_COMPARABLE", PeriodoAcademicoService.variacion("x", "X", null, BigDecimal.ONE).tendencia());
    }

    @Test
    @DisplayName("Rango de fechas: un evento sin la fecha necesaria queda fuera, igual que en la consulta JPA")
    void eventoCumple_fechasNulasCoherenteConSql() {
        Evento sinFechas = Evento.builder().id(9L).semestre("2026-1").build();
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 6, 30);

        assertTrue(ReglasFiltroPeriodo.eventoCumple(sinFechas, FiltroPeriodo.vacio()));
        assertFalse(ReglasFiltroPeriodo.eventoCumple(sinFechas, new FiltroPeriodo(null, desde, null, null, null)));
        assertFalse(ReglasFiltroPeriodo.eventoCumple(sinFechas, new FiltroPeriodo(null, null, hasta, null, null)));
        assertTrue(ReglasFiltroPeriodo.eventoCumple(evento(1L), new FiltroPeriodo(null, desde, hasta, null, null)));
    }

    @Test
    @DisplayName("Lista los semestres registrados")
    void listarSemestres() {
        when(eventoRepository.findSemestresRegistrados()).thenReturn(List.of("2026-1", "2025-2"));
        assertEquals(List.of("2026-1", "2025-2"), service.listarSemestres());
    }
}
