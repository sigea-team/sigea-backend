package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.encuesta.EncuestaParticipanteResponse;
import com.sigea.demosigea_backend.dto.encuesta.EnvioEncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResponderEncuestaRequest;
import com.sigea.demosigea_backend.dto.encuesta.RespuestaPreguntaRequest;
import com.sigea.demosigea_backend.exception.EncuestaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Encuesta;
import com.sigea.demosigea_backend.model.EstadoEncuesta;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.OpcionPreguntaEncuesta;
import com.sigea.demosigea_backend.model.ParticipacionEncuesta;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.PreguntaEncuesta;
import com.sigea.demosigea_backend.model.RespuestaEncuesta;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoRespuestaEncuesta;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AsistenciaRepository;
import com.sigea.demosigea_backend.repository.EncuestaRepository;
import com.sigea.demosigea_backend.repository.ParticipacionEncuestaRepository;
import com.sigea.demosigea_backend.repository.RespuestaEncuestaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link RespuestaEncuestaService} (HU-25, Criterios 1, 2 y 3).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RespuestaEncuestaServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 10, 30);
    private static final String CORREO = "ana@ufps.edu.co";

    @Mock private EncuestaRepository encuestaRepository;
    @Mock private ParticipacionEncuestaRepository participacionRepository;
    @Mock private RespuestaEncuestaRepository respuestaRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;

    private RespuestaEncuestaService servicio;
    private Persona ana;
    private Evento evento;
    private Actividad taller;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
        servicio = new RespuestaEncuestaService(encuestaRepository, participacionRepository, respuestaRepository,
                asistenciaRepository, usuarioRepository, auditoriaService, reloj);

        ana = Persona.builder().id(10L).nombres("Ana").apellidos("Pérez").correo(CORREO).build();
        Usuario usuario = Usuario.builder().id(50L).persona(ana).build();
        when(usuarioRepository.findByPersona_CorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CORREO, null, List.of()));

        evento = Evento.builder().id(1L).nombre("Semana de Ingeniería").estado(EstadoEvento.en_ejecucion).build();
        taller = Actividad.builder().id(201L).evento(evento).nombre("Taller de Spring Boot")
                .estado(Actividad.ESTADO_PROGRAMADA).build();

        when(asistenciaRepository.existsByInscripcion_Persona_IdAndActividad_Id(10L, 201L)).thenReturn(true);
        when(asistenciaRepository.existsByInscripcion_Persona_IdAndInscripcion_Evento_Id(10L, 1L)).thenReturn(true);
        when(participacionRepository.findByEncuesta_IdAndPersona_Id(any(), any())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Encuesta encuesta(boolean anonima, EstadoEncuesta estado) {
        Encuesta e = Encuesta.builder().id(7L).evento(evento).actividad(taller).titulo("¿Cómo te pareció el taller?")
                .anonima(anonima).estado(estado).fechaPublicacion(AHORA.minusDays(1)).build();
        e.agregarPregunta(PreguntaEncuesta.builder().id(40L).texto("Satisfacción")
                .tipoRespuesta(TipoRespuestaEncuesta.escala).orden(1).obligatoria(true)
                .escalaMinima(1).escalaMaxima(5).build());
        PreguntaEncuesta opcion = PreguntaEncuesta.builder().id(41L).texto("¿Lo recomendarías?")
                .tipoRespuesta(TipoRespuestaEncuesta.opcion).orden(2).obligatoria(true).build();
        opcion.agregarOpcion(OpcionPreguntaEncuesta.builder().id(410L).texto("Sí").orden(1).build());
        opcion.agregarOpcion(OpcionPreguntaEncuesta.builder().id(411L).texto("No").orden(2).build());
        e.agregarPregunta(opcion);
        e.agregarPregunta(PreguntaEncuesta.builder().id(42L).texto("Comentarios")
                .tipoRespuesta(TipoRespuestaEncuesta.texto).orden(3).obligatoria(false).build());
        when(encuestaRepository.findByIdParaResponder(7L)).thenReturn(Optional.of(e));
        when(encuestaRepository.findByIdConEvento(7L)).thenReturn(Optional.of(e));
        return e;
    }

    private static ResponderEncuestaRequest respuestas(RespuestaPreguntaRequest... r) {
        return new ResponderEncuestaRequest(Arrays.asList(r));
    }

    private static RespuestaPreguntaRequest escala(long pregunta, Integer valor) {
        return new RespuestaPreguntaRequest(pregunta, valor, null, null);
    }

    private static RespuestaPreguntaRequest opcion(long pregunta, Long opcion) {
        return new RespuestaPreguntaRequest(pregunta, null, opcion, null);
    }

    private static RespuestaPreguntaRequest texto(long pregunta, String texto) {
        return new RespuestaPreguntaRequest(pregunta, null, null, texto);
    }

    @SuppressWarnings("unchecked")
    private List<RespuestaEncuesta> respuestasGuardadas() {
        ArgumentCaptor<List<RespuestaEncuesta>> captor = ArgumentCaptor.forClass(List.class);
        verify(respuestaRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // ──────────────────────────── Criterio 2 ────────────────────────────

    @Test
    @DisplayName("Criterio 2: encuesta anónima → respuestas sin persona y con fecha sin hora; constancia con la persona")
    void respondeAnonima() {
        Encuesta e = encuesta(true, EstadoEncuesta.publicada);

        EnvioEncuestaResponse resp = servicio.responder(7L, respuestas(
                escala(40L, 5), opcion(41L, 410L), texto(42L, "  Muy buen taller  ")));

        assertTrue(resp.anonima());
        assertEquals(3, resp.preguntasRespondidas());
        List<RespuestaEncuesta> guardadas = respuestasGuardadas();
        assertEquals(3, guardadas.size());
        assertTrue(guardadas.stream().allMatch(r -> r.getPersona() == null));
        assertTrue(guardadas.stream().allMatch(r -> r.getFechaRespuesta().toLocalTime().equals(LocalTime.MIDNIGHT)));
        assertEquals(5, guardadas.get(0).getValorEscala());
        assertEquals("5", guardadas.get(0).getRespuesta());
        assertEquals(410L, guardadas.get(1).getOpcion().getId());
        assertEquals("Sí", guardadas.get(1).getRespuesta());
        assertEquals("Muy buen taller", guardadas.get(2).getRespuesta());

        ArgumentCaptor<ParticipacionEncuesta> participacion = ArgumentCaptor.forClass(ParticipacionEncuesta.class);
        verify(participacionRepository).save(participacion.capture());
        assertSame(ana, participacion.getValue().getPersona());
        assertSame(e, participacion.getValue().getEncuesta());
        assertEquals(AHORA, participacion.getValue().getFechaRespuesta());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ENCUESTA_RESPONDIDA), eq("encuestas"), eq(7L), any());
    }

    @Test
    @DisplayName("Criterio 2: encuesta identificada → respuestas con la persona y la fecha y hora")
    void respondeIdentificada() {
        encuesta(false, EstadoEncuesta.publicada);

        servicio.responder(7L, respuestas(escala(40L, 3), opcion(41L, 411L)));

        List<RespuestaEncuesta> guardadas = respuestasGuardadas();
        assertEquals(2, guardadas.size(), "La pregunta opcional omitida no se guarda");
        assertTrue(guardadas.stream().allMatch(r -> r.getPersona() == ana));
        assertTrue(guardadas.stream().allMatch(r -> r.getFechaRespuesta().equals(AHORA)));
    }

    @Test
    @DisplayName("Respuestas inválidas → 400 RESPUESTAS_INVALIDAS con detalle por pregunta y sin guardar nada")
    void respuestasInvalidas() {
        encuesta(true, EstadoEncuesta.publicada);

        EncuestaException ex = assertThrows(EncuestaException.class, () -> servicio.responder(7L, respuestas(
                escala(40L, 9), opcion(41L, 999L), texto(42L, "ok"), escala(77L, 1))));

        assertEquals(EncuestaException.RESPUESTAS_INVALIDAS, ex.getCodigo());
        assertEquals(400, ex.getStatus().value());
        assertTrue(ex.getErrores().get("pregunta_40").contains("fuera de la escala"));
        assertTrue(ex.getErrores().containsKey("pregunta_41"));
        assertTrue(ex.getErrores().containsKey("pregunta_77"));
        assertFalse(ex.getErrores().containsKey("pregunta_42"));
        verify(participacionRepository, never()).save(any());
        verify(respuestaRepository, never()).saveAll(anyIterable());
    }

    @Test
    @DisplayName("Falta una obligatoria, se repite una pregunta o el tipo no coincide → 400")
    void obligatoriaRepetidaYTipo() {
        encuesta(true, EstadoEncuesta.publicada);

        EncuestaException faltante = assertThrows(EncuestaException.class,
                () -> servicio.responder(7L, respuestas(escala(40L, 4))));
        assertEquals("Esta pregunta es obligatoria.", faltante.getErrores().get("pregunta_41"));

        EncuestaException repetida = assertThrows(EncuestaException.class,
                () -> servicio.responder(7L, respuestas(escala(40L, 4), escala(40L, 5), opcion(41L, 410L))));
        assertTrue(repetida.getErrores().get("pregunta_40").contains("más de una vez"));

        EncuestaException tipo = assertThrows(EncuestaException.class,
                () -> servicio.responder(7L, respuestas(texto(40L, "cinco"), opcion(41L, 410L))));
        assertTrue(tipo.getErrores().containsKey("pregunta_40"));
    }

    @Test
    @DisplayName("CU-22 flujo alterno: ya respondió → 409 ENCUESTA_YA_RESPONDIDA sin duplicar")
    void yaRespondida() {
        Encuesta e = encuesta(true, EstadoEncuesta.publicada);
        when(participacionRepository.findByEncuesta_IdAndPersona_Id(7L, 10L)).thenReturn(Optional.of(
                ParticipacionEncuesta.builder().encuesta(e).persona(ana).fechaRespuesta(AHORA.minusHours(2)).build()));

        EncuestaException ex = assertThrows(EncuestaException.class,
                () -> servicio.responder(7L, respuestas(escala(40L, 4), opcion(41L, 410L))));

        assertEquals(EncuestaException.YA_RESPONDIDA, ex.getCodigo());
        assertEquals(409, ex.getStatus().value());
        verify(respuestaRepository, never()).saveAll(anyIterable());
    }

    @Test
    @DisplayName("Dos envíos simultáneos: el índice único deja pasar uno; el otro → 409 ENCUESTA_YA_RESPONDIDA")
    void envioSimultaneo() {
        encuesta(true, EstadoEncuesta.publicada);
        when(participacionRepository.save(any(ParticipacionEncuesta.class)))
                .thenThrow(new DataIntegrityViolationException("participaciones_encuesta_encuesta_persona_key"));

        EncuestaException ex = assertThrows(EncuestaException.class,
                () -> servicio.responder(7L, respuestas(escala(40L, 4), opcion(41L, 410L))));

        assertEquals(EncuestaException.YA_RESPONDIDA, ex.getCodigo());
        verify(respuestaRepository, never()).saveAll(anyIterable());
    }

    @Test
    @DisplayName("Encuesta cerrada o con la fecha de cierre vencida → 409 ENCUESTA_CERRADA")
    void cerrada() {
        Encuesta e = encuesta(true, EstadoEncuesta.cerrada);
        e.setFechaCierre(AHORA.minusHours(1));
        EncuestaException ex = assertThrows(EncuestaException.class,
                () -> servicio.responder(7L, respuestas(escala(40L, 4), opcion(41L, 410L))));
        assertEquals(EncuestaException.CERRADA, ex.getCodigo());

        e.setEstado(EstadoEncuesta.publicada);
        e.setFechaCierre(AHORA);
        EncuestaException vencida = assertThrows(EncuestaException.class,
                () -> servicio.responder(7L, respuestas(escala(40L, 4), opcion(41L, 410L))));
        assertEquals(EncuestaException.CERRADA, vencida.getCodigo());
    }

    @Test
    @DisplayName("Un borrador no existe para el participante (404)")
    void borradorNoVisible() {
        encuesta(true, EstadoEncuesta.borrador);
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.formulario(7L));
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.responder(7L, respuestas(escala(40L, 4))));
    }

    // ──────────────────────────── Criterio 3 ────────────────────────────

    @Test
    @DisplayName("Criterio 3: sin asistencia a la actividad no se abre el formulario (403 ENCUESTA_NO_HABILITADA)")
    void sinAsistenciaNoHabilitada() {
        encuesta(true, EstadoEncuesta.publicada);
        when(asistenciaRepository.existsByInscripcion_Persona_IdAndActividad_Id(10L, 201L)).thenReturn(false);

        EncuestaException formulario = assertThrows(EncuestaException.class, () -> servicio.formulario(7L));
        assertEquals(EncuestaException.NO_HABILITADA, formulario.getCodigo());
        assertEquals(403, formulario.getStatus().value());

        EncuestaException responder = assertThrows(EncuestaException.class,
                () -> servicio.responder(7L, respuestas(escala(40L, 4), opcion(41L, 410L))));
        assertEquals(EncuestaException.NO_HABILITADA, responder.getCodigo());
        verify(participacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: la encuesta general exige asistencia a alguna actividad del evento")
    void encuestaGeneralExigeAsistenciaAlEvento() {
        Encuesta e = encuesta(false, EstadoEncuesta.publicada);
        e.setActividad(null);
        when(asistenciaRepository.existsByInscripcion_Persona_IdAndInscripcion_Evento_Id(10L, 1L)).thenReturn(false);

        EncuestaException ex = assertThrows(EncuestaException.class, () -> servicio.formulario(7L));
        assertEquals(EncuestaException.NO_HABILITADA, ex.getCodigo());
        assertTrue(ex.getMessage().contains("Semana de Ingeniería"));
    }

    // ──────────────────────────── Criterio 1: mis encuestas ────────────────────────────

    @Test
    @DisplayName("Criterio 1: mis encuestas lista las publicadas habilitadas y marca las respondidas")
    void misEncuestas() {
        Encuesta abierta = encuesta(true, EstadoEncuesta.publicada);
        Encuesta cerradaSinResponder = Encuesta.builder().id(8L).evento(evento).titulo("Cerrada")
                .estado(EstadoEncuesta.cerrada).fechaCierre(AHORA.minusDays(1)).build();
        Encuesta cerradaRespondida = Encuesta.builder().id(9L).evento(evento).titulo("Cerrada respondida")
                .estado(EstadoEncuesta.cerrada).fechaCierre(AHORA.minusDays(1)).build();
        when(encuestaRepository.findHabilitadasParaPersona(10L, EnumSet.of(EstadoEncuesta.publicada, EstadoEncuesta.cerrada)))
                .thenReturn(List.of(abierta, cerradaSinResponder, cerradaRespondida));
        when(participacionRepository.findByPersona_Id(10L)).thenReturn(List.of(
                ParticipacionEncuesta.builder().encuesta(cerradaRespondida).persona(ana).fechaRespuesta(AHORA.minusDays(2)).build()));

        List<EncuestaParticipanteResponse> lista = servicio.misEncuestas();

        assertEquals(List.of(7L, 9L), lista.stream().map(EncuestaParticipanteResponse::encuestaId).toList());
        EncuestaParticipanteResponse primera = lista.get(0);
        assertTrue(primera.recibeRespuestas());
        assertFalse(primera.respondida());
        assertEquals(3, primera.totalPreguntas());
        assertNull(primera.preguntas(), "El listado no trae las preguntas");
        assertTrue(lista.get(1).respondida());
        assertFalse(lista.get(1).recibeRespuestas());
    }

    @Test
    @DisplayName("El formulario trae las preguntas y si ya fue respondida")
    void formulario() {
        encuesta(true, EstadoEncuesta.publicada);

        EncuestaParticipanteResponse form = servicio.formulario(7L);

        assertEquals(3, form.preguntas().size());
        assertEquals(2, form.preguntas().get(1).opciones().size());
        assertFalse(form.respondida());
        assertTrue(form.recibeRespuestas());
    }
}
