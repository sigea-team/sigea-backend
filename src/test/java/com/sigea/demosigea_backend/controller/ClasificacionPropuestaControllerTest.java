package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.clasificacion.AjusteClasificacionRequest;
import com.sigea.demosigea_backend.dto.clasificacion.AplicacionClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.HistorialClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.PropuestaClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.UmbralesClasificacionRequest;
import com.sigea.demosigea_backend.dto.clasificacion.UmbralesClasificacionResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.PropuestaNoEvaluadaException;
import com.sigea.demosigea_backend.model.ClasificacionResultado;
import com.sigea.demosigea_backend.service.ClasificacionPropuestaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints de clasificación de propuestas (HU-18).
 */
@ExtendWith(MockitoExtension.class)
class ClasificacionPropuestaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClasificacionPropuestaService clasificacionService;

    @InjectMocks
    private ClasificacionPropuestaController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static PropuestaClasificacionResponse fila(String clasificacion, String etiqueta, String estado,
                                                       boolean manual) {
        return new PropuestaClasificacionResponse(40L, "Analítica educativa", estado, true, 0,
                new BigDecimal("7.60"), "aprobada", clasificacion, etiqueta, manual, true,
                LocalDateTime.of(2026, 11, 5, 10, 0));
    }

    @Test
    @DisplayName("Criterio 1: POST /propuestas/{id}/aplicar clasifica la propuesta")
    void aplicar_retorna200() throws Exception {
        when(clasificacionService.aplicar(40L)).thenReturn(fila("aprobada", "Aceptada", "aprobada", false));

        mockMvc.perform(post("/api/v1/clasificaciones/propuestas/40/aplicar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clasificacion").value("aprobada"))
                .andExpect(jsonPath("$.etiqueta").value("Aceptada"))
                .andExpect(jsonPath("$.estadoPropuesta").value("aprobada"));
    }

    @Test
    @DisplayName("Criterio 3: propuesta sin evaluación completa responde 409 PROPUESTA_NO_EVALUADA")
    void aplicar_noEvaluada_retorna409() throws Exception {
        when(clasificacionService.aplicar(41L)).thenThrow(new PropuestaNoEvaluadaException(
                "La propuesta no se puede clasificar: tiene 1 evaluación(es) pendiente(s) de 2 asignada(s)."));

        mockMvc.perform(post("/api/v1/clasificaciones/propuestas/41/aplicar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PROPUESTA_NO_EVALUADA"));
    }

    @Test
    @DisplayName("Criterio 1 y 3: POST /convocatorias/{id}/aplicar devuelve clasificadas y omitidas")
    void aplicarConvocatoria_retorna200() throws Exception {
        when(clasificacionService.aplicarConvocatoria(10L)).thenReturn(new AplicacionClasificacionResponse(10L,
                List.of(fila("aprobada", "Aceptada", "aprobada", false)),
                List.of(new AplicacionClasificacionResponse.PropuestaOmitidaResponse(41L, "Robótica",
                        "tiene 1 evaluación(es) pendiente(s) de 2 asignada(s)."))));

        mockMvc.perform(post("/api/v1/clasificaciones/convocatorias/10/aplicar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clasificadas.length()").value(1))
                .andExpect(jsonPath("$.omitidas[0].propuestaId").value(41));
    }

    @Test
    @DisplayName("Criterio 2: PUT /propuestas/{id} ajusta la clasificación con motivo")
    void ajustar_conMotivo_retorna200() throws Exception {
        when(clasificacionService.ajustar(eq(40L), any(AjusteClasificacionRequest.class)))
                .thenReturn(fila("aprobada_con_ajustes", "Aceptada con modificaciones", "ajustes", true));

        mockMvc.perform(put("/api/v1/clasificaciones/propuestas/40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clasificacion\":\"aprobada_con_ajustes\","
                                + "\"motivo\":\"El comité pidió ajustes de formato.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ajusteManual").value(true))
                .andExpect(jsonPath("$.estadoPropuesta").value("ajustes"));

        ArgumentCaptor<AjusteClasificacionRequest> captor = ArgumentCaptor.forClass(AjusteClasificacionRequest.class);
        verify(clasificacionService).ajustar(eq(40L), captor.capture());
        assertEquals(ClasificacionResultado.aprobada_con_ajustes, captor.getValue().clasificacion());
    }

    @Test
    @DisplayName("Criterio 2: sin motivo responde 400 VALIDACION_FALLIDA")
    void ajustar_sinMotivo_retorna400() throws Exception {
        mockMvc.perform(put("/api/v1/clasificaciones/propuestas/40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clasificacion\":\"rechazada\",\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.motivo").exists());
        verify(clasificacionService, never()).ajustar(any(), any());
    }

    @Test
    @DisplayName("Criterio 2: clasificación inexistente responde 400")
    void ajustar_clasificacionInvalida_retorna400() throws Exception {
        mockMvc.perform(put("/api/v1/clasificaciones/propuestas/40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clasificacion\":\"pendiente\",\"motivo\":\"Motivo suficientemente largo\"}"))
                .andExpect(status().isBadRequest());
        verify(clasificacionService, never()).ajustar(any(), any());
    }

    @Test
    @DisplayName("Criterio 2: GET /propuestas/{id}/historial devuelve los movimientos con su motivo")
    void historial_retorna200() throws Exception {
        when(clasificacionService.historial(40L)).thenReturn(List.of(new HistorialClasificacionResponse(5L, 40L,
                "manual", "aprobada", "aprobada_con_ajustes", "Aceptada con modificaciones", new BigDecimal("7.10"),
                new BigDecimal("7.00"), new BigDecimal("5.00"), "El comité pidió ajustes de formato.",
                "María Rodríguez", LocalDateTime.of(2026, 11, 5, 10, 0))));

        mockMvc.perform(get("/api/v1/clasificaciones/propuestas/40/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("manual"))
                .andExpect(jsonPath("$[0].motivo").value("El comité pidió ajustes de formato."));
    }

    @Test
    @DisplayName("Criterio 1: PUT /convocatorias/{id}/umbrales guarda los umbrales")
    void configurarUmbrales_retorna200() throws Exception {
        when(clasificacionService.configurarUmbrales(eq(10L), any(UmbralesClasificacionRequest.class)))
                .thenReturn(new UmbralesClasificacionResponse(10L, new BigDecimal("8.00"), new BigDecimal("6.00"), 2));

        mockMvc.perform(put("/api/v1/clasificaciones/convocatorias/10/umbrales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"umbralAceptacion\":8.0,\"umbralAjustes\":6.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.umbralAceptacion").value(8.00))
                .andExpect(jsonPath("$.propuestasPorReaplicar").value(2));
    }

    @Test
    @DisplayName("Umbral fuera de 0 - 10 responde 400 VALIDACION_FALLIDA")
    void configurarUmbrales_fueraDeEscala_retorna400() throws Exception {
        mockMvc.perform(put("/api/v1/clasificaciones/convocatorias/10/umbrales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"umbralAceptacion\":11,\"umbralAjustes\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
        verify(clasificacionService, never()).configurarUmbrales(any(), any());
    }
}
