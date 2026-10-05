package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
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
class ConvocatoriaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConvocatoriaService convocatoriaService;

    @InjectMocks
    private ConvocatoriaController convocatoriaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(convocatoriaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ConvocatoriaResponse response(Long id, Long eventoId, String titulo, EstadoConvocatoria estado) {
        return new ConvocatoriaResponse(
                id,
                eventoId,
                "Congreso de Prueba",
                titulo,
                "Descripción",
                "Requisitos",
                LocalDateTime.of(2026, 4, 1, 8, 0),
                LocalDateTime.of(2026, 6, 1, 23, 59),
                estado
        );
    }

    @Test
    @DisplayName("GET /api/v1/convocatorias retorna 200 y la lista de convocatorias")
    void listar_retorna200() throws Exception {
        when(convocatoriaService.listar(null, null))
                .thenReturn(List.of(response(5L, 1L, "Convocatoria 2026", EstadoConvocatoria.borrador)));

        mockMvc.perform(get("/api/v1/convocatorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].titulo").value("Convocatoria 2026"));
    }

    @Test
    @DisplayName("GET /api/v1/convocatorias/{id} retorna 200")
    void obtenerPorId_retorna200() throws Exception {
        when(convocatoriaService.obtenerPorId(5L))
                .thenReturn(response(5L, 1L, "Convocatoria 2026", EstadoConvocatoria.borrador));

        mockMvc.perform(get("/api/v1/convocatorias/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.titulo").value("Convocatoria 2026"));
    }

    @Test
    @DisplayName("POST /api/v1/convocatorias crea convocatoria y retorna 201")
    void crear_retorna201() throws Exception {
        when(convocatoriaService.crear(eq(null), any(ConvocatoriaRequest.class)))
                .thenReturn(response(5L, 1L, "Convocatoria 2026", EstadoConvocatoria.borrador));

        String body = """
                {
                    "eventoId": 1,
                    "titulo": "Convocatoria 2026",
                    "descripcion": "Descripción",
                    "requisitos": "Requisitos",
                    "fechaApertura": "2026-04-01T08:00:00",
                    "fechaCierre": "2026-06-01T23:59:59",
                    "estado": "borrador"
                }
                """;

        mockMvc.perform(post("/api/v1/convocatorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(header().string("Location", "http://localhost/api/v1/convocatorias/5"));
    }

    @Test
    @DisplayName("PUT /api/v1/convocatorias/{id} actualiza y retorna 200")
    void actualizar_retorna200() throws Exception {
        when(convocatoriaService.actualizar(eq(5L), any(ConvocatoriaRequest.class)))
                .thenReturn(response(5L, 1L, "Título Actualizado", EstadoConvocatoria.publicada));

        String body = """
                {
                    "eventoId": 1,
                    "titulo": "Título Actualizado",
                    "descripcion": "Descripción",
                    "requisitos": "Requisitos",
                    "fechaApertura": "2026-04-01T08:00:00",
                    "fechaCierre": "2026-06-01T23:59:59",
                    "estado": "publicada"
                }
                """;

        mockMvc.perform(put("/api/v1/convocatorias/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.titulo").value("Título Actualizado"))
                .andExpect(jsonPath("$.estado").value("publicada"));
    }

    @Test
    @DisplayName("DELETE /api/v1/convocatorias/{id} elimina y retorna 200")
    void eliminar_retorna200() throws Exception {
        mockMvc.perform(delete("/api/v1/convocatorias/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.id").value("5"));

        verify(convocatoriaService).eliminar(5L);
    }
}
