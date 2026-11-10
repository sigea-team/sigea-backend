package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.evaluacion.CriterioRubricaResponse;
import com.sigea.demosigea_backend.dto.rubrica.ImpactoRubricaResponse;
import com.sigea.demosigea_backend.dto.rubrica.RubricaRequest;
import com.sigea.demosigea_backend.dto.rubrica.RubricaResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RubricaEnUsoException;
import com.sigea.demosigea_backend.exception.RubricaInvalidaException;
import com.sigea.demosigea_backend.service.RubricaEvaluacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints de la rúbrica de evaluación (HU-15).
 */
@ExtendWith(MockitoExtension.class)
class RubricaEvaluacionControllerTest {

    private static final String URL = "/api/v1/convocatorias/10/rubrica";
    private static final String CUERPO = "{\"nombre\":\"Rúbrica de ponencias\",\"criterios\":["
            + "{\"nombre\":\"Originalidad\",\"pesoPorcentual\":60},"
            + "{\"nombre\":\"Pertinencia\",\"pesoPorcentual\":40,\"escalaMinima\":1,\"escalaMaxima\":5}]}";

    private MockMvc mockMvc;

    @Mock
    private RubricaEvaluacionService rubricaService;

    @InjectMocks
    private RubricaEvaluacionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static RubricaResponse ejemplo(long reabiertas) {
        return new RubricaResponse(4L, 10L, "Rúbrica de ponencias", true, new BigDecimal("100.00"), List.of(
                new CriterioRubricaResponse(1L, "Originalidad", new BigDecimal("60.00"), BigDecimal.ZERO, BigDecimal.TEN),
                new CriterioRubricaResponse(2L, "Pertinencia", new BigDecimal("40.00"), BigDecimal.ONE, new BigDecimal("5"))),
                reabiertas);
    }

    @Test
    @DisplayName("Criterio 1: PUT crea la rúbrica y responde 201")
    void configurar_crea_retorna201() throws Exception {
        when(rubricaService.configurar(eq(10L), any(RubricaRequest.class), eq(false)))
                .thenReturn(new RubricaEvaluacionService.ResultadoConfiguracion(ejemplo(0), true));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rubricaId").value(4))
                .andExpect(jsonPath("$.sumaPesos").value(100.00))
                .andExpect(jsonPath("$.criterios.length()").value(2))
                .andExpect(jsonPath("$.criterios[1].escalaMaxima").value(5));
    }

    @Test
    @DisplayName("PUT sobre una rúbrica sin calificaciones la actualiza y responde 200")
    void configurar_actualiza_retorna200() throws Exception {
        when(rubricaService.configurar(eq(10L), any(RubricaRequest.class), eq(false)))
                .thenReturn(new RubricaEvaluacionService.ResultadoConfiguracion(ejemplo(0), false));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Criterio 2: pesos que no suman 100% responden 400 RUBRICA_INVALIDA con el campo en error")
    void configurar_pesosInvalidos_retorna400() throws Exception {
        when(rubricaService.configurar(eq(10L), any(RubricaRequest.class), eq(false)))
                .thenThrow(new RubricaInvalidaException("No se guardó la rúbrica: la suma de los pesos de los criterios debe ser 100%.",
                        Map.of("pesoPorcentual", "La suma de los pesos es 90% y debe ser exactamente 100%.")));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("RUBRICA_INVALIDA"))
                .andExpect(jsonPath("$.erroresValidacion.pesoPorcentual").exists());
    }

    @Test
    @DisplayName("Rúbrica sin criterios o con peso negativo responde 400 VALIDACION_FALLIDA")
    void configurar_datosInvalidos_retorna400() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"R\",\"criterios\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"R\",\"criterios\":[{\"nombre\":\"A\",\"pesoPorcentual\":-5}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));

        verify(rubricaService, never()).configurar(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Criterio 4: rúbrica en uso sin confirmar responde 409 RUBRICA_EN_USO con el impacto")
    void configurar_rubricaEnUso_retorna409() throws Exception {
        when(rubricaService.configurar(eq(10L), any(RubricaRequest.class), eq(false)))
                .thenThrow(new RubricaEnUsoException("La rúbrica ya se usó.", 6, 3, 2));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUBRICA_EN_USO"))
                .andExpect(jsonPath("$.erroresValidacion.asignacionesAfectadas").value("3"))
                .andExpect(jsonPath("$.erroresValidacion.propuestasAfectadas").value("2"));
    }

    @Test
    @DisplayName("Criterio 4: con confirmar=true se reemplaza y se informan las asignaciones reabiertas")
    void configurar_confirmado_retorna201() throws Exception {
        when(rubricaService.configurar(eq(10L), any(RubricaRequest.class), eq(true)))
                .thenReturn(new RubricaEvaluacionService.ResultadoConfiguracion(ejemplo(3), true));

        mockMvc.perform(put(URL).param("confirmar", "true")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.asignacionesReabiertas").value(3));
    }

    @Test
    @DisplayName("GET devuelve la rúbrica activa y GET /impacto el impacto de modificarla")
    void obtener_eImpacto() throws Exception {
        when(rubricaService.obtenerActiva(10L)).thenReturn(ejemplo(0));
        when(rubricaService.impacto(10L)).thenReturn(new ImpactoRubricaResponse(4L, 6, 3, 2, true, 0, false));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criterios[0].nombre").value("Originalidad"));
        mockMvc.perform(get(URL + "/impacto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereConfirmacion").value(true));
    }
}
