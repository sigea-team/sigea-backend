package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.memoria.CierreFormalRequest;
import com.sigea.demosigea_backend.dto.memoria.EliminacionHistoricaRequest;
import com.sigea.demosigea_backend.dto.memoria.EstadoCierreResponse;
import com.sigea.demosigea_backend.dto.memoria.MemoriaHistoricaResponse;
import com.sigea.demosigea_backend.dto.memoria.MemoriaHistoricaResumenResponse;
import com.sigea.demosigea_backend.dto.memoria.PendienteCierreResponse;
import com.sigea.demosigea_backend.exception.CierreConPendientesException;
import com.sigea.demosigea_backend.exception.MemoriaHistoricaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.Indicador;
import com.sigea.demosigea_backend.model.MemoriaHistorica;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.IndicadorRepository;
import com.sigea.demosigea_backend.repository.MemoriaHistoricaRepository;
import com.sigea.demosigea_backend.repository.MemoriaHistoricaRepository.ConteoPendientes;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link MemoriaHistoricaService} (HU-28, RF55, CU-32).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemoriaHistoricaServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 21, 10, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();

    @Mock private MemoriaHistoricaRepository memoriaRepository;
    @Mock private IndicadorRepository indicadorRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EncuestaService encuestaService;
    @Mock private AuditoriaService auditoriaService;

    private MemoriaHistoricaService servicio;
    private Evento evento;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
        servicio = new MemoriaHistoricaService(memoriaRepository, indicadorRepository, eventoRepository,
                usuarioRepository, encuestaService, auditoriaService, reloj);

        evento = Evento.builder().id(1L).nombre("Congreso de Ingeniería").estado(EstadoEvento.cerrado)
                .fechaInicio(HOY.minusDays(10)).fechaFin(HOY.minusDays(8)).semestre("2026-2").build();

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(eventoRepository.findByIdParaProgramar(1L)).thenReturn(Optional.of(evento));
        when(memoriaRepository.existsByEvento_Id(1L)).thenReturn(false);
        when(memoriaRepository.findByEventoId(1L)).thenReturn(Optional.empty());
        when(memoriaRepository.contarPendientes(1L)).thenReturn(conteo(0, 0, 0, 0, 0, 0));
        when(memoriaRepository.idsEncuestasPublicadas(1L)).thenReturn(List.of());
        when(memoriaRepository.insertarIndicadores(eq(1L), any())).thenReturn(14);
        when(memoriaRepository.construirContenido(eq(1L), anyString())).thenReturn("{\"version\":1}");
        when(memoriaRepository.saveAndFlush(any(MemoriaHistorica.class))).thenAnswer(inv -> inv.getArgument(0));
        when(indicadorRepository.findByEvento_IdOrderByIdAsc(1L)).thenReturn(List.of(
                indicador("INSCRITOS", "120")));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Criterio 1: cierre formal
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 1: cierra un evento finalizado y consolida su memoria en solo consulta")
    void cierraYConsolida() {
        MemoriaHistoricaResponse respuesta = servicio.cerrarFormalmente(1L, new CierreFormalRequest(null, "  Acta  "));

        ArgumentCaptor<MemoriaHistorica> memoria = ArgumentCaptor.forClass(MemoriaHistorica.class);
        verify(memoriaRepository).saveAndFlush(memoria.capture());
        assertEquals(evento, memoria.getValue().getEvento());
        assertEquals(AHORA, memoria.getValue().getFechaConsolidacion());
        assertEquals("Acta", memoria.getValue().getResumen());
        assertEquals("{\"version\":1}", memoria.getValue().getContenido());

        verify(memoriaRepository).insertarIndicadores(1L, AHORA);
        verify(memoriaRepository).construirContenido(1L, "[]");
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.MEMORIA_HISTORICA_CONSOLIDADA), eq("eventos"),
                eq(1L), anyMap(), isNull());
        assertFalse(respuesta.editable());
        assertEquals("{\"version\":1}", respuesta.contenido());
        assertEquals(1, respuesta.indicadores().size());
        assertEquals(EstadoEvento.cerrado, evento.getEstado());
    }

    @Test
    @DisplayName("Criterio 1: el orden es encuestas → indicadores → contenido → memoria (después ya no se acepta ningún cambio)")
    void ordenDeLaConsolidacion() {
        when(memoriaRepository.contarPendientes(1L)).thenReturn(conteo(0, 0, 0, 0, 2, 0));
        when(memoriaRepository.idsEncuestasPublicadas(1L)).thenReturn(List.of(7L, 8L));

        servicio.cerrarFormalmente(1L, new CierreFormalRequest(true, null));

        InOrder orden = inOrder(encuestaService, memoriaRepository);
        orden.verify(encuestaService).cerrar(1L, 7L);
        orden.verify(encuestaService).cerrar(1L, 8L);
        orden.verify(memoriaRepository).flush();
        orden.verify(memoriaRepository).insertarIndicadores(1L, AHORA);
        orden.verify(memoriaRepository).construirContenido(eq(1L), anyString());
        orden.verify(memoriaRepository).saveAndFlush(any(MemoriaHistorica.class));
    }

    @Test
    @DisplayName("CU-32 flujo alterno: con procesos pendientes sin confirmar → 409 CIERRE_CON_PENDIENTES y no consolida")
    void pendientesSinConfirmar() {
        when(memoriaRepository.contarPendientes(1L)).thenReturn(conteo(3, 1, 0, 0, 0, 25));

        CierreConPendientesException ex = assertThrows(CierreConPendientesException.class,
                () -> servicio.cerrarFormalmente(1L, new CierreFormalRequest(false, null)));

        List<String> codigos = ex.getPendientes().stream().map(PendienteCierreResponse::codigo).toList();
        assertEquals(List.of("EVALUACIONES_SIN_FINALIZAR", "PROPUESTAS_SIN_DECISION", "CERTIFICADOS_SIN_GENERAR"),
                codigos);
        assertEquals(25, ex.getPendientes().get(2).cantidad());
        verify(memoriaRepository, never()).saveAndFlush(any());
        verify(memoriaRepository, never()).insertarIndicadores(anyLong(), any());
    }

    @Test
    @DisplayName("CU-32 flujo alterno: con confirmación explícita cierra y guarda los pendientes en el contenido")
    void pendientesConfirmados() {
        when(memoriaRepository.contarPendientes(1L)).thenReturn(conteo(3, 0, 0, 0, 0, 0));

        servicio.cerrarFormalmente(1L, new CierreFormalRequest(true, null));

        ArgumentCaptor<String> pendientes = ArgumentCaptor.forClass(String.class);
        verify(memoriaRepository).construirContenido(eq(1L), pendientes.capture());
        assertTrue(pendientes.getValue().contains("\"codigo\":\"EVALUACIONES_SIN_FINALIZAR\""));
        assertTrue(pendientes.getValue().contains("\"cantidad\":3"));
        verify(memoriaRepository).saveAndFlush(any(MemoriaHistorica.class));
    }

    @Test
    @DisplayName("Sin cuerpo en la petición se trata como sin confirmación")
    void sinCuerpo() {
        when(memoriaRepository.contarPendientes(1L)).thenReturn(conteo(0, 0, 0, 1, 0, 0));

        assertThrows(CierreConPendientesException.class, () -> servicio.cerrarFormalmente(1L, null));
    }

    @Test
    @DisplayName("Evento en ejecución en su último día: se cierra formalmente y pasa a cerrado (auditado)")
    void ultimoDiaDeEjecucion() {
        evento.setEstado(EstadoEvento.en_ejecucion);
        evento.setFechaFin(HOY);

        servicio.cerrarFormalmente(1L, new CierreFormalRequest(null, null));

        assertEquals(EstadoEvento.cerrado, evento.getEstado());
        verify(eventoRepository).saveAndFlush(evento);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.EVENTO_CERRADO), eq("eventos"), eq(1L),
                anyMap(), isNull());
        InOrder orden = inOrder(eventoRepository, memoriaRepository);
        orden.verify(eventoRepository).saveAndFlush(evento);
        orden.verify(memoriaRepository).saveAndFlush(any(MemoriaHistorica.class));
    }

    @Test
    @DisplayName("Evento que no ha finalizado → 409 EVENTO_NO_FINALIZADO")
    void eventoNoFinalizado() {
        evento.setEstado(EstadoEvento.en_ejecucion);
        evento.setFechaFin(HOY.plusDays(1));

        MemoriaHistoricaException ex = assertThrows(MemoriaHistoricaException.class,
                () -> servicio.cerrarFormalmente(1L, new CierreFormalRequest(true, null)));

        assertEquals(MemoriaHistoricaException.EVENTO_NO_FINALIZADO, ex.getCodigo());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(memoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Evento en configuración → 409 EVENTO_NO_FINALIZADO")
    void eventoEnConfiguracion() {
        evento.setEstado(EstadoEvento.en_configuracion);

        MemoriaHistoricaException ex = assertThrows(MemoriaHistoricaException.class,
                () -> servicio.cerrarFormalmente(1L, new CierreFormalRequest(true, null)));

        assertEquals(MemoriaHistoricaException.EVENTO_NO_FINALIZADO, ex.getCodigo());
    }

    @Test
    @DisplayName("Evento ya cerrado formalmente → 409 MEMORIA_YA_CONSOLIDADA")
    void yaConsolidada() {
        when(memoriaRepository.existsByEvento_Id(1L)).thenReturn(true);

        MemoriaHistoricaException ex = assertThrows(MemoriaHistoricaException.class,
                () -> servicio.cerrarFormalmente(1L, new CierreFormalRequest(true, null)));

        assertEquals(MemoriaHistoricaException.MEMORIA_YA_CONSOLIDADA, ex.getCodigo());
        verify(memoriaRepository, never()).insertarIndicadores(anyLong(), any());
    }

    @Test
    @DisplayName("Evento inexistente → 404")
    void eventoInexistente() {
        when(eventoRepository.findByIdParaProgramar(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.cerrarFormalmente(99L, new CierreFormalRequest(true, null)));
    }

    @Test
    @DisplayName("Revisión previa: muestra si se puede cerrar y los pendientes")
    void revisarCierre() {
        when(memoriaRepository.contarPendientes(1L)).thenReturn(conteo(0, 0, 2, 0, 0, 0));

        EstadoCierreResponse estado = servicio.revisarCierre(1L);

        assertFalse(estado.consolidada());
        assertTrue(estado.puedeCerrarse());
        assertNull(estado.motivoNoCerrable());
        assertEquals("PROPUESTAS_CON_AJUSTES_PENDIENTES", estado.pendientes().get(0).codigo());
    }

    @Test
    @DisplayName("Revisión previa de un evento ya cerrado formalmente: consolidada y sin poder cerrarse otra vez")
    void revisarCierreConsolidada() {
        when(memoriaRepository.findByEventoId(1L)).thenReturn(Optional.of(memoria()));

        EstadoCierreResponse estado = servicio.revisarCierre(1L);

        assertTrue(estado.consolidada());
        assertFalse(estado.puedeCerrarse());
        assertEquals(AHORA.minusDays(1), estado.fechaConsolidacion());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Criterio 2: consulta
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 2: consulta la memoria de un evento (solo lectura)")
    void obtener() {
        when(memoriaRepository.findByEventoId(1L)).thenReturn(Optional.of(memoria()));

        MemoriaHistoricaResponse respuesta = servicio.obtener(1L);

        assertEquals(1L, respuesta.eventoId());
        assertEquals("{\"evento\":{}}", respuesta.contenido());
        assertFalse(respuesta.editable());
        assertEquals("INSCRITOS", respuesta.indicadores().get(0).codigo());
    }

    @Test
    @DisplayName("Criterio 2: evento sin cierre formal → 404 MEMORIA_NO_ENCONTRADA")
    void obtenerSinMemoria() {
        MemoriaHistoricaException ex = assertThrows(MemoriaHistoricaException.class, () -> servicio.obtener(1L));

        assertEquals(MemoriaHistoricaException.MEMORIA_NO_ENCONTRADA, ex.getCodigo());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Criterio 2: el listado por familia usa el evento base aunque se pase una edición")
    void listarPorFamilia() {
        Evento base = Evento.builder().id(10L).nombre("Congreso").build();
        Evento edicion = Evento.builder().id(11L).nombre("Congreso 2026").eventoBase(base)
                .fechaInicio(HOY.minusDays(30)).fechaFin(HOY.minusDays(28)).semestre("2026-2").build();
        when(eventoRepository.findById(11L)).thenReturn(Optional.of(edicion));
        MemoriaHistorica memoria = MemoriaHistorica.builder().id(5L).evento(edicion)
                .fechaConsolidacion(AHORA).contenido("{}").build();
        when(memoriaRepository.buscar(null, 10L, "%congreso%")).thenReturn(List.of(memoria));
        when(indicadorRepository.findByEvento_IdInAndCodigoInOrderByIdAsc(any(Collection.class), any(Collection.class)))
                .thenReturn(List.of(Indicador.builder().evento(edicion).codigo("INSCRITOS").nombre("Inscritos")
                        .valor(new BigDecimal("80")).fechaCalculo(AHORA).build()));

        List<MemoriaHistoricaResumenResponse> lista = servicio.listar("  ", 11L, " Congreso ");

        assertEquals(1, lista.size());
        assertEquals(11L, lista.get(0).eventoId());
        assertEquals(10L, lista.get(0).eventoBaseId());
        assertEquals(1, lista.get(0).indicadores().size());
    }

    @Test
    @DisplayName("Criterio 2: listado vacío sin consultar indicadores")
    void listarVacio() {
        when(memoriaRepository.buscar("2025-1", null, null)).thenReturn(List.of());

        assertTrue(servicio.listar("2025-1", null, null).isEmpty());
        verify(indicadorRepository, never()).findByEvento_IdInAndCodigoInOrderByIdAsc(any(), any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Criterio 3: eliminación con autorización especial
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 3: con autorización especial elimina el evento en orden y lo deja en la auditoría")
    void eliminarConAutorizacion() {
        when(memoriaRepository.findByEventoId(1L)).thenReturn(Optional.of(memoria()));

        servicio.eliminarConAutorizacionEspecial(1L,
                new EliminacionHistoricaRequest(" Congreso de Ingeniería ", "Evento de prueba creado por error"));

        ArgumentCaptor<Map<String, ?>> detalle = ArgumentCaptor.forClass(Map.class);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.MEMORIA_HISTORICA_ELIMINADA), eq("eventos"),
                eq(1L), detalle.capture());
        assertEquals("Evento de prueba creado por error", detalle.getValue().get("motivo"));

        InOrder orden = inOrder(auditoriaService, memoriaRepository);
        orden.verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.MEMORIA_HISTORICA_ELIMINADA), anyString(),
                anyLong(), anyMap());
        orden.verify(memoriaRepository).autorizarEliminacionHistorica(1L);
        orden.verify(memoriaRepository).eliminarAsistencias(1L);
        orden.verify(memoriaRepository).eliminarCertificados(1L);
        orden.verify(memoriaRepository).eliminarEncuestas(1L);
        orden.verify(memoriaRepository).eliminarActividades(1L);
        orden.verify(memoriaRepository).eliminarPropuestas(1L);
        orden.verify(memoriaRepository).eliminarEvento(1L);
        orden.verify(memoriaRepository).revocarEliminacionHistorica();
    }

    @Test
    @DisplayName("Criterio 3: el nombre de confirmación no coincide → 400 CONFIRMACION_INVALIDA")
    void eliminarConfirmacionInvalida() {
        when(memoriaRepository.findByEventoId(1L)).thenReturn(Optional.of(memoria()));

        MemoriaHistoricaException ex = assertThrows(MemoriaHistoricaException.class,
                () -> servicio.eliminarConAutorizacionEspecial(1L,
                        new EliminacionHistoricaRequest("Congreso", "Evento de prueba creado por error")));

        assertEquals(MemoriaHistoricaException.CONFIRMACION_INVALIDA, ex.getCodigo());
        verify(memoriaRepository, never()).autorizarEliminacionHistorica(anyLong());
        verify(memoriaRepository, never()).eliminarEvento(anyLong());
    }

    @Test
    @DisplayName("Criterio 3: evento base de otras ediciones → 409 ELIMINACION_NO_PERMITIDA")
    void eliminarEventoBase() {
        when(memoriaRepository.findByEventoId(1L)).thenReturn(Optional.of(memoria()));
        when(eventoRepository.countByEventoBase_Id(1L)).thenReturn(2L);

        MemoriaHistoricaException ex = assertThrows(MemoriaHistoricaException.class,
                () -> servicio.eliminarConAutorizacionEspecial(1L,
                        new EliminacionHistoricaRequest("Congreso de Ingeniería", "Evento de prueba creado por error")));

        assertEquals(MemoriaHistoricaException.ELIMINACION_NO_PERMITIDA, ex.getCodigo());
        verify(memoriaRepository, never()).eliminarEvento(anyLong());
    }

    @Test
    @DisplayName("Criterio 3: evento sin memoria histórica → 409 MEMORIA_NO_ENCONTRADA (usa la eliminación normal)")
    void eliminarSinMemoria() {
        MemoriaHistoricaException ex = assertThrows(MemoriaHistoricaException.class,
                () -> servicio.eliminarConAutorizacionEspecial(1L,
                        new EliminacionHistoricaRequest("Congreso de Ingeniería", "Evento de prueba creado por error")));

        assertEquals(MemoriaHistoricaException.MEMORIA_NO_ENCONTRADA, ex.getCodigo());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("Criterio 3: si otra información depende del evento → 409 ELIMINACION_NO_PERMITIDA")
    void eliminarConDependencias() {
        when(memoriaRepository.findByEventoId(1L)).thenReturn(Optional.of(memoria()));
        when(memoriaRepository.eliminarEvento(1L)).thenThrow(new DataIntegrityViolationException("fk"));

        MemoriaHistoricaException ex = assertThrows(MemoriaHistoricaException.class,
                () -> servicio.eliminarConAutorizacionEspecial(1L,
                        new EliminacionHistoricaRequest("Congreso de Ingeniería", "Evento de prueba creado por error")));

        assertEquals(MemoriaHistoricaException.ELIMINACION_NO_PERMITIDA, ex.getCodigo());
    }

    // ──────────────────────────────────────────────────────────────────────────

    private MemoriaHistorica memoria() {
        return MemoriaHistorica.builder().id(5L).evento(evento).fechaConsolidacion(AHORA.minusDays(1))
                .contenido("{\"evento\":{}}").build();
    }

    private Indicador indicador(String codigo, String valor) {
        return Indicador.builder().id(1L).evento(evento).codigo(codigo).nombre(codigo).valor(new BigDecimal(valor))
                .unidad("personas").fechaCalculo(AHORA).build();
    }

    private static ConteoPendientes conteo(long evaluaciones, long propuestas, long ajustes, long porProgramar,
                                           long encuestas, long certificados) {
        return new ConteoPendientes() {
            @Override public Long getEvaluacionesSinFinalizar() { return evaluaciones; }
            @Override public Long getPropuestasSinDecision() { return propuestas; }
            @Override public Long getPropuestasConAjustesPendientes() { return ajustes; }
            @Override public Long getActividadesPorProgramar() { return porProgramar; }
            @Override public Long getEncuestasAbiertas() { return encuestas; }
            @Override public Long getCertificadosSinGenerar() { return certificados; }
        };
    }
}
