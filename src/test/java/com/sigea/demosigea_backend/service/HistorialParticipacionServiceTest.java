package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.historial.EventoHistorialResponse;
import com.sigea.demosigea_backend.dto.historial.HistorialParticipacionResponse;
import com.sigea.demosigea_backend.dto.historial.RolParticipacion;
import com.sigea.demosigea_backend.dto.periodo.FiltroPeriodo;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.InscripcionActividad;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.PropuestaAutor;
import com.sigea.demosigea_backend.model.RolAutor;
import com.sigea.demosigea_backend.model.TipoActividad;
import com.sigea.demosigea_backend.model.TipoEvento;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.InscripcionActividadRepository;
import com.sigea.demosigea_backend.repository.InscripcionEventoRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.PropuestaAutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HistorialParticipacionServiceTest {

    @Mock private PersonaRepository personaRepository;
    @Mock private InscripcionEventoRepository inscripcionEventoRepository;
    @Mock private InscripcionActividadRepository inscripcionActividadRepository;
    @Mock private ParticipacionConferencistaRepository participacionRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private PropuestaAutorRepository propuestaAutorRepository;

    @InjectMocks
    private HistorialParticipacionService service;

    private Persona persona;
    private Evento congreso2025;
    private Evento seminario2026;

    @BeforeEach
    void setUp() {
        persona = Persona.builder().id(7L).nombres("Ana").apellidos("Pérez")
                .numeroDocumento("1090").correo("ana@ufps.edu.co").build();
        congreso2025 = evento(1L, "Congreso", "2025-2", LocalDate.of(2025, 9, 10), 1L, "Congreso");
        seminario2026 = evento(2L, "Seminario", "2026-1", LocalDate.of(2026, 4, 5), 2L, "Seminario");
        when(personaRepository.findById(7L)).thenReturn(Optional.of(persona));
        when(inscripcionEventoRepository.findByPersona_IdOrderByFechaInscripcionDesc(anyLong())).thenReturn(List.of());
        when(inscripcionActividadRepository.findHistorialDePersona(anyLong())).thenReturn(List.of());
        when(participacionRepository.historialDePersona(anyLong())).thenReturn(List.of());
        when(actividadRepository.findComoPonente(anyLong())).thenReturn(List.of());
        when(propuestaAutorRepository.findHistorialDePersona(anyLong())).thenReturn(List.of());
    }

    private static Evento evento(Long id, String nombre, String semestre, LocalDate inicio, Long tipoId, String tipo) {
        return Evento.builder().id(id).nombre(nombre).semestre(semestre).fechaInicio(inicio).fechaFin(inicio.plusDays(2))
                .tipoEvento(TipoEvento.builder().id(tipoId).nombre(tipo).build()).build();
    }

    private static Actividad actividad(Long id, Evento evento, String tipo, LocalDate fecha) {
        return Actividad.builder().id(id).evento(evento).nombre("Act " + id)
                .tipoActividad(TipoActividad.builder().id(id * 10).nombre(tipo).evento(evento).build())
                .fecha(fecha).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(10, 0)).build();
    }

    private void cargarParticipacionesVariadas() {
        InscripcionEvento inscripcion = InscripcionEvento.builder().id(1L).persona(persona).evento(congreso2025).build();
        when(inscripcionEventoRepository.findByPersona_IdOrderByFechaInscripcionDesc(7L)).thenReturn(List.of(inscripcion));
        Actividad taller = actividad(11L, congreso2025, "Taller", LocalDate.of(2025, 9, 11));
        when(inscripcionActividadRepository.findHistorialDePersona(7L)).thenReturn(List.of(
                InscripcionActividad.builder().id(1L).inscripcion(inscripcion).actividad(taller).build()));

        when(participacionRepository.historialDePersona(7L)).thenReturn(List.of(
                ParticipacionConferencista.builder().id(1L).persona(persona).evento(seminario2026)
                        .tipoParticipacion(TipoParticipacionConferencista.conferencista).tema("IA").build()));
        Actividad conferencia = actividad(21L, seminario2026, "Conferencia", LocalDate.of(2026, 4, 5));
        when(actividadRepository.findComoPonente(7L)).thenReturn(List.of(conferencia));

        Propuesta propuesta = Propuesta.builder().id(5L).titulo("Mi ponencia").estado(EstadoPropuesta.aprobada)
                .convocatoria(Convocatoria.builder().id(3L).evento(congreso2025).build()).build();
        when(propuestaAutorRepository.findHistorialDePersona(7L)).thenReturn(List.of(
                PropuestaAutor.builder().propuesta(propuesta).persona(persona).rol(RolAutor.ponente).build()));
    }

    @Test
    @DisplayName("Criterio 1: muestra eventos, actividades y roles, del más reciente al más antiguo")
    void consultar_conParticipaciones_muestraEventosActividadesYRoles() {
        cargarParticipacionesVariadas();

        HistorialParticipacionResponse r = service.consultar(7L, null);

        assertTrue(r.tieneHistorial());
        assertFalse(r.primeraParticipacion());
        assertNull(r.mensaje());
        assertEquals("Ana Pérez", r.nombreCompleto());
        assertEquals(2, r.totalEventos());

        EventoHistorialResponse seminario = r.eventos().get(0);
        assertEquals(2L, seminario.eventoId());
        assertEquals(Set.of(RolParticipacion.CONFERENCISTA), seminario.roles());
        assertEquals("IA", seminario.temaConferencia());
        assertEquals(RolParticipacion.PONENTE, seminario.actividades().get(0).rol());

        EventoHistorialResponse congreso = r.eventos().get(1);
        assertEquals(Set.of(RolParticipacion.PARTICIPANTE, RolParticipacion.PONENTE), congreso.roles());
        assertEquals(1, congreso.actividades().size());
        assertEquals("Taller", congreso.actividades().get(0).tipoActividad());
        assertEquals(1, congreso.propuestas().size());

        assertEquals(1L, r.eventosPorRol().get(RolParticipacion.CONFERENCISTA));
        assertEquals(1L, r.eventosPorRol().get(RolParticipacion.PARTICIPANTE));
        assertEquals(0L, r.eventosPorRol().get(RolParticipacion.AUTOR));
    }

    @Test
    @DisplayName("Criterio 1: autor de propuesta no aprobada figura como AUTOR, no como PONENTE")
    void consultar_propuestaNoAprobada_rolAutor() {
        Propuesta propuesta = Propuesta.builder().id(5L).titulo("En evaluación").estado(EstadoPropuesta.en_evaluacion)
                .convocatoria(Convocatoria.builder().id(3L).evento(congreso2025).build()).build();
        when(propuestaAutorRepository.findHistorialDePersona(7L)).thenReturn(List.of(
                PropuestaAutor.builder().propuesta(propuesta).persona(persona).rol(RolAutor.ponente).build()));

        HistorialParticipacionResponse r = service.consultar(7L, null);

        assertEquals(Set.of(RolParticipacion.AUTOR), r.eventos().get(0).roles());
    }

    @Test
    @DisplayName("Criterio 2: sin participación registrada informa que sería su primera participación")
    void consultar_sinParticipacion_informaPrimeraParticipacion() {
        HistorialParticipacionResponse r = service.consultar(7L, null);

        assertFalse(r.tieneHistorial());
        assertTrue(r.primeraParticipacion());
        assertEquals(HistorialParticipacionService.MENSAJE_PRIMERA_PARTICIPACION, r.mensaje());
        assertTrue(r.eventos().isEmpty());
    }

    @Test
    @DisplayName("Criterio 3: filtra por semestre")
    void consultar_filtroSemestre() {
        cargarParticipacionesVariadas();

        HistorialParticipacionResponse r = service.consultar(7L,
                new FiltroPeriodo("2025-2", null, null, null, null));

        assertEquals(1, r.totalEventos());
        assertEquals(1L, r.eventos().get(0).eventoId());
    }

    @Test
    @DisplayName("Criterio 3: filtra por tipo de evento y por rango de fechas")
    void consultar_filtroTipoEventoYRango() {
        cargarParticipacionesVariadas();

        assertEquals(List.of(2L), service.consultar(7L, new FiltroPeriodo(null, null, null, 2L, null))
                .eventos().stream().map(EventoHistorialResponse::eventoId).toList());
        assertEquals(List.of(1L), service.consultar(7L,
                        new FiltroPeriodo(null, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), null, null))
                .eventos().stream().map(EventoHistorialResponse::eventoId).toList());
    }

    @Test
    @DisplayName("Criterio 3: filtra por tipo de actividad sin distinguir mayúsculas")
    void consultar_filtroTipoActividad() {
        cargarParticipacionesVariadas();

        HistorialParticipacionResponse r = service.consultar(7L, new FiltroPeriodo(null, null, null, null, " taller "));

        assertEquals(1, r.totalEventos());
        assertEquals(1L, r.eventos().get(0).eventoId());
        assertEquals(1, r.eventos().get(0).actividades().size());
    }

    @Test
    @DisplayName("Criterio 3: con historial pero sin coincidencias informa que no hay participaciones para los filtros")
    void consultar_sinCoincidencias() {
        cargarParticipacionesVariadas();

        HistorialParticipacionResponse r = service.consultar(7L, new FiltroPeriodo("2020-1", null, null, null, null));

        assertFalse(r.tieneHistorial());
        assertFalse(r.primeraParticipacion());
        assertEquals(HistorialParticipacionService.MENSAJE_SIN_COINCIDENCIAS, r.mensaje());
    }

    @Test
    @DisplayName("Semestre con formato inválido o rango invertido → SolicitudInvalidaException")
    void consultar_filtrosInvalidos() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.consultar(7L, new FiltroPeriodo("2026-3", null, null, null, null)));
        assertThrows(SolicitudInvalidaException.class, () -> service.consultar(7L,
                new FiltroPeriodo(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 1, 1), null, null)));
    }

    @Test
    @DisplayName("Persona inexistente → RecursoNoEncontradoException")
    void consultar_personaInexistente() {
        when(personaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.consultar(99L, null));
    }

    @Test
    @DisplayName("Busca a la persona por documento y, si no hay, por correo")
    void consultarPorIdentificacion() {
        when(personaRepository.findByNumeroDocumento("1090")).thenReturn(Optional.of(persona));
        when(personaRepository.findByCorreoIgnoreCase("ana@ufps.edu.co")).thenReturn(Optional.of(persona));

        assertEquals(7L, service.consultarPorIdentificacion(" 1090 ", null, null).personaId());
        assertEquals(7L, service.consultarPorIdentificacion(null, "ana@ufps.edu.co", null).personaId());
        assertThrows(SolicitudInvalidaException.class, () -> service.consultarPorIdentificacion(" ", null, null));
        verify(personaRepository, never()).findById(anyLong());
    }
}
