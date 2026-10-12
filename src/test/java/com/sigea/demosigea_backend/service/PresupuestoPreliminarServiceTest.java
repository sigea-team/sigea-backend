package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoPreliminarResponse;
import com.sigea.demosigea_backend.dto.presupuesto.RubroOperacionResponse;
import com.sigea.demosigea_backend.dto.presupuesto.RubroRequest;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.RubroDuplicadoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.HistorialRubro;
import com.sigea.demosigea_backend.model.RubroPresupuestal;
import com.sigea.demosigea_backend.model.TipoOperacionRubro;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.HistorialRubroRepository;
import com.sigea.demosigea_backend.repository.RubroPresupuestalRepository;
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

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresupuestoPreliminarServiceTest {

    @Mock
    private RubroPresupuestalRepository rubroRepository;

    @Mock
    private HistorialRubroRepository historialRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PresupuestoPreliminarService service;

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private Evento evento(EstadoEvento estado) {
        return Evento.builder()
                .id(1L).nombre("Congreso de Ingeniería de Sistemas").tipo("Congreso")
                .fechaInicio(LocalDate.of(2026, 11, 20)).fechaFin(LocalDate.of(2026, 11, 22))
                .semestre("2026-2").estado(estado)
                .build();
    }

    private RubroPresupuestal rubro(Long id, Evento evento, String nombre, String cantidad, String valor) {
        return RubroPresupuestal.builder()
                .id(id).evento(evento).nombre(nombre)
                .cantidad(bd(cantidad)).valorUnitarioProyectado(bd(valor)).activo(true)
                .build();
    }

    /** Evento editable, sin presupuesto aprobado. */
    private Evento eventoEditable() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(rubroRepository.existePresupuestoAprobado(1L)).thenReturn(false);
        return evento;
    }

    private void simularGuardadoConId(Long id) {
        when(rubroRepository.saveAndFlush(any(RubroPresupuestal.class))).thenAnswer(inv -> {
            RubroPresupuestal r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(id);
            }
            return r;
        });
    }

    private HistorialRubro historialGuardado() {
        ArgumentCaptor<HistorialRubro> captor = ArgumentCaptor.forClass(HistorialRubro.class);
        verify(historialRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---------------- Criterio 1 ----------------

    @Test
    @DisplayName("Criterio 1: agregar un rubro calcula el total con los rubros existentes y registra la creación")
    void agregar_calculaTotalYRegistraHistorial() {
        Evento evento = eventoEditable();
        List<RubroPresupuestal> vigentes = List.of(rubro(1L, evento, "Refrigerios", "100", "8000")); // 800.000
        when(rubroRepository.existsByEvento_IdAndNombreIgnoreCaseAndActivoTrue(1L, "Transporte")).thenReturn(false);
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(vigentes);
        simularGuardadoConId(2L);

        RubroOperacionResponse r = service.agregar(1L,
                new RubroRequest("  Transporte  ", bd("2"), bd("350000"), null));

        assertEquals(2L, r.rubro().id());
        assertEquals("Transporte", r.rubro().nombre());
        assertEquals(0, bd("700000").compareTo(r.rubro().subtotal()));
        assertEquals(0, bd("1500000").compareTo(r.totalPresupuesto()));
        assertEquals(2, r.cantidadRubros());

        HistorialRubro h = historialGuardado();
        assertEquals(TipoOperacionRubro.creacion, h.getTipoOperacion());
        assertNull(h.getNombreAnterior());
        assertEquals("Transporte", h.getNombreNuevo());
        assertEquals(0, bd("800000").compareTo(h.getTotalPresupuestoAnterior()));
        assertEquals(0, bd("1500000").compareTo(h.getTotalPresupuestoNuevo()));
    }

    @Test
    @DisplayName("Criterio 1: sin cantidad se asume 1")
    void agregar_sinCantidad_asumeUno() {
        eventoEditable();
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of());
        simularGuardadoConId(1L);

        RubroOperacionResponse r = service.agregar(1L, new RubroRequest("Auditorio", null, bd("2500000"), null));

        assertEquals(0, BigDecimal.ONE.compareTo(r.rubro().cantidad()));
        assertEquals(0, bd("2500000").compareTo(r.totalPresupuesto()));
    }

    @Test
    @DisplayName("Criterio 3 (decisión PO): el valor 0 se acepta")
    void agregar_valorCero_seAcepta() {
        eventoEditable();
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of());
        simularGuardadoConId(1L);

        RubroOperacionResponse r = service.agregar(1L, new RubroRequest("Auditorio institucional", bd("1"), bd("0"), null));

        assertEquals(0, BigDecimal.ZERO.compareTo(r.totalPresupuesto()));
        verify(historialRepository).save(any(HistorialRubro.class));
    }

    @Test
    @DisplayName("Nombre repetido entre rubros vigentes → RubroDuplicadoException y no se guarda")
    void agregar_nombreDuplicado_lanza409() {
        eventoEditable();
        when(rubroRepository.existsByEvento_IdAndNombreIgnoreCaseAndActivoTrue(1L, "Transporte")).thenReturn(true);

        assertThrows(RubroDuplicadoException.class,
                () -> service.agregar(1L, new RubroRequest("Transporte", bd("1"), bd("10"), null)));
        verify(rubroRepository, never()).saveAndFlush(any());
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si el índice único detecta un duplicado concurrente se responde 409, no 500")
    void agregar_duplicadoConcurrente_lanza409() {
        eventoEditable();
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of());
        ConstraintViolationException violacion = new ConstraintViolationException("duplicado",
                new SQLException("duplicate key", PresupuestoPreliminarService.SQLSTATE_VIOLACION_UNICIDAD),
                PresupuestoPreliminarService.INDICE_NOMBRE_ACTIVO);
        when(rubroRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup", violacion));

        assertThrows(RubroDuplicadoException.class,
                () -> service.agregar(1L, new RubroRequest("Transporte", bd("1"), bd("10"), null)));
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Evento inexistente → 404")
    void agregar_eventoInexistente_lanza404() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.agregar(99L, new RubroRequest("Transporte", bd("1"), bd("10"), null)));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoEvento.class, names = {"en_ejecucion", "cerrado"})
    @DisplayName("Desde la ejecución del evento el presupuesto preliminar queda congelado")
    void agregar_eventoEnEjecucionOCerrado_lanza409(EstadoEvento estado) {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(estado)));

        assertThrows(OperacionNoPermitidaException.class,
                () -> service.agregar(1L, new RubroRequest("Transporte", bd("1"), bd("10"), null)));
        verify(rubroRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Con presupuesto aprobado (HU-08) no se pueden agregar rubros")
    void agregar_presupuestoAprobado_lanza409() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.habilitado)));
        when(rubroRepository.existePresupuestoAprobado(1L)).thenReturn(true);

        assertThrows(OperacionNoPermitidaException.class,
                () -> service.agregar(1L, new RubroRequest("Transporte", bd("1"), bd("10"), null)));
        verify(rubroRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Consultar: el total solo suma rubros vigentes aunque se listen los eliminados")
    void consultar_totalSoloVigentes() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        RubroPresupuestal a = rubro(1L, evento, "Transporte", "2", "350000");     // 700.000
        RubroPresupuestal b = rubro(2L, evento, "Refrigerios", "150", "7500.50"); // 1.125.075
        RubroPresupuestal eliminado = rubro(3L, evento, "Hotel", "3", "200000");
        eliminado.desactivar();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of(a, b));
        when(rubroRepository.findByEvento_IdOrderByActivoDescIdAsc(1L)).thenReturn(List.of(a, b, eliminado));
        when(rubroRepository.existePresupuestoAprobado(1L)).thenReturn(false);

        PresupuestoPreliminarResponse r = service.consultar(1L, true);

        assertEquals(3, r.rubros().size());
        assertEquals(2, r.cantidadRubros());
        assertEquals(bd("1825075.00"), r.total());
        assertTrue(r.editable());
        assertFalse(r.presupuestoAprobado());
    }

    @Test
    @DisplayName("Consultar: con presupuesto aprobado se informa que ya no es editable")
    void consultar_aprobado_noEditable() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.habilitado)));
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of());
        when(rubroRepository.existePresupuestoAprobado(1L)).thenReturn(true);

        PresupuestoPreliminarResponse r = service.consultar(1L, false);

        assertTrue(r.presupuestoAprobado());
        assertFalse(r.editable());
        assertEquals(bd("0.00"), r.total());
    }

    // ---------------- Criterio 2 ----------------

    @Test
    @DisplayName("Criterio 2: editar actualiza el total y conserva valores anteriores y nuevos en el historial")
    void actualizar_recalculaTotalYGuardaHistorial() {
        Evento evento = eventoEditable();
        RubroPresupuestal transporte = rubro(1L, evento, "Transporte", "2", "300000");   // 600.000
        RubroPresupuestal refrigerios = rubro(2L, evento, "Refrigerios", "100", "8000"); // 800.000
        when(rubroRepository.findByIdAndEvento_Id(1L, 1L)).thenReturn(Optional.of(transporte));
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of(transporte, refrigerios));
        when(rubroRepository.existsByEvento_IdAndNombreIgnoreCaseAndActivoTrueAndIdNot(1L, "Transporte aéreo", 1L))
                .thenReturn(false);
        simularGuardadoConId(1L);

        RubroOperacionResponse r = service.actualizar(1L, 1L,
                new RubroRequest("Transporte aéreo", bd("2"), bd("450000"), "Cambio de tarifa"));

        // 1.400.000 - 600.000 (subtotal anterior) + 900.000 (subtotal nuevo)
        assertEquals(0, bd("1700000").compareTo(r.totalPresupuesto()));
        assertEquals(0, bd("900000").compareTo(r.rubro().subtotal()));

        HistorialRubro h = historialGuardado();
        assertEquals(TipoOperacionRubro.edicion, h.getTipoOperacion());
        assertEquals("Transporte", h.getNombreAnterior());
        assertEquals(0, bd("300000").compareTo(h.getValorUnitarioAnterior()));
        assertEquals("Transporte aéreo", h.getNombreNuevo());
        assertEquals(0, bd("450000").compareTo(h.getValorUnitarioNuevo()));
        assertEquals(0, bd("1400000").compareTo(h.getTotalPresupuestoAnterior()));
        assertEquals(0, bd("1700000").compareTo(h.getTotalPresupuestoNuevo()));
        assertEquals("Cambio de tarifa", h.getMotivo());
    }

    @Test
    @DisplayName("Criterio 2: editar sin cambios no genera historial")
    void actualizar_sinCambios_noRegistraHistorial() {
        Evento evento = eventoEditable();
        RubroPresupuestal transporte = rubro(1L, evento, "Transporte", "2", "300000");
        when(rubroRepository.findByIdAndEvento_Id(1L, 1L)).thenReturn(Optional.of(transporte));
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of(transporte));

        RubroOperacionResponse r = service.actualizar(1L, 1L,
                new RubroRequest("Transporte", bd("2.00"), bd("300000.00"), null));

        assertEquals(0, bd("600000").compareTo(r.totalPresupuesto()));
        verify(rubroRepository, never()).saveAndFlush(any());
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar con el nombre de otro rubro vigente → RubroDuplicadoException")
    void actualizar_nombreDeOtroRubro_lanza409() {
        Evento evento = eventoEditable();
        RubroPresupuestal transporte = rubro(1L, evento, "Transporte", "2", "300000");
        when(rubroRepository.findByIdAndEvento_Id(1L, 1L)).thenReturn(Optional.of(transporte));
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L)).thenReturn(List.of(transporte));
        when(rubroRepository.existsByEvento_IdAndNombreIgnoreCaseAndActivoTrueAndIdNot(1L, "Refrigerios", 1L))
                .thenReturn(true);

        assertThrows(RubroDuplicadoException.class,
                () -> service.actualizar(1L, 1L, new RubroRequest("Refrigerios", bd("2"), bd("300000"), null)));
        assertEquals("Transporte", transporte.getNombre());
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar un rubro eliminado → 409")
    void actualizar_rubroEliminado_lanza409() {
        Evento evento = eventoEditable();
        RubroPresupuestal eliminado = rubro(1L, evento, "Transporte", "2", "300000");
        eliminado.desactivar();
        when(rubroRepository.findByIdAndEvento_Id(1L, 1L)).thenReturn(Optional.of(eliminado));

        assertThrows(OperacionNoPermitidaException.class,
                () -> service.actualizar(1L, 1L, new RubroRequest("Transporte", bd("3"), bd("300000"), null)));
    }

    @Test
    @DisplayName("Editar un rubro de otro evento → 404")
    void actualizar_rubroDeOtroEvento_lanza404() {
        eventoEditable();
        when(rubroRepository.findByIdAndEvento_Id(50L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(1L, 50L, new RubroRequest("Transporte", bd("3"), bd("1"), null)));
    }

    @Test
    @DisplayName("Criterio 2: eliminar es borrado lógico, descuenta el subtotal y conserva lo eliminado en el historial")
    void eliminar_borradoLogicoYHistorial() {
        Evento evento = eventoEditable();
        RubroPresupuestal transporte = rubro(1L, evento, "Transporte", "2", "300000");   // 600.000
        RubroPresupuestal refrigerios = rubro(2L, evento, "Refrigerios", "100", "8000"); // 800.000
        when(rubroRepository.findByIdAndEvento_Id(1L, 1L)).thenReturn(Optional.of(transporte));
        when(rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(1L))
                .thenReturn(new ArrayList<>(List.of(transporte, refrigerios)));
        when(rubroRepository.save(any(RubroPresupuestal.class))).thenAnswer(inv -> inv.getArgument(0));

        RubroOperacionResponse r = service.eliminar(1L, 1L, "Lo cubre el patrocinador");

        assertFalse(r.rubro().activo());
        assertFalse(transporte.isActivo());
        assertEquals(0, bd("800000").compareTo(r.totalPresupuesto()));
        assertEquals(1, r.cantidadRubros());
        verify(rubroRepository, never()).delete(any());
        verify(rubroRepository, never()).deleteById(any());

        HistorialRubro h = historialGuardado();
        assertEquals(TipoOperacionRubro.eliminacion, h.getTipoOperacion());
        assertEquals("Transporte", h.getNombreAnterior());
        assertNull(h.getNombreNuevo());
        assertEquals(0, bd("1400000").compareTo(h.getTotalPresupuestoAnterior()));
        assertEquals(0, bd("800000").compareTo(h.getTotalPresupuestoNuevo()));
        assertEquals("Lo cubre el patrocinador", h.getMotivo());
    }

    @Test
    @DisplayName("Eliminar un rubro ya eliminado → 409")
    void eliminar_yaEliminado_lanza409() {
        Evento evento = eventoEditable();
        RubroPresupuestal eliminado = rubro(1L, evento, "Transporte", "2", "300000");
        eliminado.desactivar();
        when(rubroRepository.findByIdAndEvento_Id(1L, 1L)).thenReturn(Optional.of(eliminado));

        assertThrows(OperacionNoPermitidaException.class, () -> service.eliminar(1L, 1L, null));
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar con un motivo de más de 255 caracteres → 400")
    void eliminar_motivoLargo_lanza400() {
        assertThrows(SolicitudInvalidaException.class, () -> service.eliminar(1L, 1L, "x".repeat(256)));
    }

    // ---------------- Utilidades ----------------

    @Test
    @DisplayName("El total es la suma de los subtotales redondeados a 2 decimales")
    void calcularTotal_redondeo() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        List<RubroPresupuestal> rubros = List.of(
                rubro(1L, evento, "A", "1.50", "0.33"),  // 0.495 → 0.50
                rubro(2L, evento, "B", "1.50", "0.33")); // 0.495 → 0.50
        assertEquals(bd("1.00"), PresupuestoPreliminarService.calcularTotal(rubros));
    }

    @Test
    @DisplayName("Normaliza espacios del nombre")
    void normalizarNombre() {
        assertEquals("Transporte aéreo", PresupuestoPreliminarService.normalizarNombre("  Transporte    aéreo "));
    }

    @Test
    @DisplayName("Solo el índice de nombre vigente se traduce a duplicado")
    void esViolacionNombreActivo_otraRestriccion() {
        ConstraintViolationException otra = new ConstraintViolationException("check",
                new SQLException("check", "23514"), "rubros_presupuestales_cantidad_check");
        assertFalse(PresupuestoPreliminarService.esViolacionNombreActivo(new DataIntegrityViolationException("x", otra)));
    }
}
