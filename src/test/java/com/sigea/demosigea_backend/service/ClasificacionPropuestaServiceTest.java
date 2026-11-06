package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.clasificacion.AjusteClasificacionRequest;
import com.sigea.demosigea_backend.dto.clasificacion.AplicacionClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.ClasificacionConvocatoriaResponse;
import com.sigea.demosigea_backend.dto.clasificacion.PropuestaClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.UmbralesClasificacionRequest;
import com.sigea.demosigea_backend.dto.clasificacion.UmbralesClasificacionResponse;
import com.sigea.demosigea_backend.exception.PropuestaNoEvaluadaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.ClasificacionResultado;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoAsignacion;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.HistorialClasificacion;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.ResultadoEvaluacion;
import com.sigea.demosigea_backend.model.TipoClasificacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.HistorialClasificacionRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.ResultadoEvaluacionRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ClasificacionPropuestaService} (HU-18).
 */
@ExtendWith(MockitoExtension.class)
class ClasificacionPropuestaServiceTest {

    private static final String CORREO_ADMIN = "admin@ufps.edu.co";

    @Mock private PropuestaRepository propuestaRepository;
    @Mock private ConvocatoriaRepository convocatoriaRepository;
    @Mock private AsignacionEvaluacionRepository asignacionRepository;
    @Mock private ResultadoEvaluacionRepository resultadoRepository;
    @Mock private HistorialClasificacionRepository historialRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private ClasificacionPropuestaService service;

    private Usuario admin;

    @BeforeEach
    void autenticarAdministrador() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CORREO_ADMIN, null, List.of(new SimpleGrantedAuthority("PROPUESTAS_CLASIFICAR"))));
        admin = Usuario.builder().id(1L)
                .persona(Persona.builder().id(1L).nombres("María").apellidos("Rodríguez").build())
                .build();
        lenient().when(usuarioRepository.findByPersona_CorreoIgnoreCase(CORREO_ADMIN)).thenReturn(Optional.of(admin));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Datos de prueba
    // ──────────────────────────────────────────────────────────────────────────────

    private static Convocatoria convocatoria() {
        return Convocatoria.builder().id(10L).titulo("Convocatoria 2026").build(); // umbrales 7 y 5
    }

    private static Propuesta propuesta(Long id, Convocatoria convocatoria) {
        return Propuesta.builder().id(id).convocatoria(convocatoria).titulo("Propuesta " + id)
                .estado(EstadoPropuesta.en_evaluacion).build();
    }

    private static AsignacionEvaluacion asignacion(Long id, Propuesta propuesta, EstadoAsignacion estado) {
        return AsignacionEvaluacion.builder().id(id).propuesta(propuesta).estado(estado).build();
    }

    private static ResultadoEvaluacion resultado(Long id, Propuesta propuesta, String puntuacion,
                                                 ClasificacionResultado clasificacion) {
        return ResultadoEvaluacion.builder().id(id).propuesta(propuesta)
                .puntuacionPonderada(new BigDecimal(puntuacion)).clasificacion(clasificacion)
                .fechaCalculo(LocalDateTime.now()).build();
    }

    private void propuestaEvaluada(Propuesta propuesta, ResultadoEvaluacion resultado) {
        when(propuestaRepository.findByIdParaActualizar(propuesta.getId())).thenReturn(Optional.of(propuesta));
        when(asignacionRepository.findByPropuesta_Id(propuesta.getId())).thenReturn(List.of(
                asignacion(1L, propuesta, EstadoAsignacion.completada),
                asignacion(2L, propuesta, EstadoAsignacion.completada)));
        when(resultadoRepository.findByPropuesta_Id(propuesta.getId())).thenReturn(Optional.ofNullable(resultado));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 1
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 1: con puntuación calculada asigna automáticamente la clasificación y el estado")
    void criterio1_aplicaClasificacionAutomatica() {
        Propuesta propuesta = propuesta(40L, convocatoria());
        ResultadoEvaluacion resultado = resultado(8L, propuesta, "6.40", ClasificacionResultado.aprobada_con_ajustes);
        propuestaEvaluada(propuesta, resultado);

        PropuestaClasificacionResponse respuesta = service.aplicar(40L);

        assertEquals(EstadoPropuesta.ajustes, propuesta.getEstado());
        assertEquals(ClasificacionResultado.aprobada_con_ajustes, resultado.getClasificacion());
        assertNotNull(resultado.getFechaClasificacion());
        assertTrue(respuesta.aplicada());
        assertEquals("Aceptada con modificaciones", respuesta.etiqueta());

        ArgumentCaptor<HistorialClasificacion> captor = ArgumentCaptor.forClass(HistorialClasificacion.class);
        verify(historialRepository).save(captor.capture());
        HistorialClasificacion h = captor.getValue();
        assertEquals(TipoClasificacion.automatica, h.getTipo());
        assertNull(h.getClasificacionAnterior());
        assertEquals(ClasificacionResultado.aprobada_con_ajustes, h.getClasificacionNueva());
        assertEquals(0, new BigDecimal("7").compareTo(h.getUmbralAceptacion()));
        assertEquals(admin, h.getUsuario());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.PROPUESTA_CLASIFICADA),
                eq("resultados_evaluacion"), eq(8L), anyMap(), eq(admin));
    }

    @Test
    @DisplayName("Criterio 1: usa los umbrales definidos en la convocatoria")
    void criterio1_usaUmbralesDeLaConvocatoria() {
        Convocatoria convocatoria = convocatoria();
        convocatoria.setUmbralAceptacion(new BigDecimal("8.50"));
        convocatoria.setUmbralAjustes(new BigDecimal("6.00"));
        Propuesta propuesta = propuesta(40L, convocatoria);
        // HU-17 la había calculado como aprobada con los umbrales anteriores
        ResultadoEvaluacion resultado = resultado(8L, propuesta, "7.60", ClasificacionResultado.aprobada);
        propuestaEvaluada(propuesta, resultado);

        service.aplicar(40L);

        assertEquals(ClasificacionResultado.aprobada_con_ajustes, resultado.getClasificacion());
        assertEquals(EstadoPropuesta.ajustes, propuesta.getEstado());
    }

    @Test
    @DisplayName("Criterio 1: si la clasificación ya estaba aplicada no duplica el historial")
    void criterio1_yaAplicada_noRegistraOtraVez() {
        Propuesta propuesta = propuesta(40L, convocatoria());
        propuesta.setEstado(EstadoPropuesta.aprobada);
        ResultadoEvaluacion resultado = resultado(8L, propuesta, "8.00", ClasificacionResultado.aprobada);
        resultado.setFechaClasificacion(LocalDateTime.now().minusDays(1));
        propuestaEvaluada(propuesta, resultado);

        PropuestaClasificacionResponse respuesta = service.aplicar(40L);

        assertTrue(respuesta.aplicada());
        verify(historialRepository, never()).save(any());
        verify(resultadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 1: al reaplicar respeta un ajuste manual previo")
    void criterio1_respetaAjusteManual() {
        Propuesta propuesta = propuesta(40L, convocatoria());
        propuesta.setEstado(EstadoPropuesta.en_evaluacion); // p. ej. tras volver a evaluación
        ResultadoEvaluacion resultado = resultado(8L, propuesta, "8.00", ClasificacionResultado.rechazada);
        resultado.setAjusteManual(true);
        propuestaEvaluada(propuesta, resultado);

        service.aplicar(40L);

        assertEquals(ClasificacionResultado.rechazada, resultado.getClasificacion());
        assertEquals(EstadoPropuesta.rechazada, propuesta.getEstado());
        verify(historialRepository, never()).save(any()); // el ajuste ya quedó en el historial
    }

    @Test
    @DisplayName("Criterio 1 y 3: clasifica las propuestas evaluadas de la convocatoria y omite las incompletas")
    void aplicarConvocatoria_clasificaEvaluadasYOmiteIncompletas() {
        Convocatoria convocatoria = convocatoria();
        Propuesta evaluada = propuesta(40L, convocatoria);
        Propuesta pendiente = propuesta(41L, convocatoria);
        Propuesta sinEvaluadores = propuesta(42L, convocatoria);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(convocatoria));
        when(propuestaRepository.findByConvocatoriaIdParaActualizar(10L))
                .thenReturn(List.of(evaluada, pendiente, sinEvaluadores));
        when(asignacionRepository.findDetalleByPropuestaIds(List.of(40L, 41L, 42L))).thenReturn(List.of(
                asignacion(1L, evaluada, EstadoAsignacion.completada),
                asignacion(2L, pendiente, EstadoAsignacion.completada),
                asignacion(3L, pendiente, EstadoAsignacion.en_proceso)));
        ResultadoEvaluacion resultado = resultado(8L, evaluada, "4.20", ClasificacionResultado.rechazada);
        when(resultadoRepository.findByPropuesta_IdIn(List.of(40L, 41L, 42L))).thenReturn(List.of(resultado));

        AplicacionClasificacionResponse respuesta = service.aplicarConvocatoria(10L);

        assertEquals(1, respuesta.clasificadas().size());
        assertEquals("rechazada", respuesta.clasificadas().get(0).clasificacion());
        assertEquals(EstadoPropuesta.rechazada, evaluada.getEstado());
        assertEquals(2, respuesta.omitidas().size());
        assertTrue(respuesta.omitidas().get(0).motivo().contains("pendiente"));
        assertTrue(respuesta.omitidas().get(1).motivo().contains("no tiene evaluadores"));
        assertEquals(EstadoPropuesta.en_evaluacion, pendiente.getEstado());
        assertEquals(EstadoPropuesta.en_evaluacion, sinEvaluadores.getEstado());
    }

    @Test
    @DisplayName("Concurrencia: la aplicación masiva bloquea las propuestas antes de leer sus resultados")
    void aplicarConvocatoria_bloqueaPropuestasAntesDeLeerResultados() {
        Convocatoria convocatoria = convocatoria();
        Propuesta propuesta = propuesta(40L, convocatoria);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(convocatoria));
        when(propuestaRepository.findByConvocatoriaIdParaActualizar(10L)).thenReturn(List.of(propuesta));
        when(asignacionRepository.findDetalleByPropuestaIds(List.of(40L)))
                .thenReturn(List.of(asignacion(1L, propuesta, EstadoAsignacion.completada)));
        when(resultadoRepository.findByPropuesta_IdIn(List.of(40L)))
                .thenReturn(List.of(resultado(8L, propuesta, "8.00", ClasificacionResultado.aprobada)));

        service.aplicarConvocatoria(10L);

        InOrder orden = inOrder(propuestaRepository, resultadoRepository);
        orden.verify(propuestaRepository).findByConvocatoriaIdParaActualizar(10L);
        orden.verify(resultadoRepository).findByPropuesta_IdIn(List.of(40L));
        verify(propuestaRepository, never()).findByConvocatoria_IdOrderByFechaEnvioDesc(any());
    }

    @Test
    @DisplayName("Concurrencia: si otra solicitud ya aplicó la clasificación, la masiva no duplica historial ni auditoría")
    void aplicarConvocatoria_yaAplicadaPorOtraSolicitud_noDuplica() {
        Convocatoria convocatoria = convocatoria();
        Propuesta propuesta = propuesta(40L, convocatoria);
        propuesta.setEstado(EstadoPropuesta.aprobada); // estado que dejó la solicitud que obtuvo primero el bloqueo
        ResultadoEvaluacion resultado = resultado(8L, propuesta, "8.00", ClasificacionResultado.aprobada);
        resultado.setFechaClasificacion(LocalDateTime.now());
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(convocatoria));
        when(propuestaRepository.findByConvocatoriaIdParaActualizar(10L)).thenReturn(List.of(propuesta));
        when(asignacionRepository.findDetalleByPropuestaIds(List.of(40L)))
                .thenReturn(List.of(asignacion(1L, propuesta, EstadoAsignacion.completada)));
        when(resultadoRepository.findByPropuesta_IdIn(List.of(40L))).thenReturn(List.of(resultado));

        AplicacionClasificacionResponse respuesta = service.aplicarConvocatoria(10L);

        assertEquals(1, respuesta.clasificadas().size());
        assertTrue(respuesta.clasificadas().get(0).aplicada());
        verify(historialRepository, never()).save(any());
        verify(resultadoRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 2
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 2: el ajuste manual guarda el motivo en el historial y cambia el estado")
    void criterio2_ajusteManualConMotivo() {
        Propuesta propuesta = propuesta(40L, convocatoria());
        propuesta.setEstado(EstadoPropuesta.aprobada);
        ResultadoEvaluacion resultado = resultado(8L, propuesta, "7.10", ClasificacionResultado.aprobada);
        resultado.setFechaClasificacion(LocalDateTime.now().minusDays(1));
        propuestaEvaluada(propuesta, resultado);

        PropuestaClasificacionResponse respuesta = service.ajustar(40L, new AjusteClasificacionRequest(
                ClasificacionResultado.aprobada_con_ajustes, "  El comité pidió ajustes de formato.  "));

        assertEquals(EstadoPropuesta.ajustes, propuesta.getEstado());
        assertTrue(resultado.getAjusteManual());
        assertTrue(respuesta.ajusteManual());
        assertEquals("aprobada", respuesta.clasificacionPorUmbrales());
        assertEquals("aprobada_con_ajustes", respuesta.clasificacion());

        ArgumentCaptor<HistorialClasificacion> captor = ArgumentCaptor.forClass(HistorialClasificacion.class);
        verify(historialRepository).save(captor.capture());
        HistorialClasificacion h = captor.getValue();
        assertEquals(TipoClasificacion.manual, h.getTipo());
        assertEquals(ClasificacionResultado.aprobada, h.getClasificacionAnterior());
        assertEquals(ClasificacionResultado.aprobada_con_ajustes, h.getClasificacionNueva());
        assertEquals("El comité pidió ajustes de formato.", h.getMotivo());
        assertEquals(admin, h.getUsuario());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CLASIFICACION_AJUSTADA),
                eq("resultados_evaluacion"), eq(8L), anyMap(), eq(admin));
    }

    @Test
    @DisplayName("Criterio 2: sin motivo suficiente no se ajusta la clasificación")
    void criterio2_sinMotivo_rechaza() {
        assertThrows(SolicitudInvalidaException.class, () -> service.ajustar(40L,
                new AjusteClasificacionRequest(ClasificacionResultado.rechazada, "   corto   ")));
        verify(propuestaRepository, never()).findByIdParaActualizar(any());
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 2: ajustar a la misma clasificación ya aplicada se rechaza")
    void criterio2_mismaClasificacion_rechaza() {
        Propuesta propuesta = propuesta(40L, convocatoria());
        propuesta.setEstado(EstadoPropuesta.aprobada);
        ResultadoEvaluacion resultado = resultado(8L, propuesta, "7.10", ClasificacionResultado.aprobada);
        resultado.setFechaClasificacion(LocalDateTime.now());
        propuestaEvaluada(propuesta, resultado);

        assertThrows(SolicitudInvalidaException.class, () -> service.ajustar(40L,
                new AjusteClasificacionRequest(ClasificacionResultado.aprobada, "Se confirma la aprobación.")));
        verify(historialRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 3
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 3: con evaluaciones pendientes impide la clasificación")
    void criterio3_evaluacionesPendientes_impide() {
        Propuesta propuesta = propuesta(40L, convocatoria());
        when(propuestaRepository.findByIdParaActualizar(40L)).thenReturn(Optional.of(propuesta));
        when(asignacionRepository.findByPropuesta_Id(40L)).thenReturn(List.of(
                asignacion(1L, propuesta, EstadoAsignacion.completada),
                asignacion(2L, propuesta, EstadoAsignacion.pendiente)));
        when(resultadoRepository.findByPropuesta_Id(40L)).thenReturn(Optional.empty());

        PropuestaNoEvaluadaException ex = assertThrows(PropuestaNoEvaluadaException.class, () -> service.aplicar(40L));

        assertTrue(ex.getMessage().contains("1 evaluación(es) pendiente(s) de 2"));
        assertEquals(EstadoPropuesta.en_evaluacion, propuesta.getEstado());
        verify(resultadoRepository, never()).save(any());
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: sin evaluadores asignados impide la clasificación")
    void criterio3_sinEvaluadores_impide() {
        Propuesta propuesta = propuesta(40L, convocatoria());
        when(propuestaRepository.findByIdParaActualizar(40L)).thenReturn(Optional.of(propuesta));
        when(asignacionRepository.findByPropuesta_Id(40L)).thenReturn(List.of());
        when(resultadoRepository.findByPropuesta_Id(40L)).thenReturn(Optional.empty());

        assertThrows(PropuestaNoEvaluadaException.class, () -> service.aplicar(40L));
    }

    @Test
    @DisplayName("Criterio 3: sin puntuación calculada impide también el ajuste manual")
    void criterio3_sinPuntuacion_impideAjusteManual() {
        Propuesta propuesta = propuesta(40L, convocatoria());
        propuestaEvaluada(propuesta, null);

        assertThrows(PropuestaNoEvaluadaException.class, () -> service.ajustar(40L,
                new AjusteClasificacionRequest(ClasificacionResultado.aprobada, "Decisión del comité organizador.")));
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Propuesta inexistente responde no encontrada")
    void aplicar_propuestaInexistente() {
        when(propuestaRepository.findByIdParaActualizar(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.aplicar(99L));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Umbrales y listado
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Umbrales: el de ajustes no puede superar al de aprobación")
    void configurarUmbrales_ordenInvalido() {
        assertThrows(SolicitudInvalidaException.class, () -> service.configurarUmbrales(10L,
                new UmbralesClasificacionRequest(new BigDecimal("5"), new BigDecimal("7"))));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Umbrales: reclasifica resultados sin aplicar e informa los ya aplicados que cambiarían")
    void configurarUmbrales_reclasificaPendientes() {
        Convocatoria convocatoria = convocatoria();
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(convocatoria));
        Propuesta p1 = propuesta(40L, convocatoria);
        Propuesta p2 = propuesta(41L, convocatoria);
        Propuesta p3 = propuesta(42L, convocatoria);
        ResultadoEvaluacion sinAplicar = resultado(1L, p1, "7.50", ClasificacionResultado.aprobada);
        ResultadoEvaluacion aplicado = resultado(2L, p2, "7.50", ClasificacionResultado.aprobada);
        aplicado.setFechaClasificacion(LocalDateTime.now());
        ResultadoEvaluacion manual = resultado(3L, p3, "7.50", ClasificacionResultado.rechazada);
        manual.setAjusteManual(true);
        when(resultadoRepository.findByPropuesta_Convocatoria_Id(10L)).thenReturn(List.of(sinAplicar, aplicado, manual));

        UmbralesClasificacionResponse respuesta = service.configurarUmbrales(10L,
                new UmbralesClasificacionRequest(new BigDecimal("8.00"), new BigDecimal("6.00")));

        assertEquals(0, new BigDecimal("8").compareTo(convocatoria.getUmbralAceptacion()));
        assertEquals(ClasificacionResultado.aprobada_con_ajustes, sinAplicar.getClasificacion());
        assertEquals(ClasificacionResultado.aprobada, aplicado.getClasificacion());
        assertEquals(ClasificacionResultado.rechazada, manual.getClasificacion());
        assertEquals(1L, respuesta.propuestasPorReaplicar());
        verify(resultadoRepository).saveAll(anyList());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.UMBRALES_CLASIFICACION_ACTUALIZADOS),
                eq("convocatorias"), eq(10L), anyMap());
    }

    @Test
    @DisplayName("Listado: agrupa por resultado y cuenta las propuestas de cada grupo")
    void listarPorConvocatoria_agrupa() {
        Convocatoria convocatoria = convocatoria();
        Propuesta aprobada = propuesta(40L, convocatoria);
        aprobada.setEstado(EstadoPropuesta.aprobada);
        Propuesta sinAplicar = propuesta(41L, convocatoria);
        Propuesta incompleta = propuesta(42L, convocatoria);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(convocatoria));
        when(propuestaRepository.findByConvocatoria_IdOrderByFechaEnvioDesc(10L))
                .thenReturn(List.of(incompleta, sinAplicar, aprobada));
        when(asignacionRepository.findDetalleByPropuestaIds(List.of(42L, 41L, 40L))).thenReturn(List.of(
                asignacion(1L, aprobada, EstadoAsignacion.completada),
                asignacion(2L, sinAplicar, EstadoAsignacion.completada),
                asignacion(3L, incompleta, EstadoAsignacion.pendiente)));
        ResultadoEvaluacion r1 = resultado(1L, aprobada, "8.20", ClasificacionResultado.aprobada);
        r1.setFechaClasificacion(LocalDateTime.now());
        ResultadoEvaluacion r2 = resultado(2L, sinAplicar, "5.50", ClasificacionResultado.aprobada_con_ajustes);
        when(resultadoRepository.findByPropuesta_IdIn(List.of(42L, 41L, 40L))).thenReturn(List.of(r1, r2));

        ClasificacionConvocatoriaResponse respuesta = service.listarPorConvocatoria(10L);

        assertEquals(1L, respuesta.totales().get("aprobada"));
        assertEquals(0L, respuesta.totales().get("rechazada"));
        assertEquals(1L, respuesta.totales().get("sin_clasificar"));
        assertEquals(1L, respuesta.totales().get("evaluacion_incompleta"));
        assertEquals(List.of(40L, 41L, 42L),
                respuesta.propuestas().stream().map(PropuestaClasificacionResponse::propuestaId).toList());
        PropuestaClasificacionResponse filaIncompleta = respuesta.propuestas().get(2);
        assertFalse(filaIncompleta.evaluacionCompleta());
        assertEquals(1L, filaIncompleta.asignacionesPendientes());
        assertNull(filaIncompleta.clasificacion());
    }
}
