package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.observacion.ObservacionRequest;
import com.sigea.demosigea_backend.dto.observacion.ObservacionResponse;
import com.sigea.demosigea_backend.dto.observacion.ObservacionesPropuestaResponse;
import com.sigea.demosigea_backend.dto.observacion.PropuestaProgramableResponse;
import com.sigea.demosigea_backend.dto.observacion.RegistrarObservacionesRequest;
import com.sigea.demosigea_backend.dto.observacion.VersionDefinitivaResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.PropuestaBloqueadaException;
import com.sigea.demosigea_backend.exception.PropuestaNoEvaluadaException;
import com.sigea.demosigea_backend.exception.VersionDefinitivaNoAprobableException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.ClasificacionResultado;
import com.sigea.demosigea_backend.model.ComiteEvaluador;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.ObservacionPropuesta;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.PropuestaAutor;
import com.sigea.demosigea_backend.model.ResultadoEvaluacion;
import com.sigea.demosigea_backend.model.RolAutor;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.model.VersionPropuesta;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.ObservacionPropuestaRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PropuestaAutorRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.ResultadoEvaluacionRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import com.sigea.demosigea_backend.repository.VersionPropuestaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de observaciones/ajustes y aprobación de la versión definitiva (HU-19).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObservacionPropuestaServiceTest {

    private static final String CORREO_EVALUADOR = "evaluador@ufps.edu.co";
    private static final String CORREO_ADMIN = "admin@ufps.edu.co";
    private static final String CORREO_AUTOR = "autor@ufps.edu.co";

    @Mock private PropuestaRepository propuestaRepository;
    @Mock private VersionPropuestaRepository versionRepository;
    @Mock private ObservacionPropuestaRepository observacionRepository;
    @Mock private ResultadoEvaluacionRepository resultadoRepository;
    @Mock private AsignacionEvaluacionRepository asignacionRepository;
    @Mock private PropuestaAutorRepository propuestaAutorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private ParticipacionConferencistaRepository participacionRepository;

    @InjectMocks
    private ObservacionPropuestaService service;

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Datos de prueba
    // ──────────────────────────────────────────────────────────────────────────────

    private final Persona personaEvaluador = Persona.builder().id(30L).nombres("Elena").apellidos("Ruiz").build();
    private final Persona personaAutor = Persona.builder().id(10L).nombres("Carlos").apellidos("Gómez").build();
    private final Usuario usuarioAutor = Usuario.builder().id(4L).persona(personaAutor).build();

    private Usuario autenticar(Long usuarioId, Persona persona, String correo, String... autoridades) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(correo, null,
                Arrays.stream(autoridades).map(SimpleGrantedAuthority::new).toList()));
        Usuario usuario = Usuario.builder().id(usuarioId).persona(persona).build();
        when(usuarioRepository.findByPersona_CorreoIgnoreCase(correo)).thenReturn(Optional.of(usuario));
        return usuario;
    }

    private Usuario comoEvaluador() {
        return autenticar(7L, personaEvaluador, CORREO_EVALUADOR, "PROPUESTAS_EVALUAR");
    }

    private Usuario comoAdmin() {
        return autenticar(1L, Persona.builder().id(1L).nombres("María").apellidos("Rodríguez").build(), CORREO_ADMIN,
                "PROPUESTAS_APROBAR_DEFINITIVA");
    }

    private Propuesta propuesta(EstadoPropuesta estado) {
        Evento evento = Evento.builder().id(2L).nombre("Simposio").build();
        Convocatoria convocatoria = Convocatoria.builder().id(5L).evento(evento).titulo("Convocatoria 2026").build();
        Propuesta p = Propuesta.builder().id(50L).convocatoria(convocatoria).titulo("IA en la Educación")
                .estado(estado).autores(new ArrayList<>()).build();
        when(propuestaRepository.findByIdParaActualizar(50L)).thenReturn(Optional.of(p));
        when(propuestaRepository.findById(50L)).thenReturn(Optional.of(p));
        return p;
    }

    private void clasificada(Propuesta p, ClasificacionResultado clasificacion) {
        when(resultadoRepository.findByPropuesta_Id(p.getId())).thenReturn(Optional.of(ResultadoEvaluacion.builder()
                .id(8L).propuesta(p).puntuacionPonderada(new BigDecimal("6.40")).clasificacion(clasificacion)
                .fechaCalculo(LocalDateTime.now()).fechaClasificacion(LocalDateTime.now()).build()));
    }

    private VersionPropuesta version(Propuesta p, int numero) {
        return VersionPropuesta.builder().id(100L + numero).propuesta(p).numeroVersion(numero)
                .documentoUrl("propuestas/50/v" + numero + "/a.pdf").creadoPor(usuarioAutor)
                .fechaCreacion(LocalDateTime.now().minusDays(numero)).esDefinitiva(false).build();
    }

    private void versionActual(Propuesta p, VersionPropuesta v) {
        when(versionRepository.findTopByPropuesta_IdOrderByNumeroVersionDesc(p.getId())).thenReturn(Optional.of(v));
    }

    private ComiteEvaluador evaluadorAsignado(Propuesta p) {
        ComiteEvaluador comite = ComiteEvaluador.builder().id(3L).convocatoria(p.getConvocatoria())
                .persona(personaEvaluador).build();
        when(asignacionRepository.findDetalleByPropuestaId(p.getId())).thenReturn(List.of(
                AsignacionEvaluacion.builder().id(9L).propuesta(p).comiteEvaluador(comite).build()));
        return comite;
    }

    private void autorConCuenta(Propuesta p, VersionPropuesta primera) {
        when(propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(p.getId())).thenReturn(List.of(
                PropuestaAutor.builder().propuesta(p).persona(personaAutor).rol(RolAutor.autor_principal).orden(1).build()));
        when(usuarioRepository.findByPersona_Id(personaAutor.getId())).thenReturn(Optional.of(usuarioAutor));
        when(versionRepository.findFirstByPropuesta_IdOrderByNumeroVersionAsc(p.getId())).thenReturn(Optional.of(primera));
    }

    private ObservacionPropuesta observacion(Long id, VersionPropuesta v, boolean ajuste, boolean resuelto,
                                             VersionPropuesta atencion) {
        return ObservacionPropuesta.builder().id(id).version(v).descripcion("Observación " + id)
                .requiereAjuste(ajuste).resuelto(resuelto).versionAtencion(atencion)
                .fechaResolucion(resuelto ? LocalDateTime.now() : null)
                .fecha(LocalDateTime.now().minusDays(1)).build();
    }

    private static RegistrarObservacionesRequest solicitud(ObservacionRequest... items) {
        return new RegistrarObservacionesRequest(List.of(items));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 1: registrar observaciones y notificar al autor
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 1: el evaluador asignado registra observaciones y ajustes, se guardan y se notifica al autor")
    void criterio1_registraYNotifica() {
        Usuario evaluador = comoEvaluador();
        Propuesta p = propuesta(EstadoPropuesta.ajustes);
        clasificada(p, ClasificacionResultado.aprobada_con_ajustes);
        VersionPropuesta v1 = version(p, 1);
        versionActual(p, v1);
        ComiteEvaluador comite = evaluadorAsignado(p);
        autorConCuenta(p, v1);
        when(observacionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ObservacionesPropuestaResponse r = service.registrar(50L, solicitud(
                new ObservacionRequest("  Ampliar la metodología  ", null),
                new ObservacionRequest("Buen marco teórico", false)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ObservacionPropuesta>> captor = ArgumentCaptor.forClass(List.class);
        verify(observacionRepository).saveAll(captor.capture());
        List<ObservacionPropuesta> guardadas = captor.getValue();
        assertEquals(2, guardadas.size());
        assertEquals("Ampliar la metodología", guardadas.get(0).getDescripcion());
        assertTrue(guardadas.get(0).getRequiereAjuste());
        assertFalse(guardadas.get(1).getRequiereAjuste());
        assertSame(v1, guardadas.get(0).getVersion());
        assertSame(comite, guardadas.get(0).getComiteEvaluador());
        assertSame(evaluador, guardadas.get(0).getUsuario());
        assertFalse(guardadas.get(0).getResuelto());

        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.OBSERVACIONES_PROPUESTA_REGISTRADAS),
                eq("propuestas"), eq(50L), anyMap(), eq(evaluador));
        // Autor y emisor son el mismo usuario: una sola notificación.
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService, times(1)).notificarAUsuario(eq(usuarioAutor),
                eq(TipoNotificacion.OBSERVACIONES_PROPUESTA_REGISTRADAS), anyString(), mensaje.capture(),
                eq("propuestas"), eq(50L));
        assertTrue(mensaje.getValue().contains("1 requiere ajustes"), mensaje.getValue());
        assertEquals(50L, r.propuestaId());
    }

    @Test
    @DisplayName("Criterio 1: el administrador registra observaciones sin pertenecer al comité")
    void criterio1_administradorSinComite() {
        Usuario admin = comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.ajustes);
        clasificada(p, ClasificacionResultado.aprobada_con_ajustes);
        versionActual(p, version(p, 1));
        when(observacionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        service.registrar(50L, solicitud(new ObservacionRequest("Corregir referencias", true)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ObservacionPropuesta>> captor = ArgumentCaptor.forClass(List.class);
        verify(observacionRepository).saveAll(captor.capture());
        assertNull(captor.getValue().get(0).getComiteEvaluador());
        assertSame(admin, captor.getValue().get(0).getUsuario());
    }

    @Test
    @DisplayName("Criterio 1: un evaluador no asignado a la propuesta no puede registrar observaciones")
    void criterio1_evaluadorNoAsignado_rechaza() {
        comoEvaluador();
        propuesta(EstadoPropuesta.ajustes);
        when(asignacionRepository.findDetalleByPropuestaId(50L)).thenReturn(List.of());

        assertThrows(AccessDeniedException.class,
                () -> service.registrar(50L, solicitud(new ObservacionRequest("x", true))));
        verify(observacionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Seguridad: el permiso de clasificar (HU-18) no basta para registrar ni resolver observaciones")
    void soloClasificar_sinAsignacion_rechaza() {
        autenticar(8L, Persona.builder().id(80L).build(), "clasificador@ufps.edu.co", "PROPUESTAS_CLASIFICAR");
        Propuesta p = propuesta(EstadoPropuesta.ajustes);
        clasificada(p, ClasificacionResultado.aprobada_con_ajustes);
        when(asignacionRepository.findDetalleByPropuestaId(50L)).thenReturn(List.of());

        assertThrows(AccessDeniedException.class,
                () -> service.registrar(50L, solicitud(new ObservacionRequest("x", true))));
        assertThrows(AccessDeniedException.class, () -> service.resolver(50L, 60L));
        assertThrows(AccessDeniedException.class, () -> service.aprobarVersionDefinitiva(50L));
        verify(observacionRepository, never()).saveAll(anyList());
        verify(observacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 1: una propuesta sin clasificar no admite observaciones")
    void criterio1_sinClasificar_rechaza() {
        comoEvaluador();
        Propuesta p = propuesta(EstadoPropuesta.en_evaluacion);
        evaluadorAsignado(p);
        when(resultadoRepository.findByPropuesta_Id(50L)).thenReturn(Optional.empty());

        assertThrows(PropuestaNoEvaluadaException.class,
                () -> service.registrar(50L, solicitud(new ObservacionRequest("x", true))));
        verify(observacionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Criterio 1: una propuesta rechazada no admite observaciones")
    void criterio1_rechazada_rechaza() {
        comoAdmin();
        propuesta(EstadoPropuesta.rechazada);

        assertThrows(OperacionNoPermitidaException.class,
                () -> service.registrar(50L, solicitud(new ObservacionRequest("x", true))));
    }

    @Test
    @DisplayName("Criterio 1: una propuesta aprobada sin modificaciones no lleva ajustes")
    void criterio1_aprobadaSinModificaciones_rechaza() {
        comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.aprobada);
        clasificada(p, ClasificacionResultado.aprobada);

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> service.registrar(50L, solicitud(new ObservacionRequest("x", true))));
        assertTrue(ex.getMessage().contains("aceptada sin modificaciones"));
        verify(observacionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Criterio 3: con versión definitiva no se registran más observaciones")
    void criterio1_conVersionDefinitiva_rechaza() {
        comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.ajustes);
        clasificada(p, ClasificacionResultado.aprobada_con_ajustes);
        when(versionRepository.existsByPropuesta_IdAndEsDefinitivaTrue(50L)).thenReturn(true);

        assertThrows(PropuestaBloqueadaException.class,
                () -> service.registrar(50L, solicitud(new ObservacionRequest("x", true))));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 2: resolver ajustes atendidos por una nueva versión
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 2: un ajuste sin versión del autor que lo atienda no se puede resolver")
    void criterio2_sinVersionDeAtencion_noSeResuelve() {
        comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.ajustes);
        ObservacionPropuesta o = observacion(60L, version(p, 1), true, false, null);
        when(observacionRepository.findByIdAndVersion_Propuesta_Id(60L, 50L)).thenReturn(Optional.of(o));

        assertThrows(OperacionNoPermitidaException.class, () -> service.resolver(50L, 60L));
        assertFalse(o.getResuelto());
        verify(observacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 2: un ajuste atendido por la versión 2 se da por resuelto y se audita")
    void criterio2_atendido_seResuelve() {
        comoEvaluador();
        Propuesta p = propuesta(EstadoPropuesta.ajustes);
        evaluadorAsignado(p);
        VersionPropuesta v2 = version(p, 2);
        ObservacionPropuesta o = observacion(60L, version(p, 1), true, false, v2);
        when(observacionRepository.findByIdAndVersion_Propuesta_Id(60L, 50L)).thenReturn(Optional.of(o));
        when(observacionRepository.save(any(ObservacionPropuesta.class))).thenAnswer(inv -> inv.getArgument(0));

        ObservacionResponse r = service.resolver(50L, 60L);

        assertTrue(r.resuelto());
        assertEquals(ObservacionResponse.RESUELTA, r.estado());
        assertEquals(1, r.numeroVersion());
        assertEquals(2, r.versionAtencionNumero());
        assertNotNull(o.getFechaResolucion());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.OBSERVACION_PROPUESTA_RESUELTA),
                eq("observaciones_propuesta"), eq(60L), anyMap(), any(Usuario.class));
    }

    @Test
    @DisplayName("Criterio 2: el autor no puede dar por resueltas sus propias observaciones")
    void criterio2_autor_noResuelve() {
        autenticar(4L, personaAutor, CORREO_AUTOR);
        propuesta(EstadoPropuesta.ajustes);
        when(asignacionRepository.findDetalleByPropuestaId(50L)).thenReturn(List.of());

        assertThrows(AccessDeniedException.class, () -> service.resolver(50L, 60L));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterios 3 y 4: aprobar la versión definitiva
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 4: con ajustes sin resolver se impide la aprobación y se indican los pendientes")
    void criterio4_conPendientes_impideAprobacion() {
        comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.ajustes);
        clasificada(p, ClasificacionResultado.aprobada_con_ajustes);
        VersionPropuesta v1 = version(p, 1);
        VersionPropuesta v2 = version(p, 2);
        versionActual(p, v2);
        when(observacionRepository.findDePropuesta(50L)).thenReturn(List.of(
                observacion(60L, v1, true, true, v2),
                observacion(61L, v1, true, false, v2),
                observacion(62L, v1, true, false, null),
                observacion(63L, v1, false, false, null)));

        VersionDefinitivaNoAprobableException ex = assertThrows(VersionDefinitivaNoAprobableException.class,
                () -> service.aprobarVersionDefinitiva(50L));

        assertEquals(VersionDefinitivaNoAprobableException.OBSERVACIONES_PENDIENTES, ex.getCodigo());
        assertEquals(List.of(61L, 62L), ex.getPendientes().stream().map(ObservacionResponse::id).toList());
        assertEquals(ObservacionResponse.ATENDIDA, ex.getPendientes().get(0).estado());
        assertEquals(ObservacionResponse.PENDIENTE, ex.getPendientes().get(1).estado());
        assertTrue(ex.getMessage().contains("2 ajuste(s)"));
        assertFalse(v2.getEsDefinitiva());
        verify(versionRepository, never()).save(any());
        verify(propuestaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 4: aprobada con ajustes pero sin ajustes registrados no se aprueba")
    void criterio4_ajustesSinRegistrar_impideAprobacion() {
        comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.ajustes);
        clasificada(p, ClasificacionResultado.aprobada_con_ajustes);
        versionActual(p, version(p, 1));
        when(observacionRepository.findDePropuesta(50L)).thenReturn(List.of());

        VersionDefinitivaNoAprobableException ex = assertThrows(VersionDefinitivaNoAprobableException.class,
                () -> service.aprobarVersionDefinitiva(50L));

        assertEquals(VersionDefinitivaNoAprobableException.AJUSTES_SIN_REGISTRAR, ex.getCodigo());
        assertTrue(ex.getPendientes().isEmpty());
    }

    @Test
    @DisplayName("Criterio 3: con los ajustes resueltos se marca la versión definitiva, se bloquea y se notifica")
    void criterio3_aprobarVersionDefinitiva() {
        Usuario admin = comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.ajustes);
        p.setEdicionAutorizada(true);
        clasificada(p, ClasificacionResultado.aprobada_con_ajustes);
        VersionPropuesta v1 = version(p, 1);
        VersionPropuesta v2 = version(p, 2);
        versionActual(p, v2);
        autorConCuenta(p, v1);
        when(observacionRepository.findDePropuesta(50L)).thenReturn(List.of(
                observacion(60L, v1, true, true, v2),
                observacion(61L, v1, false, false, null)));
        when(versionRepository.findByPropuesta_IdAndEsDefinitivaTrue(50L)).thenReturn(Optional.empty());
        when(versionRepository.save(any(VersionPropuesta.class))).thenAnswer(inv -> inv.getArgument(0));

        VersionDefinitivaResponse r = service.aprobarVersionDefinitiva(50L);

        assertTrue(v2.getEsDefinitiva());
        assertNotNull(v2.getFechaDefinitiva());
        assertSame(admin, v2.getAprobadaPor());
        assertFalse(v1.getEsDefinitiva());
        assertEquals(EstadoPropuesta.aprobada, p.getEstado());
        assertFalse(p.getEdicionAutorizada());
        assertEquals(2, r.numeroVersion());
        assertNull(r.versionDefinitivaAnterior());
        assertEquals(1, r.ajustesResueltos());
        assertEquals("María Rodríguez", r.aprobadaPor());
        verify(propuestaRepository).save(p);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.VERSION_DEFINITIVA_APROBADA),
                eq("versiones_propuesta"), eq(102L), anyMap(), eq(admin));
        verify(notificacionService).notificarAUsuario(eq(usuarioAutor), eq(TipoNotificacion.VERSION_DEFINITIVA_APROBADA),
                anyString(), anyString(), eq("propuestas"), eq(50L));
    }

    @Test
    @DisplayName("Criterio 3: una propuesta aceptada sin observaciones se aprueba directamente")
    void criterio3_aprobadaSinObservaciones() {
        comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.aprobada);
        clasificada(p, ClasificacionResultado.aprobada);
        VersionPropuesta v1 = version(p, 1);
        versionActual(p, v1);
        when(observacionRepository.findDePropuesta(50L)).thenReturn(List.of());
        when(versionRepository.save(any(VersionPropuesta.class))).thenAnswer(inv -> inv.getArgument(0));

        VersionDefinitivaResponse r = service.aprobarVersionDefinitiva(50L);

        assertTrue(v1.getEsDefinitiva());
        assertEquals(1, r.numeroVersion());
    }

    @Test
    @DisplayName("Criterio 3: tras una modificación autorizada, la marca de definitiva pasa a la nueva versión")
    void criterio3_reaprobacion_mueveLaMarca() {
        comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.aprobada);
        clasificada(p, ClasificacionResultado.aprobada);
        VersionPropuesta v1 = version(p, 1);
        v1.setEsDefinitiva(true);
        v1.setFechaDefinitiva(LocalDateTime.now().minusDays(3));
        VersionPropuesta v2 = version(p, 2);
        versionActual(p, v2);
        when(observacionRepository.findDePropuesta(50L)).thenReturn(List.of());
        when(versionRepository.findByPropuesta_IdAndEsDefinitivaTrue(50L)).thenReturn(Optional.of(v1));
        when(versionRepository.save(any(VersionPropuesta.class))).thenAnswer(inv -> inv.getArgument(0));

        VersionDefinitivaResponse r = service.aprobarVersionDefinitiva(50L);

        assertFalse(v1.getEsDefinitiva());
        assertNull(v1.getFechaDefinitiva());
        assertTrue(v2.getEsDefinitiva());
        assertEquals(1, r.versionDefinitivaAnterior());
        // La anterior se desmarca y se escribe antes de marcar la nueva (índice único parcial).
        InOrder orden = inOrder(versionRepository);
        orden.verify(versionRepository).saveAndFlush(v1);
        orden.verify(versionRepository).save(v2);
    }

    @Test
    @DisplayName("Criterio 3: si la versión actual ya es la definitiva no se vuelve a aprobar")
    void criterio3_yaDefinitiva_rechaza() {
        comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.aprobada);
        clasificada(p, ClasificacionResultado.aprobada);
        VersionPropuesta v1 = version(p, 1);
        v1.setEsDefinitiva(true);
        versionActual(p, v1);

        assertThrows(OperacionNoPermitidaException.class, () -> service.aprobarVersionDefinitiva(50L));
    }

    @Test
    @DisplayName("Criterio 3: un evaluador no puede aprobar la versión definitiva")
    void criterio3_evaluador_noAprueba() {
        comoEvaluador();
        propuesta(EstadoPropuesta.ajustes);

        assertThrows(AccessDeniedException.class, () -> service.aprobarVersionDefinitiva(50L));
        verify(versionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: una propuesta rechazada no se aprueba")
    void criterio3_rechazada_rechaza() {
        comoAdmin();
        propuesta(EstadoPropuesta.rechazada);

        assertThrows(OperacionNoPermitidaException.class, () -> service.aprobarVersionDefinitiva(50L));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Consulta
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Consulta: el autor ve sus observaciones y por qué aún no se puede aprobar")
    void listar_autorVePendientes() {
        autenticar(4L, personaAutor, CORREO_AUTOR);
        Propuesta p = propuesta(EstadoPropuesta.ajustes);
        clasificada(p, ClasificacionResultado.aprobada_con_ajustes);
        VersionPropuesta v1 = version(p, 1);
        versionActual(p, v1);
        when(asignacionRepository.findDetalleByPropuestaId(50L)).thenReturn(List.of());
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(50L, 10L)).thenReturn(true);
        when(observacionRepository.findDePropuesta(50L)).thenReturn(List.of(observacion(60L, v1, true, false, null)));

        ObservacionesPropuestaResponse r = service.listar(50L);

        assertEquals(1, r.versionActual());
        assertNull(r.versionDefinitiva());
        assertEquals(1, r.ajustesPendientes());
        assertEquals(1, r.ajustesPorAtender());
        assertFalse(r.puedeAprobarse());
        assertTrue(r.motivoNoAprobable().contains("sin resolver"));
        assertEquals(1, r.observaciones().size());
    }

    @Test
    @DisplayName("Consulta: un usuario ajeno a la propuesta no ve sus observaciones")
    void listar_ajeno_rechaza() {
        autenticar(9L, Persona.builder().id(99L).build(), "otro@ufps.edu.co");
        propuesta(EstadoPropuesta.ajustes);
        when(asignacionRepository.findDetalleByPropuestaId(50L)).thenReturn(List.of());
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(50L, 99L)).thenReturn(false);
        when(versionRepository.findFirstByPropuesta_IdOrderByNumeroVersionAsc(50L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.listar(50L));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 3: disponibles para la agenda
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 3: las propuestas con versión definitiva quedan disponibles para programar en la agenda")
    void criterio3_listarProgramables() {
        Propuesta p = propuesta(EstadoPropuesta.aprobada);
        VersionPropuesta v2 = version(p, 2);
        v2.setEsDefinitiva(true);
        v2.setFechaDefinitiva(LocalDateTime.now());
        Propuesta otra = Propuesta.builder().id(51L).convocatoria(p.getConvocatoria()).titulo("Robótica")
                .estado(EstadoPropuesta.aprobada).build();
        VersionPropuesta otraV1 = VersionPropuesta.builder().id(201L).propuesta(otra).numeroVersion(1)
                .esDefinitiva(true).fechaDefinitiva(LocalDateTime.now()).build();
        Persona ponente = Persona.builder().id(11L).nombres("Ana").apellidos("Pérez").build();
        when(eventoRepository.existsById(2L)).thenReturn(true);
        when(versionRepository.findDefinitivasDelEvento(2L)).thenReturn(List.of(v2, otraV1));
        when(propuestaAutorRepository.findByPropuestaIdsConPersona(List.of(50L, 51L))).thenReturn(List.of(
                PropuestaAutor.builder().propuesta(p).persona(personaAutor).rol(RolAutor.autor_principal).orden(1).build(),
                PropuestaAutor.builder().propuesta(p).persona(ponente).rol(RolAutor.ponente).orden(2).build()));
        when(actividadRepository.findByEvento_IdAndPropuestaIsNotNull(2L)).thenReturn(List.of(
                Actividad.builder().id(77L).propuesta(otra).estado(Actividad.ESTADO_PROGRAMADA).build(),
                Actividad.builder().id(78L).propuesta(p).estado(Actividad.ESTADO_CANCELADA).build()));

        List<PropuestaProgramableResponse> todas = service.listarProgramables(2L, false);

        assertEquals(2, todas.size());
        PropuestaProgramableResponse primera = todas.get(0);
        assertEquals(50L, primera.propuestaId());
        assertEquals(2, primera.numeroVersion());
        assertEquals(List.of("Carlos Gómez", "Ana Pérez"), primera.autores());
        assertEquals(11L, primera.ponenteSugeridoId());
        assertFalse(primera.programada()); // la actividad cancelada no cuenta
        assertTrue(todas.get(1).programada());
        assertEquals(List.of(77L), todas.get(1).actividadIds());

        List<PropuestaProgramableResponse> sinProgramar = service.listarProgramables(2L, true);
        assertEquals(List.of(50L), sinProgramar.stream().map(PropuestaProgramableResponse::propuestaId).toList());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Al aprobar la versión definitiva, el ponente queda registrado en el evento (para programarlo en la agenda)
    // ──────────────────────────────────────────────────────────────────────────────

    private Propuesta aprobablePorAdmin() {
        comoAdmin();
        Propuesta p = propuesta(EstadoPropuesta.aprobada);
        clasificada(p, ClasificacionResultado.aprobada);
        VersionPropuesta v1 = version(p, 1);
        versionActual(p, v1);
        when(observacionRepository.findDePropuesta(50L)).thenReturn(List.of());
        when(versionRepository.save(any(VersionPropuesta.class))).thenAnswer(inv -> inv.getArgument(0));
        return p;
    }

    @Test
    @DisplayName("Al aprobar la versión definitiva, el ponente de la propuesta queda registrado en el evento")
    void aprobar_registraPonenteEnEvento() {
        Propuesta p = aprobablePorAdmin();
        Persona ponente = Persona.builder().id(11L).nombres("Lucía").apellidos("Pardo").build();
        when(propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(50L)).thenReturn(List.of(
                PropuestaAutor.builder().propuesta(p).persona(personaAutor).rol(RolAutor.autor_principal).orden(1).build(),
                PropuestaAutor.builder().propuesta(p).persona(ponente).rol(RolAutor.ponente).orden(2).build()));
        when(participacionRepository.existsByEvento_IdAndPersona_Id(2L, 11L)).thenReturn(false);

        service.aprobarVersionDefinitiva(50L);

        org.mockito.ArgumentCaptor<ParticipacionConferencista> registro =
                org.mockito.ArgumentCaptor.forClass(ParticipacionConferencista.class);
        verify(participacionRepository).save(registro.capture());
        assertSame(ponente, registro.getValue().getPersona(), "Se registra al autor con rol ponente");
        assertEquals(2L, registro.getValue().getEvento().getId());
        assertEquals(TipoParticipacionConferencista.ponente, registro.getValue().getTipoParticipacion());
        assertEquals("IA en la Educación", registro.getValue().getTema());
    }

    @Test
    @DisplayName("Sin autor con rol ponente se registra al autor principal")
    void aprobar_sinPonente_registraAutorPrincipal() {
        Propuesta p = aprobablePorAdmin();
        when(propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(50L)).thenReturn(List.of(
                PropuestaAutor.builder().propuesta(p).persona(personaAutor).rol(RolAutor.autor_principal).orden(1).build()));
        when(participacionRepository.existsByEvento_IdAndPersona_Id(2L, 10L)).thenReturn(false);

        service.aprobarVersionDefinitiva(50L);

        org.mockito.ArgumentCaptor<ParticipacionConferencista> registro =
                org.mockito.ArgumentCaptor.forClass(ParticipacionConferencista.class);
        verify(participacionRepository).save(registro.capture());
        assertSame(personaAutor, registro.getValue().getPersona());
    }

    @Test
    @DisplayName("Si el ponente ya participa en el evento no se duplica")
    void aprobar_ponenteYaRegistrado_noDuplica() {
        Propuesta p = aprobablePorAdmin();
        when(propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(50L)).thenReturn(List.of(
                PropuestaAutor.builder().propuesta(p).persona(personaAutor).rol(RolAutor.ponente).orden(1).build()));
        when(participacionRepository.existsByEvento_IdAndPersona_Id(2L, 10L)).thenReturn(true);

        service.aprobarVersionDefinitiva(50L);

        verify(participacionRepository, never()).save(any());
    }
}
