package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.parametro.TipoActividadRequest;
import com.sigea.demosigea_backend.dto.parametro.TipoActividadResponse;
import com.sigea.demosigea_backend.dto.parametro.UsoParametroResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.ParametroEventoDuplicadoException;
import com.sigea.demosigea_backend.exception.ParametroEventoEnUsoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.service.TipoActividadService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
class TipoActividadControllerTest {

    private static final String URL = "/api/v1/eventos/1/tipos-actividad";

    private MockMvc mockMvc;

    @Mock
    private TipoActividadService tipoService;

    @InjectMocks
    private TipoActividadController tipoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tipoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Criterio 1: POST retorna 201 con Location")
    void crear_retorna201() throws Exception {
        when(tipoService.crear(eq(1L), any(TipoActividadRequest.class)))
                .thenReturn(new TipoActividadResponse(4L, 1L, "Taller", "Sesión práctica"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Taller\",\"descripcion\":\"Sesión práctica\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.nombre").value("Taller"))
                .andExpect(header().string("Location", "http://localhost" + URL + "/4"));
    }

    @Test
    @DisplayName("Criterio 1: sin nombre → 400 VALIDACION_FALLIDA")
    void crear_sinNombre_retorna400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
        verify(tipoService, never()).crear(anyLong(), any());
    }

    @Test
    @DisplayName("Criterio 1: nombre de más de 80 caracteres → 400")
    void crear_nombreLargo_retorna400() throws Exception {
        String largo = "x".repeat(81);
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + largo + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Criterio 3: nombre duplicado → 409 PARAMETRO_DUPLICADO")
    void crear_duplicado_retorna409() throws Exception {
        when(tipoService.crear(eq(1L), any(TipoActividadRequest.class)))
                .thenThrow(new ParametroEventoDuplicadoException("Ya existe un tipo de actividad llamado 'Taller'."));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"taller\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_DUPLICADO"));
    }

    @Test
    @DisplayName("Criterio 2: eliminar un tipo en uso → 409 PARAMETRO_EN_USO")
    void eliminar_enUso_retorna409() throws Exception {
        doThrow(new ParametroEventoEnUsoException("Está en uso en 3 actividad(es) de la agenda."))
                .when(tipoService).eliminar(1L, 4L);

        mockMvc.perform(delete(URL + "/4"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_EN_USO"));
    }

    @Test
    @DisplayName("Criterio 2: eliminar un tipo sin uso → 204")
    void eliminar_sinUso_retorna204() throws Exception {
        mockMvc.perform(delete(URL + "/4")).andExpect(status().isNoContent());
        verify(tipoService).eliminar(1L, 4L);
    }

    @Test
    @DisplayName("Criterio 2: GET /uso informa el impacto")
    void consultarUso_retorna200() throws Exception {
        when(tipoService.consultarUso(1L, 4L)).thenReturn(UsoParametroResponse.de(4L, "Taller", 3, 0));

        mockMvc.perform(get(URL + "/4/uso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividades").value(3))
                .andExpect(jsonPath("$.eliminable").value(false));
    }

    @Test
    @DisplayName("Listar y actualizar responden 200; evento inexistente → 404")
    void listarActualizarYNoEncontrado() throws Exception {
        when(tipoService.listar(1L)).thenReturn(List.of(new TipoActividadResponse(4L, 1L, "Taller", null)));
        when(tipoService.actualizar(eq(1L), eq(4L), any(TipoActividadRequest.class)))
                .thenReturn(new TipoActividadResponse(4L, 1L, "Taller avanzado", null));
        when(tipoService.listar(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el evento con ID: 99"));

        mockMvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$[0].nombre").value("Taller"));
        mockMvc.perform(put(URL + "/4").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Taller avanzado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Taller avanzado"));
        mockMvc.perform(get("/api/v1/eventos/99/tipos-actividad"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
    }
}
