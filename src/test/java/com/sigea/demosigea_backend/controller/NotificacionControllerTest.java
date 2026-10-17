package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.notificacion.NotificacionResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.service.NotificacionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints de la campana de notificaciones (HU-11, Criterio 2).
 */
@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private NotificacionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private NotificacionResponse ejemplo(boolean leida) {
        return new NotificacionResponse(15L, TipoNotificacion.CONVOCATORIA_FECHA_MODIFICADA,
                "Nueva fecha límite: Convocatoria Ponencias 2026", "Aún estás a tiempo de enviar tu propuesta.",
                "convocatorias", 10L, leida, LocalDateTime.of(2026, 10, 17, 12, 0),
                leida ? LocalDateTime.of(2026, 10, 17, 12, 30) : null);
    }

    @Test
    @DisplayName("GET /api/v1/notificaciones lista las notificaciones del usuario")
    void listar_retorna200() throws Exception {
        when(notificacionService.listarMias(false, 20)).thenReturn(List.of(ejemplo(false)));

        mockMvc.perform(get("/api/v1/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("CONVOCATORIA_FECHA_MODIFICADA"))
                .andExpect(jsonPath("$[0].entidadId").value(10))
                .andExpect(jsonPath("$[0].leida").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/notificaciones/no-leidas/conteo devuelve el número de la campana")
    void contarNoLeidas_retorna200() throws Exception {
        when(notificacionService.contarNoLeidas()).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notificaciones/no-leidas/conteo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(3));
    }

    @Test
    @DisplayName("PATCH /api/v1/notificaciones/{id}/leida marca la notificación como leída")
    void marcarComoLeida_retorna200() throws Exception {
        when(notificacionService.marcarComoLeida(15L)).thenReturn(ejemplo(true));

        mockMvc.perform(patch("/api/v1/notificaciones/15/leida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));
    }

    @Test
    @DisplayName("PATCH sobre una notificación ajena o inexistente retorna 404")
    void marcarComoLeida_noEncontrada_retorna404() throws Exception {
        when(notificacionService.marcarComoLeida(99L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la notificación con ID: 99"));

        mockMvc.perform(patch("/api/v1/notificaciones/99/leida"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("PATCH /api/v1/notificaciones/leidas marca todas como leídas")
    void marcarTodasComoLeidas_retorna200() throws Exception {
        when(notificacionService.marcarTodasComoLeidas()).thenReturn(4);

        mockMvc.perform(patch("/api/v1/notificaciones/leidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizadas").value(4));
    }
}
