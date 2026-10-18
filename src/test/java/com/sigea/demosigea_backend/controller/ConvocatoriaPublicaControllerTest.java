package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import com.sigea.demosigea_backend.service.ConvocatoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la consulta pública de convocatorias (HU-11, Criterio 1).
 */
@ExtendWith(MockitoExtension.class)
class ConvocatoriaPublicaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConvocatoriaService convocatoriaService;

    @InjectMocks
    private ConvocatoriaPublicaController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ConvocatoriaResponse publicada(Long id) {
        return new ConvocatoriaResponse(id, 1L, "Congreso de Prueba", "Convocatoria Ponencias 2026",
                "Descripción", "Requisitos", LocalDateTime.of(2026, 10, 10, 8, 0),
                LocalDateTime.of(2026, 11, 15, 23, 59), EstadoConvocatoria.publicada, true);
    }

    @Test
    @DisplayName("HU-11 Criterio 1: GET /api/v1/publico/convocatorias lista las convocatorias publicadas")
    void listarPublicadas_retorna200() throws Exception {
        when(convocatoriaService.listarPublicadas(1L)).thenReturn(List.of(publicada(10L)));

        mockMvc.perform(get("/api/v1/publico/convocatorias").param("eventoId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].estado").value("publicada"));
    }

    @Test
    @DisplayName("HU-11 Criterio 1: GET /api/v1/publico/convocatorias/{id} de una publicada retorna 200")
    void obtenerPublicada_retorna200() throws Exception {
        when(convocatoriaService.obtenerPublicada(10L)).thenReturn(publicada(10L));

        mockMvc.perform(get("/api/v1/publico/convocatorias/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Convocatoria Ponencias 2026"));
    }

    @Test
    @DisplayName("HU-11 Criterio 1: una convocatoria en borrador no es visible públicamente (404)")
    void obtenerPublicada_noPublicada_retorna404() throws Exception {
        when(convocatoriaService.obtenerPublicada(11L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró una convocatoria publicada con ID: 11"));

        mockMvc.perform(get("/api/v1/publico/convocatorias/11"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
    }
}
