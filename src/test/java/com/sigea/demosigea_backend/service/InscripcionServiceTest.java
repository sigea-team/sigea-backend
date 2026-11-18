package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.inscripcion.CupoActividadResponse;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionActividadResponse;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionEventoRequest;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionEventoResponse;
import com.sigea.demosigea_backend.dto.inscripcion.MiInscripcionResponse;
import com.sigea.demosigea_backend.exception.ConflictoHorarioInscripcionException;
import com.sigea.demosigea_backend.exception.CupoAgotadoException;
import com.sigea.demosigea_backend.exception.InscripcionDuplicadaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.CategoriaParticipante;
import com.sigea.demosigea_backend.model.CodigoQr;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.InscripcionActividad;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Sala;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoSala;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.AsistenciaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.InscripcionActividadRepository;
import com.sigea.demosigea_backend.repository.InscripcionEventoRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link InscripcionService} cubriendo los criterios de aceptación de HU-23.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InscripcionServiceTest {

    @Mock private InscripcionEventoRepository inscripcionEventoRepository;
    @Mock private InscripcionActividadRepository inscripcionActividadRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CodigoQrService codigoQrService;
    @Mock private AsistenciaRepository asistenciaRepository;

    @InjectMocks
    private InscripcionService inscripcionService;

    private Evento eventoHabilitado;
    private Persona personaParticipante;
    private Usuario usuarioParticipante;
    private Sala salaAforo2;
    private Actividad actividad1;
    private Actividad actividad2Traslapada;
    private Actividad actividad3NoTraslapada;
    private InscripcionEvento inscripcionEventoExistente;

    @BeforeEach
    void setUp() {
        eventoHabilitado = Evento.builder()
                .id(1L)
                .nombre("Semana de Ingeniería")
                .estado(EstadoEvento.habilitado)
                .fechaInicio(LocalDate.now().plusDays(2))
                .fechaFin(LocalDate.now().plusDays(5))
                .build();

        personaParticipante = Persona.builder()
                .id(10L)
                .nombres("Carlos")
                .apellidos("Gómez")
                .correo("carlos.gomez@ufps.edu.co")
                .build();

        usuarioParticipante = Usuario.builder()
                .id(100L)
                .persona(personaParticipante)
                .build();

        salaAforo2 = Sala.builder()
                .id(50L)
                .nombre("Aula 101")
                .tipo(TipoSala.fisica)
                .aforoMaximo(2)
                .build();

        actividad1 = Actividad.builder()
                .id(201L)
                .evento(eventoHabilitado)
                .nombre("Taller de Spring Boot")
                .fecha(LocalDate.now().plusDays(3))
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(11, 0))
                .sala(salaAforo2)
                .estado(Actividad.ESTADO_PROGRAMADA)
                .build();

        actividad2Traslapada = Actividad.builder()
                .id(202L)
                .evento(eventoHabilitado)
                .nombre("Conferencia de Ciberseguridad")
                .fecha(LocalDate.now().plusDays(3))
                .horaInicio(LocalTime.of(10, 0)) // Cruza con actividad1 (9:00 - 11:00)
                .horaFin(LocalTime.of(12, 0))
                .sala(salaAforo2)
                .estado(Actividad.ESTADO_PROGRAMADA)
                .build();

        actividad3NoTraslapada = Actividad.builder()
                .id(203L)
                .evento(eventoHabilitado)
                .nombre("Panel de Emprendimiento")
                .fecha(LocalDate.now().plusDays(3))
                .horaInicio(LocalTime.of(14, 0)) // Sin cruce
                .horaFin(LocalTime.of(16, 0))
                .sala(salaAforo2)
                .estado(Actividad.ESTADO_PROGRAMADA)
                .build();

        inscripcionEventoExistente = InscripcionEvento.builder()
                .id(501L)
                .evento(eventoHabilitado)
                .persona(personaParticipante)
                .categoriaParticipante(CategoriaParticipante.estudiante)
                .fechaInscripcion(LocalDateTime.now())
                .build();

        // Autenticación por defecto como participante
        autenticarComo("carlos.gomez@ufps.edu.co", "ROLE_PARTICIPANTE");
        when(usuarioRepository.findByPersona_CorreoIgnoreCase("carlos.gomez@ufps.edu.co"))
                .thenReturn(Optional.of(usuarioParticipante));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoHabilitado));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String correo, String rol) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                correo, "n/a", List.of(new SimpleGrantedAuthority(rol)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 1: Inscripción al evento
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Criterio 1: Inscripción al evento")
    class Criterio1InscripcionEvento {

        @Test
        @DisplayName("Registra exitosamente la inscripción con la categoría seleccionada")
        void inscribirEventoExitoso() {
            InscripcionEventoRequest request = new InscripcionEventoRequest(CategoriaParticipante.estudiante, null);
            when(inscripcionEventoRepository.existsByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(false);
            when(inscripcionEventoRepository.save(any(InscripcionEvento.class)))
                    .thenAnswer(inv -> {
                        InscripcionEvento e = inv.getArgument(0);
                        e.setId(501L);
                        return e;
                    });

            InscripcionEventoResponse resp = inscripcionService.inscribirAEvento(1L, request);

            assertNotNull(resp);
            assertEquals(501L, resp.inscripcionId());
            assertEquals(1L, resp.eventoId());
            assertEquals(10L, resp.personaId());
            assertEquals(CategoriaParticipante.estudiante, resp.categoria());
            verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.INSCRIPCION_EVENTO_REGISTRADA),
                    eq("inscripciones_evento"), eq(501L), any());
        }

        @Test
        @DisplayName("Rechaza inscripción si el evento se encuentra en configuración")
        void rechazaEventoEnConfiguracion() {
            eventoHabilitado.setEstado(EstadoEvento.en_configuracion);
            InscripcionEventoRequest request = new InscripcionEventoRequest(CategoriaParticipante.docente, null);

            OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                    () -> inscripcionService.inscribirAEvento(1L, request));
            assertTrue(ex.getMessage().contains("configuración"));
            verify(inscripcionEventoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rechaza inscripción si el evento está cerrado")
        void rechazaEventoCerrado() {
            eventoHabilitado.setEstado(EstadoEvento.cerrado);
            InscripcionEventoRequest request = new InscripcionEventoRequest(CategoriaParticipante.particular, null);

            OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                    () -> inscripcionService.inscribirAEvento(1L, request));
            assertTrue(ex.getMessage().contains("concluido"));
        }

        @Test
        @DisplayName("Rechaza inscripción si la fecha de finalización del evento ya expiró")
        void rechazaEventoExpirado() {
            eventoHabilitado.setFechaFin(LocalDate.now().minusDays(1));
            InscripcionEventoRequest request = new InscripcionEventoRequest(CategoriaParticipante.egresado, null);

            OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                    () -> inscripcionService.inscribirAEvento(1L, request));
            assertTrue(ex.getMessage().contains("expiró"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 2: Inscripción puntual a actividad con aforo
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Criterio 2: Inscripción a actividad y control de aforo")
    class Criterio2InscripcionActividad {

        @Test
        @DisplayName("Registra inscripción puntual si hay cupo disponible")
        void inscribirActividadConCupo() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            when(actividadRepository.findByIdAndEvento_IdParaInscripcion(201L, 1L)).thenReturn(Optional.of(actividad1));
            when(inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(501L, 201L)).thenReturn(false);
            when(inscripcionActividadRepository.countByActividad_Id(201L)).thenReturn(1L); // Aforo es 2, hay 1 libre
            when(inscripcionActividadRepository.findActivasPorPersonaYFecha(10L, actividad1.getFecha()))
                    .thenReturn(Collections.emptyList());
            when(inscripcionActividadRepository.save(any(InscripcionActividad.class)))
                    .thenAnswer(inv -> {
                        InscripcionActividad ia = inv.getArgument(0);
                        ia.setId(701L);
                        return ia;
                    });

            InscripcionActividadResponse resp = inscripcionService.inscribirAActividad(1L, 201L);

            assertNotNull(resp);
            assertEquals(701L, resp.inscripcionActividadId());
            assertEquals(201L, resp.actividadId());
            verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.INSCRIPCION_ACTIVIDAD_REGISTRADA),
                    eq("inscripciones_actividad"), eq(701L), any());
        }

        @Test
        @DisplayName("Rechaza inscripción si el participante no está inscrito previamente al evento")
        void rechazaSinInscripcionAlEvento() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(Optional.empty());

            OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                    () -> inscripcionService.inscribirAActividad(1L, 201L));
            assertTrue(ex.getMessage().contains("inscrito al evento"));
        }

        @Test
        @DisplayName("Cierra automáticamente la inscripción cuando se alcanza el aforo máximo de la sala")
        void cierraInscripcionPorAforoAlcanzado() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            when(actividadRepository.findByIdAndEvento_IdParaInscripcion(201L, 1L)).thenReturn(Optional.of(actividad1));
            when(inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(501L, 201L)).thenReturn(false);
            when(inscripcionActividadRepository.countByActividad_Id(201L)).thenReturn(2L); // Aforo es 2 -> lleno

            CupoAgotadoException ex = assertThrows(CupoAgotadoException.class,
                    () -> inscripcionService.inscribirAActividad(1L, 201L));
            assertTrue(ex.getMessage().contains("cupo máximo"));
            verify(inscripcionActividadRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 3: Horario traslapado
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Criterio 3: Conflicto de horario traslapado")
    class Criterio3ConflictoHorario {

        @Test
        @DisplayName("Advierte conflicto e impide inscripción si hay traslape con otra actividad inscrita")
        void bloqueaInscripcionPorHorarioTraslapado() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            when(actividadRepository.findByIdAndEvento_IdParaInscripcion(202L, 1L)).thenReturn(Optional.of(actividad2Traslapada));
            when(inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(501L, 202L)).thenReturn(false);
            when(inscripcionActividadRepository.countByActividad_Id(202L)).thenReturn(0L);

            // El usuario ya tiene inscrita la actividad1 (09:00 - 11:00) en la misma fecha
            InscripcionActividad inscripcionActividad1 = InscripcionActividad.builder()
                    .id(701L)
                    .actividad(actividad1)
                    .inscripcion(inscripcionEventoExistente)
                    .build();

            when(inscripcionActividadRepository.findActivasPorPersonaYFecha(10L, actividad2Traslapada.getFecha()))
                    .thenReturn(List.of(inscripcionActividad1));

            ConflictoHorarioInscripcionException ex = assertThrows(ConflictoHorarioInscripcionException.class,
                    () -> inscripcionService.inscribirAActividad(1L, 202L));

            assertTrue(ex.getMessage().contains("Conflicto de horario"));
            assertTrue(ex.getMessage().contains("Taller de Spring Boot"));
            assertTrue(ex.getMessage().contains("cancelar la inscripción anterior"));
            verify(inscripcionActividadRepository, never()).save(any());
        }

        @Test
        @DisplayName("Permite inscripción si la nueva actividad tiene marcado permiteSimultaneidad=true aunque haya traslape")
        void permiteInscripcionCuandoNuevaActividadPermiteSimultaneidad() {
            actividad2Traslapada.setPermiteSimultaneidad(true);

            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            when(actividadRepository.findByIdAndEvento_IdParaInscripcion(202L, 1L)).thenReturn(Optional.of(actividad2Traslapada));
            when(inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(501L, 202L)).thenReturn(false);
            when(inscripcionActividadRepository.countByActividad_Id(202L)).thenReturn(0L);

            InscripcionActividad inscripcionActividad1 = InscripcionActividad.builder()
                    .id(701L)
                    .actividad(actividad1)
                    .inscripcion(inscripcionEventoExistente)
                    .build();

            when(inscripcionActividadRepository.findActivasPorPersonaYFecha(10L, actividad2Traslapada.getFecha()))
                    .thenReturn(List.of(inscripcionActividad1));
            when(inscripcionActividadRepository.save(any(InscripcionActividad.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InscripcionActividadResponse resp = inscripcionService.inscribirAActividad(1L, 202L);
            assertNotNull(resp);
            assertEquals(202L, resp.actividadId());
        }

        @Test
        @DisplayName("Permite inscripción si la actividad ya inscrita tiene marcado permiteSimultaneidad=true")
        void permiteInscripcionCuandoActividadExistentePermiteSimultaneidad() {
            actividad1.setPermiteSimultaneidad(true);

            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            when(actividadRepository.findByIdAndEvento_IdParaInscripcion(202L, 1L)).thenReturn(Optional.of(actividad2Traslapada));
            when(inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(501L, 202L)).thenReturn(false);
            when(inscripcionActividadRepository.countByActividad_Id(202L)).thenReturn(0L);

            InscripcionActividad inscripcionActividad1 = InscripcionActividad.builder()
                    .id(701L)
                    .actividad(actividad1)
                    .inscripcion(inscripcionEventoExistente)
                    .build();

            when(inscripcionActividadRepository.findActivasPorPersonaYFecha(10L, actividad2Traslapada.getFecha()))
                    .thenReturn(List.of(inscripcionActividad1));
            when(inscripcionActividadRepository.save(any(InscripcionActividad.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InscripcionActividadResponse resp = inscripcionService.inscribirAActividad(1L, 202L);
            assertNotNull(resp);
            assertEquals(202L, resp.actividadId());
        }

        @Test
        @DisplayName("Permite inscripción a actividades consecutivas o sin traslape de horario")
        void permiteInscripcionSinTraslape() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            when(actividadRepository.findByIdAndEvento_IdParaInscripcion(203L, 1L)).thenReturn(Optional.of(actividad3NoTraslapada));
            when(inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(501L, 203L)).thenReturn(false);
            when(inscripcionActividadRepository.countByActividad_Id(203L)).thenReturn(0L);

            // Actividad1 es de 09:00 a 11:00; Actividad3 es de 14:00 a 16:00
            InscripcionActividad inscripcionActividad1 = InscripcionActividad.builder()
                    .id(701L)
                    .actividad(actividad1)
                    .inscripcion(inscripcionEventoExistente)
                    .build();

            when(inscripcionActividadRepository.findActivasPorPersonaYFecha(10L, actividad3NoTraslapada.getFecha()))
                    .thenReturn(List.of(inscripcionActividad1));
            when(inscripcionActividadRepository.save(any(InscripcionActividad.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            InscripcionActividadResponse resp = inscripcionService.inscribirAActividad(1L, 203L);
            assertNotNull(resp);
            assertEquals(203L, resp.actividadId());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 4: Duplicados e información de motivo
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Criterio 4: Rechazo de duplicados y cupo máximo")
    class Criterio4DuplicadosYMotivo {

        @Test
        @DisplayName("Rechaza inscripción al evento si ya está previamente inscrito")
        void rechazaInscripcionDuplicadaEvento() {
            InscripcionEventoRequest request = new InscripcionEventoRequest(CategoriaParticipante.estudiante, null);
            when(inscripcionEventoRepository.existsByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(true);

            InscripcionDuplicadaException ex = assertThrows(InscripcionDuplicadaException.class,
                    () -> inscripcionService.inscribirAEvento(1L, request));
            assertTrue(ex.getMessage().contains("previamente"));
            verify(inscripcionEventoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rechaza inscripción a la actividad si ya está previamente inscrito")
        void rechazaInscripcionDuplicadaActividad() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            when(actividadRepository.findByIdAndEvento_IdParaInscripcion(201L, 1L)).thenReturn(Optional.of(actividad1));
            when(inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(501L, 201L)).thenReturn(true);

            InscripcionDuplicadaException ex = assertThrows(InscripcionDuplicadaException.class,
                    () -> inscripcionService.inscribirAActividad(1L, 201L));
            assertTrue(ex.getMessage().contains("previamente"));
            verify(inscripcionActividadRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 5: Cancelación y liberación de cupo
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Criterio 5: Cancelación y liberación de cupo")
    class Criterio5CancelacionYCupo {

        @Test
        @DisplayName("Cancela inscripción a actividad, libera el cupo y registra auditoría")
        void cancelaInscripcionActividadLiberaCupo() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));

            InscripcionActividad ia = InscripcionActividad.builder()
                    .id(701L)
                    .inscripcion(inscripcionEventoExistente)
                    .actividad(actividad1)
                    .build();

            when(inscripcionActividadRepository.findByInscripcion_IdAndActividad_Id(501L, 201L))
                    .thenReturn(Optional.of(ia));

            inscripcionService.cancelarInscripcionActividad(1L, 201L);

            verify(inscripcionActividadRepository).delete(ia);
            verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.INSCRIPCION_ACTIVIDAD_CANCELADA),
                    eq("inscripciones_actividad"), eq(701L), any());
        }

        @Test
        @DisplayName("Consulta de cupo refleja liberación de cupos en tiempo real")
        void consultaCupoReflejaDisponibilidad() {
            when(actividadRepository.findByIdAndEvento_Id(201L, 1L)).thenReturn(Optional.of(actividad1));
            when(inscripcionActividadRepository.countByActividad_Id(201L)).thenReturn(1L);
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            when(inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(501L, 201L)).thenReturn(false);

            CupoActividadResponse cupo = inscripcionService.consultarCupo(1L, 201L);

            assertNotNull(cupo);
            assertEquals(2, cupo.aforoMaximo());
            assertEquals(1L, cupo.inscritos());
            assertEquals(1L, cupo.cuposDisponibles());
            assertFalse(cupo.cupoAgotado());
            assertFalse(cupo.estaInscrito());
        }

        @Test
        @DisplayName("Consulta mi inscripción retorna actividades y datos del participante")
        void consultaMiInscripcion() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));

            InscripcionActividad ia = InscripcionActividad.builder()
                    .id(701L)
                    .inscripcion(inscripcionEventoExistente)
                    .actividad(actividad1)
                    .fechaInscripcion(LocalDateTime.now())
                    .build();

            when(inscripcionActividadRepository.findByInscripcion_Id(501L)).thenReturn(List.of(ia));

            MiInscripcionResponse resp = inscripcionService.obtenerMiInscripcion(1L);

            assertNotNull(resp);
            assertEquals(501L, resp.inscripcionId());
            assertEquals(1, resp.actividades().size());
            assertEquals("Taller de Spring Boot", resp.actividades().get(0).actividadNombre());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // HU-24: código QR al inscribirse y protección de la asistencia registrada
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("HU-24: código QR y asistencias")
    class Hu24CodigoQr {

        @Test
        @DisplayName("Criterio 1: al confirmar la inscripción se genera el código QR y se devuelve en la respuesta")
        void inscribirGeneraCodigoQr() {
            InscripcionEventoRequest request = new InscripcionEventoRequest(CategoriaParticipante.estudiante, null);
            when(inscripcionEventoRepository.existsByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(false);
            when(inscripcionEventoRepository.save(any(InscripcionEvento.class)))
                    .thenAnswer(inv -> {
                        InscripcionEvento e = inv.getArgument(0);
                        e.setId(501L);
                        return e;
                    });
            when(codigoQrService.generarAlConfirmarInscripcion(any(InscripcionEvento.class)))
                    .thenReturn("SIGEA-0123456789abcdef0123456789abcdef");

            InscripcionEventoResponse resp = inscripcionService.inscribirAEvento(1L, request);

            assertEquals("SIGEA-0123456789abcdef0123456789abcdef", resp.codigoQr());
            verify(codigoQrService).generarAlConfirmarInscripcion(any(InscripcionEvento.class));
        }

        @Test
        @DisplayName("Criterio 1: si la inscripción está duplicada no se genera código QR")
        void inscripcionDuplicadaNoGeneraQr() {
            InscripcionEventoRequest request = new InscripcionEventoRequest(CategoriaParticipante.estudiante, null);
            when(inscripcionEventoRepository.existsByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(true);

            assertThrows(InscripcionDuplicadaException.class, () -> inscripcionService.inscribirAEvento(1L, request));
            verify(codigoQrService, never()).generarAlConfirmarInscripcion(any());
        }

        @Test
        @DisplayName("Mi inscripción incluye el código QR de la inscripción")
        void miInscripcionIncluyeCodigoQr() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            when(inscripcionActividadRepository.findByInscripcion_Id(501L)).thenReturn(List.of());
            when(codigoQrService.obtenerOCrear(inscripcionEventoExistente)).thenReturn(CodigoQr.builder()
                    .id(9L).inscripcion(inscripcionEventoExistente).codigo("SIGEA-abc").build());

            MiInscripcionResponse resp = inscripcionService.obtenerMiInscripcion(1L);

            assertEquals("SIGEA-abc", resp.codigoQr());
        }

        @Test
        @DisplayName("No se cancela la inscripción a una actividad con asistencia registrada")
        void noCancelaActividadConAsistencia() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            InscripcionActividad ia = InscripcionActividad.builder()
                    .id(701L).inscripcion(inscripcionEventoExistente).actividad(actividad1).build();
            when(inscripcionActividadRepository.findByInscripcion_IdAndActividad_Id(501L, 201L))
                    .thenReturn(Optional.of(ia));
            when(asistenciaRepository.existsByInscripcion_IdAndActividad_Id(501L, 201L)).thenReturn(true);

            OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                    () -> inscripcionService.cancelarInscripcionActividad(1L, 201L));
            assertTrue(ex.getMessage().contains("asistencia"));
            verify(inscripcionActividadRepository, never()).delete(any());
        }

        @Test
        @DisplayName("No se cancela la inscripción al evento si tiene asistencias registradas")
        void noCancelaEventoConAsistencia() {
            when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                    .thenReturn(Optional.of(inscripcionEventoExistente));
            when(asistenciaRepository.existsByInscripcion_Id(501L)).thenReturn(true);

            assertThrows(OperacionNoPermitidaException.class, () -> inscripcionService.cancelarMiInscripcionEvento(1L));
            verify(inscripcionEventoRepository, never()).delete(any());
        }
    }
}
