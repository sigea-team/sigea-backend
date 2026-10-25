package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.conferencista.ActualizarConferencistaRequest;
import com.sigea.demosigea_backend.dto.conferencista.AsociarConferencistaRequest;
import com.sigea.demosigea_backend.dto.conferencista.CoincidenciaConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.CompletarInvitacionRequest;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistaRegistradoResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistaEventoResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistaPublicoResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistasPublicosEventoResponse;
import com.sigea.demosigea_backend.dto.conferencista.EstudioRequest;
import com.sigea.demosigea_backend.dto.conferencista.PerfilConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.PublicacionRequest;
import com.sigea.demosigea_backend.dto.conferencista.TrayectoriaRequest;
import com.sigea.demosigea_backend.exception.ConferencistaExistenteException;
import com.sigea.demosigea_backend.exception.ConferencistaYaAsociadoException;
import com.sigea.demosigea_backend.exception.DatosConferencistaInvalidosException;
import com.sigea.demosigea_backend.exception.DatosPersonaDuplicadosException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.EstudioConferencista;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.NivelEstudio;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.PerfilConferencista;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.model.TipoPublicacion;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PerfilConferencistaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConferencistaServiceTest {

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private PerfilConferencistaRepository perfilRepository;

    @Mock
    private ParticipacionConferencistaRepository participacionRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ConferencistaService service;

    private static final LocalDateTime ACTUALIZADO_ANTES = LocalDateTime.of(2026, 9, 1, 10, 0);

    // ---------------- Datos de prueba ----------------

    private Evento evento(Long id, String nombre, EstadoEvento estado) {
        return Evento.builder()
                .id(id).nombre(nombre)
                .fechaInicio(LocalDate.of(2026, 10, 20)).fechaFin(LocalDate.of(2026, 10, 22))
                .semestre("2026-2").estado(estado)
                .build();
    }

    private Evento evento(EstadoEvento estado) {
        return evento(1L, "Congreso de Ingeniería de Sistemas", estado);
    }

    private Persona persona() {
        return Persona.builder()
                .id(12L).tipoDocumento("CC").numeroDocumento("1090123456")
                .nombres("Ana María").apellidos("Pérez Gómez").correo("ana.perez@ufps.edu.co")
                .telefono("3001234567")
                .build();
    }

    private PerfilConferencista perfil(Persona persona) {
        PerfilConferencista perfil = PerfilConferencista.builder()
                .id(4L).persona(persona)
                .biografia("Ingeniera de sistemas con 15 años de experiencia.")
                .cargoActual("Profesora titular").institucion("UFPS")
                .fechaRegistro(LocalDateTime.of(2026, 8, 1, 9, 0))
                .fechaActualizacion(ACTUALIZADO_ANTES)
                .build();
        List<EstudioConferencista> estudios = new ArrayList<>();
        estudios.add(EstudioConferencista.builder().id(1L).nivel(NivelEstudio.doctorado)
                .titulo("Doctora en Ingeniería").institucion("Universidad Nacional").anio(2018).build());
        perfil.reemplazarEstudios(estudios);
        return perfil;
    }

    private TrayectoriaRequest trayectoria(String biografia) {
        return new TrayectoriaRequest(biografia, "Profesora titular", "UFPS", null,
                List.of(new EstudioRequest(NivelEstudio.doctorado, "Doctora en Ingeniería", "Universidad Nacional", 2018)),
                List.of());
    }

    private CompletarInvitacionRequest completar(String documento, TrayectoriaRequest trayectoria) {
        return new CompletarInvitacionRequest("cc", " " + documento + " ", " Ana María ", " Pérez Gómez ", " ",
                " Inteligencia artificial en la educación ", trayectoria);
    }

    private ActualizarConferencistaRequest actualizacion(String documento, String correo, TrayectoriaRequest trayectoria) {
        return new ActualizarConferencistaRequest("CC", documento, "Ana María", "Pérez Gómez", correo,
                "3001234567", trayectoria);
    }

    private ParticipacionConferencista participacion(Long id, Evento evento, Persona persona) {
        return ParticipacionConferencista.builder()
                .id(id).evento(evento).persona(persona)
                .tipoParticipacion(TipoParticipacionConferencista.conferencista)
                .tema("Inteligencia artificial en la educación")
                .fechaRegistro(LocalDateTime.of(2026, 9, 10, 8, 0))
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Map<String, ?>> captorDetalle() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }

    private void guardadosDevuelvenConId() {
        when(personaRepository.saveAndFlush(any(Persona.class))).thenAnswer(inv -> {
            Persona p = inv.getArgument(0);
            p.setId(30L);
            return p;
        });
        when(perfilRepository.save(any(PerfilConferencista.class))).thenAnswer(inv -> {
            PerfilConferencista p = inv.getArgument(0);
            p.setId(8L);
            return p;
        });
        when(participacionRepository.saveAndFlush(any(ParticipacionConferencista.class))).thenAnswer(inv -> {
            ParticipacionConferencista p = inv.getArgument(0);
            p.setId(7L);
            p.setFechaRegistro(LocalDateTime.of(2026, 10, 1, 9, 0));
            return p;
        });
    }

    // ---------------- Criterio 1: registro del conferencista desde su invitación ----------------

    @Test
    @DisplayName("Criterio 1: el invitado registra sus datos y trayectoria y queda asociado al evento y a su perfil")
    void registrarDesdeInvitacion_creaPersonaPerfilYParticipacion() {
        Evento evento = evento(EstadoEvento.habilitado);
        guardadosDevuelvenConId();
        TrayectoriaRequest trayectoria = new TrayectoriaRequest("  Ingeniera de sistemas con 15 años de experiencia.  ",
                " Profesora titular ", "UFPS", "https://orcid.org/0000-0002-1825-0097",
                List.of(new EstudioRequest(NivelEstudio.doctorado, " Doctora en Ingeniería ", "Universidad Nacional", 2018),
                        new EstudioRequest(NivelEstudio.maestria, "Magíster en Software", "UFPS", 2012)),
                List.of(new PublicacionRequest(TipoPublicacion.articulo, "Microservicios académicos",
                        "Revista Colombiana de Computación", 2024, "")));

        ParticipacionConferencista participacion = service.registrarDesdeInvitacion(evento, "ana.perez@ufps.edu.co",
                TipoParticipacionConferencista.conferencista, completar("1090123456", trayectoria));

        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).saveAndFlush(personaCaptor.capture());
        Persona guardada = personaCaptor.getValue();
        assertEquals("CC", guardada.getTipoDocumento());
        assertEquals("1090123456", guardada.getNumeroDocumento());
        assertEquals("Ana María", guardada.getNombres());
        assertEquals("ana.perez@ufps.edu.co", guardada.getCorreo());
        assertNull(guardada.getTelefono());

        ArgumentCaptor<PerfilConferencista> perfilCaptor = ArgumentCaptor.forClass(PerfilConferencista.class);
        verify(perfilRepository).save(perfilCaptor.capture());
        PerfilConferencista perfil = perfilCaptor.getValue();
        assertEquals(guardada, perfil.getPersona());
        assertEquals("Ingeniera de sistemas con 15 años de experiencia.", perfil.getBiografia());
        assertEquals(2, perfil.getEstudios().size());
        assertEquals("Doctora en Ingeniería", perfil.getEstudios().get(0).getTitulo());
        assertEquals(2, perfil.getEstudios().get(1).getOrden());
        assertNull(perfil.getPublicaciones().get(0).getEnlace());

        assertEquals(7L, participacion.getId());
        assertEquals(evento, participacion.getEvento());
        assertEquals("Inteligencia artificial en la educación", participacion.getTema());

        ArgumentCaptor<Map<String, ?>> detalle = captorDetalle();
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONFERENCISTA_REGISTRADO),
                eq(ConferencistaService.ENTIDAD_PARTICIPACION), eq(7L), detalle.capture());
        assertEquals("invitacion", detalle.getValue().get("medio"));
        assertEquals("Ana María Pérez Gómez", detalle.getValue().get("persona"));
    }

    @Test
    @DisplayName("Criterio 1: si el correo ya es de una persona (p. ej. un participante), se completan sus datos sin duplicarla")
    void registrarDesdeInvitacion_completaPersonaExistente() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        Persona participante = Persona.builder().id(12L).tipoDocumento("TI").numeroDocumento("1090123456")
                .nombres("Ana").apellidos("Pérez").correo("ana.perez@ufps.edu.co").build();
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@ufps.edu.co")).thenReturn(Optional.of(participante));
        when(personaRepository.findByNumeroDocumento("1090123456")).thenReturn(Optional.of(participante));
        when(personaRepository.saveAndFlush(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilRepository.save(any(PerfilConferencista.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participacionRepository.saveAndFlush(any(ParticipacionConferencista.class))).thenAnswer(inv -> {
            ParticipacionConferencista p = inv.getArgument(0);
            p.setId(9L);
            return p;
        });

        ParticipacionConferencista participacion = service.registrarDesdeInvitacion(evento, "ana.perez@ufps.edu.co",
                TipoParticipacionConferencista.conferencista, completar("1090123456", trayectoria("Bio del invitado.")));

        assertEquals(12L, participacion.getPersona().getId());
        assertEquals("CC", participante.getTipoDocumento());
        assertEquals("Ana María", participante.getNombres());
        verify(perfilRepository).save(any(PerfilConferencista.class));
    }

    @Test
    @DisplayName("Datos sensibles: si la persona ya tiene documento y teléfono y llegan vacíos, se conservan")
    void registrarDesdeInvitacion_documentoYTelefonoVacios_conservaLosRegistrados() {
        Evento evento = evento(EstadoEvento.habilitado);
        Persona participante = Persona.builder().id(12L).tipoDocumento("CC").numeroDocumento("1090123456")
                .telefono("3001234567").nombres("Ana").apellidos("Pérez").correo("ana.perez@ufps.edu.co").build();
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@ufps.edu.co")).thenReturn(Optional.of(participante));
        when(personaRepository.findByNumeroDocumento("1090123456")).thenReturn(Optional.of(participante));
        when(personaRepository.saveAndFlush(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilRepository.save(any(PerfilConferencista.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participacionRepository.saveAndFlush(any(ParticipacionConferencista.class))).thenAnswer(inv -> {
            ParticipacionConferencista p = inv.getArgument(0);
            p.setId(9L);
            return p;
        });

        service.registrarDesdeInvitacion(evento, "ana.perez@ufps.edu.co",
                TipoParticipacionConferencista.conferencista, completar("", trayectoria("Bio del invitado.")));

        assertEquals("1090123456", participante.getNumeroDocumento());
        assertEquals("3001234567", participante.getTelefono());
        assertEquals("Ana María", participante.getNombres());
    }

    @Test
    @DisplayName("Criterio 2: persona nueva sin documento → 400 y no se registra nada")
    void registrarDesdeInvitacion_personaNuevaSinDocumento_400() {
        DatosConferencistaInvalidosException ex = assertThrows(DatosConferencistaInvalidosException.class,
                () -> service.registrarDesdeInvitacion(evento(EstadoEvento.habilitado), "nuevo@ufps.edu.co",
                        TipoParticipacionConferencista.conferencista, completar("", trayectoria("Bio"))));
        assertTrue(ex.getCampos().containsKey("numeroDocumento"));
        verify(personaRepository, never()).saveAndFlush(any());
        verify(participacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 3: el documento ya es de otra persona con otro correo → 409 y no se crea un duplicado")
    void registrarDesdeInvitacion_documentoDeOtraPersona_409() {
        Persona otra = Persona.builder().id(40L).numeroDocumento("1090123456").correo("otra@ufps.edu.co")
                .nombres("Otra").apellidos("Persona").build();
        when(personaRepository.findByNumeroDocumento("1090123456")).thenReturn(Optional.of(otra));

        DatosPersonaDuplicadosException ex = assertThrows(DatosPersonaDuplicadosException.class,
                () -> service.registrarDesdeInvitacion(evento(EstadoEvento.habilitado), "ana.perez@ufps.edu.co",
                        TipoParticipacionConferencista.conferencista, completar("1090123456", trayectoria("Bio"))));
        assertEquals("numeroDocumento", ex.getCampo());
        verify(personaRepository, never()).saveAndFlush(any());
        verify(participacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 1: si ya estaba asociado al evento, no se crea otra participación")
    void registrarDesdeInvitacion_yaAsociado_devuelveLaExistente() {
        Evento evento = evento(EstadoEvento.habilitado);
        Persona persona = persona();
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@ufps.edu.co")).thenReturn(Optional.of(persona));
        when(personaRepository.findByNumeroDocumento("1090123456")).thenReturn(Optional.of(persona));
        when(personaRepository.saveAndFlush(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil(persona)));
        when(perfilRepository.save(any(PerfilConferencista.class))).thenAnswer(inv -> inv.getArgument(0));
        ParticipacionConferencista existente = participacion(7L, evento, persona);
        when(participacionRepository.historialDePersona(12L)).thenReturn(List.of(existente));

        ParticipacionConferencista resultado = service.registrarDesdeInvitacion(evento, "ana.perez@ufps.edu.co",
                TipoParticipacionConferencista.conferencista, completar("1090123456", trayectoria("Bio nueva.")));

        assertEquals(existente, resultado);
        verify(participacionRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest(name = "Criterio 1: con el evento {0} el invitado ya no puede registrarse")
    @EnumSource(value = EstadoEvento.class, names = {"en_ejecucion", "cerrado"})
    void registrarDesdeInvitacion_eventoNoModificable_409(EstadoEvento estado) {
        assertThrows(OperacionNoPermitidaException.class, () -> service.registrarDesdeInvitacion(evento(estado),
                "ana.perez@ufps.edu.co", TipoParticipacionConferencista.conferencista,
                completar("1090123456", trayectoria("Bio"))));
        verify(personaRepository, never()).saveAndFlush(any());
    }

    // ---------------- Criterio 2 ----------------

    @Test
    @DisplayName("Criterio 2: un año de estudio posterior al actual rechaza el registro sin guardar nada")
    void registrarDesdeInvitacion_anioFuturo_rechaza() {
        int futuro = Year.now().getValue() + 1;
        TrayectoriaRequest trayectoria = new TrayectoriaRequest("Biografía", null, null, null,
                List.of(new EstudioRequest(NivelEstudio.maestria, "Magíster", "UFPS", futuro)), null);

        DatosConferencistaInvalidosException ex = assertThrows(DatosConferencistaInvalidosException.class,
                () -> service.registrarDesdeInvitacion(evento(EstadoEvento.habilitado), "ana.perez@ufps.edu.co",
                        TipoParticipacionConferencista.conferencista, completar("1090123456", trayectoria)));

        assertTrue(ex.getCampos().containsKey("trayectoria.estudios[0].anio"));
        verify(personaRepository, never()).saveAndFlush(any());
        verify(perfilRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 2: un año de publicación posterior al actual también se rechaza")
    void validarTrayectoria_publicacionFutura() {
        int futuro = Year.now().getValue() + 3;
        TrayectoriaRequest trayectoria = new TrayectoriaRequest("Biografía", null, null, null, null,
                List.of(new PublicacionRequest(TipoPublicacion.libro, "Libro", null, futuro, null)));

        DatosConferencistaInvalidosException ex = assertThrows(DatosConferencistaInvalidosException.class,
                () -> service.validarTrayectoria(trayectoria));
        assertTrue(ex.getCampos().containsKey("trayectoria.publicaciones[0].anio"));
    }

    // ---------------- Criterio 3 ----------------

    @Test
    @DisplayName("Criterio 3: lista de conferencistas registrados con sus eventos y si ya están en el evento actual")
    void listarRegistrados_marcaEventosYAsociacion() {
        Persona persona = persona();
        when(perfilRepository.buscarRegistrados(eq("%pérez%"), any(Pageable.class))).thenReturn(List.of(perfil(persona)));
        when(participacionRepository.findByPersona_IdIn(List.of(12L))).thenReturn(List.of(
                participacion(7L, evento(EstadoEvento.habilitado), persona),
                participacion(8L, evento(2L, "Seminario de Software", EstadoEvento.cerrado), persona)));

        List<ConferencistaRegistradoResponse> lista = service.listarRegistrados("  Pérez ", 1L);

        assertEquals(1, lista.size());
        ConferencistaRegistradoResponse c = lista.get(0);
        assertEquals(12L, c.personaId());
        assertEquals("Ana María Pérez Gómez", c.nombreCompleto());
        assertEquals("Profesora titular", c.cargoActual());
        assertEquals(2L, c.eventosPrevios());
        assertEquals(Boolean.TRUE, c.yaAsociadoAlEvento());
    }

    @Test
    @DisplayName("Criterio 3: sin texto de búsqueda lista todos; sin resultados no consulta participaciones")
    void listarRegistrados_sinBusqueda() {
        when(perfilRepository.buscarRegistrados(eq("%"), any(Pageable.class))).thenReturn(List.of());

        assertTrue(service.listarRegistrados(" ", null).isEmpty());
        verify(participacionRepository, never()).findByPersona_IdIn(any());
    }

    @Test
    @DisplayName("Criterio 3: búsqueda previa por documento y correo de la misma persona")
    void buscarCoincidencia_documentoYCorreo() {
        Persona existente = persona();
        when(personaRepository.findByNumeroDocumento("1090123456")).thenReturn(Optional.of(existente));
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@ufps.edu.co")).thenReturn(Optional.of(existente));

        Optional<CoincidenciaConferencistaResponse> c =
                service.buscarCoincidencia(" 1090123456 ", "ANA.PEREZ@ufps.edu.co", null);

        assertTrue(c.isPresent());
        assertEquals(ConferencistaService.COINCIDE_AMBOS, c.get().coincidePor());
        assertNull(c.get().yaAsociadoAlEvento());
    }

    @Test
    @DisplayName("Criterio 3: búsqueda sin coincidencias → vacío; sin parámetros → 400")
    void buscarCoincidencia_sinResultadosOSinParametros() {
        assertTrue(service.buscarCoincidencia("555", null, 1L).isEmpty());
        assertThrows(SolicitudInvalidaException.class, () -> service.buscarCoincidencia(" ", null, 1L));
    }

    @Test
    @DisplayName("Criterio 3: asocia a la persona existente con su trayectoria, sin pedirla de nuevo")
    void asociar_personaConPerfil() {
        Persona existente = persona();
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(personaRepository.findById(12L)).thenReturn(Optional.of(existente));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil(existente)));
        when(participacionRepository.saveAndFlush(any(ParticipacionConferencista.class))).thenAnswer(inv -> {
            ParticipacionConferencista p = inv.getArgument(0);
            p.setId(9L);
            return p;
        });

        ConferencistaEventoResponse response = service.asociar(1L,
                new AsociarConferencistaRequest(12L, TipoParticipacionConferencista.ponente, "Tema", null));

        assertEquals(9L, response.participacionId());
        assertEquals(12L, response.personaId());
        assertEquals(TipoParticipacionConferencista.ponente, response.tipoParticipacion());
        assertTrue(response.tienePerfil());
        assertEquals("Profesora titular", response.cargoActual());
        verify(perfilRepository, never()).save(any());
        verify(personaRepository, never()).saveAndFlush(any());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONFERENCISTA_ASOCIADO),
                eq(ConferencistaService.ENTIDAD_PARTICIPACION), eq(9L), anyMap());
    }

    @Test
    @DisplayName("Criterio 3: persona sin trayectoria (p. ej. un participante) → se exige la biografía para asociarla")
    void asociar_personaSinPerfilNiTrayectoria_400() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.habilitado)));
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona()));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.empty());

        DatosConferencistaInvalidosException ex = assertThrows(DatosConferencistaInvalidosException.class,
                () -> service.asociar(1L, new AsociarConferencistaRequest(12L,
                        TipoParticipacionConferencista.conferencista, null, null)));
        assertTrue(ex.getCampos().containsKey("biografia"));
        verify(participacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 3: persona sin trayectoria que la envía al asociar → se crea su perfil")
    void asociar_personaSinPerfilConTrayectoria_creaPerfil() {
        Persona existente = persona();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.habilitado)));
        when(personaRepository.findById(12L)).thenReturn(Optional.of(existente));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.empty());
        when(perfilRepository.save(any(PerfilConferencista.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participacionRepository.saveAndFlush(any(ParticipacionConferencista.class))).thenAnswer(inv -> {
            ParticipacionConferencista p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        ConferencistaEventoResponse response = service.asociar(1L, new AsociarConferencistaRequest(12L,
                TipoParticipacionConferencista.conferencista, null, trayectoria("Investigadora en IA.")));

        ArgumentCaptor<PerfilConferencista> captor = ArgumentCaptor.forClass(PerfilConferencista.class);
        verify(perfilRepository).save(captor.capture());
        assertEquals(existente, captor.getValue().getPersona());
        assertEquals("Investigadora en IA.", captor.getValue().getBiografia());
        assertTrue(response.tienePerfil());
    }

    @Test
    @DisplayName("Criterio 3: la persona ya está en el evento → 409 sin crear otra asociación")
    void asociar_yaAsociado_409() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.habilitado)));
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona()));
        when(participacionRepository.existsByEvento_IdAndPersona_Id(1L, 12L)).thenReturn(true);

        assertThrows(ConferencistaYaAsociadoException.class, () -> service.asociar(1L,
                new AsociarConferencistaRequest(12L, TipoParticipacionConferencista.conferencista, null, null)));
        verify(participacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 3: dos asociaciones simultáneas → el índice único responde 409, no 500")
    void asociar_carreraDetectadaPorIndice_409() {
        Persona existente = persona();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.habilitado)));
        when(personaRepository.findById(12L)).thenReturn(Optional.of(existente));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil(existente)));
        ConstraintViolationException violacion = new ConstraintViolationException("duplicado",
                new SQLException("duplicate key", ConferencistaService.SQLSTATE_VIOLACION_UNICIDAD),
                ConferencistaService.INDICE_PARTICIPACION_UNICA);
        when(participacionRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup", violacion));

        assertThrows(ConferencistaYaAsociadoException.class, () -> service.asociar(1L,
                new AsociarConferencistaRequest(12L, TipoParticipacionConferencista.conferencista, null, null)));
    }

    @Test
    @DisplayName("Otras violaciones de integridad no se confunden con un conferencista duplicado")
    void esViolacionParticipacionUnica_otraRestriccion_false() {
        ConstraintViolationException otra = new ConstraintViolationException("fk",
                new SQLException("fk", "23503"), "participaciones_conferencista_persona_id_fkey");
        assertFalse(ConferencistaService.esViolacionParticipacionUnica(new DataIntegrityViolationException("x", otra)));
    }

    @Test
    @DisplayName("Retirar del evento conserva la persona y su trayectoria")
    void desasociar_eliminaSoloLaAsociacion() {
        Evento evento = evento(EstadoEvento.habilitado);
        ParticipacionConferencista p = participacion(7L, evento, persona());
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(participacionRepository.findByIdAndEvento_Id(7L, 1L)).thenReturn(Optional.of(p));

        service.desasociar(1L, 7L);

        verify(participacionRepository).delete(p);
        verify(perfilRepository, never()).delete(any());
        verify(personaRepository, never()).delete(any());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONFERENCISTA_DESASOCIADO),
                eq(ConferencistaService.ENTIDAD_PARTICIPACION), eq(7L), anyMap());
    }

    // ---------------- Criterio 4 ----------------

    @Test
    @DisplayName("Criterio 4: actualizar la trayectoria la refleja en los eventos publicados donde participa")
    void actualizar_trayectoria_reflejaEnEventosPublicos() {
        Persona persona = persona();
        PerfilConferencista perfil = perfil(persona);
        Evento publicado = evento(1L, "Congreso de Ingeniería de Sistemas", EstadoEvento.habilitado);
        Evento enConfiguracion = evento(2L, "Seminario de Software", EstadoEvento.en_configuracion);
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil));
        when(participacionRepository.historialDePersona(12L)).thenReturn(List.of(
                participacion(7L, publicado, persona), participacion(8L, enConfiguracion, persona)));
        when(personaRepository.saveAndFlush(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilRepository.save(any(PerfilConferencista.class))).thenAnswer(inv -> inv.getArgument(0));

        PerfilConferencistaResponse response = service.actualizar(12L, actualizacion("1090123456",
                "ana.perez@ufps.edu.co", trayectoria("Nueva biografía: directora del grupo de IA.")));

        assertEquals("Nueva biografía: directora del grupo de IA.", response.trayectoria().biografia());
        assertTrue(response.trayectoria().fechaActualizacion().isAfter(ACTUALIZADO_ANTES));
        assertEquals(2, response.participaciones().size());
        assertTrue(response.participaciones().get(0).visiblePublicamente());
        assertFalse(response.participaciones().get(1).visiblePublicamente());

        ArgumentCaptor<Map<String, ?>> detalle = captorDetalle();
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONFERENCISTA_ACTUALIZADO),
                eq(ConferencistaService.ENTIDAD_PERFIL), eq(4L), detalle.capture());
        assertEquals(List.of("trayectoria"), detalle.getValue().get("camposModificados"));
        assertEquals(List.of("Congreso de Ingeniería de Sistemas"), detalle.getValue().get("eventosPublicosAfectados"));
    }

    @Test
    @DisplayName("Criterio 4: guardar sin cambios no modifica la fecha pública ni audita")
    void actualizar_sinCambios_noGuarda() {
        Persona persona = persona();
        PerfilConferencista perfil = perfil(persona);
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil));
        when(participacionRepository.historialDePersona(12L)).thenReturn(List.of());

        PerfilConferencistaResponse response = service.actualizar(12L, actualizacion(" 1090123456 ",
                "ANA.PEREZ@ufps.edu.co", trayectoria("  Ingeniera de sistemas con 15 años de experiencia.  ")));

        assertEquals(ACTUALIZADO_ANTES, response.trayectoria().fechaActualizacion());
        assertEquals(1L, perfil.getEstudios().get(0).getId());
        verify(personaRepository, never()).saveAndFlush(any());
        verify(perfilRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), anyString(), anyLong(), anyMap());
    }

    @Test
    @DisplayName("Criterio 4: el documento nuevo pertenece a otra persona → 409 en el campo documento")
    void actualizar_documentoDeOtraPersona_409() {
        Persona persona = persona();
        Persona otra = Persona.builder().id(40L).numeroDocumento("88234567").nombres("Carlos").apellidos("Rojas").build();
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil(persona)));
        when(personaRepository.findByNumeroDocumento("88234567")).thenReturn(Optional.of(otra));

        DatosPersonaDuplicadosException ex = assertThrows(DatosPersonaDuplicadosException.class,
                () -> service.actualizar(12L, actualizacion("88234567", "ana.perez@ufps.edu.co", trayectoria("Bio"))));
        assertEquals("numeroDocumento", ex.getCampo());
        verify(personaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 4: el correo de una persona con cuenta de usuario no se cambia desde este módulo")
    void actualizar_correoConCuentaDeUsuario_400() {
        Persona persona = persona();
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil(persona)));
        when(usuarioRepository.existsByPersona_Id(12L)).thenReturn(true);

        DatosConferencistaInvalidosException ex = assertThrows(DatosConferencistaInvalidosException.class,
                () -> service.actualizar(12L, actualizacion("1090123456", "otro@ufps.edu.co", trayectoria("Bio"))));
        assertTrue(ex.getCampos().containsKey("correo"));
    }

    @Test
    @DisplayName("Criterio 4: actualizar a una persona que no es conferencista → 404")
    void actualizar_sinPerfil_404() {
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona()));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(12L, actualizacion("1090123456", "ana.perez@ufps.edu.co", trayectoria("Bio"))));
    }

    @Test
    @DisplayName("Criterio 4: la vista pública lee la trayectoria vigente, sin datos de contacto")
    void listarPublico_trayectoriaVigente() {
        Persona persona = persona();
        PerfilConferencista perfil = perfil(persona);
        perfil.setBiografia("Biografía actualizada por el organizador.");
        Evento evento = evento(EstadoEvento.en_ejecucion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(participacionRepository.findByEvento_IdOrderByFechaRegistroAscIdAsc(1L))
                .thenReturn(List.of(participacion(7L, evento, persona)));
        when(perfilRepository.findByPersona_IdIn(List.of(12L))).thenReturn(List.of(perfil));

        ConferencistasPublicosEventoResponse response = service.listarPublico(1L);

        assertEquals("Congreso de Ingeniería de Sistemas", response.eventoNombre());
        assertEquals(1, response.conferencistas().size());
        ConferencistaPublicoResponse c = response.conferencistas().get(0);
        assertEquals("Ana María Pérez Gómez", c.nombreCompleto());
        assertEquals("Biografía actualizada por el organizador.", c.biografia());
        assertEquals(1, c.estudios().size());
        assertNotNull(c.fechaActualizacion());
    }

    @Test
    @DisplayName("Criterio 4: un evento en configuración todavía no tiene vista pública → 404")
    void listarPublico_eventoNoPublicado_404() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        assertThrows(RecursoNoEncontradoException.class, () -> service.listarPublico(1L));
    }

    @Test
    @DisplayName("RF17: el perfil incluye el historial de participaciones en los eventos")
    void obtenerPerfil_incluyeHistorial() {
        Persona persona = persona();
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil(persona)));
        when(participacionRepository.historialDePersona(12L)).thenReturn(List.of(
                participacion(7L, evento(EstadoEvento.cerrado), persona)));

        PerfilConferencistaResponse response = service.obtenerPerfil(12L);

        assertEquals("Ana María Pérez Gómez", response.nombreCompleto());
        assertEquals(1, response.participaciones().size());
        assertTrue(response.participaciones().get(0).visiblePublicamente());
        assertFalse(response.tieneCuentaUsuario());
    }

    @Test
    @DisplayName("RF17: una persona sin trayectoria ni participaciones no es conferencista → 404")
    void obtenerPerfil_noConferencista_404() {
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona()));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.empty());
        when(participacionRepository.historialDePersona(12L)).thenReturn(List.of());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPerfil(12L));
    }

    // ---------------- Perfil de ingreso "Conferencista" ----------------

    @Test
    @DisplayName("Mi perfil: el conferencista de la sesión ve su trayectoria e historial")
    void obtenerMiPerfil_devuelvePerfilDelUsuario() {
        Persona persona = persona();
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@ufps.edu.co")).thenReturn(Optional.of(persona));
        when(perfilRepository.existsByPersona_Id(12L)).thenReturn(true);
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil(persona)));
        when(participacionRepository.historialDePersona(12L)).thenReturn(List.of(
                participacion(7L, evento(EstadoEvento.habilitado), persona)));

        PerfilConferencistaResponse response = service.obtenerMiPerfil("ana.perez@ufps.edu.co");

        assertEquals(12L, response.personaId());
        assertEquals("Ingeniera de sistemas con 15 años de experiencia.", response.trayectoria().biografia());
        assertEquals(1, response.participaciones().size());
    }

    @Test
    @DisplayName("Mi perfil: un usuario que el organizador no ha registrado como conferencista → 404 con indicaciones")
    void obtenerMiPerfil_sinRegistroDeConferencista_404() {
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@ufps.edu.co")).thenReturn(Optional.of(persona()));
        when(perfilRepository.existsByPersona_Id(12L)).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerMiPerfil("ana.perez@ufps.edu.co"));
        assertTrue(ex.getMessage().contains("organizador"));
    }

    @Test
    @DisplayName("Mi perfil: el conferencista actualiza su trayectoria y se refleja en la vista pública")
    void actualizarMiPerfil_actualizaSuPropiaTrayectoria() {
        Persona persona = persona();
        PerfilConferencista perfil = perfil(persona);
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@ufps.edu.co")).thenReturn(Optional.of(persona));
        when(perfilRepository.existsByPersona_Id(12L)).thenReturn(true);
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil));
        when(participacionRepository.historialDePersona(12L)).thenReturn(List.of(
                participacion(7L, evento(EstadoEvento.habilitado), persona)));
        when(personaRepository.saveAndFlush(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilRepository.save(any(PerfilConferencista.class))).thenAnswer(inv -> inv.getArgument(0));

        PerfilConferencistaResponse response = service.actualizarMiPerfil("ana.perez@ufps.edu.co",
                actualizacion("1090123456", "ana.perez@ufps.edu.co", trayectoria("Biografía escrita por ella misma.")));

        assertEquals("Biografía escrita por ella misma.", response.trayectoria().biografia());
        assertTrue(response.participaciones().get(0).visiblePublicamente());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONFERENCISTA_ACTUALIZADO),
                eq(ConferencistaService.ENTIDAD_PERFIL), eq(4L), anyMap());
    }

    @Test
    @DisplayName("Mi perfil: el conferencista no puede cambiar su correo (es su usuario de inicio de sesión)")
    void actualizarMiPerfil_noCambiaCorreo() {
        Persona persona = persona();
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@ufps.edu.co")).thenReturn(Optional.of(persona));
        when(perfilRepository.existsByPersona_Id(12L)).thenReturn(true);
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona));
        when(perfilRepository.findByPersona_Id(12L)).thenReturn(Optional.of(perfil(persona)));
        when(usuarioRepository.existsByPersona_Id(12L)).thenReturn(true);

        assertThrows(DatosConferencistaInvalidosException.class, () -> service.actualizarMiPerfil("ana.perez@ufps.edu.co",
                actualizacion("1090123456", "otro.correo@ufps.edu.co", trayectoria("Bio"))));
        verify(personaRepository, never()).saveAndFlush(any());
    }
}
