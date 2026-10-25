package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.conferencista.CoincidenciaConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.CompletarInvitacionRequest;
import com.sigea.demosigea_backend.dto.conferencista.EstudioRequest;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionCompletadaResponse;
import com.sigea.demosigea_backend.dto.conferencista.DatosRegistradosResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionPublicaResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitarConferencistaRequest;
import com.sigea.demosigea_backend.dto.conferencista.TrayectoriaRequest;
import com.sigea.demosigea_backend.exception.ConferencistaExistenteException;
import com.sigea.demosigea_backend.exception.ConferencistaYaAsociadoException;
import com.sigea.demosigea_backend.exception.InvitacionNoDisponibleException;
import com.sigea.demosigea_backend.exception.InvitacionPendienteException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.EstadoInvitacion;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.InvitacionConferencista;
import com.sigea.demosigea_backend.model.NivelEstudio;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.repository.InvitacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PerfilConferencistaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
class InvitacionConferencistaServiceTest {

    @Mock
    private InvitacionConferencistaRepository invitacionRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private PerfilConferencistaRepository perfilRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConferencistaService conferencistaService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private InvitacionConferencistaService service;

    private static final String ENLACE = "http://localhost:5173/#/conferencista/invitacion?token=abc";

    private Evento evento(EstadoEvento estado) {
        return Evento.builder()
                .id(1L).nombre("Congreso de Ingeniería de Sistemas")
                .fechaInicio(LocalDate.of(2026, 10, 20)).fechaFin(LocalDate.of(2026, 10, 22))
                .semestre("2026-2").estado(estado)
                .build();
    }

    private InvitacionConferencista invitacion(Evento evento, EstadoInvitacion estado, LocalDateTime expira) {
        return InvitacionConferencista.builder()
                .id(4L).evento(evento).nombre("Ana María Pérez").correo("ana.perez@unal.edu.co")
                .tipoParticipacion(TipoParticipacionConferencista.conferencista)
                .token("token-viejo").estado(estado)
                .fechaEnvio(expira.minusHours(72)).fechaExpiracion(expira)
                .build();
    }

    private CoincidenciaConferencistaResponse coincidencia(boolean tienePerfil, boolean yaAsociado) {
        return new CoincidenciaConferencistaResponse(12L, "Ana María Pérez Gómez", "CC", "1090123456",
                "ana.perez@unal.edu.co", "correo", tienePerfil, yaAsociado, 1L);
    }

    private CompletarInvitacionRequest datos() {
        return new CompletarInvitacionRequest("CC", "1090123456", "Ana María", "Pérez Gómez", "3001234567",
                "IA en la educación", new TrayectoriaRequest("Ingeniera de sistemas.", null, null, null,
                List.of(new EstudioRequest(NivelEstudio.doctorado, "Doctora", "UNAL", 2018)), List.of()));
    }

    /** Invitar guarda con saveAndFlush para detectar duplicados simultáneos. */
    private void guardarConFlushDevuelveConId() {
        when(invitacionRepository.saveAndFlush(any(InvitacionConferencista.class))).thenAnswer(inv -> {
            InvitacionConferencista i = inv.getArgument(0);
            if (i.getId() == null) {
                i.setId(4L);
            }
            return i;
        });
    }

    private void guardarDevuelveConId() {
        when(invitacionRepository.save(any(InvitacionConferencista.class))).thenAnswer(inv -> {
            InvitacionConferencista i = inv.getArgument(0);
            if (i.getId() == null) {
                i.setId(4L);
            }
            return i;
        });
    }

    // ---------------- Invitar (administrador) ----------------

    @Test
    @DisplayName("Invitar: con solo nombre y correo se guarda la invitación y se envía el enlace por correo")
    void invitar_nuevoCorreo_guardaYEnvia() {
        Evento evento = evento(EstadoEvento.habilitado);
        when(conferencistaService.buscarEvento(1L)).thenReturn(evento);
        guardarConFlushDevuelveConId();
        when(emailService.enlaceInvitacionConferencista(anyString())).thenReturn(ENLACE);

        InvitacionResponse response = service.invitar(1L,
                new InvitarConferencistaRequest("  Ana   María Pérez ", " Ana.Perez@UNAL.edu.co "));

        ArgumentCaptor<InvitacionConferencista> captor = ArgumentCaptor.forClass(InvitacionConferencista.class);
        verify(invitacionRepository).saveAndFlush(captor.capture());
        InvitacionConferencista guardada = captor.getValue();
        assertEquals("Ana María Pérez", guardada.getNombre());
        assertEquals("ana.perez@unal.edu.co", guardada.getCorreo());
        assertEquals(EstadoInvitacion.pendiente, guardada.getEstado());
        assertEquals(43, guardada.getToken().length());
        assertEquals(Duration.ofHours(72), Duration.between(guardada.getFechaEnvio(), guardada.getFechaExpiracion()));

        verify(conferencistaService).validarAsociacionModificable(evento);
        verify(emailService).enviarInvitacionConferencista(eq("ana.perez@unal.edu.co"), eq("Ana María Pérez"),
                eq("Congreso de Ingeniería de Sistemas"), eq(ENLACE), eq(guardada.getFechaExpiracion()));
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONFERENCISTA_INVITADO),
                eq(InvitacionConferencistaService.ENTIDAD_INVITACION), eq(4L), anyMap());
        assertEquals("pendiente", response.estado());
        assertEquals(ENLACE, response.enlace());
    }

    @Test
    @DisplayName("Criterio 3: el correo ya es de un conferencista registrado → 409 con la persona para vincularla")
    void invitar_conferencistaRegistrado_409() {
        when(conferencistaService.buscarEvento(1L)).thenReturn(evento(EstadoEvento.habilitado));
        when(conferencistaService.coincidencia(null, "ana.perez@unal.edu.co", 1L))
                .thenReturn(Optional.of(coincidencia(true, false)));

        ConferencistaExistenteException ex = assertThrows(ConferencistaExistenteException.class,
                () -> service.invitar(1L, new InvitarConferencistaRequest("Ana", "ana.perez@unal.edu.co")));
        assertEquals(12L, ex.getCoincidencia().personaId());
        verify(invitacionRepository, never()).saveAndFlush(any());
        verify(emailService, never()).enviarInvitacionConferencista(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Criterio 3: la persona ya está en el evento → 409 sin enviar invitación")
    void invitar_yaAsociado_409() {
        when(conferencistaService.buscarEvento(1L)).thenReturn(evento(EstadoEvento.habilitado));
        when(conferencistaService.coincidencia(null, "ana.perez@unal.edu.co", 1L))
                .thenReturn(Optional.of(coincidencia(true, true)));

        assertThrows(ConferencistaYaAsociadoException.class,
                () -> service.invitar(1L, new InvitarConferencistaRequest("Ana", "ana.perez@unal.edu.co")));
        verify(invitacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Invitar: una persona registrada sin trayectoria (p. ej. un participante) sí recibe la invitación")
    void invitar_personaSinTrayectoria_enviaInvitacion() {
        when(conferencistaService.buscarEvento(1L)).thenReturn(evento(EstadoEvento.habilitado));
        when(conferencistaService.coincidencia(null, "ana.perez@unal.edu.co", 1L))
                .thenReturn(Optional.of(coincidencia(false, false)));
        guardarConFlushDevuelveConId();
        when(emailService.enlaceInvitacionConferencista(anyString())).thenReturn(ENLACE);

        InvitacionResponse response = service.invitar(1L, new InvitarConferencistaRequest("Ana", "ana.perez@unal.edu.co"));

        assertEquals(4L, response.id());
        verify(emailService).enviarInvitacionConferencista(eq("ana.perez@unal.edu.co"), eq("Ana"), any(), eq(ENLACE), any());
    }

    @Test
    @DisplayName("Invitar: ya hay una invitación vigente para ese correo → 409 INVITACION_PENDIENTE")
    void invitar_invitacionVigente_409() {
        Evento evento = evento(EstadoEvento.habilitado);
        when(conferencistaService.buscarEvento(1L)).thenReturn(evento);
        when(invitacionRepository.findFirstByEvento_IdAndCorreoIgnoreCaseAndEstado(1L, "ana.perez@unal.edu.co",
                EstadoInvitacion.pendiente)).thenReturn(Optional.of(
                invitacion(evento, EstadoInvitacion.pendiente, LocalDateTime.now().plusDays(2))));

        assertThrows(InvitacionPendienteException.class,
                () -> service.invitar(1L, new InvitarConferencistaRequest("Ana", "ana.perez@unal.edu.co")));
        verify(invitacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Invitar: si la invitación anterior venció, se renueva con un enlace nuevo")
    void invitar_invitacionVencida_seRenueva() {
        Evento evento = evento(EstadoEvento.habilitado);
        InvitacionConferencista vencida = invitacion(evento, EstadoInvitacion.pendiente, LocalDateTime.now().minusHours(1));
        when(conferencistaService.buscarEvento(1L)).thenReturn(evento);
        when(invitacionRepository.findFirstByEvento_IdAndCorreoIgnoreCaseAndEstado(1L, "ana.perez@unal.edu.co",
                EstadoInvitacion.pendiente)).thenReturn(Optional.of(vencida));
        guardarConFlushDevuelveConId();
        when(emailService.enlaceInvitacionConferencista(anyString())).thenReturn(ENLACE);

        InvitacionResponse response = service.invitar(1L, new InvitarConferencistaRequest("Ana Pérez", "ana.perez@unal.edu.co"));

        assertEquals(4L, response.id());
        assertEquals("pendiente", response.estado());
        assertNotEquals("token-viejo", vencida.getToken());
        assertTrue(vencida.getFechaExpiracion().isAfter(LocalDateTime.now()));
        assertEquals("Ana Pérez", vencida.getNombre());
    }

        @Test
    @DisplayName("Invitar: dos administradores invitan el mismo correo a la vez → el índice único lo frena y se responde 409, no 500")
    void invitar_carreraDetectadaPorIndice_409() {
        when(conferencistaService.buscarEvento(1L)).thenReturn(evento(EstadoEvento.habilitado));
        when(invitacionRepository.saveAndFlush(any(InvitacionConferencista.class))).thenThrow(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \""
                        + InvitacionConferencistaService.INDICE_INVITACION_PENDIENTE + "\""));

        assertThrows(InvitacionPendienteException.class,
                () -> service.invitar(1L, new InvitarConferencistaRequest("Ana", "ana.perez@unal.edu.co")));
        verify(emailService, never()).enviarInvitacionConferencista(any(), any(), any(), any(), any());
        verify(auditoriaService, never()).registrar(any(), any(), any(), anyMap());
    }

    // ---------------- Reenviar y cancelar ----------------

    @Test
    @DisplayName("Reenviar: genera un enlace nuevo con 3 días más y lo envía otra vez")
    void reenviar_generaEnlaceNuevo() {
        Evento evento = evento(EstadoEvento.habilitado);
        InvitacionConferencista inv = invitacion(evento, EstadoInvitacion.pendiente, LocalDateTime.now().minusDays(1));
        when(invitacionRepository.findByIdAndEvento_Id(4L, 1L)).thenReturn(Optional.of(inv));
        guardarDevuelveConId();
        when(emailService.enlaceInvitacionConferencista(anyString())).thenReturn(ENLACE);

        InvitacionResponse response = service.reenviar(1L, 4L);

        assertNotEquals("token-viejo", inv.getToken());
        assertEquals("pendiente", response.estado());
        verify(emailService).enviarInvitacionConferencista(eq("ana.perez@unal.edu.co"), any(), any(), eq(ENLACE), any());
    }

    @Test
    @DisplayName("Reenviar o cancelar una invitación ya completada → 409")
    void reenviar_noPendiente_409() {
        InvitacionConferencista inv = invitacion(evento(EstadoEvento.habilitado), EstadoInvitacion.completada,
                LocalDateTime.now().plusDays(1));
        when(invitacionRepository.findByIdAndEvento_Id(4L, 1L)).thenReturn(Optional.of(inv));

        assertThrows(OperacionNoPermitidaException.class, () -> service.reenviar(1L, 4L));
        assertThrows(OperacionNoPermitidaException.class, () -> service.cancelar(1L, 4L));
    }

    @Test
    @DisplayName("Cancelar: la invitación queda cancelada y se audita")
    void cancelar_marcaCancelada() {
        InvitacionConferencista inv = invitacion(evento(EstadoEvento.habilitado), EstadoInvitacion.pendiente,
                LocalDateTime.now().plusDays(1));
        when(invitacionRepository.findByIdAndEvento_Id(4L, 1L)).thenReturn(Optional.of(inv));

        service.cancelar(1L, 4L);

        assertEquals(EstadoInvitacion.cancelada, inv.getEstado());
        verify(invitacionRepository).save(inv);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.INVITACION_CONFERENCISTA_CANCELADA),
                eq(InvitacionConferencistaService.ENTIDAD_INVITACION), eq(4L), anyMap());
    }

    @Test
    @DisplayName("Listar: muestra las pendientes y marca las vencidas")
    void listarPendientes_marcaVencidas() {
        Evento evento = evento(EstadoEvento.habilitado);
        InvitacionConferencista vigente = invitacion(evento, EstadoInvitacion.pendiente, LocalDateTime.now().plusDays(1));
        InvitacionConferencista vencida = invitacion(evento, EstadoInvitacion.pendiente, LocalDateTime.now().minusDays(1));
        vencida.setId(5L);
        when(invitacionRepository.findByEvento_IdAndEstadoOrderByFechaEnvioDescIdDesc(1L, EstadoInvitacion.pendiente))
                .thenReturn(List.of(vigente, vencida));
        when(emailService.enlaceInvitacionConferencista(anyString())).thenReturn(ENLACE);

        List<InvitacionResponse> lista = service.listarPendientes(1L);

        assertEquals("pendiente", lista.get(0).estado());
        assertEquals("vencida", lista.get(1).estado());
        verify(conferencistaService).buscarEvento(1L);
    }

    // ---------------- Enlace del conferencista (público) ----------------

    @Test
    @DisplayName("Consultar: invitación vigente con los datos que SIGEA ya tiene, sin exponer documento ni teléfono")
    void consultar_vigente_conDatosRegistrados() {
        when(invitacionRepository.findByToken("abc")).thenReturn(Optional.of(
                invitacion(evento(EstadoEvento.habilitado), EstadoInvitacion.pendiente, LocalDateTime.now().plusDays(2))));
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@unal.edu.co")).thenReturn(Optional.of(Persona.builder()
                .id(12L).tipoDocumento("CC").numeroDocumento("1090123456").nombres("Ana María").apellidos("Pérez")
                .telefono("3001234567").correo("ana.perez@unal.edu.co").build()));

        InvitacionPublicaResponse response = service.consultar("abc");

        assertEquals("Congreso de Ingeniería de Sistemas", response.eventoNombre());
        assertEquals("an•••@unal.edu.co", response.correo());
        assertNotNull(response.datosRegistrados());
        assertEquals("CC", response.datosRegistrados().tipoDocumento());
        assertEquals("••••••456", response.datosRegistrados().documentoEnmascarado());
        assertEquals("••••••567", response.datosRegistrados().telefonoEnmascarado());
        assertNull(response.datosRegistrados().trayectoria());
        String json = response.toString();
        assertTrue(!json.contains("1090123456") && !json.contains("3001234567") && !json.contains("ana.perez@"),
                "El enlace público no debe devolver el correo, el documento ni el teléfono completos");
    }

    @Test
    @DisplayName("Enmascarar: solo quedan visibles los 3 últimos caracteres")
    void enmascarar_ocultaElDato() {
        assertEquals("••••••456", DatosRegistradosResponse.enmascarar(" 1090123456 "));
        assertEquals("••••567", DatosRegistradosResponse.enmascarar("1234567"));
        assertEquals("••", DatosRegistradosResponse.enmascarar("12"));
        assertNull(DatosRegistradosResponse.enmascarar("  "));
        assertNull(DatosRegistradosResponse.enmascarar(null));
    }

    @Test
    @DisplayName("Enmascarar correo: se reconoce el dominio, pero no el usuario completo")
    void enmascararCorreo_ocultaElUsuario() {
        assertEquals("an•••@unal.edu.co", DatosRegistradosResponse.enmascararCorreo(" ana.perez@unal.edu.co "));
        assertEquals("j•••@ufps.edu.co", DatosRegistradosResponse.enmascararCorreo("jo@ufps.edu.co"));
        assertEquals("•••@x.co", DatosRegistradosResponse.enmascararCorreo("a@x.co"));
        assertNull(DatosRegistradosResponse.enmascararCorreo(" "));
    }

    @Test
    @DisplayName("Consultar: enlace vencido → 410 INVITACION_VENCIDA")
    void consultar_vencida_410() {
        when(invitacionRepository.findByToken("abc")).thenReturn(Optional.of(
                invitacion(evento(EstadoEvento.habilitado), EstadoInvitacion.pendiente, LocalDateTime.now().minusMinutes(1))));

        InvitacionNoDisponibleException ex = assertThrows(InvitacionNoDisponibleException.class, () -> service.consultar("abc"));
        assertEquals(InvitacionNoDisponibleException.VENCIDA, ex.getCodigo());
    }

    @Test
    @DisplayName("Consultar: enlace ya usado o cancelado → 410 con su código")
    void consultar_completadaOCancelada_410() {
        Evento evento = evento(EstadoEvento.habilitado);
        when(invitacionRepository.findByToken("usado")).thenReturn(Optional.of(
                invitacion(evento, EstadoInvitacion.completada, LocalDateTime.now().plusDays(1))));
        when(invitacionRepository.findByToken("cancelado")).thenReturn(Optional.of(
                invitacion(evento, EstadoInvitacion.cancelada, LocalDateTime.now().plusDays(1))));

        assertEquals(InvitacionNoDisponibleException.COMPLETADA,
                assertThrows(InvitacionNoDisponibleException.class, () -> service.consultar("usado")).getCodigo());
        assertEquals(InvitacionNoDisponibleException.CANCELADA,
                assertThrows(InvitacionNoDisponibleException.class, () -> service.consultar("cancelado")).getCodigo());
    }

    @Test
    @DisplayName("Consultar: el evento ya no recibe conferencistas → 410 INVITACION_EVENTO_CERRADO")
    void consultar_eventoCerrado_410() {
        when(invitacionRepository.findByToken("abc")).thenReturn(Optional.of(
                invitacion(evento(EstadoEvento.en_ejecucion), EstadoInvitacion.pendiente, LocalDateTime.now().plusDays(1))));

        InvitacionNoDisponibleException ex = assertThrows(InvitacionNoDisponibleException.class, () -> service.consultar("abc"));
        assertEquals(InvitacionNoDisponibleException.EVENTO_CERRADO, ex.getCodigo());
    }

    @Test
    @DisplayName("Consultar: enlace inexistente → 404")
    void consultar_tokenInexistente_404() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.consultar("no-existe"));
        assertThrows(RecursoNoEncontradoException.class, () -> service.consultar(" "));
    }

    @Test
    @DisplayName("Criterio 1: al completar, el conferencista queda registrado y vinculado, y la invitación se cierra")
    void completar_registraYCierraInvitacion() {
        Evento evento = evento(EstadoEvento.habilitado);
        InvitacionConferencista inv = invitacion(evento, EstadoInvitacion.pendiente, LocalDateTime.now().plusDays(2));
        when(invitacionRepository.findByToken("abc")).thenReturn(Optional.of(inv));
        Persona persona = Persona.builder().id(30L).nombres("Ana María").apellidos("Pérez Gómez").build();
        CompletarInvitacionRequest datos = datos();
        when(conferencistaService.registrarDesdeInvitacion(evento, "ana.perez@unal.edu.co",
                TipoParticipacionConferencista.conferencista, datos))
                .thenReturn(ParticipacionConferencista.builder().id(7L).evento(evento).persona(persona).build());

        InvitacionCompletadaResponse response = service.completar("abc", datos);

        assertEquals(EstadoInvitacion.completada, inv.getEstado());
        assertEquals(persona, inv.getPersona());
        assertNotNull(inv.getFechaCompletada());
        verify(invitacionRepository).save(inv);
        assertEquals("Ana María Pérez Gómez", response.nombreCompleto());
        assertTrue(response.vistaPublicaDisponible());
    }

    @Test
    @DisplayName("Completar: con el enlace vencido no se registra nada")
    void completar_vencida_noRegistra() {
        when(invitacionRepository.findByToken("abc")).thenReturn(Optional.of(
                invitacion(evento(EstadoEvento.habilitado), EstadoInvitacion.pendiente, LocalDateTime.now().minusDays(1))));

        assertThrows(InvitacionNoDisponibleException.class, () -> service.completar("abc", datos()));
        verify(conferencistaService, never()).registrarDesdeInvitacion(any(), any(), any(), any());
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("El token del enlace es aleatorio, de 43 caracteres y seguro para URL")
    void generarToken_unicoYSeguroParaUrl() {
        String a = InvitacionConferencistaService.generarToken();
        String b = InvitacionConferencistaService.generarToken();
        assertEquals(43, a.length());
        assertNotEquals(a, b);
        assertTrue(a.matches("[A-Za-z0-9_-]+"));
        verify(auditoriaService, never()).registrar(any(), anyString(), anyLong(), anyMap());
    }
}
