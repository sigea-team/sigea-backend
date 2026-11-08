package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.actividad.ConflictoActividadResponse.TipoConflicto;
import com.sigea.demosigea_backend.dto.agenda.AgendaPublicaResponse;
import com.sigea.demosigea_backend.dto.agenda.GeneracionAgendaResponse;
import com.sigea.demosigea_backend.dto.agenda.PublicacionAgendaResponse;
import com.sigea.demosigea_backend.dto.agenda.ValidacionAgendaResponse;
import com.sigea.demosigea_backend.exception.AgendaNoPublicableException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.AgendaPublicada;
import com.sigea.demosigea_backend.model.DisponibilidadPonente;
import com.sigea.demosigea_backend.model.DisponibilidadSala;
import com.sigea.demosigea_backend.model.EstadoAgenda;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.ModalidadEvento;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Sala;
import com.sigea.demosigea_backend.model.TipoActividad;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoSala;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.AgendaPublicadaRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadPonenteRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadSalaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.SalaRepository;
import com.sigea.demosigea_backend.service.GeneradorAgenda.MotivoPendiente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link AgendaService} (HU-22: generar y publicar la agenda del evento).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendaServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2027, 3, 1, 10, 0);
    private static final Long EVENTO = 1L;
    private static final LocalDate DIA = LocalDate.of(2027, 3, 10);

    @Mock private EventoRepository eventoRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private SalaRepository salaRepository;
    @Mock private DisponibilidadSalaRepository disponibilidadSalaRepository;
    @Mock private DisponibilidadPonenteRepository disponibilidadPonenteRepository;
    @Mock private AgendaPublicadaRepository agendaPublicadaRepository;
    @Mock private AvisoCambiosAgenda avisoCambiosAgenda;
    @Mock private AuditoriaService auditoriaService;

    private AgendaService service;
    private Evento evento;
    private Sala auditorio;
    private Sala virtual;
    private TipoActividad taller;
    private Persona ana;
    private Persona luis;
    private final List<Actividad> agenda = new ArrayList<>();
    private final List<AgendaPublicada> filas = new ArrayList<>();

    @BeforeEach
    void preparar() {
        service = new AgendaService(eventoRepository, actividadRepository, salaRepository,
                disponibilidadSalaRepository, disponibilidadPonenteRepository, agendaPublicadaRepository,
                avisoCambiosAgenda, auditoriaService, Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA));

        evento = Evento.builder().id(EVENTO).nombre("Simposio").estado(EstadoEvento.habilitado)
                .fechaInicio(DIA).fechaFin(DIA.plusDays(1)).build();
        auditorio = Sala.builder().id(10L).evento(evento).nombre("Auditorio").ubicacion("Edificio A")
                .tipo(TipoSala.fisica).aforoMaximo(100).build();
        virtual = Sala.builder().id(12L).evento(evento).nombre("Sala virtual").ubicacion("https://meet.example/x")
                .tipo(TipoSala.virtual).aforoMaximo(300).build();
        taller = TipoActividad.builder().id(5L).evento(evento).nombre("Taller").build();
        ana = Persona.builder().id(20L).nombres("Ana").apellidos("Pérez").build();
        luis = Persona.builder().id(21L).nombres("Luis").apellidos("Gómez").build();

        when(eventoRepository.findById(EVENTO)).thenReturn(Optional.of(evento));
        when(eventoRepository.findByIdParaProgramar(EVENTO)).thenReturn(Optional.of(evento));
        when(actividadRepository.findAgendaDelEvento(EVENTO)).thenReturn(agenda);
        when(salaRepository.findByEvento_IdOrderByNombreAscIdAsc(EVENTO)).thenReturn(List.of(auditorio, virtual));
        when(disponibilidadSalaRepository.findBySala_IdInOrderByFechaAscHoraInicioAsc(anyCollection())).thenReturn(List.of(
                franjaSala(auditorio, DIA, 8, 12), franjaSala(virtual, DIA, 8, 18)));
        when(disponibilidadPonenteRepository.findByEvento_IdOrderByFechaAscHoraInicioAsc(EVENTO)).thenReturn(List.of(
                franjaPonente(ana, DIA, 8, 12), franjaPonente(luis, DIA, 10, 12)));
        when(agendaPublicadaRepository.findByEvento_IdOrderByFechaDiaAsc(EVENTO)).thenReturn(filas);
        when(actividadRepository.findDeOtrosEventosEnSalas(eq(EVENTO), anyCollection(), any(), any()))
                .thenReturn(otrosEventos);
    }

    private final List<Actividad> otrosEventos = new ArrayList<>();

    /** Actividad de otro evento que usa una sala compartida (salas reutilizables). */
    private Actividad deOtroEvento(Long id, LocalDate dia, int desde, int hasta, Sala sala) {
        Evento otro = Evento.builder().id(2L).nombre("Feria").estado(EstadoEvento.habilitado).build();
        Actividad a = Actividad.builder().id(id).evento(otro).nombre("Stand").tipoActividad(taller).fecha(dia)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).sala(sala).build();
        otrosEventos.add(a);
        return a;
    }

    // ── Helpers ──

    private DisponibilidadSala franjaSala(Sala s, LocalDate dia, int desde, int hasta) {
        return DisponibilidadSala.builder().sala(s).fecha(dia)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).build();
    }

    private DisponibilidadPonente franjaPonente(Persona p, LocalDate dia, int desde, int hasta) {
        return DisponibilidadPonente.builder().persona(p).evento(evento).fecha(dia)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).build();
    }

    private Actividad porProgramar(Long id, String nombre, int minutos, Persona ponente, ModalidadEvento modalidad) {
        Actividad a = Actividad.builder().id(id).evento(evento).nombre(nombre).tipoActividad(taller)
                .estado(Actividad.ESTADO_POR_PROGRAMAR).duracionMinutos(minutos).ponente(ponente)
                .modalidad(modalidad).build();
        agenda.add(a);
        return a;
    }

    private Actividad programada(Long id, String nombre, LocalDate dia, int desde, int hasta, Sala sala,
                                 Persona ponente) {
        Actividad a = Actividad.builder().id(id).evento(evento).nombre(nombre).tipoActividad(taller).fecha(dia)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).sala(sala).ponente(ponente)
                .duracionMinutos((hasta - desde) * 60).build();
        agenda.add(a);
        return a;
    }

    // ── Criterios 1 y 2 ──

    @Nested
    @DisplayName("Criterios 1 y 2: generación automática")
    class Generacion {

        @Test
        @DisplayName("Criterio 1: propone una distribución sin conflictos y no guarda nada")
        void proponeSinGuardar() {
            programada(1L, "Inauguración", DIA, 8, 9, auditorio, null);
            Actividad a = porProgramar(2L, "Taller de Ana", 120, ana, ModalidadEvento.presencial);
            porProgramar(3L, "Charla de Luis", 60, luis, ModalidadEvento.presencial);

            GeneracionAgendaResponse r = service.proponer(EVENTO, false);

            assertFalse(r.aplicada());
            assertEquals(2, r.totalPorUbicar());
            assertEquals(2, r.asignadas().size());
            assertTrue(r.pendientes().isEmpty());
            // Ana: después de la inauguración, en el auditorio (la actividad es presencial)
            var deAna = r.asignadas().stream().filter(x -> x.actividadId() == 2L).findFirst().orElseThrow();
            assertEquals(LocalTime.of(9, 0), deAna.horaInicio());
            assertEquals(LocalTime.of(11, 0), deAna.horaFin());
            assertEquals("Auditorio", deAna.salaNombre());
            // Luis solo está de 10 a 12 y el auditorio está ocupado hasta las 11
            var deLuis = r.asignadas().stream().filter(x -> x.actividadId() == 3L).findFirst().orElseThrow();
            assertEquals(LocalTime.of(11, 0), deLuis.horaInicio());
            // No se modificó la entidad ni se guardó
            assertNull(a.getFecha());
            verify(actividadRepository, never()).saveAll(any());
            verify(auditoriaService, never()).registrar(any(), any(), any(), anyMap());
        }

        @Test
        @DisplayName("Criterio 2: informa qué actividades quedan pendientes de asignación manual y por qué")
        void informaPendientes() {
            porProgramar(2L, "Sin disponibilidad", 60, Persona.builder().id(99L).nombres("Eva").apellidos("Ruiz").build(),
                    null);
            porProgramar(3L, "Muy larga", 300, null, ModalidadEvento.presencial);

            GeneracionAgendaResponse r = service.proponer(EVENTO, false);

            assertTrue(r.asignadas().isEmpty());
            assertEquals(2, r.pendientes().size());
            var sinDisp = r.pendientes().stream().filter(p -> p.actividadId() == 2L).findFirst().orElseThrow();
            assertEquals(MotivoPendiente.SIN_DISPONIBILIDAD_PONENTE, sinDisp.motivo());
            assertTrue(sinDisp.mensaje().contains("Eva Ruiz"));
            var larga = r.pendientes().stream().filter(p -> p.actividadId() == 3L).findFirst().orElseThrow();
            assertEquals(MotivoPendiente.SIN_FRANJA_COMUN, larga.motivo());
            assertTrue(larga.mensaje().contains("300 minutos"));
        }

        @Test
        @DisplayName("una actividad virtual se ubica en la sala virtual")
        void virtualEnSalaVirtual() {
            porProgramar(2L, "Webinar", 60, null, ModalidadEvento.virtual);
            GeneracionAgendaResponse r = service.proponer(EVENTO, false);
            assertEquals(virtual.getId(), r.asignadas().get(0).salaId());
        }

        @Test
        @DisplayName("Criterio 1: aplicar guarda la propuesta como programada y automática, y la audita")
        void aplicaYGuarda() {
            Actividad a = porProgramar(2L, "Taller de Ana", 120, ana, ModalidadEvento.presencial);

            GeneracionAgendaResponse r = service.aplicar(EVENTO, false);

            assertTrue(r.aplicada());
            assertEquals(Actividad.ESTADO_PROGRAMADA, a.getEstado());
            assertTrue(a.isAsignacionAutomatica());
            assertEquals(DIA, a.getFecha());
            assertEquals(LocalTime.of(8, 0), a.getHoraInicio());
            assertEquals(LocalTime.of(10, 0), a.getHoraFin());
            assertEquals(auditorio, a.getSala());
            verify(actividadRepository).saveAll(any());
            verify(avisoCambiosAgenda).actividadModificada(eq(a), any(AvisoCambiosAgenda.Programacion.class));
            verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.AGENDA_GENERADA), eq("eventos"),
                    eq(EVENTO), anyMap());
        }

        @Test
        @DisplayName("Criterio 2: al aplicar, las que no caben siguen por programar")
        void aplicaConPendientes() {
            Actividad a = porProgramar(3L, "Muy larga", 300, null, ModalidadEvento.presencial);
            GeneracionAgendaResponse r = service.aplicar(EVENTO, false);
            assertEquals(1, r.pendientes().size());
            assertTrue(a.estaPorProgramar());
            assertNull(a.getFecha());
        }

        @Test
        @DisplayName("las actividades ajustadas a mano no se mueven al reubicar las automáticas")
        void reubicarRespetaManuales() {
            Actividad manual = programada(1L, "Manual", DIA, 8, 10, auditorio, ana);
            Actividad automatica = programada(2L, "Automática", DIA, 10, 11, auditorio, luis);
            automatica.setAsignacionAutomatica(true);
            porProgramar(3L, "Nueva de Luis", 60, luis, ModalidadEvento.presencial);

            GeneracionAgendaResponse r = service.aplicar(EVENTO, true);

            assertEquals(2, r.totalPorUbicar());
            assertEquals(LocalTime.of(8, 0), manual.getHoraInicio());
            assertTrue(r.pendientes().isEmpty());
            assertEquals(Actividad.ESTADO_PROGRAMADA, automatica.getEstado());
        }

        @Test
        @DisplayName("salas reutilizables: no ubica nada donde otro evento ya usa la sala")
        void respetaSalaUsadaPorOtroEvento() {
            deOtroEvento(500L, DIA, 8, 11, auditorio);
            porProgramar(2L, "Taller de Ana", 60, ana, ModalidadEvento.presencial);

            GeneracionAgendaResponse r = service.proponer(EVENTO, false);

            assertEquals(LocalTime.of(11, 0), r.asignadas().get(0).horaInicio());
            assertEquals("Auditorio", r.asignadas().get(0).salaNombre());
        }

        @Test
        @DisplayName("no genera la agenda de un evento cerrado")
        void eventoCerrado() {
            evento.setEstado(EstadoEvento.cerrado);
            assertThrows(OperacionNoPermitidaException.class, () -> service.proponer(EVENTO, false));
            assertThrows(OperacionNoPermitidaException.class, () -> service.aplicar(EVENTO, false));
        }
    }

    // ── Criterios 3 y 4 ──

    @Nested
    @DisplayName("Criterios 3 y 4: validación y publicación")
    class Publicacion {

        @Test
        @DisplayName("Criterio 4: con actividades sin sala u horario no publica y las lista")
        void incompletaNoPublica() {
            programada(1L, "Inauguración", DIA, 8, 9, auditorio, null);
            porProgramar(2L, "Sin horario", 60, null, null);

            AgendaNoPublicableException e = assertThrows(AgendaNoPublicableException.class,
                    () -> service.publicar(EVENTO));

            assertEquals(AgendaNoPublicableException.AGENDA_INCOMPLETA, e.getCodigo());
            assertEquals(1, e.getIncompletas().size());
            assertEquals(2L, e.getIncompletas().get(0).actividadId());
            assertEquals(List.of("fecha", "horario", "sala"), e.getIncompletas().get(0).faltantes());
            verify(agendaPublicadaRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Criterio 3: con cruces no publica y lista cada pareja una sola vez")
        void conflictosNoPublica() {
            programada(1L, "A", DIA, 8, 10, auditorio, ana);
            programada(2L, "B", DIA, 9, 11, auditorio, ana);

            AgendaNoPublicableException e = assertThrows(AgendaNoPublicableException.class,
                    () -> service.publicar(EVENTO));

            assertEquals(AgendaNoPublicableException.AGENDA_CON_CONFLICTOS, e.getCodigo());
            assertTrue(e.getConflictos().stream().allMatch(c -> c.actividadId() == 1L && c.conflicto().actividadId() == 2L));
            assertTrue(e.getConflictos().stream().anyMatch(c -> c.conflicto().tipo() == TipoConflicto.PONENTE));
            assertTrue(e.getConflictos().stream().anyMatch(c -> c.conflicto().tipo() == TipoConflicto.SALA));
        }

        @Test
        @DisplayName("salas reutilizables: no publica si una actividad se cruza con otro evento en la misma sala")
        void conflictoConOtroEventoNoPublica() {
            programada(1L, "A", DIA, 8, 10, auditorio, ana);
            deOtroEvento(500L, DIA, 9, 11, auditorio);

            AgendaNoPublicableException e = assertThrows(AgendaNoPublicableException.class,
                    () -> service.publicar(EVENTO));

            assertEquals(AgendaNoPublicableException.AGENDA_CON_CONFLICTOS, e.getCodigo());
            assertEquals(500L, e.getConflictos().get(0).conflicto().actividadId());
            assertTrue(e.getConflictos().get(0).conflicto().mensaje().contains("Feria"));
        }

        @Test
        @DisplayName("sin actividades no publica")
        void vaciaNoPublica() {
            AgendaNoPublicableException e = assertThrows(AgendaNoPublicableException.class,
                    () -> service.publicar(EVENTO));
            assertEquals(AgendaNoPublicableException.AGENDA_VACIA, e.getCodigo());
        }

        @Test
        @DisplayName("un evento que todavía no está publicado no puede publicar su agenda")
        void eventoSinPublicar() {
            evento.setEstado(EstadoEvento.en_configuracion);
            programada(1L, "A", DIA, 8, 10, auditorio, ana);
            assertThrows(OperacionNoPermitidaException.class, () -> service.publicar(EVENTO));
        }

        @Test
        @DisplayName("Criterio 3: publica la agenda por día y deja de mostrar los días que quedaron vacíos")
        @SuppressWarnings("unchecked")
        void publicaPorDia() {
            programada(1L, "A", DIA, 8, 10, auditorio, ana);
            programada(2L, "B", DIA.plusDays(1), 8, 10, auditorio, ana);
            programada(3L, "Cancelada", DIA, 8, 10, auditorio, luis).setEstado(Actividad.ESTADO_CANCELADA);
            AgendaPublicada viejo = AgendaPublicada.builder().id(7L).evento(evento).fechaDia(DIA.minusDays(1))
                    .estado(EstadoAgenda.publicada).fechaPublicacion(AHORA.minusDays(3)).build();
            filas.add(viejo);

            PublicacionAgendaResponse r = service.publicar(EVENTO);

            assertEquals(List.of(DIA, DIA.plusDays(1)), r.diasPublicados());
            assertEquals(2, r.totalActividades());
            assertEquals(AHORA, r.fechaPublicacion());
            ArgumentCaptor<Collection<AgendaPublicada>> guardadas = ArgumentCaptor.forClass(Collection.class);
            verify(agendaPublicadaRepository).saveAll(guardadas.capture());
            assertEquals(3, guardadas.getValue().size());
            assertEquals(EstadoAgenda.no_publicada, viejo.getEstado());
            assertTrue(guardadas.getValue().stream().filter(f -> f != viejo)
                    .allMatch(f -> f.getEstado() == EstadoAgenda.publicada && AHORA.equals(f.getFechaPublicacion())));
            verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.AGENDA_PUBLICADA), eq("eventos"),
                    eq(EVENTO), anyMap());
        }

        @Test
        @DisplayName("la revisión previa indica si se puede publicar y qué falta")
        void validacion() {
            programada(1L, "A", DIA, 8, 10, auditorio, ana);
            porProgramar(2L, "Sin horario", 60, null, null);

            ValidacionAgendaResponse r = service.validar(EVENTO);

            assertFalse(r.puedePublicar());
            assertTrue(r.motivoNoPublicable().contains("sin sala u horario"));
            assertFalse(r.publicada());
            assertEquals(1, r.totalActividades());
            assertEquals(1, r.incompletas().size());
            assertEquals(1, r.dias().size());
            assertEquals(EstadoAgenda.no_publicada, r.dias().get(0).estado());
        }

        @Test
        @DisplayName("una agenda completa y sin cruces se puede publicar")
        void validacionLista() {
            programada(1L, "A", DIA, 8, 10, auditorio, ana);
            ValidacionAgendaResponse r = service.validar(EVENTO);
            assertTrue(r.puedePublicar());
            assertNull(r.motivoNoPublicable());
        }
    }

    // ── Criterio 3: consulta pública ──

    @Nested
    @DisplayName("Criterio 3: agenda visible para los participantes")
    class AgendaPublica {

        @Test
        @DisplayName("sin agenda publicada responde no encontrada")
        void noPublicada() {
            when(agendaPublicadaRepository.findByEvento_IdAndEstadoOrderByFechaDiaAsc(EVENTO, EstadoAgenda.publicada))
                    .thenReturn(List.of());
            assertThrows(RecursoNoEncontradoException.class, () -> service.agendaPublica(EVENTO));
        }

        @Test
        @DisplayName("un evento sin publicar no expone su agenda")
        void eventoSinPublicar() {
            evento.setEstado(EstadoEvento.en_configuracion);
            assertThrows(RecursoNoEncontradoException.class, () -> service.agendaPublica(EVENTO));
        }

        @Test
        @DisplayName("organiza las actividades por día y hora y oculta el enlace de las salas virtuales")
        void organizadaPorDia() {
            when(agendaPublicadaRepository.findByEvento_IdAndEstadoOrderByFechaDiaAsc(EVENTO, EstadoAgenda.publicada))
                    .thenReturn(List.of(
                            AgendaPublicada.builder().evento(evento).fechaDia(DIA).estado(EstadoAgenda.publicada)
                                    .fechaPublicacion(AHORA).build(),
                            AgendaPublicada.builder().evento(evento).fechaDia(DIA.plusDays(1))
                                    .estado(EstadoAgenda.publicada).fechaPublicacion(AHORA.minusHours(1)).build()));
            Actividad a1 = programada(1L, "Apertura", DIA, 8, 9, auditorio, null);
            Actividad a2 = programada(2L, "Webinar", DIA, 10, 11, virtual, ana);
            Actividad a3 = programada(3L, "Cierre", DIA.plusDays(1), 16, 17, auditorio, luis);
            when(actividadRepository.findEnFechasSinEstados(eq(EVENTO), anyCollection(), anyCollection()))
                    .thenReturn(List.of(a1, a2, a3));

            AgendaPublicaResponse r = service.agendaPublica(EVENTO);

            assertEquals("Simposio", r.eventoNombre());
            assertEquals(AHORA, r.fechaPublicacion());
            assertEquals(2, r.dias().size());
            assertEquals(DIA, r.dias().get(0).fecha());
            assertEquals(List.of("Apertura", "Webinar"),
                    r.dias().get(0).actividades().stream().map(x -> x.nombre()).toList());
            assertEquals("Edificio A", r.dias().get(0).actividades().get(0).ubicacion());
            assertNull(r.dias().get(0).actividades().get(1).ubicacion());
            assertEquals("Ana Pérez", r.dias().get(0).actividades().get(1).ponente());
            assertEquals("Cierre", r.dias().get(1).actividades().get(0).nombre());
        }
    }
}
