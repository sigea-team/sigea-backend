package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.parametro.LineaTematicaRequest;
import com.sigea.demosigea_backend.dto.parametro.LineaTematicaResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.ParametroEventoDuplicadoException;
import com.sigea.demosigea_backend.exception.ParametroEventoEnUsoException;
import com.sigea.demosigea_backend.service.LineaTematicaService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LineaTematicaControllerTest {

    private static final String URL = "/api/v1/eventos/1/lineas-tematicas";

    private MockMvc mockMvc;

    @Mock
    private LineaTematicaService lineaService;

    @InjectMocks
    private LineaTematicaController lineaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(lineaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Criterio 1: POST retorna 201")
    void crear_retorna201() throws Exception {
        when(lineaService.crear(eq(1L), any(LineaTematicaRequest.class)))
                .thenReturn(new LineaTematicaResponse(7L, 1L, "Ingeniería de software", null));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ingeniería de software\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @DisplayName("Criterio 1: nombre de más de 120 caracteres → 400")
    void crear_nombreLargo_retorna400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + "x".repeat(121) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
    }

    @Test
    @DisplayName("Criterio 3: duplicado → 409 PARAMETRO_DUPLICADO")
    void crear_duplicado_retorna409() throws Exception {
        when(lineaService.crear(eq(1L), any(LineaTematicaRequest.class)))
                .thenThrow(new ParametroEventoDuplicadoException("Ya existe una línea temática llamada 'Redes'."));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"redes\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_DUPLICADO"));
    }

    @Test
    @DisplayName("Criterio 2: en uso → 409 PARAMETRO_EN_USO")
    void eliminar_enUso_retorna409() throws Exception {
        doThrow(new ParametroEventoEnUsoException("Está en uso en 5 propuesta(s)."))
                .when(lineaService).eliminar(1L, 7L);

        mockMvc.perform(delete(URL + "/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_EN_USO"));
    }
}
