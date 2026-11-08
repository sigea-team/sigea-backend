package com.sigea.demosigea_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigea.demosigea_backend.dto.sala.OcupacionSalaResponse;
import com.sigea.demosigea_backend.dto.sala.SalaRequest;
import com.sigea.demosigea_backend.dto.sala.SalaResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.model.TipoSala;
import com.sigea.demosigea_backend.service.SalaService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SalaGlobalControllerTest {

    @Mock
    private SalaService salaService;

    @InjectMocks
    private SalaGlobalController controller;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/salas lista todas las salas institucionales")
    void listarTodas_ok() throws Exception {
        SalaResponse sala = new SalaResponse(1L, null, "Auditorio 1", "Piso 1", TipoSala.fisica, 100, Collections.emptyList(), false);
        when(salaService.listarTodas()).thenReturn(List.of(sala));

        mockMvc.perform(get("/api/v1/salas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Auditorio 1"));
    }

    @Test
    @DisplayName("POST /api/v1/salas crea sala desvinculada")
    void crear_ok() throws Exception {
        SalaRequest req = new SalaRequest("Aula Magna", "Bloque A", TipoSala.fisica, 80, Collections.emptyList(), false);
        SalaResponse res = new SalaResponse(5L, null, "Aula Magna", "Bloque A", TipoSala.fisica, 80, Collections.emptyList(), false);

        when(salaService.crearGlobal(any(SalaRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/salas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nombre").value("Aula Magna"));
    }

    @Test
    @DisplayName("GET /api/v1/salas/{id}/ocupaciones devuelve los horarios ocupados por eventos")
    void obtenerOcupaciones_ok() throws Exception {
        OcupacionSalaResponse ocupacion = new OcupacionSalaResponse(
                12L, "Keynote", 3L, "Seminario 2026",
                LocalDate.of(2026, 11, 20), LocalTime.of(8, 0), LocalTime.of(10, 0)
        );
        when(salaService.obtenerOcupacionesGlobal(eq(1L), any())).thenReturn(List.of(ocupacion));

        mockMvc.perform(get("/api/v1/salas/1/ocupaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actividadNombre").value("Keynote"))
                .andExpect(jsonPath("$[0].eventoNombre").value("Seminario 2026"));
    }

    @Test
    @DisplayName("DELETE /api/v1/salas/{id} elimina la sala")
    void eliminar_ok() throws Exception {
        mockMvc.perform(delete("/api/v1/salas/1"))
                .andExpect(status().isNoContent());

        verify(salaService).eliminarGlobal(1L);
    }
}
