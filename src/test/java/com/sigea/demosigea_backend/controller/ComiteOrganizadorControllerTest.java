package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.comite.MiembroComiteRequest;
import com.sigea.demosigea_backend.dto.comite.MiembroComiteResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.MiembroComiteDuplicadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.service.ComiteOrganizadorService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ComiteOrganizadorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ComiteOrganizadorService comiteService;

    @InjectMocks
    private ComiteOrganizadorController comiteController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(comiteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MiembroComiteResponse respuesta(Long id, boolean activo) {
        return new MiembroComiteResponse(id, 1L, 12L, "Ana María Pérez Gómez", "1090123456",
                "ana.perez@ufps.edu.co", "Coordinador general", LocalDateTime.of(2026, 9, 20, 10, 0),
                activo ? null : LocalDateTime.of(2026, 9, 25, 9, 0), activo);
    }

    @Test
    @DisplayName("Criterio 1: POST retorna 201 con el miembro y su rol")
    void agregar_retorna201() throws Exception {
        when(comiteService.agregar(eq(1L), any(MiembroComiteRequest.class))).thenReturn(respuesta(5L, true));

        mockMvc.perform(post("/api/v1/eventos/1/comite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":12,\"rolComite\":\"Coordinador general\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.rolComite").value("Coordinador general"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(header().string("Location", "http://localhost/api/v1/eventos/1/comite/5"));
    }

    @Test
    @DisplayName("Criterio 1: sin rol o sin persona → 400 VALIDACION_FALLIDA")
    void agregar_datosIncompletos_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/eventos/1/comite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rolComite\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
        verify(comiteService, never()).agregar(anyLong(), any());
    }

    @Test
    @DisplayName("Criterio 1: persona inexistente → 404")
    void agregar_personaInexistente_retorna404() throws Exception {
        when(comiteService.agregar(eq(1L), any(MiembroComiteRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la persona con ID: 12"));

        mockMvc.perform(post("/api/v1/eventos/1/comite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":12,\"rolComite\":\"Logística\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Criterio 2: miembro duplicado → 409 MIEMBRO_COMITE_DUPLICADO")
    void agregar_duplicado_retorna409() throws Exception {
        when(comiteService.agregar(eq(1L), any(MiembroComiteRequest.class)))
                .thenThrow(new MiembroComiteDuplicadoException("Ana María Pérez Gómez ya es miembro vigente..."));

        mockMvc.perform(post("/api/v1/eventos/1/comite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroDocumento\":\"1090123456\",\"rolComite\":\"Logística\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MIEMBRO_COMITE_DUPLICADO"));
    }

    @Test
    @DisplayName("Criterio 3: DELETE retira al miembro y lo devuelve con activo=false")
    void retirar_retorna200() throws Exception {
        when(comiteService.retirar(1L, 5L)).thenReturn(respuesta(5L, false));

        mockMvc.perform(delete("/api/v1/eventos/1/comite/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.fechaRetiro").exists());
    }

    @Test
    @DisplayName("Criterio 3: GET con incluirHistorial=true devuelve vigentes y retirados")
    void listar_conHistorial() throws Exception {
        when(comiteService.listar(1L, true)).thenReturn(List.of(respuesta(5L, true), respuesta(4L, false)));

        mockMvc.perform(get("/api/v1/eventos/1/comite").param("incluirHistorial", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].activo").value(false));
    }

    @Test
    @DisplayName("GET por defecto solo devuelve el comité vigente")
    void listar_porDefectoSoloVigentes() throws Exception {
        when(comiteService.listar(1L, false)).thenReturn(List.of(respuesta(5L, true)));

        mockMvc.perform(get("/api/v1/eventos/1/comite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
