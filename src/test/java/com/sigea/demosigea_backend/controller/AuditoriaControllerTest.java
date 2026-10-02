package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auditoria.AuditoriaResponse;
import com.sigea.demosigea_backend.dto.auditoria.FiltroAuditoria;
import com.sigea.demosigea_backend.dto.auditoria.PaginaResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.service.AuditoriaService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuditoriaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private AuditoriaController auditoriaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditoriaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Criterio 2: GET /api/v1/auditoria pasa los filtros al servicio y responde 200")
    void buscar_conFiltros() throws Exception {
        AuditoriaResponse registro = new AuditoriaResponse(15L, null, 3L, "admin@ufps.edu.co", "Ana Pérez",
                "ROL_ACTUALIZADO", "Modificación de un rol o de sus permisos", "roles", 4L, "{}");
        when(auditoriaService.buscar(any(FiltroAuditoria.class), eq(0), eq(20)))
                .thenReturn(new PaginaResponse<>(List.of(registro), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/auditoria")
                        .param("usuarioId", "3")
                        .param("accion", "ROL_ACTUALIZADO")
                        .param("desde", "2026-09-01")
                        .param("hasta", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].accion").value("ROL_ACTUALIZADO"))
                .andExpect(jsonPath("$.contenido[0].usuarioId").value(3));

        verify(auditoriaService).buscar(
                new FiltroAuditoria(3L, null, "ROL_ACTUALIZADO", null,
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)),
                0, 20);
    }

    @Test
    @DisplayName("Criterio 2: filtros inválidos responden 400")
    void buscar_filtroInvalido() throws Exception {
        when(auditoriaService.buscar(any(FiltroAuditoria.class), anyInt(), anyInt()))
                .thenThrow(new SolicitudInvalidaException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'."));

        mockMvc.perform(get("/api/v1/auditoria").param("desde", "2026-09-30").param("hasta", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    @DisplayName("Criterio 2: fecha con formato incorrecto responde 400")
    void buscar_fechaMalFormada() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria").param("desde", "30/09/2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Criterio 3: PUT, PATCH y DELETE sobre un registro responden 405 y no tocan el servicio")
    void modificarOEliminar_responde405() throws Exception {
        mockMvc.perform(put("/api/v1/auditoria/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("AUDITORIA_INMUTABLE"));
        mockMvc.perform(patch("/api/v1/auditoria/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/auditoria/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("AUDITORIA_INMUTABLE"));
        mockMvc.perform(delete("/api/v1/auditoria"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(auditoriaService);
    }

    @Test
    @DisplayName("Criterio 3: no se pueden crear registros manualmente por la API")
    void crearManual_responde405() throws Exception {
        mockMvc.perform(post("/api/v1/auditoria").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
