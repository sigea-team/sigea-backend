package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.presupuesto.HistorialRubroResponse;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoPreliminarResponse;
import com.sigea.demosigea_backend.dto.presupuesto.RubroOperacionResponse;
import com.sigea.demosigea_backend.dto.presupuesto.RubroRequest;
import com.sigea.demosigea_backend.dto.presupuesto.RubroResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.RubroDuplicadoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.TipoOperacionRubro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sigea.demosigea_backend.service.PresupuestoPreliminarService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PresupuestoPreliminarControllerTest {

    private static final String BASE = "/api/v1/eventos/1/presupuesto";

    private MockMvc mockMvc;

    @Mock
    private PresupuestoPreliminarService presupuestoService;

    @InjectMocks
    private PresupuestoPreliminarController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RubroResponse rubro(Long id, boolean activo) {
        return new RubroResponse(id, 1L, "Transporte", new BigDecimal("2.00"), new BigDecimal("350000.00"),
                new BigDecimal("700000.00"), activo);
    }

    // ---------------- Criterio 1 ----------------

    @Test
    @DisplayName("Criterio 1: POST crea el rubro → 201 con el total actualizado")
    void agregar_retorna201ConTotal() throws Exception {
        when(presupuestoService.agregar(eq(1L), any(RubroRequest.class)))
                .thenReturn(new RubroOperacionResponse(rubro(7L, true), new BigDecimal("1500000.00"), 2));

        mockMvc.perform(post(BASE + "/rubros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Transporte\",\"cantidad\":2,\"valorUnitarioProyectado\":350000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rubro.id").value(7))
                .andExpect(jsonPath("$.rubro.subtotal").value(700000.00))
                .andExpect(jsonPath("$.totalPresupuesto").value(1500000.00))
                .andExpect(jsonPath("$.cantidadRubros").value(2))
                .andExpect(header().string("Location", "http://localhost" + BASE + "/rubros/7"));
    }

    @Test
    @DisplayName("Criterio 1: GET devuelve rubros y total")
    void consultar_retornaRubrosYTotal() throws Exception {
        when(presupuestoService.consultar(1L, false)).thenReturn(new PresupuestoPreliminarResponse(
                1L, "Congreso", EstadoEvento.en_configuracion, List.of(rubro(7L, true)), 1,
                new BigDecimal("700000.00"), false, true));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rubros.length()").value(1))
                .andExpect(jsonPath("$.total").value(700000.00))
                .andExpect(jsonPath("$.editable").value(true));
    }

    @Test
    @DisplayName("Criterio 3 (decisión PO): valor 0 pasa la validación")
    void agregar_valorCero_retorna201() throws Exception {
        when(presupuestoService.agregar(eq(1L), any(RubroRequest.class)))
                .thenReturn(new RubroOperacionResponse(rubro(8L, true), new BigDecimal("0.00"), 1));

        mockMvc.perform(post(BASE + "/rubros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Auditorio institucional\",\"valorUnitarioProyectado\":0}"))
                .andExpect(status().isCreated());
    }

    // ---------------- Criterio 3 ----------------

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"nombre\":\"\",\"valorUnitarioProyectado\":1000}",
            "{\"nombre\":\"   \",\"valorUnitarioProyectado\":1000}",
            "{\"valorUnitarioProyectado\":1000}"
    })
    @DisplayName("Criterio 3: sin nombre → 400 con el error en el campo nombre")
    void agregar_sinNombre_retorna400(String cuerpo) throws Exception {
        mockMvc.perform(post(BASE + "/rubros").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.nombre").value("El nombre del rubro es obligatorio."));
        verify(presupuestoService, never()).agregar(anyLong(), any());
    }

    @Test
    @DisplayName("Criterio 3: valor vacío → 400 con el error en el campo valorUnitarioProyectado")
    void agregar_valorVacio_retorna400() throws Exception {
        mockMvc.perform(post(BASE + "/rubros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Transporte\",\"valorUnitarioProyectado\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.valorUnitarioProyectado")
                        .value("El valor estimado del rubro es obligatorio."));
        verify(presupuestoService, never()).agregar(anyLong(), any());
    }

    @Test
    @DisplayName("Criterio 3: valor negativo → 400 con el error en el campo valorUnitarioProyectado")
    void agregar_valorNegativo_retorna400() throws Exception {
        mockMvc.perform(post(BASE + "/rubros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Transporte\",\"valorUnitarioProyectado\":-5000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.valorUnitarioProyectado")
                        .value("El valor estimado del rubro no puede ser negativo."));
        verify(presupuestoService, never()).agregar(anyLong(), any());
    }

    @Test
    @DisplayName("Criterio 3: cantidad negativa → 400")
    void agregar_cantidadNegativa_retorna400() throws Exception {
        mockMvc.perform(post(BASE + "/rubros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Transporte\",\"cantidad\":-1,\"valorUnitarioProyectado\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.cantidad").value("La cantidad no puede ser negativa."));
    }

    @Test
    @DisplayName("Criterio 3: valor no numérico → 400")
    void agregar_valorNoNumerico_retorna400() throws Exception {
        mockMvc.perform(post(BASE + "/rubros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Transporte\",\"valorUnitarioProyectado\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
    }

    @Test
    @DisplayName("Criterio 3: editar con valor negativo también se rechaza")
    void actualizar_valorNegativo_retorna400() throws Exception {
        mockMvc.perform(put(BASE + "/rubros/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Transporte\",\"valorUnitarioProyectado\":-1}"))
                .andExpect(status().isBadRequest());
        verify(presupuestoService, never()).actualizar(anyLong(), anyLong(), any());
    }

    // ---------------- Criterio 2 ----------------

    @Test
    @DisplayName("Criterio 2: PUT edita y devuelve el total actualizado")
    void actualizar_retorna200() throws Exception {
        when(presupuestoService.actualizar(eq(1L), eq(7L), any(RubroRequest.class)))
                .thenReturn(new RubroOperacionResponse(rubro(7L, true), new BigDecimal("1700000.00"), 2));

        mockMvc.perform(put(BASE + "/rubros/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Transporte\",\"cantidad\":2,\"valorUnitarioProyectado\":350000,"
                                + "\"motivo\":\"Cambio de tarifa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPresupuesto").value(1700000.00));
    }

    @Test
    @DisplayName("Criterio 2: DELETE desactiva el rubro y devuelve el total actualizado")
    void eliminar_retorna200() throws Exception {
        when(presupuestoService.eliminar(1L, 7L, "Lo cubre el patrocinador"))
                .thenReturn(new RubroOperacionResponse(rubro(7L, false), new BigDecimal("800000.00"), 1));

        mockMvc.perform(delete(BASE + "/rubros/7").param("motivo", "Lo cubre el patrocinador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rubro.activo").value(false))
                .andExpect(jsonPath("$.totalPresupuesto").value(800000.00));
    }

    @Test
    @DisplayName("Criterio 2: GET historial del rubro")
    void historialRubro_retorna200() throws Exception {
        when(presupuestoService.historialRubro(1L, 7L)).thenReturn(List.of(new HistorialRubroResponse(
                15L, 7L, TipoOperacionRubro.edicion,
                "Transporte", new BigDecimal("2.00"), new BigDecimal("300000.00"), new BigDecimal("600000.00"),
                "Transporte", new BigDecimal("2.00"), new BigDecimal("350000.00"), new BigDecimal("700000.00"),
                new BigDecimal("1400000.00"), new BigDecimal("1500000.00"), "Cambio de tarifa",
                2L, "Cristian Rodríguez", LocalDateTime.of(2026, 10, 8, 16, 0))));

        mockMvc.perform(get(BASE + "/rubros/7/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoOperacion").value("edicion"))
                .andExpect(jsonPath("$[0].valorUnitarioAnterior").value(300000.00))
                .andExpect(jsonPath("$[0].valorUnitarioNuevo").value(350000.00));
    }

    // ---------------- Errores de negocio ----------------

    @Test
    @DisplayName("Nombre duplicado → 409 RUBRO_DUPLICADO")
    void agregar_duplicado_retorna409() throws Exception {
        when(presupuestoService.agregar(eq(1L), any(RubroRequest.class)))
                .thenThrow(new RubroDuplicadoException("Ya existe un rubro llamado 'Transporte'..."));

        mockMvc.perform(post(BASE + "/rubros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Transporte\",\"valorUnitarioProyectado\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUBRO_DUPLICADO"));
    }

    @Test
    @DisplayName("Presupuesto aprobado o evento en ejecución → 409 OPERACION_NO_PERMITIDA")
    void eliminar_noModificable_retorna409() throws Exception {
        when(presupuestoService.eliminar(1L, 7L, null))
                .thenThrow(new OperacionNoPermitidaException("El evento ya tiene un presupuesto aprobado..."));

        mockMvc.perform(delete(BASE + "/rubros/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
    }

    @Test
    @DisplayName("Evento inexistente → 404")
    void consultar_eventoInexistente_retorna404() throws Exception {
        when(presupuestoService.consultar(1L, false))
                .thenThrow(new RecursoNoEncontradoException("No se encontró el evento con ID: 1"));

        mockMvc.perform(get(BASE))
                .andExpect(status().isNotFound());
    }
}
