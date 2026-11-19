package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.encuesta.EncuestaRequest;
import com.sigea.demosigea_backend.dto.encuesta.EncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.PreguntaEncuestaRequest;
import com.sigea.demosigea_backend.dto.encuesta.RespuestasParticipanteResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResultadoPreguntaResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResultadosEncuestaResponse;
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
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoRespuestaEncuesta;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.AsistenciaRepository;
import com.sigea.demosigea_backend.repository.EncuestaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link EncuestaService} (HU-25, Criterios 1, 2 y 4).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EncuestaServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 10, 0);

    @Mock private EncuestaRepository encuestaRepository;
    @Mock private ParticipacionEncuestaRepository participacionRepository;
    @Mock private RespuestaEncuestaRepository respuestaRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private NotificacionService notificacionService;

    private EncuestaService servicio;
    private Evento evento;
    private Actividad taller;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
        servicio = new EncuestaService(encuestaRepository, participacionRepository, respuestaRepository,
                asistenciaRepository, eventoRepository, actividadRepository, usuarioRepository, auditoriaService,
                notificacionService, reloj);

        evento = Evento.builder().id(1L).nombre("Semana de Ingeniería").estado(EstadoEvento.en_ejecucion).build();
        taller = Actividad.builder().id(201L).evento(evento).nombre("Taller de Spring Boot")
                .estado(Actividad.ESTADO_PROGRAMADA).build();

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(eventoRepository.existsById(1L)).thenReturn(true);
        when(actividadRepository.findByIdAndEvento_Id(201L, 1L)).thenReturn(Optional.of(taller));
        when(encuestaRepository.save(any(Encuesta.class))).thenAnswer(inv -> {
            Encuesta e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(7L);
            }
            long id = 40;
            for (PreguntaEncuesta p : e.getPreguntas()) {
                if (p.getId() == null) {
                    p.setId(id++);
                }
                for (OpcionPreguntaEncuesta o : p.getOpciones()) {
                    if (o.getId() == null) {
                        o.setId(id++ * 10);
                    }
                }
            }
            return e;
        });
        when(asistenciaRepository.contarPersonasConAsistenciaEnActividad(201L)).thenReturn(2L);
        when(asistenciaRepository.contarPersonasConAsistenciaEnEvento(1L)).thenReturn(3L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static PreguntaEncuestaRequest pregunta(String texto, TipoRespuestaEncuesta tipo, Boolean obligatoria,
                                                    Integer min, Integer max, List<String> opciones) {
        return new PreguntaEncuestaRequest(texto, tipo, obligatoria, min, max, opciones);
    }

    private static EncuestaRequest solicitud(Long actividadId, boolean anonima, List<PreguntaEncuestaRequest> preguntas) {
        return new EncuestaRequest("¿Cómo te pareció?", null, actividadId, anonima, null, preguntas);
    }

    private Encuesta encuesta(EstadoEncuesta estado, Actividad actividad, boolean anonima) {
        Encuesta e = Encuesta.builder().id(7L).evento(evento).actividad(actividad).titulo("¿Cómo te pareció?")
                .anonima(anonima).estado(estado).fechaCreacion(AHORA.minusDays(1)).build();
        PreguntaEncuesta escala = PreguntaEncuesta.builder().id(40L).texto("Satisfacción")
                .tipoRespuesta(TipoRespuestaEncuesta.escala).orden(1).obligatoria(true)
                .escalaMinima(1).escalaMaxima(5).build();
        PreguntaEncuesta opcion = PreguntaEncuesta.builder().id(41L).texto("¿Lo recomendarías?")
                .tipoRespuesta(TipoRespuestaEncuesta.opcion).orden(2).obligatoria(true).build();
        opcion.agregarOpcion(OpcionPreguntaEncuesta.builder().id(410L).texto("Sí").orden(1).build());
        opcion.agregarOpcion(OpcionPreguntaEncuesta.builder().id(411L).texto("No").orden(2).build());
        PreguntaEncuesta texto = PreguntaEncuesta.builder().id(42L).texto("Comentarios")
                .tipoRespuesta(TipoRespuestaEncuesta.texto).orden(3).obligatoria(false).build();
        e.agregarPregunta(escala);
        e.agregarPregunta(opcion);
        e.agregarPregunta(texto);
        return e;
    }

    // ──────────────────────────── Criterio 1: configurar ────────────────────────────

    @Test
    @DisplayName("Criterio 1: crea la encuesta de una actividad en borrador con sus preguntas en orden")
    void creaEnBorrador() {
        EncuestaResponse resp = servicio.crear(1L, solicitud(201L, true, List.of(
                pregunta("Satisfacción", TipoRespuestaEncuesta.escala, null, null, null, null),
                pregunta("¿Lo recomendarías?", TipoRespuestaEncuesta.opcion, true, null, null, List.of(" Sí ", "No")),
                pregunta("Comentarios", TipoRespuestaEncuesta.texto, false, null, null, null))));

        assertEquals(EstadoEncuesta.borrador, resp.estado());
        assertEquals("actividad", resp.ambito());
        assertEquals(201L, resp.actividadId());
        assertTrue(resp.anonima());
        assertEquals(3, resp.preguntas().size());
        assertEquals(1, resp.preguntas().get(0).escalaMinima());
        assertEquals(5, resp.preguntas().get(0).escalaMaxima());
        assertEquals("Sí", resp.preguntas().get(1).opciones().get(0).texto());
        assertFalse(resp.preguntas().get(2).obligatoria());
        assertEquals(List.of(1, 2, 3), resp.preguntas().stream().map(p -> p.orden()).toList());
        assertEquals(2, resp.totalHabilitados());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ENCUESTA_CREADA), eq("encuestas"), eq(7L), any());
    }

    @Test
    @DisplayName("Sin actividad es la encuesta general del evento; habilitados = asistentes del evento")
    void encuestaGeneral() {
        EncuestaResponse resp = servicio.crear(1L, solicitud(null, false, List.of(
                pregunta("Organización", TipoRespuestaEncuesta.escala, null, 0, 10, null))));

        assertEquals("evento", resp.ambito());
        assertNull(resp.actividadId());
        assertEquals(3, resp.totalHabilitados());
        assertEquals(0, resp.preguntas().get(0).escalaMinima());
        assertEquals(10, resp.preguntas().get(0).escalaMaxima());
    }

    @Test
    @DisplayName("Configuración inválida → 400 ENCUESTA_CONFIGURACION_INVALIDA con detalle por pregunta, sin guardar")
    void configuracionInvalida() {
        EncuestaException ex = assertThrows(EncuestaException.class, () -> servicio.crear(1L, solicitud(null, false, List.of(
                pregunta("Una sola opción", TipoRespuestaEncuesta.opcion, null, null, null, List.of("Sí")),
                pregunta("Escala al revés", TipoRespuestaEncuesta.escala, null, 5, 3, null),
                pregunta("Texto con opciones", TipoRespuestaEncuesta.texto, null, null, null, List.of("x")),
                pregunta("Opciones repetidas", TipoRespuestaEncuesta.opcion, null, null, null, List.of("Sí", " sí")),
                pregunta("Texto con rango", TipoRespuestaEncuesta.texto, null, 1, 5, null)))));

        assertEquals(EncuestaException.CONFIGURACION_INVALIDA, ex.getCodigo());
        assertEquals(400, ex.getStatus().value());
        Map<String, String> errores = ex.getErrores();
        assertTrue(errores.containsKey("preguntas[0].opciones"));
        assertTrue(errores.containsKey("preguntas[1].escalaMaxima"));
        assertTrue(errores.containsKey("preguntas[2].opciones"));
        assertTrue(errores.containsKey("preguntas[3].opciones[1]"));
        assertTrue(errores.containsKey("preguntas[4].escalaMinima"));
        verify(encuestaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se aplica encuesta a una actividad cancelada ni con fecha de cierre pasada")
    void actividadCanceladaYFechaPasada() {
        taller.setEstado(Actividad.ESTADO_CANCELADA);
        EncuestaException ex = assertThrows(EncuestaException.class, () -> servicio.crear(1L,
                new EncuestaRequest("X", null, 201L, false, AHORA.minusMinutes(1), List.of())));

        assertTrue(ex.getErrores().containsKey("actividadId"));
        assertTrue(ex.getErrores().containsKey("fechaCierre"));
    }

    @Test
    @DisplayName("Actividad de otro evento → 404")
    void actividadDeOtroEvento() {
        when(actividadRepository.findByIdAndEvento_Id(999L, 1L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.crear(1L, solicitud(999L, false, List.of())));
    }

    @Test
    @DisplayName("Una encuesta publicada no se modifica (409 ENCUESTA_NO_EDITABLE)")
    void publicadaNoEditable() {
        when(encuestaRepository.findByIdAndEvento_IdParaActualizar(7L, 1L))
                .thenReturn(Optional.of(encuesta(EstadoEncuesta.publicada, taller, true)));

        EncuestaException ex = assertThrows(EncuestaException.class,
                () -> servicio.actualizar(1L, 7L, solicitud(null, false, List.of())));
        assertEquals(EncuestaException.NO_EDITABLE, ex.getCodigo());
        verify(encuestaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar un borrador reemplaza todas las preguntas")
    void editarBorradorReemplazaPreguntas() {
        Encuesta borrador = encuesta(EstadoEncuesta.borrador, taller, true);
        when(encuestaRepository.findByIdAndEvento_IdParaActualizar(7L, 1L)).thenReturn(Optional.of(borrador));

        EncuestaResponse resp = servicio.actualizar(1L, 7L, solicitud(null, false, List.of(
                pregunta("Nueva", TipoRespuestaEncuesta.texto, null, null, null, null))));

        assertEquals(1, resp.preguntas().size());
        assertEquals("Nueva", resp.preguntas().get(0).texto());
        assertEquals("evento", resp.ambito());
        assertFalse(resp.anonima());
        verify(encuestaRepository).flush();
    }

    // ──────────────────────────── Criterio 1: publicar ────────────────────────────

    @Test
    @DisplayName("Criterio 1: publicar habilita la encuesta y avisa a los asistentes de la actividad")
    void publicaYNotifica() {
        when(encuestaRepository.findByIdAndEvento_IdParaActualizar(7L, 1L))
                .thenReturn(Optional.of(encuesta(EstadoEncuesta.borrador, taller, true)));
        when(notificacionService.notificarAAsistentes(eq(1L), eq(201L), eq(TipoNotificacion.ENCUESTA_PUBLICADA),
                anyString(), anyString(), eq("encuestas"), eq(7L))).thenReturn(2);

        EncuestaResponse resp = servicio.publicar(1L, 7L);

        assertEquals(EstadoEncuesta.publicada, resp.estado());
        assertEquals(AHORA, resp.fechaPublicacion());
        assertTrue(resp.recibeRespuestas());
        verify(notificacionService).notificarAAsistentes(eq(1L), eq(201L), eq(TipoNotificacion.ENCUESTA_PUBLICADA),
                anyString(), anyString(), eq("encuestas"), eq(7L));
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ENCUESTA_PUBLICADA), eq("encuestas"), eq(7L), any());
    }

    @Test
    @DisplayName("La encuesta general se notifica a los asistentes del evento (actividad null)")
    void publicaGeneral() {
        when(encuestaRepository.findByIdAndEvento_IdParaActualizar(7L, 1L))
                .thenReturn(Optional.of(encuesta(EstadoEncuesta.borrador, null, false)));

        servicio.publicar(1L, 7L);

        verify(notificacionService).notificarAAsistentes(eq(1L), isNull(), eq(TipoNotificacion.ENCUESTA_PUBLICADA),
                anyString(), anyString(), eq("encuestas"), eq(7L));
    }

    @Test
    @DisplayName("No se publica sin preguntas (409 ENCUESTA_INCOMPLETA)")
    void noPublicaSinPreguntas() {
        Encuesta vacia = Encuesta.builder().id(7L).evento(evento).titulo("Vacía").estado(EstadoEncuesta.borrador)
                .preguntas(new ArrayList<>()).build();
        when(encuestaRepository.findByIdAndEvento_IdParaActualizar(7L, 1L)).thenReturn(Optional.of(vacia));

        EncuestaException ex = assertThrows(EncuestaException.class, () -> servicio.publicar(1L, 7L));
        assertEquals(EncuestaException.INCOMPLETA, ex.getCodigo());
        assertTrue(ex.getErrores().containsKey("preguntas"));
        verify(notificacionService, never()).notificarAAsistentes(anyLong(), any(), any(), anyString(), anyString(),
                anyString(), anyLong());
    }

    @Test
    @DisplayName("No se publica en un evento en configuración (409 EVENTO_NO_DISPONIBLE)")
    void noPublicaEventoEnConfiguracion() {
        evento.setEstado(EstadoEvento.en_configuracion);
        when(encuestaRepository.findByIdAndEvento_IdParaActualizar(7L, 1L))
                .thenReturn(Optional.of(encuesta(EstadoEncuesta.borrador, null, false)));

        EncuestaException ex = assertThrows(EncuestaException.class, () -> servicio.publicar(1L, 7L));
        assertEquals(EncuestaException.EVENTO_NO_DISPONIBLE, ex.getCodigo());
    }

    @Test
    @DisplayName("Publicar dos veces → 409 ENCUESTA_ESTADO_INVALIDO")
    void publicarDosVeces() {
        when(encuestaRepository.findByIdAndEvento_IdParaActualizar(7L, 1L))
                .thenReturn(Optional.of(encuesta(EstadoEncuesta.publicada, taller, true)));

        EncuestaException ex = assertThrows(EncuestaException.class, () -> servicio.publicar(1L, 7L));
        assertEquals(EncuestaException.ESTADO_INVALIDO, ex.getCodigo());
    }

    @Test
    @DisplayName("Cerrar fija la fecha de cierre y deja de recibir respuestas")
    void cerrar() {
        Encuesta publicada = encuesta(EstadoEncuesta.publicada, taller, true);
        publicada.setFechaCierre(AHORA.plusDays(3));
        when(encuestaRepository.findByIdAndEvento_IdParaActualizar(7L, 1L)).thenReturn(Optional.of(publicada));

        EncuestaResponse resp = servicio.cerrar(1L, 7L);

        assertEquals(EstadoEncuesta.cerrada, resp.estado());
        assertEquals(AHORA, resp.fechaCierre());
        assertFalse(resp.recibeRespuestas());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ENCUESTA_CERRADA), eq("encuestas"), eq(7L), any());
    }

    @Test
    @DisplayName("No se elimina una encuesta con respuestas")
    void noEliminaConRespuestas() {
        when(encuestaRepository.findByIdAndEvento_IdParaActualizar(7L, 1L))
                .thenReturn(Optional.of(encuesta(EstadoEncuesta.publicada, taller, true)));
        when(participacionRepository.countByEncuesta_Id(7L)).thenReturn(4L);

        EncuestaException ex = assertThrows(EncuestaException.class, () -> servicio.eliminar(1L, 7L));
        assertEquals(EncuestaException.NO_EDITABLE, ex.getCodigo());
        verify(encuestaRepository, never()).delete(any());
    }

    // ──────────────────────────── Criterio 4: resultados ────────────────────────────

    @Test
    @DisplayName("Criterio 4: consolida promedio y distribución de la escala, opciones y textos")
    void resultadosConsolidados() {
        Encuesta e = encuesta(EstadoEncuesta.publicada, taller, true);
        when(encuestaRepository.findByIdAndEvento_Id(7L, 1L)).thenReturn(Optional.of(e));
        when(participacionRepository.countByEncuesta_Id(7L)).thenReturn(2L);
        when(respuestaRepository.contarPorValorEscala(7L)).thenReturn(List.of(
                new Object[]{40L, 5, 1L}, new Object[]{40L, 2, 1L}));
        when(respuestaRepository.contarPorOpcion(7L)).thenReturn(List.<Object[]>of(new Object[]{410L, 2L}));
        RespuestaEncuesta texto = RespuestaEncuesta.builder().id(1L).pregunta(e.getPreguntas().get(2))
                .respuesta("Muy buen taller").fechaRespuesta(AHORA.toLocalDate().atStartOfDay()).build();
        when(respuestaRepository.findTextosDeEncuesta(7L)).thenReturn(List.of(texto));

        ResultadosEncuestaResponse res = servicio.resultados(1L, 7L);

        assertEquals(2, res.totalHabilitados());
        assertEquals(2, res.totalRespuestas());
        assertEquals("100.0", res.tasaRespuesta().toPlainString());

        ResultadoPreguntaResponse escala = res.preguntas().get(0);
        assertEquals("3.50", escala.promedio().toPlainString());
        assertEquals(5, escala.distribucion().size());
        assertEquals(1, escala.distribucion().get(1).cantidad());
        assertEquals(0, escala.distribucion().get(2).cantidad());
        assertEquals("50.0", escala.distribucion().get(4).porcentaje().toPlainString());

        ResultadoPreguntaResponse opcion = res.preguntas().get(1);
        assertEquals(2, opcion.totalRespuestas());
        assertEquals(2, opcion.distribucion().get(0).cantidad());
        assertEquals(0, opcion.distribucion().get(1).cantidad());
        assertEquals("100.0", opcion.distribucion().get(0).porcentaje().toPlainString());

        ResultadoPreguntaResponse textos = res.preguntas().get(2);
        assertEquals(1, textos.totalRespuestas());
        assertEquals(1, textos.omitidas());
        assertEquals("Muy buen taller", textos.respuestasTexto().get(0).texto());
        assertNull(textos.respuestasTexto().get(0).participanteNombre(), "En una anónima no se muestra quién respondió");
    }

    @Test
    @DisplayName("Criterio 4: sin respuestas no hay promedio y la tasa es 0")
    void resultadosSinRespuestas() {
        when(encuestaRepository.findByIdAndEvento_Id(7L, 1L))
                .thenReturn(Optional.of(encuesta(EstadoEncuesta.publicada, taller, false)));
        when(respuestaRepository.contarPorValorEscala(7L)).thenReturn(List.of());
        when(respuestaRepository.contarPorOpcion(7L)).thenReturn(List.of());
        when(respuestaRepository.findTextosDeEncuesta(7L)).thenReturn(List.of());

        ResultadosEncuestaResponse res = servicio.resultados(1L, 7L);

        assertEquals(0, res.totalRespuestas());
        assertEquals("0.0", res.tasaRespuesta().toPlainString());
        assertNull(res.preguntas().get(0).promedio());
        assertNotNull(res.preguntas().get(0).distribucion());
    }

    @Test
    @DisplayName("Criterio 2: en una encuesta anónima no se consultan respuestas por participante")
    void anonimaSinRespuestasIndividuales() {
        when(encuestaRepository.findByIdAndEvento_Id(7L, 1L))
                .thenReturn(Optional.of(encuesta(EstadoEncuesta.publicada, taller, true)));

        EncuestaException ex = assertThrows(EncuestaException.class, () -> servicio.respuestasPorParticipante(1L, 7L));
        assertEquals(EncuestaException.ANONIMA, ex.getCodigo());
        verify(respuestaRepository, never()).findIdentificadasDeEncuesta(anyLong());
    }

    @Test
    @DisplayName("Criterio 2: en una identificada se listan las respuestas de cada participante")
    void identificadaRespuestasIndividuales() {
        Encuesta e = encuesta(EstadoEncuesta.publicada, null, false);
        Persona ana = Persona.builder().id(10L).nombres("Ana").apellidos("Pérez").numeroDocumento("1090")
                .correo("ana@ufps.edu.co").build();
        when(encuestaRepository.findByIdAndEvento_Id(7L, 1L)).thenReturn(Optional.of(e));
        when(respuestaRepository.findIdentificadasDeEncuesta(7L)).thenReturn(List.of(
                RespuestaEncuesta.builder().pregunta(e.getPreguntas().get(0)).persona(ana).respuesta("4").valorEscala(4).build()));
        when(participacionRepository.findByEncuestaConPersona(7L)).thenReturn(List.of(
                ParticipacionEncuesta.builder().encuesta(e).persona(ana).fechaRespuesta(AHORA).build()));

        List<RespuestasParticipanteResponse> lista = servicio.respuestasPorParticipante(1L, 7L);

        assertEquals(1, lista.size());
        assertEquals("Ana Pérez", lista.get(0).participanteNombre());
        assertEquals("4", lista.get(0).respuestas().get(0).respuesta());
    }

    @Test
    @DisplayName("Encuesta de otro evento → 404")
    void encuestaDeOtroEvento() {
        when(encuestaRepository.findByIdAndEvento_Id(7L, 2L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.resultados(2L, 7L));
    }

    @Test
    @DisplayName("La auditoría de la creación guarda un resumen de la configuración")
    void auditoriaCreacionIncluyeResumen() {
        servicio.crear(1L, solicitud(201L, true, List.of(
                pregunta("Satisfacción", TipoRespuestaEncuesta.escala, null, null, null, null))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ENCUESTA_CREADA), eq("encuestas"), eq(7L),
                captor.capture());
        assertEquals(true, captor.getValue().get("anonima"));
        assertEquals(1, captor.getValue().get("preguntas"));
    }
}
