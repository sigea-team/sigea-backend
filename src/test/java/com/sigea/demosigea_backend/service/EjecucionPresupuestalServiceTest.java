package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.presupuesto.EjecucionPresupuestalResponse;
import com.sigea.demosigea_backend.dto.presupuesto.EjecucionRubroResponse;
import com.sigea.demosigea_backend.dto.presupuesto.GastoEjecutadoRequest;
import com.sigea.demosigea_backend.dto.presupuesto.GastoRegistradoResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.EstadoPresupuestoAprobado;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.GastoEjecutado;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.PresupuestoAprobado;
import com.sigea.demosigea_backend.model.RubroPresupuestal;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.GastoEjecutadoRepository;
import com.sigea.demosigea_backend.repository.GastoEjecutadoRepository.AcumuladoRubro;
import com.sigea.demosigea_backend.repository.PresupuestoAprobadoRepository;
import com.sigea.demosigea_backend.repository.RubroPresupuestalRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EjecucionPresupuestalServiceTest {

    private static final String CORREO = "carlos.gomez@ufps.edu.co";

    @Mock
    private GastoEjecutadoRepository gastoRepository;

    @Mock
    private RubroPresupuestalRepository rubroRepository;

    @Mock
    private PresupuestoAprobadoRepository presupuestoAprobadoRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private EjecucionPresupuestalService service;

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Datos de prueba
    // ------------------------------------------------------------------

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private Evento evento(EstadoEvento estado) {
        return Evento.builder()
                .id(1L).nombre("Congreso de Ingeniería de Sistemas")
                .fechaInicio(LocalDate.of(2026, 11, 20)).fechaFin(LocalDate.of(2026, 11, 22))
                .semestre("2026-2").estado(estado)
                .build();
    }

    /** Rubro de 2 × 500.000 = 1.000.000. */
    private RubroPresupuestal rubro(Long id, Evento evento, boolean activo) {
        return RubroPresupuestal.builder()
                .id(id).evento(evento).nombre("Transporte de conferencistas")
                .cantidad(bd("2.00")).valorUnitarioProyectado(bd("500000.00")).activo(activo)
                .build();
    }

    private PresupuestoAprobado aprobado(Evento evento) {
        return PresupuestoAprobado.builder()
                .id(10L).evento(evento).valorTotalAprobado(bd("5000000.00"))
                .evidenciaUrl("https://soporte/acta.pdf").estado(EstadoPresupuestoAprobado.aprobado)
                .build();
    }

    private Usuario usuario() {
        Persona persona = Persona.builder()
                .id(5L).nombres("Carlos Daniel").apellidos("Gómez").correo(CORREO)
                .numeroDocumento("1090111222").tipoDocumento("CC")
                .build();
        return Usuario.builder().id(2L).persona(persona).build();
    }

    private GastoEjecutadoRequest request(String valor) {
        return new GastoEjecutadoRequest(7L, valor == null ? null : bd(valor), LocalDate.now(),
                "  https://archivos.ufps.edu.co/gastos/factura-001.pdf  ", "Tiquetes aéreos");
    }

    private static AcumuladoRubro acumulado(Long rubroId, String total, long cantidad) {
        return new AcumuladoRubro() {
            @Override
            public Long getRubroId() {
                return rubroId;
            }

            @Override
            public BigDecimal getTotalEjecutado() {
                return new BigDecimal(total);
            }

            @Override
            public Long getCantidadGastos() {
                return cantidad;
            }
        };
    }

    /** Evento con presupuesto aprobado y el rubro 7 con {@code ejecutado} ya gastado. */
    private RubroPresupuestal prepararRegistro(Evento evento, String ejecutado) {
        RubroPresupuestal rubro = rubro(7L, evento, true);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(presupuestoAprobadoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.of(aprobado(evento)));
        when(rubroRepository.findByIdAndEvento_Id(7L, 1L)).thenReturn(Optional.of(rubro));
        when(gastoRepository.sumarPorRubro(7L)).thenReturn(bd(ejecutado));
        return rubro;
    }

    /** Usuario en sesión, guardado del gasto y totales después del registro. */
    private void prepararGuardado(String totalEvento, long gastosRubro) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CORREO, "pwd", Collections.emptyList()));
        when(usuarioRepository.findByPersona_CorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario()));
        when(gastoRepository.save(any(GastoEjecutado.class))).thenAnswer(inv -> {
            GastoEjecutado g = inv.getArgument(0);
            g.setId(15L);
            return g;
        });
        when(gastoRepository.countByRubro_Id(7L)).thenReturn(gastosRubro);
        when(gastoRepository.sumarPorEvento(1L)).thenReturn(bd(totalEvento));
    }

    // ==================================================================
    // Criterio 1: registrar gasto, actualizar acumulado y recalcular %
    // ==================================================================

    @Test
    @DisplayName("Criterio 1: guarda el gasto, actualiza el acumulado del rubro y recalcula el % del evento")
    void registrar_dentroDelDisponible_guardaYRecalcula() {
        prepararRegistro(evento(EstadoEvento.en_ejecucion), "200000.00");
        prepararGuardado("1250000.00", 2);

        GastoRegistradoResponse r = service.registrarGasto(1L, request("300000"));

        ArgumentCaptor<GastoEjecutado> captor = ArgumentCaptor.forClass(GastoEjecutado.class);
        verify(gastoRepository).save(captor.capture());
        GastoEjecutado guardado = captor.getValue();
        assertEquals(bd("300000.00"), guardado.getValor());
        assertEquals("https://archivos.ufps.edu.co/gastos/factura-001.pdf", guardado.getSoporteUrl());
        assertEquals(2L, guardado.getRegistradoPor().getId());

        assertEquals(15L, r.gasto().id());
        assertEquals("Carlos Daniel Gómez", r.gasto().registradoPorNombre());
        assertEquals(bd("1000000.00"), r.rubro().presupuestado());
        assertEquals(bd("500000.00"), r.rubro().ejecutado());
        assertEquals(bd("500000.00"), r.rubro().disponible());
        assertEquals(bd("50.00"), r.rubro().porcentajeEjecucion());
        assertEquals(2L, r.rubro().cantidadGastos());
        assertFalse(r.rubro().excedido());

        assertEquals(bd("5000000.00"), r.valorTotalAprobado());
        assertEquals(bd("1250000.00"), r.totalEjecutado());
        assertEquals(bd("25.00"), r.porcentajeEjecucion());
        assertFalse(r.excedioDisponible());
        assertNull(r.advertencia());

        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.GASTO_REGISTRADO),
                eq(EjecucionPresupuestalService.ENTIDAD_GASTO), eq(15L), anyMap());
    }

    @Test
    @DisplayName("Criterio 1: sin presupuesto aprobado no se pueden registrar gastos")
    void registrar_sinPresupuestoAprobado_rechaza() {
        when(eventoRepository.findById(1L))
                .thenReturn(Optional.of(evento(EstadoEvento.en_ejecucion)));
        when(presupuestoAprobadoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.empty());

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> service.registrarGasto(1L, request("300000")));
        assertTrue(ex.getMessage().contains("presupuesto aprobado"));
        verify(gastoRepository, never()).save(any());
    }

    @Test
    @DisplayName("El rubro debe existir en el evento")
    void registrar_rubroDeOtroEvento_404() {
        Evento evento = evento(EstadoEvento.en_ejecucion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(presupuestoAprobadoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.of(aprobado(evento)));
        when(rubroRepository.findByIdAndEvento_Id(7L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.registrarGasto(1L, request("300000")));
        verify(gastoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un rubro eliminado no admite gastos")
    void registrar_rubroEliminado_rechaza() {
        Evento evento = evento(EstadoEvento.en_ejecucion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(presupuestoAprobadoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.of(aprobado(evento)));
        when(rubroRepository.findByIdAndEvento_Id(7L, 1L)).thenReturn(Optional.of(rubro(7L, evento, false)));

        assertThrows(OperacionNoPermitidaException.class,
                () -> service.registrarGasto(1L, request("300000")));
        verify(gastoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un evento cerrado no admite gastos")
    void registrar_eventoCerrado_rechaza() {
        when(eventoRepository.findById(1L))
                .thenReturn(Optional.of(evento(EstadoEvento.cerrado)));

        assertThrows(OperacionNoPermitidaException.class,
                () -> service.registrarGasto(1L, request("300000")));
        verify(gastoRepository, never()).save(any());
    }

    // ==================================================================
    // Gasto que supera el disponible del rubro: siempre se permite
    // ==================================================================

    @Test
    @DisplayName("Exceso: un gasto que supera el disponible se registra y la respuesta lo informa")
    void registrar_exceso_seRegistraEInforma() {
        prepararRegistro(evento(EstadoEvento.en_ejecucion), "200000.00");
        prepararGuardado("1100000.00", 2);

        GastoRegistradoResponse r = service.registrarGasto(1L, request("900000"));

        verify(gastoRepository).save(any(GastoEjecutado.class));
        assertTrue(r.excedioDisponible());
        assertNotNull(r.advertencia());
        assertTrue(r.advertencia().contains("800.000"), r.advertencia());   // disponible
        assertTrue(r.advertencia().contains("100.000"), r.advertencia());   // exceso total del rubro
        assertTrue(r.rubro().excedido());
        assertEquals(bd("1100000.00"), r.rubro().ejecutado());
        assertEquals(bd("-100000.00"), r.rubro().disponible());
        assertEquals(bd("110.00"), r.rubro().porcentajeEjecucion());
        assertEquals(bd("22.00"), r.porcentajeEjecucion());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.GASTO_REGISTRADO),
                eq(EjecucionPresupuestalService.ENTIDAD_GASTO), eq(15L), anyMap());
    }

    @Test
    @DisplayName("Exceso: si el rubro ya estaba sobregirado, se informa el exceso total acumulado")
    void registrar_rubroYaSobregirado_informaExcesoTotal() {
        // Presupuestado 1.000.000, ya ejecutado 1.050.000 (disponible −50.000); nuevo gasto de 10.000.
        prepararRegistro(evento(EstadoEvento.en_ejecucion), "1050000.00");
        prepararGuardado("1060000.00", 3);

        GastoRegistradoResponse r = service.registrarGasto(1L, request("10000"));

        assertTrue(r.excedioDisponible());
        assertTrue(r.advertencia().contains("ya superaba"), r.advertencia());
        assertTrue(r.advertencia().contains("50.000"), r.advertencia());   // exceso previo
        assertTrue(r.advertencia().contains("60.000"), r.advertencia());   // exceso total tras el gasto
        assertEquals(bd("1060000.00"), r.rubro().ejecutado());
        assertEquals(bd("-60000.00"), r.rubro().disponible());
        assertTrue(r.rubro().excedido());
    }

    @Test
    @DisplayName("Exceso: un gasto igual al disponible no se marca como exceso")
    void registrar_igualAlDisponible_noEsExceso() {
        prepararRegistro(evento(EstadoEvento.en_ejecucion), "200000.00");
        prepararGuardado("1000000.00", 2);

        GastoRegistradoResponse r = service.registrarGasto(1L, request("800000"));

        assertFalse(r.excedioDisponible());
        assertNull(r.advertencia());
        assertEquals(bd("0.00"), r.rubro().disponible());
        assertEquals(bd("100.00"), r.rubro().porcentajeEjecucion());
    }

    // ==================================================================
    // Criterio 3: sin valor o sin soporte
    // ==================================================================

    @Test
    @DisplayName("Criterio 3: gasto sin valor → rechazado sin tocar la base de datos")
    void registrar_sinValor_rechaza() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.registrarGasto(1L, request(null)));
        assertTrue(ex.getMessage().contains("valor"));
        verifyNoInteractions(gastoRepository, eventoRepository, rubroRepository);
    }

    @Test
    @DisplayName("Criterio 3: gasto con valor negativo → rechazado")
    void registrar_valorNegativo_rechaza() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.registrarGasto(1L, request("-5000")));
        assertTrue(ex.getMessage().contains("negativo"));
        verifyNoInteractions(gastoRepository);
    }

    @Test
    @DisplayName("Criterio 3: gasto con valor 0 → se acepta y no cambia el acumulado")
    void registrar_valorCero_seRegistra() {
        prepararRegistro(evento(EstadoEvento.en_ejecucion), "200000.00");
        prepararGuardado("200000.00", 2);

        GastoRegistradoResponse r = service.registrarGasto(1L, request("0"));

        verify(gastoRepository).save(any(GastoEjecutado.class));
        assertEquals(bd("0.00"), r.gasto().valor());
        assertEquals(bd("200000.00"), r.rubro().ejecutado());
        assertEquals(bd("20.00"), r.rubro().porcentajeEjecucion());
        assertFalse(r.excedioDisponible());
    }

    @Test
    @DisplayName("Criterio 3: el soporte es opcional; vacío se guarda como null")
    void registrar_sinSoporte_seRegistra() {
        prepararRegistro(evento(EstadoEvento.en_ejecucion), "0.00");
        prepararGuardado("1000.00", 1);
        GastoEjecutadoRequest sinSoporte = new GastoEjecutadoRequest(7L, bd("1000"), LocalDate.now(), "   ", null);

        GastoRegistradoResponse r = service.registrarGasto(1L, sinSoporte);

        ArgumentCaptor<GastoEjecutado> captor = ArgumentCaptor.forClass(GastoEjecutado.class);
        verify(gastoRepository).save(captor.capture());
        assertNull(captor.getValue().getSoporteUrl());
        assertNull(r.gasto().soporteUrl());
        assertEquals(bd("1000.00"), r.gasto().valor());
    }

    @Test
    @DisplayName("La fecha del gasto no puede ser futura")
    void registrar_fechaFutura_rechaza() {
        GastoEjecutadoRequest futuro = new GastoEjecutadoRequest(7L, bd("1000"), LocalDate.now().plusDays(1),
                "https://soporte.pdf", null);

        assertThrows(SolicitudInvalidaException.class, () -> service.registrarGasto(1L, futuro)) ;
        verifyNoInteractions(gastoRepository);
    }

    // ==================================================================
    // Criterio 4: consulta de la ejecución
    // ==================================================================

    @Test
    @DisplayName("Criterio 4: sin gastos → 0 % de ejecución sin error")
    void consultar_sinGastos_ceroPorCiento() {
        Evento evento = evento(EstadoEvento.habilitado);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(presupuestoAprobadoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.of(aprobado(evento)));
        when(gastoRepository.acumuladosPorRubro(1L)).thenReturn(List.of());
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of(rubro(7L, evento, true)));
        when(gastoRepository.sumarPorEvento(1L)).thenReturn(BigDecimal.ZERO);

        EjecucionPresupuestalResponse r = service.consultarEjecucion(1L);

        assertTrue(r.tienePresupuestoAprobado());
        assertEquals(bd("0.00"), r.totalEjecutado());
        assertEquals(bd("0.00"), r.porcentajeEjecucion());
        assertEquals(bd("5000000.00"), r.saldoDisponible());
        assertEquals(0L, r.cantidadGastos());
        assertEquals(1, r.rubros().size());
        assertEquals(bd("0.00"), r.rubros().get(0).ejecutado());
        assertEquals(bd("0.00"), r.rubros().get(0).porcentajeEjecucion());
        assertEquals(bd("1000000.00"), r.rubros().get(0).disponible());
    }

    @Test
    @DisplayName("Criterio 4: sin presupuesto aprobado ni rubros → 0 % sin error")
    void consultar_sinPresupuestoAprobado_ceroPorCiento() {
        when(eventoRepository.findById(1L))
                .thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(presupuestoAprobadoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.empty());
        when(gastoRepository.acumuladosPorRubro(1L)).thenReturn(List.of());
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of());
        when(gastoRepository.sumarPorEvento(1L)).thenReturn(BigDecimal.ZERO);

        EjecucionPresupuestalResponse r = service.consultarEjecucion(1L);

        assertFalse(r.tienePresupuestoAprobado());
        assertEquals(bd("0.00"), r.valorTotalAprobado());
        assertEquals(bd("0.00"), r.porcentajeEjecucion());
        assertTrue(r.rubros().isEmpty());
    }

    @Test
    @DisplayName("Criterio 1: la consulta muestra acumulado y % por rubro y del evento")
    void consultar_conGastos_calculaPorRubroYEvento() {
        Evento evento = evento(EstadoEvento.en_ejecucion);
        RubroPresupuestal refrigerios = RubroPresupuestal.builder()
                .id(8L).evento(evento).nombre("Refrigerios")
                .cantidad(bd("100.00")).valorUnitarioProyectado(bd("8000.00")).activo(true)
                .build();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(presupuestoAprobadoRepository.findByEvento_IdAndEstado(1L, EstadoPresupuestoAprobado.aprobado))
                .thenReturn(Optional.of(aprobado(evento)));
        when(gastoRepository.acumuladosPorRubro(1L)).thenReturn(List.of(acumulado(7L, "250000.00", 2)));
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L))
                .thenReturn(List.of(rubro(7L, evento, true), refrigerios));
        when(gastoRepository.sumarPorEvento(1L)).thenReturn(bd("250000.00"));

        EjecucionPresupuestalResponse r = service.consultarEjecucion(1L);

        assertEquals(bd("1800000.00"), r.totalPresupuestadoRubros());
        assertEquals(bd("250000.00"), r.totalEjecutado());
        assertEquals(bd("4750000.00"), r.saldoDisponible());
        assertEquals(bd("5.00"), r.porcentajeEjecucion());
        assertEquals(2L, r.cantidadGastos());

        EjecucionRubroResponse transporte = r.rubros().get(0);
        assertEquals(bd("25.00"), transporte.porcentajeEjecucion());
        assertEquals(bd("750000.00"), transporte.disponible());
        assertEquals(2L, transporte.cantidadGastos());

        EjecucionRubroResponse refri = r.rubros().get(1);
        assertEquals(bd("0.00"), refri.porcentajeEjecucion());
        assertEquals(0L, refri.cantidadGastos());
    }

    @Test
    @DisplayName("El porcentaje no divide por cero y redondea a 2 decimales")
    void porcentaje_baseCeroYRedondeo() {
        assertEquals(bd("0.00"), EjecucionPresupuestalService.porcentaje(bd("1000"), BigDecimal.ZERO));
        assertEquals(bd("0.00"), EjecucionPresupuestalService.porcentaje(null, bd("1000")));
        assertEquals(bd("33.33"), EjecucionPresupuestalService.porcentaje(bd("1"), bd("3")));
        assertEquals(bd("66.67"), EjecucionPresupuestalService.porcentaje(bd("2"), bd("3")));
    }

    @Test
    @DisplayName("Consultar un evento inexistente → 404")
    void consultar_eventoInexistente_404() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.consultarEjecucion(99L));
    }
}
