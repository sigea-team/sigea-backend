package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.actividad.ActividadRequest;
import com.sigea.demosigea_backend.dto.actividad.ActividadResponse;
import com.sigea.demosigea_backend.dto.actividad.AdvertenciaActividadResponse.TipoAdvertencia;
import com.sigea.demosigea_backend.dto.actividad.ConflictoActividadResponse;
import com.sigea.demosigea_backend.dto.actividad.ConflictoActividadResponse.TipoConflicto;
import com.sigea.demosigea_backend.dto.actividad.ValidacionActividadResponse;
import com.sigea.demosigea_backend.dto.agenda.ActividadPorProgramarRequest;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.ProgramacionActividadException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.DisponibilidadPonente;
import com.sigea.demosigea_backend.model.DisponibilidadSala;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.Sala;
import com.sigea.demosigea_backend.model.TipoActividad;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoSala;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadPonenteRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadSalaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.LineaTematicaRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.VersionPropuestaRepository;
import com.sigea.demosigea_backend.repository.SalaRepository;
import com.sigea.demosigea_backend.repository.TipoActividadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ActividadService} (HU-21: programar actividades controlando conflictos).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActividadServiceTest {

    @Mock private ActividadRepository actividadRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private SalaRepository salaRepository;
    @Mock private TipoActividadRepository tipoActividadRepository;
    @Mock private LineaTematicaRepository lineaTematicaRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private PropuestaRepository propuestaRepository;
    @Mock private VersionPropuestaRepository versionPropuestaRepository;
    @Mock private ParticipacionConferencistaRepository participacionRepository;
    @Mock private DisponibilidadSalaRepository disponibilidadSalaRepository;
    @Mock private DisponibilidadPonenteRepository disponibilidadPonenteRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private AvisoCambiosAgenda avisoCambiosAgenda;

    @InjectMocks
    private ActividadService service;

    private static final Long EVENTO = 1L;
    private static final LocalDate DIA = LocalDate.of(2027, 3, 10);

    private Evento evento;
    private Sala auditorio;
    private Sala salaB;
    private Sala salaVirtual;
    private TipoActividad taller;
    private Persona ana;
    private Persona luis;
    private final List<Actividad> agenda = new ArrayList<>();

    @BeforeEach
    void preparar() {
        evento = Evento.builder().id(EVENTO).nombre("Simposio").estado(EstadoEvento.habilitado)
                .fechaInicio(DIA).fechaFin(DIA.plusDays(2)).build();
        auditorio = sala(10L, "Auditorio", false);
        salaB = sala(11L, "Sala B", false);
        salaVirtual = sala(12L, "Sala virtual", true);
        taller = TipoActividad.builder().id(5L).evento(evento).nombre("Taller").build();
        ana = Persona.builder().id(20L).nombres("Ana").apellidos("Pérez").build();
        luis = Persona.builder().id(21L).nombres("Luis").apellidos("Gómez").build();

        when(eventoRepository.findById(EVENTO)).thenReturn(Optional.of(evento));
        when(eventoRepository.findByIdParaProgramar(EVENTO)).thenReturn(Optional.of(evento));
        for (Sala s : List.of(auditorio, salaB, salaVirtual)) {
            when(salaRepository.findByIdAndEvento_Id(s.getId(), EVENTO)).thenReturn(Optional.of(s));
            // Disponibilidad amplia: sin advertencias salvo que la prueba lo cambie
            when(disponibilidadSalaRepository.findBySala_IdOrderByFechaAscHoraInicioAsc(s.getId()))
                    .thenReturn(List.of(franjaSala(s, 7, 20)));
        }
        when(tipoActividadRepository.findByIdAndEvento_Id(5L, EVENTO)).thenReturn(Optional.of(taller));
        for (Persona p : List.of(ana, luis)) {
            when(personaRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(participacionRepository.existsByEvento_IdAndPersona_Id(EVENTO, p.getId())).thenReturn(true);
            when(disponibilidadPonenteRepository.findByPersona_IdAndEvento_IdOrderByFechaAscHoraInicioAsc(p.getId(), EVENTO))
                    .thenReturn(List.of(franjaPonente(p, 7, 20)));
        }
        when(actividadRepository.findAgendaDelEventoEnFecha(EVENTO, DIA)).thenReturn(agenda);
        when(actividadRepository.save(any(Actividad.class))).thenAnswer(inv -> {
            Actividad a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(99L);
            }
            return a;
        });
    }

    // ── Helpers ──

    private Sala sala(Long id, String nombre, boolean simultaneidad) {
        return Sala.builder().id(id).evento(evento).nombre(nombre).tipo(TipoSala.fisica).aforoMaximo(100)
                .permiteSimultaneidad(simultaneidad).build();
    }

    private DisponibilidadSala franjaSala(Sala s, int desde, int hasta) {
        return DisponibilidadSala.builder().sala(s).fecha(DIA)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).build();
    }

    private DisponibilidadPonente franjaPonente(Persona p, int desde, int hasta) {
        return DisponibilidadPonente.builder().persona(p).evento(evento).fecha(DIA)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).build();
    }

    private Actividad programada(Long id, String nombre, int desde, int hasta, Sala s, Persona p, boolean simultanea) {
        Actividad a = Actividad.builder().id(id).evento(evento).nombre(nombre).tipoActividad(taller).fecha(DIA)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).sala(s).ponente(p)
                .permiteSimultaneidad(simultanea).build();
        agenda.add(a);
        return a;
    }

    private ActividadRequest solicitud(int desde, int hasta, Sala s, Persona p, boolean simultanea, boolean confirmar) {
        return new ActividadRequest("Nueva actividad", 5L, DIA, LocalTime.of(desde, 0), LocalTime.of(hasta, 0),
                s.getId(), p != null ? p.getId() : null, null, null, null, simultanea, confirmar);
    }

    private List<TipoConflicto> tipos(ProgramacionActividadException e) {
        return e.getConflictos().stream().map(ConflictoActividadResponse::tipo).toList();
    }

    // ── Criterio 1 ──

    @Nested
    @DisplayName("Criterio 1: agregar la actividad a la agenda")
    class Criterio1 {

        @Test
        @DisplayName("programa una actividad sin cruces y la audita")
        void programaSinCruces() {
            programada(1L, "Inauguración", 8, 9, auditorio, luis, false);

            ActividadResponse r = service.crear(EVENTO, solicitud(9, 11, auditorio, ana, false, false));

            assertEquals(99L, r.id());
            assertEquals("Auditorio", r.salaNombre());
            assertEquals("Ana Pérez", r.ponenteNombre());
            assertTrue(r.advertencias().isEmpty());
            verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ACTIVIDAD_PROGRAMADA), eq("actividades"),
                    eq(99L), anyMap());
        }

        @Test
        @DisplayName("una actividad que termina justo cuando empieza otra no es un cruce")
        void bordesNoSonCruce() {
            programada(1L, "Inauguración", 8, 9, auditorio, ana, false);
            ActividadResponse r = service.crear(EVENTO, solicitud(9, 10, auditorio, ana, false, false));
            assertEquals(99L, r.id());
        }

        @Test
        @DisplayName("la agenda se consulta en orden cronológico desde el repositorio")
        void agendaCronologica() {
            Actividad tarde = Actividad.builder().id(2L).evento(evento).nombre("Tarde").tipoActividad(taller)
                    .fecha(DIA).horaInicio(LocalTime.of(14, 0)).horaFin(LocalTime.of(15, 0)).sala(auditorio).build();
            Actividad manana = Actividad.builder().id(1L).evento(evento).nombre("Mañana").tipoActividad(taller)
                    .fecha(DIA).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(9, 0)).sala(auditorio).build();
            when(actividadRepository.findAgendaDelEvento(EVENTO)).thenReturn(List.of(manana, tarde));

            List<ActividadResponse> lista = service.listar(EVENTO, null);
            assertEquals(List.of("Mañana", "Tarde"), lista.stream().map(ActividadResponse::nombre).toList());
        }
    }

    // ── Criterio 2 ──

    @Nested
    @DisplayName("Criterio 2: detectar cruces antes de guardar")
    class Criterio2 {

        @Test
        @DisplayName("misma sala a la misma hora: conflicto de SALA y no se guarda")
        void cruceDeSala() {
            programada(1L, "Conferencia", 9, 11, auditorio, luis, false);

            ProgramacionActividadException e = assertThrows(ProgramacionActividadException.class,
                    () -> service.crear(EVENTO, solicitud(10, 12, auditorio, ana, false, false)));

            assertEquals(ProgramacionActividadException.CONFLICTO_PROGRAMACION, e.getCodigo());
            assertEquals(List.of(TipoConflicto.SALA), tipos(e));
            verify(actividadRepository, never()).save(any());
        }

        @Test
        @DisplayName("mismo ponente a la misma hora en otra sala: conflicto de PONENTE")
        void cruceDePonente() {
            programada(1L, "Conferencia", 9, 11, salaB, ana, false);

            ProgramacionActividadException e = assertThrows(ProgramacionActividadException.class,
                    () -> service.crear(EVENTO, solicitud(10, 12, auditorio, ana, false, false)));

            assertTrue(tipos(e).contains(TipoConflicto.PONENTE));
            assertTrue(e.getConflictos().get(0).mensaje().contains("Ana Pérez"));
        }

        @Test
        @DisplayName("salas y ponentes distintos que se traslapan sin marcarse simultáneas: conflicto de HORARIO")
        void cruceDeHorario() {
            programada(1L, "Conferencia", 9, 11, salaB, luis, false);

            ProgramacionActividadException e = assertThrows(ProgramacionActividadException.class,
                    () -> service.crear(EVENTO, solicitud(10, 12, auditorio, ana, false, false)));

            assertEquals(List.of(TipoConflicto.HORARIO), tipos(e));
        }

        @Test
        @DisplayName("validar revisa la actividad sin guardarla")
        void validarNoGuarda() {
            programada(1L, "Conferencia", 9, 11, auditorio, luis, false);

            ValidacionActividadResponse v = service.validar(EVENTO, null, solicitud(10, 12, auditorio, ana, false, false));

            assertFalse(v.puedeGuardar());
            assertEquals(1, v.conflictos().size());
            verify(actividadRepository, never()).save(any());
        }

        @Test
        @DisplayName("las actividades canceladas no generan cruces")
        void canceladaNoCruza() {
            programada(1L, "Conferencia", 9, 11, auditorio, ana, false).setEstado("cancelada");
            ActividadResponse r = service.crear(EVENTO, solicitud(10, 12, auditorio, ana, false, false));
            assertEquals(99L, r.id());
        }
    }

    // ── Criterio 3 ──

    @Nested
    @DisplayName("Criterio 3: actividades simultáneas intencionales")
    class Criterio3 {

        @Test
        @DisplayName("dos actividades simultáneas en salas distintas se permiten sin alerta de horario")
        void simultaneasEnSalasDistintas() {
            programada(1L, "Taller A", 9, 11, salaB, luis, true);
            ActividadResponse r = service.crear(EVENTO, solicitud(9, 11, auditorio, ana, true, false));
            assertTrue(r.simultanea());
        }

        @Test
        @DisplayName("si solo la nueva está marcada como simultánea, sigue habiendo conflicto de horario")
        void ambasDebenSerSimultaneas() {
            programada(1L, "Conferencia", 9, 11, salaB, luis, false);

            ProgramacionActividadException e = assertThrows(ProgramacionActividadException.class,
                    () -> service.crear(EVENTO, solicitud(9, 11, auditorio, ana, true, false)));

            assertEquals(List.of(TipoConflicto.HORARIO), tipos(e));
            assertTrue(e.getConflictos().get(0).mensaje().contains("no está marcada como simultánea"));
        }

        @Test
        @DisplayName("simultáneas en la misma sala no configurada para ello: conflicto de SALA")
        void simultaneasMismaSalaNoConfigurada() {
            programada(1L, "Taller A", 9, 11, auditorio, luis, true);

            ProgramacionActividadException e = assertThrows(ProgramacionActividadException.class,
                    () -> service.crear(EVENTO, solicitud(9, 11, auditorio, ana, true, false)));

            assertEquals(List.of(TipoConflicto.SALA), tipos(e));
            assertTrue(e.getConflictos().get(0).mensaje().contains("no está configurada para actividades simultáneas"));
        }

        @Test
        @DisplayName("simultáneas en la misma sala configurada para simultaneidad: se permiten")
        void simultaneasMismaSalaConfigurada() {
            programada(1L, "Taller A", 9, 11, salaVirtual, luis, true);
            ActividadResponse r = service.crear(EVENTO, solicitud(9, 11, salaVirtual, ana, true, false));
            assertEquals("Sala virtual", r.salaNombre());
        }

        @Test
        @DisplayName("el mismo ponente nunca puede estar en dos actividades, aunque sean simultáneas")
        void ponenteNoSeDuplicaAunqueSimultaneas() {
            programada(1L, "Taller A", 9, 11, salaB, ana, true);

            ProgramacionActividadException e = assertThrows(ProgramacionActividadException.class,
                    () -> service.crear(EVENTO, solicitud(9, 11, auditorio, ana, true, false)));

            assertEquals(List.of(TipoConflicto.PONENTE), tipos(e));
        }
    }

    // ── Criterio 4 (reglas del servicio; los campos obligatorios se prueban en el controlador) ──

    @Nested
    @DisplayName("Criterio 4: datos obligatorios y coherentes")
    class Criterio4 {

        @Test
        @DisplayName("rechaza una hora de fin que no es posterior a la de inicio")
        void horaFinAntesDeInicio() {
            assertThrows(SolicitudInvalidaException.class,
                    () -> service.crear(EVENTO, solicitud(11, 11, auditorio, ana, false, false)));
        }

        @Test
        @DisplayName("rechaza una fecha fuera del rango del evento")
        void fechaFueraDelEvento() {
            ActividadRequest r = new ActividadRequest("X", 5L, DIA.plusDays(10), LocalTime.of(9, 0),
                    LocalTime.of(10, 0), auditorio.getId(), null, null, null, null, false, false);
            assertThrows(SolicitudInvalidaException.class, () -> service.crear(EVENTO, r));
        }

        @Test
        @DisplayName("rechaza una sala que no es del evento")
        void salaDeOtroEvento() {
            ActividadRequest r = new ActividadRequest("X", 5L, DIA, LocalTime.of(9, 0), LocalTime.of(10, 0),
                    777L, null, null, null, null, false, false);
            assertThrows(RecursoNoEncontradoException.class, () -> service.crear(EVENTO, r));
        }

        @Test
        @DisplayName("rechaza un ponente que no está asociado al evento")
        void ponenteNoAsociado() {
            when(participacionRepository.existsByEvento_IdAndPersona_Id(EVENTO, ana.getId())).thenReturn(false);
            assertThrows(SolicitudInvalidaException.class,
                    () -> service.crear(EVENTO, solicitud(9, 10, auditorio, ana, false, false)));
        }

        @Test
        @DisplayName("no permite programar en un evento cerrado")
        void eventoCerrado() {
            evento.setEstado(EstadoEvento.cerrado);
            assertThrows(OperacionNoPermitidaException.class,
                    () -> service.crear(EVENTO, solicitud(9, 10, auditorio, ana, false, false)));
        }
    }

    // ── Criterio 5 ──

    @Nested
    @DisplayName("Criterio 5: revalidar al modificar")
    class Criterio5 {

        @Test
        @DisplayName("mover una actividad a un horario ocupado genera conflicto y no se guarda")
        void moverAHorarioOcupado() {
            programada(1L, "Conferencia", 9, 11, auditorio, luis, false);
            Actividad propia = programada(2L, "Mi taller", 14, 15, auditorio, ana, false);
            when(actividadRepository.findByIdAndEvento_Id(2L, EVENTO)).thenReturn(Optional.of(propia));

            ProgramacionActividadException e = assertThrows(ProgramacionActividadException.class,
                    () -> service.actualizar(EVENTO, 2L, solicitud(10, 12, auditorio, ana, false, false)));

            assertEquals(List.of(TipoConflicto.SALA), tipos(e));
            assertEquals(LocalTime.of(14, 0), propia.getHoraInicio());
            verify(actividadRepository, never()).save(any());
        }

        @Test
        @DisplayName("la actividad no choca consigo misma al reprogramarla dentro de su propio horario")
        void noChocaConsigoMisma() {
            Actividad propia = programada(2L, "Mi taller", 9, 11, auditorio, ana, false);
            when(actividadRepository.findByIdAndEvento_Id(2L, EVENTO)).thenReturn(Optional.of(propia));

            ActividadResponse r = service.actualizar(EVENTO, 2L, solicitud(9, 10, auditorio, ana, false, false));

            assertEquals(LocalTime.of(10, 0), r.horaFin());
            verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ACTIVIDAD_REPROGRAMADA), eq("actividades"),
                    eq(2L), anyMap());
        }

        @Test
        @DisplayName("cambiar el ponente a uno ocupado se detecta al revalidar")
        void cambiarPonenteOcupado() {
            programada(1L, "Conferencia", 9, 11, salaB, luis, true);
            Actividad propia = programada(2L, "Mi taller", 9, 11, auditorio, ana, true);
            when(actividadRepository.findByIdAndEvento_Id(2L, EVENTO)).thenReturn(Optional.of(propia));

            ProgramacionActividadException e = assertThrows(ProgramacionActividadException.class,
                    () -> service.actualizar(EVENTO, 2L, solicitud(9, 11, auditorio, luis, true, false)));

            assertEquals(List.of(TipoConflicto.PONENTE), tipos(e));
        }
    }

    // ── Disponibilidad (HU-20): advertencias con confirmación ──

    @Nested
    @DisplayName("Advertencias de disponibilidad")
    class Advertencias {

        @Test
        @DisplayName("fuera de la disponibilidad de la sala: pide confirmación y no guarda")
        void fueraDeDisponibilidadPideConfirmar() {
            when(disponibilidadSalaRepository.findBySala_IdOrderByFechaAscHoraInicioAsc(auditorio.getId()))
                    .thenReturn(List.of(franjaSala(auditorio, 8, 12)));

            ProgramacionActividadException e = assertThrows(ProgramacionActividadException.class,
                    () -> service.crear(EVENTO, solicitud(14, 16, auditorio, ana, false, false)));

            assertEquals(ProgramacionActividadException.CONFIRMACION_REQUERIDA, e.getCodigo());
            assertEquals(TipoAdvertencia.FUERA_DISPONIBILIDAD_SALA, e.getAdvertencias().get(0).tipo());
            verify(actividadRepository, never()).save(any());
        }

        @Test
        @DisplayName("con la confirmación, guarda y devuelve las advertencias aceptadas")
        void confirmadaSeGuarda() {
            when(disponibilidadSalaRepository.findBySala_IdOrderByFechaAscHoraInicioAsc(auditorio.getId()))
                    .thenReturn(List.of(franjaSala(auditorio, 8, 12)));

            ActividadResponse r = service.crear(EVENTO, solicitud(14, 16, auditorio, ana, false, true));

            assertEquals(1, r.advertencias().size());
        }

        @Test
        @DisplayName("un ponente sin disponibilidad registrada genera advertencia")
        void ponenteSinDisponibilidad() {
            when(disponibilidadPonenteRepository.findByPersona_IdAndEvento_IdOrderByFechaAscHoraInicioAsc(ana.getId(), EVENTO))
                    .thenReturn(List.of());

            ValidacionActividadResponse v = service.validar(EVENTO, null, solicitud(9, 10, auditorio, ana, false, false));

            assertTrue(v.puedeGuardar());
            assertTrue(v.requiereConfirmacion());
            assertEquals(TipoAdvertencia.FUERA_DISPONIBILIDAD_PONENTE, v.advertencias().get(0).tipo());
        }

        @Test
        @DisplayName("los cruces tienen prioridad: con conflictos no basta con confirmar")
        void confirmarNoSaltaConflictos() {
            programada(1L, "Conferencia", 9, 11, auditorio, luis, false);

            ProgramacionActividadException e = assertThrows(ProgramacionActividadException.class,
                    () -> service.crear(EVENTO, solicitud(10, 12, auditorio, ana, false, true)));

            assertEquals(ProgramacionActividadException.CONFLICTO_PROGRAMACION, e.getCodigo());
        }
    }

    // ── HU-22: actividades por programar y avisos de cambios ──

    @Nested
    @DisplayName("HU-22: actividades por programar")
    class PorProgramar {

        private ActividadPorProgramarRequest porProgramar(int minutos, Persona p) {
            return new ActividadPorProgramarRequest("Ponencia IA", 5L, minutos, p != null ? p.getId() : null,
                    null, null, null, false);
        }

        private Actividad pendiente(Long id) {
            Actividad a = Actividad.builder().id(id).evento(evento).nombre("Pendiente").tipoActividad(taller)
                    .estado(Actividad.ESTADO_POR_PROGRAMAR).duracionMinutos(90).build();
            when(actividadRepository.findByIdAndEvento_Id(id, EVENTO)).thenReturn(Optional.of(a));
            return a;
        }

        @Test
        @DisplayName("Criterio 1: registra una actividad sin fecha, horario ni sala, con su duración")
        void registraPorProgramar() {
            ActividadResponse r = service.registrarPorProgramar(EVENTO, porProgramar(60, ana));

            assertEquals(Actividad.ESTADO_POR_PROGRAMAR, r.estado());
            assertNull(r.fecha());
            assertNull(r.horaInicio());
            assertNull(r.salaId());
            assertEquals(60, r.duracionMinutos());
            assertEquals("Ana Pérez", r.ponenteNombre());
            verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ACTIVIDAD_REGISTRADA_POR_PROGRAMAR),
                    eq("actividades"), eq(99L), anyMap());
        }

        @Test
        @DisplayName("valida el ponente igual que la programación manual")
        void validaPonente() {
            when(participacionRepository.existsByEvento_IdAndPersona_Id(EVENTO, ana.getId())).thenReturn(false);
            assertThrows(SolicitudInvalidaException.class,
                    () -> service.registrarPorProgramar(EVENTO, porProgramar(60, ana)));
            verify(actividadRepository, never()).save(any());
        }

        @Test
        @DisplayName("no modifica como por programar una actividad que ya tiene horario")
        void noModificaProgramada() {
            Actividad propia = programada(2L, "Mi taller", 9, 11, auditorio, ana, false);
            when(actividadRepository.findByIdAndEvento_Id(2L, EVENTO)).thenReturn(Optional.of(propia));
            assertThrows(OperacionNoPermitidaException.class,
                    () -> service.actualizarPorProgramar(EVENTO, 2L, porProgramar(60, null)));
        }

        @Test
        @DisplayName("Criterio 1 (ajuste manual): programarla a mano la deja programada y no automática")
        void programarAMano() {
            Actividad a = pendiente(3L);

            ActividadResponse r = service.actualizar(EVENTO, 3L, solicitud(14, 16, auditorio, ana, false, false));

            assertEquals(Actividad.ESTADO_PROGRAMADA, r.estado());
            assertEquals(120, r.duracionMinutos());
            assertFalse(r.asignacionAutomatica());
            assertEquals(LocalTime.of(14, 0), a.getHoraInicio());
            ArgumentCaptor<AvisoCambiosAgenda.Programacion> antes =
                    ArgumentCaptor.forClass(AvisoCambiosAgenda.Programacion.class);
            verify(avisoCambiosAgenda).actividadModificada(eq(a), antes.capture());
            assertFalse(antes.getValue().programada());
        }

        @Test
        @DisplayName("Criterio 1 (ajuste manual): un ajuste a mano quita la marca de asignación automática")
        void ajusteQuitaMarcaAutomatica() {
            Actividad propia = programada(2L, "Mi taller", 9, 11, auditorio, ana, false);
            propia.setAsignacionAutomatica(true);
            when(actividadRepository.findByIdAndEvento_Id(2L, EVENTO)).thenReturn(Optional.of(propia));

            service.actualizar(EVENTO, 2L, solicitud(9, 10, auditorio, ana, false, false));

            assertFalse(propia.isAsignacionAutomatica());
        }

        @Test
        @DisplayName("devolver a por programar quita fecha, horario y sala, conserva la duración y avisa")
        void desprogramar() {
            Actividad propia = programada(2L, "Mi taller", 9, 11, auditorio, ana, false);
            when(actividadRepository.findByIdAndEvento_Id(2L, EVENTO)).thenReturn(Optional.of(propia));

            ActividadResponse r = service.desprogramar(EVENTO, 2L);

            assertEquals(Actividad.ESTADO_POR_PROGRAMAR, r.estado());
            assertNull(propia.getFecha());
            assertNull(propia.getSala());
            assertEquals(120, propia.getDuracionMinutos());
            ArgumentCaptor<AvisoCambiosAgenda.Programacion> antes =
                    ArgumentCaptor.forClass(AvisoCambiosAgenda.Programacion.class);
            verify(avisoCambiosAgenda).actividadRetirada(eq(propia), antes.capture());
            assertEquals(DIA, antes.getValue().fecha());
            assertEquals(LocalTime.of(9, 0), antes.getValue().inicio());
            verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ACTIVIDAD_DESPROGRAMADA), eq("actividades"),
                    eq(2L), anyMap());
        }

        @Test
        @DisplayName("Criterio 5: modificar una actividad pasa su programación anterior al aviso de cambios")
        void modificarAvisa() {
            Actividad propia = programada(2L, "Mi taller", 9, 11, auditorio, ana, false);
            when(actividadRepository.findByIdAndEvento_Id(2L, EVENTO)).thenReturn(Optional.of(propia));

            service.actualizar(EVENTO, 2L, solicitud(14, 15, salaB, ana, false, false));

            ArgumentCaptor<AvisoCambiosAgenda.Programacion> antes =
                    ArgumentCaptor.forClass(AvisoCambiosAgenda.Programacion.class);
            verify(avisoCambiosAgenda).actividadModificada(eq(propia), antes.capture());
            assertEquals(LocalTime.of(9, 0), antes.getValue().inicio());
            assertEquals(auditorio.getId(), antes.getValue().salaId());
        }

        @Test
        @DisplayName("Criterio 5: al eliminar se avisa a los inscritos antes de borrar la actividad")
        void eliminarAvisaAntesDeBorrar() {
            Actividad propia = programada(2L, "Mi taller", 9, 11, auditorio, ana, false);
            when(actividadRepository.findByIdAndEvento_Id(2L, EVENTO)).thenReturn(Optional.of(propia));

            service.eliminar(EVENTO, 2L);

            InOrder orden = inOrder(avisoCambiosAgenda, actividadRepository);
            orden.verify(avisoCambiosAgenda).actividadRetirada(eq(propia), any(AvisoCambiosAgenda.Programacion.class));
            orden.verify(actividadRepository).delete(propia);
        }
    }

    // ── HU-19, Criterio 3: solo propuestas con versión definitiva ──

    @Nested
    @DisplayName("HU-19: propuestas disponibles para programar")
    class PropuestasDefinitivas {

        private Propuesta propuestaDelEvento(Long id) {
            Convocatoria convocatoria = Convocatoria.builder().id(3L).evento(evento).titulo("Convocatoria").build();
            Propuesta propuesta = Propuesta.builder().id(id).convocatoria(convocatoria).titulo("Robótica educativa").build();
            when(propuestaRepository.findById(id)).thenReturn(Optional.of(propuesta));
            return propuesta;
        }

        private ActividadRequest conPropuesta(Long propuestaId) {
            return new ActividadRequest("Ponencia", 5L, DIA, LocalTime.of(9, 0), LocalTime.of(10, 0),
                    auditorio.getId(), ana.getId(), propuestaId, null, null, false, false);
        }

        @Test
        @DisplayName("programa una actividad con una propuesta aprobada en versión definitiva")
        void conVersionDefinitiva_programa() {
            propuestaDelEvento(70L);
            when(versionPropuestaRepository.existsByPropuesta_IdAndEsDefinitivaTrue(70L)).thenReturn(true);

            ActividadResponse r = service.crear(EVENTO, conPropuesta(70L));

            assertEquals(99L, r.id());
        }

        @Test
        @DisplayName("rechaza vincular una propuesta sin versión definitiva")
        void sinVersionDefinitiva_rechaza() {
            propuestaDelEvento(71L);
            when(versionPropuestaRepository.existsByPropuesta_IdAndEsDefinitivaTrue(71L)).thenReturn(false);

            SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                    () -> service.crear(EVENTO, conPropuesta(71L)));

            assertTrue(e.getMessage().contains("versión definitiva"));
            verify(actividadRepository, never()).save(any());
        }

        @Test
        @DisplayName("al editar una actividad conserva la propuesta que ya tenía vinculada")
        void edicion_conservaPropuestaVinculada() {
            Propuesta propuesta = propuestaDelEvento(72L);
            Actividad existente = programada(5L, "Ponencia", 14, 15, auditorio, ana, false);
            existente.setPropuesta(propuesta);
            when(actividadRepository.findByIdAndEvento_Id(5L, EVENTO)).thenReturn(Optional.of(existente));

            ActividadResponse r = service.actualizar(EVENTO, 5L, conPropuesta(72L));

            assertEquals(5L, r.id());
            verify(versionPropuestaRepository, never()).existsByPropuesta_IdAndEsDefinitivaTrue(72L);
        }
    }
}
