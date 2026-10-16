package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.presupuesto.EjecucionPresupuestalResponse;
import com.sigea.demosigea_backend.dto.presupuesto.EjecucionRubroResponse;
import com.sigea.demosigea_backend.dto.presupuesto.GastoEjecutadoRequest;
import com.sigea.demosigea_backend.dto.presupuesto.GastoEjecutadoResponse;
import com.sigea.demosigea_backend.dto.presupuesto.GastoRegistradoResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.service.EjecucionPresupuestalService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EjecucionPresupuestalControllerTest {

    private static final String BASE = "/api/v1/eventos/1/ejecucion-presupuestal";

    private MockMvc mockMvc;

    @Mock
    private EjecucionPresupuestalService ejecucionService;

    @InjectMocks
    private EjecucionPresupuestalController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private String cuerpo(String valor, String soporte) {
        String hoy = LocalDate.now().toString();
        StringBuilder sb = new StringBuilder("{\"rubroId\":7,\"fechaGasto\":\"").append(hoy).append("\"");
        if (valor != null) {
            sb.append(",\"valor\":").append(valor);
        }
        if (soporte != null) {
            sb.append(",\"soporteUrl\":\"").append(soporte).append("\"");
        }
        return sb.append("}").toString();
    }

    private GastoRegistradoResponse registrado(boolean exceso) {
        GastoEjecutadoResponse gasto = new GastoEjecutadoResponse(15L, 1L, 7L, "Transporte", null,
                bd("300000.00"), LocalDate.now(), "https://soporte.pdf", 2L, "Carlos Gómez");
        EjecucionRubroResponse rubro = new EjecucionRubroResponse(7L, "Transporte", bd("1000000.00"),
                bd("500000.00"), bd("500000.00"), bd("50.00"), false, 2);
        return new GastoRegistradoResponse(gasto, rubro, bd("5000000.00"), bd("1250000.00"), bd("25.00"),
                exceso, exceso ? "supera el disponible" : null);
    }

    // ---------------- Criterio 1 ----------------

    @Test
    @DisplayName("Criterio 1: POST registra el gasto → 201 con acumulado y % recalculados")
    void registrar_retorna201() throws Exception {
        when(ejecucionService.registrarGasto(eq(1L), any(GastoEjecutadoRequest.class)))
                .thenReturn(registrado(false));

        mockMvc.perform(post(BASE + "/gastos").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("300000", "https://soporte.pdf")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/gastos/15")))
                .andExpect(jsonPath("$.gasto.id").value(15))
                .andExpect(jsonPath("$.rubro.ejecutado").value(500000.00))
                .andExpect(jsonPath("$.porcentajeEjecucion").value(25.00))
                .andExpect(jsonPath("$.excedioDisponible").value(false));
    }

    // ---------------- Criterio 2: gasto que supera el rubro (se permite y se advierte) ----------------

    @Test
    @DisplayName("Criterio 2: el gasto que supera el rubro → 201 con excedioDisponible=true")
    void registrar_exceso_201() throws Exception {
        when(ejecucionService.registrarGasto(eq(1L), any(GastoEjecutadoRequest.class)))
                .thenReturn(registrado(true));

        mockMvc.perform(post(BASE + "/gastos").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("900000", "https://soporte.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.excedioDisponible").value(true))
                .andExpect(jsonPath("$.advertencia").value("supera el disponible"));
    }

    @Test
    @DisplayName("Sin presupuesto aprobado → 409 OPERACION_NO_PERMITIDA")
    void registrar_sinPresupuestoAprobado_409() throws Exception {
        when(ejecucionService.registrarGasto(eq(1L), any(GastoEjecutadoRequest.class)))
                .thenThrow(new OperacionNoPermitidaException("El evento no tiene un presupuesto aprobado."));

        mockMvc.perform(post(BASE + "/gastos").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("1000", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
    }

    // ---------------- Criterio 3 ----------------

    @Test
    @DisplayName("Criterio 3: sin valor → 400 con el error en el campo valor")
    void registrar_sinValor_400() throws Exception {
        mockMvc.perform(post(BASE + "/gastos").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(null, "https://soporte.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.valor").value("El valor del gasto es obligatorio."));
        verify(ejecucionService, never()).registrarGasto(anyLong(), any());
    }

    @Test
    @DisplayName("Criterio 3: valor negativo → 400")
    void registrar_valorNegativo_400() throws Exception {
        mockMvc.perform(post(BASE + "/gastos").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("-1500", "https://soporte.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.valor").value("El valor del gasto no puede ser negativo."));
        verify(ejecucionService, never()).registrarGasto(anyLong(), any());
    }

    @Test
    @DisplayName("Criterio 3: valor 0 es válido → 201")
    void registrar_valorCero_201() throws Exception {
        when(ejecucionService.registrarGasto(eq(1L), any(GastoEjecutadoRequest.class)))
                .thenReturn(registrado(false));

        mockMvc.perform(post(BASE + "/gastos").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("0", null)))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Criterio 3: soporte vacío es válido (opcional) → 201")
    void registrar_soporteVacio_201(String soporte) throws Exception {
        when(ejecucionService.registrarGasto(eq(1L), any(GastoEjecutadoRequest.class)))
                .thenReturn(registrado(false));

        mockMvc.perform(post(BASE + "/gastos").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("1000", soporte)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Criterio 3: sin soporte es válido (opcional) → 201")
    void registrar_sinSoporte_201() throws Exception {
        when(ejecucionService.registrarGasto(eq(1L), any(GastoEjecutadoRequest.class)))
                .thenReturn(registrado(false));

        mockMvc.perform(post(BASE + "/gastos").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("1000", null)))
                .andExpect(status().isCreated());
    }

    // ---------------- Criterio 4 ----------------

    @Test
    @DisplayName("Criterio 4: GET sin gastos → 200 con 0 %")
    void consultar_sinGastos_200() throws Exception {
        when(ejecucionService.consultarEjecucion(1L)).thenReturn(new EjecucionPresupuestalResponse(
                1L, "Congreso", true, bd("5000000.00"), bd("1000000.00"), bd("0.00"), bd("5000000.00"),
                bd("0.00"), 0, List.of()));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeEjecucion").value(0.0))
                .andExpect(jsonPath("$.totalEjecutado").value(0.0));
    }

    @Test
    @DisplayName("GET /gastos lista los gastos del evento")
    void listarGastos_200() throws Exception {
        when(ejecucionService.listarGastos(1L, null)).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/gastos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
