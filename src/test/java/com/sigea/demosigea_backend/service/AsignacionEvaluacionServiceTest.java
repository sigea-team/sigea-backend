package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.asignacion.AsignacionResponse;
import com.sigea.demosigea_backend.dto.asignacion.AsignarEvaluadoresRequest;
import com.sigea.demosigea_backend.dto.asignacion.EvaluadorDisponibleResponse;
import com.sigea.demosigea_backend.dto.asignacion.HistorialAsignacionResponse;
import com.sigea.demosigea_backend.dto.asignacion.ReasignarEvaluadorRequest;
import com.sigea.demosigea_backend.exception.AsignacionDuplicadaException;
import com.sigea.demosigea_backend.exception.ConfirmacionRequeridaException;
import com.sigea.demosigea_backend.exception.ConflictoInteresException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Afiliacion;
import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.ComiteEvaluador;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoAsignacion;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.HistorialAsignacion;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.PropuestaAutor;
import com.sigea.demosigea_backend.model.RolAutor;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.TipoOperacionAsignacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.ComiteEvaluadorRepository;
import com.sigea.demosigea_backend.repository.EvaluacionRepository;
import com.sigea.demosigea_backend.repository.HistorialAsignacionRepository;
import com.sigea.demosigea_backend.repository.PropuestaAutorRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AsignacionEvaluacionService} (HU-16).
 */
@ExtendWith(MockitoExtension.class)
class AsignacionEvaluacionServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 25, 10, 0);
    private static final String CORREO_ORGANIZADOR = "organizador@ufps.edu.co";

    @Mock private AsignacionEvaluacionRepository asignacionRepository;
    @Mock private HistorialAsignacionRepository historialRepository;
    @Mock private ComiteEvaluadorRepository comiteEvaluadorRepository;
    @Mock private PropuestaRepository propuestaRepository;
    @Mock private PropuestaAutorRepository propuestaAutorRepository;
    @Mock private EvaluacionRepository evaluacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private AuditoriaService auditoriaService;

    private AsignacionEvaluacionService service;

    private Convocatoria convocatoria;
    private Propuesta propuesta;
    private Afiliacion ufps;
    private Afiliacion otraU;
    private Persona autor;
    private ComiteEvaluador evaluadorA;
    private ComiteEvaluador evaluadorB;
    private Usuario usuarioA;
    private Usuario usuarioB;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
        service = new AsignacionEvaluacionService(asignacionRepository, historialRepository,
                comiteEvaluadorRepository, propuestaRepository, propuestaAutorRepository, evaluacionRepository,
                usuarioRepository, notificacionService, auditoriaService, reloj);

        ufps = Afiliacion.builder().id(1L).nombreAfiliacion("UFPS").build();
        otraU = Afiliacion.builder().id(2L).nombreAfiliacion("UIS").build();

        convocatoria = Convocatoria.builder().id(10L).titulo("Convocatoria de Ponencias 2026").build();
        propuesta = Propuesta.builder().id(40L).convocatoria(convocatoria).titulo("Analítica educativa")
                .estado(EstadoPropuesta.recibida).build();

        autor = persona(30L, "Carlos", "Ruiz", ufps);
        evaluadorA = evaluador(3L, persona(21L, "Ana", "Pérez", otraU), convocatoria);
        evaluadorB = evaluador(5L, persona(22L, "Luis", "Gómez", otraU), convocatoria);
        usuarioA = Usuario.builder().id(100L).persona(evaluadorA.getPersona()).build();
        usuarioB = Usuario.builder().id(101L).persona(evaluadorB.getPersona()).build();
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static Persona persona(Long id, String nombres, String apellidos, Afiliacion afiliacion) {
        return Persona.builder().id(id).nombres(nombres).apellidos(apellidos)
                .correo(nombres.toLowerCase() + "@ufps.edu.co").afiliacion(afiliacion).build();
    }

    private static ComiteEvaluador evaluador(Long id, Persona persona, Convocatoria convocatoria) {
        return ComiteEvaluador.builder().id(id).persona(persona).convocatoria(convocatoria)
                .areaExperticia("Ingeniería de software").build();
    }

    private PropuestaAutor autoria(Persona persona, RolAutor rol) {
        return PropuestaAutor.builder().propuesta(propuesta).persona(persona).rol(rol).orden(1).build();
    }

    private void autoresDeLaPropuesta(PropuestaAutor... autores) {
        when(propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(40L)).thenReturn(List.of(autores));
    }

    private void evaluadorExiste(ComiteEvaluador ce) {
        when(comiteEvaluadorRepository.findDetalleById(ce.getId())).thenReturn(Optional.of(ce));
    }

    private void guardarAsignacionesConId() {
        when(asignacionRepository.saveAndFlush(any(AsignacionEvaluacion.class))).thenAnswer(inv -> {
            AsignacionEvaluacion a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(500L + a.getComiteEvaluador().getId());
            }
            return a;
        });
    }

    private AsignacionEvaluacion asignacionExistente(ComiteEvaluador ce, EstadoAsignacion estado) {
        return AsignacionEvaluacion.builder().id(12L).propuesta(propuesta).comiteEvaluador(ce)
                .fechaAsignacion(AHORA.minusDays(2)).estado(estado).build();
    }

    // ------------------------------------------------------------------
    // Criterio 1: asignar y notificar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("HU-16 Criterio 1: asigna a varios evaluadores, notifica a cada uno, registra historial y pasa la propuesta a en_evaluacion")
    void asignar_variosEvaluadores_notificaYRegistraHistorial() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CORREO_ORGANIZADOR, null, List.of()));
        Usuario organizador = Usuario.builder().id(1L).persona(persona(1L, "María", "Rodríguez", ufps)).build();
        when(usuarioRepository.findByPersona_CorreoIgnoreCase(CORREO_ORGANIZADOR)).thenReturn(Optional.of(organizador));
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        autoresDeLaPropuesta(autoria(autor, RolAutor.autor_principal));
        evaluadorExiste(evaluadorA);
        evaluadorExiste(evaluadorB);
        guardarAsignacionesConId();
        when(usuarioRepository.findByPersona_Id(21L)).thenReturn(Optional.of(usuarioA));
        when(usuarioRepository.findByPersona_Id(22L)).thenReturn(Optional.of(usuarioB));

        List<AsignacionResponse> respuesta = service.asignar(40L,
                new AsignarEvaluadoresRequest(List.of(3L, 5L, 3L), null));

        assertEquals(2, respuesta.size(), "Los IDs repetidos se asignan una sola vez");
        assertEquals("pendiente", respuesta.get(0).estado());
        assertEquals("Ana Pérez", respuesta.get(0).evaluadorNombre());
        assertEquals(AHORA, respuesta.get(0).fechaAsignacion());
        assertTrue(respuesta.get(0).advertencias().isEmpty());
        assertEquals(EstadoPropuesta.en_evaluacion, propuesta.getEstado());
        verify(propuestaRepository).save(propuesta);

        verify(notificacionService).notificarAUsuario(eq(usuarioA), eq(TipoNotificacion.PROPUESTA_ASIGNADA),
                anyString(), anyString(), eq("asignaciones_evaluacion"), eq(503L));
        verify(notificacionService).notificarAUsuario(eq(usuarioB), eq(TipoNotificacion.PROPUESTA_ASIGNADA),
                anyString(), anyString(), eq("asignaciones_evaluacion"), eq(505L));

        ArgumentCaptor<HistorialAsignacion> historial = ArgumentCaptor.forClass(HistorialAsignacion.class);
        verify(historialRepository, times(2)).save(historial.capture());
        HistorialAsignacion primero = historial.getAllValues().get(0);
        assertEquals(TipoOperacionAsignacion.asignacion, primero.getTipoOperacion());
        assertNull(primero.getEvaluadorAnterior());
        assertEquals(evaluadorA, primero.getEvaluadorNuevo());
        assertEquals(organizador, primero.getUsuario());
        assertFalse(primero.getConflictoAdvertido());

        verify(auditoriaService, times(2)).registrar(eq(TipoOperacionAuditoria.PROPUESTA_ASIGNADA),
                eq("asignaciones_evaluacion"), anyLong(), anyMap());
    }

    @Test
    @DisplayName("HU-16 Criterio 1: si el evaluador no tiene cuenta, la asignación se guarda sin notificación")
    void asignar_evaluadorSinCuenta_noNotifica() {
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        autoresDeLaPropuesta();
        evaluadorExiste(evaluadorA);
        guardarAsignacionesConId();
        when(usuarioRepository.findByPersona_Id(21L)).thenReturn(Optional.empty());

        List<AsignacionResponse> respuesta = service.asignar(40L, new AsignarEvaluadoresRequest(List.of(3L), false));

        assertEquals(1, respuesta.size());
        verify(notificacionService, never()).notificarAUsuario(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("HU-16: la propuesta ya en evaluación conserva su estado al sumar evaluadores")
    void asignar_propuestaEnEvaluacion_noCambiaEstado() {
        propuesta.setEstado(EstadoPropuesta.en_evaluacion);
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        autoresDeLaPropuesta();
        evaluadorExiste(evaluadorA);
        guardarAsignacionesConId();
        when(usuarioRepository.findByPersona_Id(21L)).thenReturn(Optional.of(usuarioA));

        service.asignar(40L, new AsignarEvaluadoresRequest(List.of(3L), false));

        verify(propuestaRepository, never()).save(any(Propuesta.class));
    }

    @Test
    @DisplayName("HU-16: no se asigna una propuesta aprobada o rechazada")
    void asignar_propuestaNoAsignable_lanzaOperacionNoPermitida() {
        propuesta.setEstado(EstadoPropuesta.aprobada);
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));

        assertThrows(OperacionNoPermitidaException.class,
                () -> service.asignar(40L, new AsignarEvaluadoresRequest(List.of(3L), false)));
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("HU-16: propuesta inexistente responde no encontrada")
    void asignar_propuestaInexistente_lanzaNoEncontrado() {
        when(propuestaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.asignar(99L, new AsignarEvaluadoresRequest(List.of(3L), false)));
    }

    @Test
    @DisplayName("HU-16: un evaluador de otra convocatoria no puede recibir la propuesta")
    void asignar_evaluadorDeOtraConvocatoria_lanzaSolicitudInvalida() {
        ComiteEvaluador ajeno = evaluador(9L, persona(29L, "Pedro", "Díaz", otraU),
                Convocatoria.builder().id(77L).titulo("Otra").build());
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        autoresDeLaPropuesta();
        evaluadorExiste(ajeno);

        assertThrows(SolicitudInvalidaException.class,
                () -> service.asignar(40L, new AsignarEvaluadoresRequest(List.of(9L), false)));
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("HU-16: no se repite la asignación de la misma propuesta al mismo evaluador")
    void asignar_yaAsignado_lanzaAsignacionDuplicada() {
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        autoresDeLaPropuesta();
        evaluadorExiste(evaluadorA);
        when(asignacionRepository.existsByPropuesta_IdAndComiteEvaluador_Id(40L, 3L)).thenReturn(true);

        assertThrows(AsignacionDuplicadaException.class,
                () -> service.asignar(40L, new AsignarEvaluadoresRequest(List.of(3L), false)));
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    // ------------------------------------------------------------------
    // Criterio 2: conflicto de interés
    // ------------------------------------------------------------------

    @Test
    @DisplayName("HU-16 Criterio 2: si el evaluador es autor de la propuesta, se impide la asignación (y no se guarda ninguna)")
    void asignar_evaluadorEsAutor_impideAsignacion() {
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        autoresDeLaPropuesta(autoria(autor, RolAutor.autor_principal), autoria(evaluadorB.getPersona(), RolAutor.coautor));
        evaluadorExiste(evaluadorA);
        evaluadorExiste(evaluadorB);

        ConflictoInteresException ex = assertThrows(ConflictoInteresException.class,
                () -> service.asignar(40L, new AsignarEvaluadoresRequest(List.of(3L, 5L), true)));

        assertTrue(ex.getMessage().contains("Luis Gómez"));
        assertTrue(ex.getMessage().contains("coautor"));
        verify(asignacionRepository, never()).saveAndFlush(any());
        verify(notificacionService, never()).notificarAUsuario(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("HU-16 Criterio 2: si comparte afiliación con un autor, advierte y exige confirmación")
    void asignar_afiliacionCompartida_sinConfirmar_exigeConfirmacion() {
        ComiteEvaluador mismaU = evaluador(7L, persona(27L, "Sofía", "Mora", ufps), convocatoria);
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        autoresDeLaPropuesta(autoria(autor, RolAutor.autor_principal));
        evaluadorExiste(mismaU);

        ConfirmacionRequeridaException ex = assertThrows(ConfirmacionRequeridaException.class,
                () -> service.asignar(40L, new AsignarEvaluadoresRequest(List.of(7L), false)));

        assertTrue(ex.getMessage().contains("UFPS"));
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("HU-16 Criterio 2: confirmada la advertencia, asigna y deja el conflicto marcado en el historial")
    void asignar_afiliacionCompartida_confirmada_asignaConAdvertencia() {
        ComiteEvaluador mismaU = evaluador(7L, persona(27L, "Sofía", "Mora", ufps), convocatoria);
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        autoresDeLaPropuesta(autoria(autor, RolAutor.autor_principal));
        evaluadorExiste(mismaU);
        guardarAsignacionesConId();
        when(usuarioRepository.findByPersona_Id(27L)).thenReturn(Optional.empty());

        List<AsignacionResponse> respuesta = service.asignar(40L, new AsignarEvaluadoresRequest(List.of(7L), true));

        assertEquals(1, respuesta.get(0).advertencias().size());
        ArgumentCaptor<HistorialAsignacion> historial = ArgumentCaptor.forClass(HistorialAsignacion.class);
        verify(historialRepository).save(historial.capture());
        assertTrue(historial.getValue().getConflictoAdvertido());
    }

    @Test
    @DisplayName("HU-16 Criterio 2: evaluadores disponibles indica carga, si ya está asignado y el nivel de conflicto")
    void evaluadoresDisponibles_marcaConflictos() {
        ComiteEvaluador mismaU = evaluador(7L, persona(27L, "Sofía", "Mora", ufps), convocatoria);
        ComiteEvaluador esAutor = evaluador(8L, autor, convocatoria);
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        autoresDeLaPropuesta(autoria(autor, RolAutor.autor_principal));
        when(comiteEvaluadorRepository.findByConvocatoriaConPersona(10L)).thenReturn(List.of(evaluadorA, mismaU, esAutor));
        when(asignacionRepository.countByComiteEvaluador_IdAndEstadoNot(anyLong(), eq(EstadoAsignacion.completada)))
                .thenReturn(2L);
        when(asignacionRepository.existsByPropuesta_IdAndComiteEvaluador_Id(eq(40L), anyLong())).thenReturn(false);
        when(asignacionRepository.existsByPropuesta_IdAndComiteEvaluador_Id(40L, 3L)).thenReturn(true);

        Map<Long, EvaluadorDisponibleResponse> porId = service.evaluadoresDisponibles(40L).stream()
                .collect(java.util.stream.Collectors.toMap(EvaluadorDisponibleResponse::comiteEvaluadorId, e -> e));

        assertEquals("NINGUNO", porId.get(3L).conflicto());
        assertTrue(porId.get(3L).yaAsignado());
        assertEquals(2L, porId.get(3L).asignacionesAbiertas());
        assertEquals("ADVERTENCIA", porId.get(7L).conflicto());
        assertEquals("BLOQUEANTE", porId.get(8L).conflicto());
    }

    // ------------------------------------------------------------------
    // Criterio 3: reasignar conservando historial
    // ------------------------------------------------------------------

    @Test
    @DisplayName("HU-16 Criterio 3: reasigna a otro evaluador, actualiza la asignación y conserva el historial")
    void reasignar_actualizaAsignacionYRegistraHistorial() {
        AsignacionEvaluacion asignacion = asignacionExistente(evaluadorA, EstadoAsignacion.en_proceso);
        when(asignacionRepository.findById(12L)).thenReturn(Optional.of(asignacion));
        when(evaluacionRepository.existsByAsignacion_Id(12L)).thenReturn(false);
        evaluadorExiste(evaluadorB);
        autoresDeLaPropuesta(autoria(autor, RolAutor.autor_principal));
        guardarAsignacionesConId();
        when(usuarioRepository.findByPersona_Id(21L)).thenReturn(Optional.of(usuarioA));
        when(usuarioRepository.findByPersona_Id(22L)).thenReturn(Optional.of(usuarioB));

        AsignacionResponse respuesta = service.reasignar(12L,
                new ReasignarEvaluadorRequest(5L, "  Sin disponibilidad  ", null));

        assertEquals(12L, respuesta.asignacionId(), "La asignación conserva su ID");
        assertEquals(5L, respuesta.comiteEvaluadorId());
        assertEquals("pendiente", respuesta.estado());
        assertEquals(AHORA, respuesta.fechaAsignacion());

        ArgumentCaptor<HistorialAsignacion> historial = ArgumentCaptor.forClass(HistorialAsignacion.class);
        verify(historialRepository).save(historial.capture());
        HistorialAsignacion h = historial.getValue();
        assertEquals(TipoOperacionAsignacion.reasignacion, h.getTipoOperacion());
        assertEquals(evaluadorA, h.getEvaluadorAnterior());
        assertEquals(evaluadorB, h.getEvaluadorNuevo());
        assertEquals("Sin disponibilidad", h.getMotivo());

        verify(notificacionService).notificarAUsuario(eq(usuarioB), eq(TipoNotificacion.PROPUESTA_ASIGNADA),
                anyString(), anyString(), eq("asignaciones_evaluacion"), eq(12L));
        verify(notificacionService).notificarAUsuario(eq(usuarioA), eq(TipoNotificacion.ASIGNACION_RETIRADA),
                anyString(), anyString(), eq("asignaciones_evaluacion"), eq(12L));
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.PROPUESTA_REASIGNADA),
                eq("asignaciones_evaluacion"), eq(12L), anyMap());
    }

    @Test
    @DisplayName("HU-16 Criterio 3: no se reasigna una evaluación ya completada")
    void reasignar_completada_lanzaOperacionNoPermitida() {
        when(asignacionRepository.findById(12L))
                .thenReturn(Optional.of(asignacionExistente(evaluadorA, EstadoAsignacion.completada)));

        assertThrows(OperacionNoPermitidaException.class,
                () -> service.reasignar(12L, new ReasignarEvaluadorRequest(5L, null, false)));
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-16 Criterio 3: no se reasigna si el evaluador ya registró calificaciones")
    void reasignar_conCalificaciones_lanzaOperacionNoPermitida() {
        when(asignacionRepository.findById(12L))
                .thenReturn(Optional.of(asignacionExistente(evaluadorA, EstadoAsignacion.pendiente)));
        when(evaluacionRepository.existsByAsignacion_Id(12L)).thenReturn(true);

        assertThrows(OperacionNoPermitidaException.class,
                () -> service.reasignar(12L, new ReasignarEvaluadorRequest(5L, null, false)));
    }

    @Test
    @DisplayName("HU-16 Criterio 3: reasignar al mismo evaluador es una solicitud inválida")
    void reasignar_mismoEvaluador_lanzaSolicitudInvalida() {
        when(asignacionRepository.findById(12L))
                .thenReturn(Optional.of(asignacionExistente(evaluadorA, EstadoAsignacion.pendiente)));
        when(evaluacionRepository.existsByAsignacion_Id(12L)).thenReturn(false);

        assertThrows(SolicitudInvalidaException.class,
                () -> service.reasignar(12L, new ReasignarEvaluadorRequest(3L, null, false)));
    }

    @Test
    @DisplayName("HU-16 Criterio 3: no se reasigna a un evaluador que ya tiene la propuesta")
    void reasignar_nuevoYaAsignado_lanzaAsignacionDuplicada() {
        when(asignacionRepository.findById(12L))
                .thenReturn(Optional.of(asignacionExistente(evaluadorA, EstadoAsignacion.pendiente)));
        when(evaluacionRepository.existsByAsignacion_Id(12L)).thenReturn(false);
        evaluadorExiste(evaluadorB);
        when(asignacionRepository.existsByPropuesta_IdAndComiteEvaluador_Id(40L, 5L)).thenReturn(true);

        assertThrows(AsignacionDuplicadaException.class,
                () -> service.reasignar(12L, new ReasignarEvaluadorRequest(5L, null, false)));
    }

    @Test
    @DisplayName("HU-16 Criterios 2 y 3: no se reasigna a un evaluador que es autor de la propuesta")
    void reasignar_nuevoEsAutor_lanzaConflictoInteres() {
        when(asignacionRepository.findById(12L))
                .thenReturn(Optional.of(asignacionExistente(evaluadorA, EstadoAsignacion.pendiente)));
        when(evaluacionRepository.existsByAsignacion_Id(12L)).thenReturn(false);
        evaluadorExiste(evaluadorB);
        autoresDeLaPropuesta(autoria(evaluadorB.getPersona(), RolAutor.ponente));

        assertThrows(ConflictoInteresException.class,
                () -> service.reasignar(12L, new ReasignarEvaluadorRequest(5L, null, true)));
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("HU-16: reasignar una asignación inexistente responde no encontrada")
    void reasignar_inexistente_lanzaNoEncontrado() {
        when(asignacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.reasignar(99L, new ReasignarEvaluadorRequest(5L, null, false)));
    }

    // ------------------------------------------------------------------
    // Consultas: historial y bandeja
    // ------------------------------------------------------------------

    @Test
    @DisplayName("HU-16 Criterio 3: el historial de la propuesta muestra evaluador anterior y nuevo")
    void historial_mapeaMovimientos() {
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        HistorialAsignacion h = HistorialAsignacion.builder().id(8L)
                .asignacion(asignacionExistente(evaluadorB, EstadoAsignacion.pendiente)).propuesta(propuesta)
                .tipoOperacion(TipoOperacionAsignacion.reasignacion).evaluadorAnterior(evaluadorA)
                .evaluadorNuevo(evaluadorB).conflictoAdvertido(false).motivo("Sin disponibilidad")
                .fechaHora(AHORA).build();
        when(historialRepository.findHistorialDePropuesta(40L)).thenReturn(List.of(h));

        List<HistorialAsignacionResponse> historial = service.historialPorPropuesta(40L);

        assertEquals(1, historial.size());
        assertEquals("reasignacion", historial.get(0).tipoOperacion());
        assertEquals("Ana Pérez", historial.get(0).evaluadorAnteriorNombre());
        assertEquals("Luis Gómez", historial.get(0).evaluadorNuevoNombre());
    }

    private void sesionDeAna() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ana@ufps.edu.co", null, List.of()));
        when(usuarioRepository.findByPersona_CorreoIgnoreCase("ana@ufps.edu.co")).thenReturn(Optional.of(usuarioA));
    }

    @Test
    @DisplayName("HU-16 Criterio 1: por defecto la bandeja muestra solo el trabajo actual (excluye completadas)")
    void miBandeja_porDefecto_excluyeCompletadas() {
        sesionDeAna();
        when(asignacionRepository.findBandejaByPersonaIdAndEstadoNot(21L, EstadoAsignacion.completada))
                .thenReturn(List.of(asignacionExistente(evaluadorA, EstadoAsignacion.pendiente)));

        List<AsignacionResponse> bandeja = service.miBandeja(false);

        assertEquals(1, bandeja.size());
        assertEquals("Analítica educativa", bandeja.get(0).propuestaTitulo());
        assertEquals("Convocatoria de Ponencias 2026", bandeja.get(0).convocatoriaTitulo());
        verify(asignacionRepository, never()).findBandejaByPersonaId(anyLong());
    }

    @Test
    @DisplayName("HU-16 Criterio 1: con incluirCompletadas la bandeja también muestra las completadas")
    void miBandeja_incluirCompletadas_listaTodas() {
        sesionDeAna();
        when(asignacionRepository.findBandejaByPersonaId(21L)).thenReturn(List.of(
                asignacionExistente(evaluadorA, EstadoAsignacion.pendiente),
                asignacionExistente(evaluadorA, EstadoAsignacion.completada)));

        List<AsignacionResponse> bandeja = service.miBandeja(true);

        assertEquals(2, bandeja.size());
        assertEquals("completada", bandeja.get(1).estado());
        verify(asignacionRepository, never()).findBandejaByPersonaIdAndEstadoNot(anyLong(), any());
    }

    @Test
    @DisplayName("HU-16: la bandeja exige sesión iniciada")
    void miBandeja_sinSesion_lanzaAccesoDenegado() {
        assertThrows(AccessDeniedException.class, () -> service.miBandeja(false));
    }

    // ------------------------------------------------------------------
    // Concurrencia: el índice único responde 409, no 500
    // ------------------------------------------------------------------

    private static DataIntegrityViolationException violacionIndiceUnico() {
        ConstraintViolationException violacion = new ConstraintViolationException("duplicado",
                new SQLException("duplicate key", AsignacionEvaluacionService.SQLSTATE_VIOLACION_UNICIDAD),
                AsignacionEvaluacionService.INDICE_ASIGNACION_UNICA);
        return new DataIntegrityViolationException("dup", violacion);
    }

    @Test
    @DisplayName("HU-16: dos asignaciones simultáneas detectadas por el índice único responden 409, no 500")
    void asignar_carreraDetectadaPorIndice_lanzaAsignacionDuplicada() {
        when(propuestaRepository.findById(40L)).thenReturn(Optional.of(propuesta));
        autoresDeLaPropuesta();
        evaluadorExiste(evaluadorA);
        when(asignacionRepository.saveAndFlush(any(AsignacionEvaluacion.class))).thenThrow(violacionIndiceUnico());

        assertThrows(AsignacionDuplicadaException.class,
                () -> service.asignar(40L, new AsignarEvaluadoresRequest(List.of(3L), false)));
        verify(historialRepository, never()).save(any());
        verify(notificacionService, never()).notificarAUsuario(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("HU-16: una reasignación simultánea detectada por el índice único responde 409, no 500")
    void reasignar_carreraDetectadaPorIndice_lanzaAsignacionDuplicada() {
        when(asignacionRepository.findById(12L))
                .thenReturn(Optional.of(asignacionExistente(evaluadorA, EstadoAsignacion.pendiente)));
        when(evaluacionRepository.existsByAsignacion_Id(12L)).thenReturn(false);
        evaluadorExiste(evaluadorB);
        autoresDeLaPropuesta();
        when(asignacionRepository.saveAndFlush(any(AsignacionEvaluacion.class))).thenThrow(violacionIndiceUnico());

        assertThrows(AsignacionDuplicadaException.class,
                () -> service.reasignar(12L, new ReasignarEvaluadorRequest(5L, null, false)));
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-16: otras violaciones de integridad no se confunden con una asignación duplicada")
    void esViolacionAsignacionUnica_otraRestriccion_false() {
        ConstraintViolationException otra = new ConstraintViolationException("fk",
                new SQLException("fk", "23503"), "asignaciones_evaluacion_comite_evaluador_id_fkey");
        assertFalse(AsignacionEvaluacionService.esViolacionAsignacionUnica(
                new DataIntegrityViolationException("x", otra)));
        assertTrue(AsignacionEvaluacionService.esViolacionAsignacionUnica(violacionIndiceUnico()));
    }
}
