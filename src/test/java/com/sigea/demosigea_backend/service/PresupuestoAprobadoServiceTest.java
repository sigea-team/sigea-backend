package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoAprobadoRequest;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoAprobadoResponse;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoResumenResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.EstadoPresupuestoAprobado;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.PresupuestoAprobado;
import com.sigea.demosigea_backend.model.RubroPresupuestal;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.PresupuestoAprobadoRepository;
import com.sigea.demosigea_backend.repository.RubroPresupuestalRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresupuestoAprobadoServiceTest {

    @Mock
    private PresupuestoAprobadoRepository presupuestoRepository;

    @Mock
    private RubroPresupuestalRepository rubroRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PresupuestoAprobadoService presupuestoService;

    private Evento evento(EstadoEvento estado) {
        return Evento.builder()
                .id(1L)
                .nombre("Congreso Internacional de TIC")
                .fechaInicio(LocalDate.of(2026, 11, 10))
                .fechaFin(LocalDate.of(2026, 11, 12))
                .semestre("2026-2")
                .estado(estado)
                .build();
    }

    private Usuario usuario() {
        Persona persona = Persona.builder()
                .id(5L)
                .nombres("Carlos Daniel")
                .apellidos("Gómez")
                .correo("carlos.gomez@ufps.edu.co")
                .numeroDocumento("1090111222")
                .tipoDocumento("CC")
                .build();

        return Usuario.builder()
                .id(2L)
                .persona(persona)
                .correoVerificado(true)
                .build();
    }

    // ==================================================================
    // CRITERIO 1: Registro con presupuesto preliminar existente
    // ==================================================================

    @Test
    @DisplayName("Criterio 1: Dado que existe presupuesto preliminar, registra presupuesto aprobado que difiere del preliminar y lo guarda como oficial")
    void registrar_conPreliminarExistente_guardaComoVersionOficial() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        Usuario usuario = usuario();
        BigDecimal preliminar = new BigDecimal("45000000.00");
        BigDecimal valorAprobado = new BigDecimal("50000000.00");
        String evidencia = "https://archivos.ufps.edu.co/acta-aprobacion.pdf";

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(rubroRepository.existsByEvento_IdAndActivoTrue(1L)).thenReturn(true);
        when(rubroRepository.calcularTotalPreliminarPorEvento(1L)).thenReturn(preliminar);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(presupuestoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.empty());

        when(presupuestoRepository.save(any(PresupuestoAprobado.class))).thenAnswer(inv -> {
            PresupuestoAprobado p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        PresupuestoAprobadoRequest request = new PresupuestoAprobadoRequest(valorAprobado, evidencia, 2L);
        PresupuestoAprobadoResponse response = presupuestoService.registrarOActualizar(1L, request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(1L, response.eventoId());
        assertEquals(valorAprobado, response.valorTotalAprobado());
        assertEquals(preliminar, response.valorPreliminarProyectado());
        assertEquals(new BigDecimal("5000000.00"), response.diferenciaConPreliminar());
        assertEquals(evidencia, response.evidenciaUrl());
        assertEquals(EstadoPresupuestoAprobado.aprobado, response.estado());
        assertTrue(response.esOficial());

        ArgumentCaptor<PresupuestoAprobado> captor = ArgumentCaptor.forClass(PresupuestoAprobado.class);
        verify(presupuestoRepository).save(captor.capture());
        PresupuestoAprobado guardado = captor.getValue();
        assertEquals(EstadoPresupuestoAprobado.aprobado, guardado.getEstado());
        assertEquals(valorAprobado, guardado.getValorTotalAprobado());
        assertEquals(evidencia, guardado.getEvidenciaUrl());
        assertEquals(usuario, guardado.getAprobadoPor());

        verify(auditoriaService).registrar(
                eq(TipoOperacionAuditoria.PRESUPUESTO_APROBADO),
                eq("presupuestos_aprobados"),
                eq(100L),
                anyMap()
        );
    }

    @Test
    @DisplayName("Criterio 1: Si NO existe presupuesto preliminar registrado para el evento, rechaza la operación")
    void registrar_sinPresupuestoPreliminar_lanzaOperacionNoPermitida() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(rubroRepository.existsByEvento_IdAndActivoTrue(1L)).thenReturn(false);

        PresupuestoAprobadoRequest request = new PresupuestoAprobadoRequest(
                new BigDecimal("50000000.00"), "https://soporte.pdf", 2L);

        OperacionNoPermitidaException ex = assertThrows(
                OperacionNoPermitidaException.class,
                () -> presupuestoService.registrarOActualizar(1L, request)
        );

        assertTrue(ex.getMessage().contains("presupuesto preliminar"));
        verify(presupuestoRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), any(), any(), anyMap());
    }

    @Test
    @DisplayName("Criterio 1: Resuelve el usuario aprobador a partir del contexto de seguridad si aprobadoPorId es nulo")
    void registrar_conUsuarioDeSesion_resuelveUsuarioAutenticado() {
        Evento evento = evento(EstadoEvento.habilitado);
        Usuario usuario = usuario();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("carlos.gomez@ufps.edu.co", "pwd",
                        Collections.emptyList()));

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(rubroRepository.existsByEvento_IdAndActivoTrue(1L)).thenReturn(true);
        when(rubroRepository.calcularTotalPreliminarPorEvento(1L)).thenReturn(new BigDecimal("30000000.00"));
        when(usuarioRepository.findByPersona_CorreoIgnoreCase("carlos.gomez@ufps.edu.co"))
                .thenReturn(Optional.of(usuario));
        when(presupuestoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.empty());
        when(presupuestoRepository.save(any(PresupuestoAprobado.class))).thenAnswer(inv -> {
            PresupuestoAprobado p = inv.getArgument(0);
            p.setId(101L);
            return p;
        });

        PresupuestoAprobadoRequest request = new PresupuestoAprobadoRequest(
                new BigDecimal("35000000.00"), "https://soporte.pdf", null);

        PresupuestoAprobadoResponse response = presupuestoService.registrarOActualizar(1L, request);

        assertNotNull(response);
        assertEquals(2L, response.aprobadoPorId());
        assertEquals("Carlos Daniel Gómez", response.aprobadoPorNombre());

        SecurityContextHolder.clearContext();
    }

    // ==================================================================
    // CRITERIO 2: Conservar versión anterior y registrar nueva con historial
    // ==================================================================

    @Test
    @DisplayName("Criterio 2: Dado que ya existe un presupuesto aprobado, la actualización conserva el anterior como histórico y guarda el nuevo como oficial")
    void actualizar_cuandoYaExistePresupuestoAprobado_conservaAnteriorYGuardaNuevo() {
        Evento evento = evento(EstadoEvento.habilitado);
        Usuario usuario = usuario();
        BigDecimal preliminar = new BigDecimal("45000000.00");

        PresupuestoAprobado versionAnterior = PresupuestoAprobado.builder()
                .id(50L)
                .evento(evento)
                .valorTotalAprobado(new BigDecimal("48000000.00"))
                .evidenciaUrl("https://archivos.ufps.edu.co/acta-v1.pdf")
                .fechaAprobacion(LocalDateTime.of(2026, 9, 15, 10, 0))
                .aprobadoPor(usuario)
                .estado(EstadoPresupuestoAprobado.aprobado)
                .build();

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(rubroRepository.existsByEvento_IdAndActivoTrue(1L)).thenReturn(true);
        when(rubroRepository.calcularTotalPreliminarPorEvento(1L)).thenReturn(preliminar);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(presupuestoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.of(versionAnterior));

        when(presupuestoRepository.save(any(PresupuestoAprobado.class))).thenAnswer(inv -> {
            PresupuestoAprobado p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(51L);
            }
            return p;
        });

        BigDecimal nuevoValor = new BigDecimal("55000000.00");
        String nuevaEvidencia = "https://archivos.ufps.edu.co/resolucion-adicion-v2.pdf";
        PresupuestoAprobadoRequest request = new PresupuestoAprobadoRequest(nuevoValor, nuevaEvidencia, 2L);

        PresupuestoAprobadoResponse response = presupuestoService.registrarOActualizar(1L, request);

        // La versión anterior fue modificada a histórico
        assertEquals(EstadoPresupuestoAprobado.historico, versionAnterior.getEstado());
        assertFalse(versionAnterior.esOficial());

        // La nueva versión es la oficial
        assertNotNull(response);
        assertEquals(51L, response.id());
        assertEquals(nuevoValor, response.valorTotalAprobado());
        assertEquals(nuevaEvidencia, response.evidenciaUrl());
        assertEquals(EstadoPresupuestoAprobado.aprobado, response.estado());
        assertTrue(response.esOficial());

        // Se verifican las 2 llamadas a save: la anterior como histórico y la nueva como oficial
        verify(presupuestoRepository).save(versionAnterior);
        verify(auditoriaService).registrar(
                eq(TipoOperacionAuditoria.PRESUPUESTO_ACTUALIZADO),
                eq("presupuestos_aprobados"),
                eq(51L),
                anyMap()
        );
    }

    @Test
    @DisplayName("Criterio 2: Listar historial retorna todas las versiones conservadas ordenadas cronológicamente")
    void listarHistorial_retornaTodasLasVersionesOrdenadas() {
        Evento evento = evento(EstadoEvento.habilitado);
        Usuario usuario = usuario();
        BigDecimal preliminar = new BigDecimal("40000000.00");

        PresupuestoAprobado v2 = PresupuestoAprobado.builder()
                .id(2L)
                .evento(evento)
                .valorTotalAprobado(new BigDecimal("45000000.00"))
                .evidenciaUrl("https://evidencia-v2.pdf")
                .fechaAprobacion(LocalDateTime.of(2026, 10, 1, 14, 0))
                .aprobadoPor(usuario)
                .estado(EstadoPresupuestoAprobado.aprobado)
                .build();

        PresupuestoAprobado v1 = PresupuestoAprobado.builder()
                .id(1L)
                .evento(evento)
                .valorTotalAprobado(new BigDecimal("42000000.00"))
                .evidenciaUrl("https://evidencia-v1.pdf")
                .fechaAprobacion(LocalDateTime.of(2026, 9, 1, 10, 0))
                .aprobadoPor(usuario)
                .estado(EstadoPresupuestoAprobado.historico)
                .build();

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(rubroRepository.calcularTotalPreliminarPorEvento(1L)).thenReturn(preliminar);
        when(presupuestoRepository.findByEvento_IdOrderByFechaAprobacionDescIdDesc(1L))
                .thenReturn(List.of(v2, v1));

        List<PresupuestoAprobadoResponse> historial = presupuestoService.listarHistorial(1L);

        assertEquals(2, historial.size());
        assertEquals(2L, historial.get(0).id());
        assertTrue(historial.get(0).esOficial());
        assertEquals(1L, historial.get(1).id());
        assertFalse(historial.get(1).esOficial());
        assertEquals(EstadoPresupuestoAprobado.historico, historial.get(1).estado());
    }

    // ==================================================================
    // CRITERIO 3: Exigir evidencia documental antes de guardar
    // ==================================================================

    @Test
    @DisplayName("Criterio 3: Si se intenta registrar sin adjuntar evidencia (null), exige el soporte y no guarda")
    void registrar_evidenciaNula_exigeSoporteYLanzaExcepcion() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        PresupuestoAprobadoRequest request = new PresupuestoAprobadoRequest(
                new BigDecimal("50000000.00"), null, 2L);

        SolicitudInvalidaException ex = assertThrows(
                SolicitudInvalidaException.class,
                () -> presupuestoService.registrarOActualizar(1L, request)
        );

        assertTrue(ex.getMessage().contains("evidencia de aprobación es obligatoria"));
        verify(presupuestoRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), any(), any(), anyMap());
    }

    @Test
    @DisplayName("Criterio 3: Si la evidencia es una cadena vacía o solo espacios, exige el soporte y no guarda")
    void registrar_evidenciaVacia_exigeSoporteYLanzaExcepcion() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        PresupuestoAprobadoRequest request = new PresupuestoAprobadoRequest(
                new BigDecimal("50000000.00"), "   ", 2L);

        SolicitudInvalidaException ex = assertThrows(
                SolicitudInvalidaException.class,
                () -> presupuestoService.registrarOActualizar(1L, request)
        );

        assertTrue(ex.getMessage().contains("evidencia de aprobación es obligatoria"));
        verify(presupuestoRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), any(), any(), anyMap());
    }

    @Test
    @DisplayName("Validación: Monto negativo es rechazado con SolicitudInvalidaException")
    void registrar_montoNegativo_lanzaSolicitudInvalida() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        PresupuestoAprobadoRequest request = new PresupuestoAprobadoRequest(
                new BigDecimal("-500.00"), "https://soporte.pdf", 2L);

        assertThrows(
                SolicitudInvalidaException.class,
                () -> presupuestoService.registrarOActualizar(1L, request)
        );

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validación: Evento cerrado rechaza registrar o modificar presupuesto")
    void registrar_eventoCerrado_lanzaOperacionNoPermitida() {
        Evento evento = evento(EstadoEvento.cerrado);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        PresupuestoAprobadoRequest request = new PresupuestoAprobadoRequest(
                new BigDecimal("50000000.00"), "https://soporte.pdf", 2L);

        assertThrows(
                OperacionNoPermitidaException.class,
                () -> presupuestoService.registrarOActualizar(1L, request)
        );

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Consulta: obtenerOficial lanza RecursoNoEncontradoException si no hay presupuesto oficial")
    void obtenerOficial_cuandoNoExiste_lanzaRecursoNoEncontrado() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(presupuestoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> presupuestoService.obtenerOficial(1L)
        );
    }

    @Test
    @DisplayName("Consulta: obtenerResumen consolida rubros preliminares, oficial e historial")
    void obtenerResumen_retornaConsolidado() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        Usuario usuario = usuario();
        BigDecimal preliminar = new BigDecimal("12000000.00");

        RubroPresupuestal rubro = RubroPresupuestal.builder()
                .id(1L)
                .evento(evento)
                .nombre("Refrigerios")
                .cantidad(new BigDecimal("100"))
                .valorUnitarioProyectado(new BigDecimal("120000.00"))
                .activo(true)
                .build();

        PresupuestoAprobado oficial = PresupuestoAprobado.builder()
                .id(10L)
                .evento(evento)
                .valorTotalAprobado(new BigDecimal("15000000.00"))
                .evidenciaUrl("https://acta.pdf")
                .fechaAprobacion(LocalDateTime.now())
                .aprobadoPor(usuario)
                .estado(EstadoPresupuestoAprobado.aprobado)
                .build();

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(rubroRepository.findByEvento_IdAndActivoTrue(1L)).thenReturn(List.of(rubro));
        when(rubroRepository.calcularTotalPreliminarPorEvento(1L)).thenReturn(preliminar);
        when(presupuestoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.of(oficial));
        when(presupuestoRepository.findByEvento_IdOrderByFechaAprobacionDescIdDesc(1L))
                .thenReturn(List.of(oficial));

        PresupuestoResumenResponse resumen = presupuestoService.obtenerResumen(1L);

        assertNotNull(resumen);
        assertEquals(1L, resumen.eventoId());
        assertEquals("Congreso Internacional de TIC", resumen.eventoNombre());
        assertEquals(preliminar, resumen.totalPreliminarProyectado());
        assertEquals(1, resumen.cantidadRubrosPreliminares());
        assertNotNull(resumen.presupuestoOficial());
        assertEquals(1, resumen.historialVersiones().size());
    }
}
