package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.lineatematica.LineaTematicaRequest;
import com.sigea.demosigea_backend.dto.lineatematica.LineaTematicaResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.LineaTematicaDuplicadaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class LineaTematicaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LineaTematicaService lineaTematicaService;

    @InjectMocks
    private LineaTematicaController lineaTematicaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(lineaTematicaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private LineaTematicaResponse response(Long id, Long eventoId, String nombre, String descripcion) {
        return new LineaTematicaResponse(id, eventoId, nombre, descripcion);
    }

    @Test
    @DisplayName("Criterio 1: POST /api/v1/eventos/{eventoId}/lineas-tematicas registra la línea temática y retorna 201")
    void crearEnEvento_retorna201() throws Exception {
        when(lineaTematicaService.crear(eq(1L), any(LineaTematicaRequest.class)))
                .thenReturn(response(10L, 1L, "Inteligencia Artificial", "Modelos de IA"));

        mockMvc.perform(post("/api/v1/eventos/1/lineas-tematicas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Inteligencia Artificial\",\"descripcion\":\"Modelos de IA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.eventoId").value(1))
                .andExpect(jsonPath("$.nombre").value("Inteligencia Artificial"))
                .andExpect(header().string("Location", "http://localhost/api/v1/lineas-tematicas/10"));
    }

    @Test
    @DisplayName("Criterio 2: POST con nombre vacío retorna 400 VALIDACION_FALLIDA")
    void crear_nombreVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/eventos/1/lineas-tematicas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"   \",\"descripcion\":\"Algo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.nombre").exists());
    }

    @Test
    @DisplayName("Criterio 2: POST con nombre duplicado en el evento retorna 409 LINEA_TEMATICA_DUPLICADA")
    void crear_nombreDuplicado_retorna409() throws Exception {
        when(lineaTematicaService.crear(eq(1L), any(LineaTematicaRequest.class)))
                .thenThrow(new LineaTematicaDuplicadaException("Ya existe una línea temática con el nombre 'Inteligencia Artificial' en el evento 'Congreso'."));

        mockMvc.perform(post("/api/v1/eventos/1/lineas-tematicas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Inteligencia Artificial\",\"descripcion\":\"Modelos de IA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("LINEA_TEMATICA_DUPLICADA"))
                .andExpect(jsonPath("$.message").value("Ya existe una línea temática con el nombre 'Inteligencia Artificial' en el evento 'Congreso'."));
    }

    @Test
    @DisplayName("Criterio 3: PUT /api/v1/lineas-tematicas/{id} actualiza la línea y retorna 200")
    void actualizar_retorna200() throws Exception {
        when(lineaTematicaService.actualizar(eq(10L), any(LineaTematicaRequest.class)))
                .thenReturn(response(10L, 1L, "IA y Robótica", "Nuevos alcances"));

        mockMvc.perform(put("/api/v1/lineas-tematicas/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"IA y Robótica\",\"descripcion\":\"Nuevos alcances\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("IA y Robótica"))
                .andExpect(jsonPath("$.descripcion").value("Nuevos alcances"));
    }

    @Test
    @DisplayName("GET /api/v1/eventos/{eventoId}/lineas-tematicas retorna 200 con el listado")
    void listarPorEvento_retorna200() throws Exception {
        when(lineaTematicaService.listarPorEvento(1L))
                .thenReturn(List.of(
                        response(10L, 1L, "Inteligencia Artificial", "Modelos de IA"),
                        response(11L, 1L, "Ciberseguridad", "Seguridad en redes")
                ));

        mockMvc.perform(get("/api/v1/eventos/1/lineas-tematicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Inteligencia Artificial"))
                .andExpect(jsonPath("$[1].nombre").value("Ciberseguridad"));
    }

    @Test
    @DisplayName("GET /api/v1/lineas-tematicas/{id} retorna 200 con la línea")
    void obtenerPorId_retorna200() throws Exception {
        when(lineaTematicaService.obtenerPorId(10L))
                .thenReturn(response(10L, 1L, "Inteligencia Artificial", "Modelos de IA"));

        mockMvc.perform(get("/api/v1/lineas-tematicas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("Inteligencia Artificial"));
    }

    @Test
    @DisplayName("GET /api/v1/lineas-tematicas/{id} no encontrada retorna 404")
    void obtenerPorId_noEncontrado_retorna404() throws Exception {
        when(lineaTematicaService.obtenerPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la línea temática con ID: 99"));

        mockMvc.perform(get("/api/v1/lineas-tematicas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("DELETE /api/v1/lineas-tematicas/{id} elimina exitosamente y retorna 200")
    void eliminar_retorna200() throws Exception {
        mockMvc.perform(delete("/api/v1/lineas-tematicas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.id").value("10"));

        verify(lineaTematicaService).eliminar(10L);
    }
}
