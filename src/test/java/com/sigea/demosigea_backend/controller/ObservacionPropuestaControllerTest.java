package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.observacion.ObservacionResponse;
import com.sigea.demosigea_backend.dto.observacion.ObservacionesPropuestaResponse;
import com.sigea.demosigea_backend.dto.observacion.PropuestaProgramableResponse;
import com.sigea.demosigea_backend.dto.observacion.RegistrarObservacionesRequest;
import com.sigea.demosigea_backend.dto.observacion.VersionDefinitivaResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.VersionDefinitivaNoAprobableException;
import com.sigea.demosigea_backend.service.ObservacionPropuestaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints de observaciones y versión definitiva (HU-19).
 */
@ExtendWith(MockitoExtension.class)
class ObservacionPropuestaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ObservacionPropuestaService observacionService;

    @InjectMocks
    private ObservacionPropuestaController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static ObservacionResponse pendiente(Long id, String estado) {
        return new ObservacionResponse(id, "Ampliar la metodología", true, 1, null, null, false, estado,
                "Elena Ruiz", true, LocalDateTime.of(2026, 11, 8, 10, 0), null, null);
    }

    private static ObservacionesPropuestaResponse resumen() {
        return new ObservacionesPropuestaResponse(50L, "IA en la Educación", "ajustes", 1, null, null, 1, 1, false,
                "No se puede aprobar la versión definitiva: la propuesta tiene 1 ajuste(s) sin resolver.",
                List.of(pendiente(60L, ObservacionResponse.PENDIENTE)));
    }

    @Test
    @DisplayName("Criterio 1: POST /propuestas/{id}/observaciones registra y responde 201")
    void registrar_retorna201() throws Exception {
        when(observacionService.registrar(eq(50L), any(RegistrarObservacionesRequest.class))).thenReturn(resumen());

        mockMvc.perform(post("/api/v1/propuestas/50/observaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":[{\"descripcion\":\"Ampliar la metodología\"},"
                                + "{\"descripcion\":\"Buen marco teórico\",\"requiereAjuste\":false}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ajustesPendientes").value(1))
                .andExpect(jsonPath("$.observaciones[0].estado").value("pendiente"));

        ArgumentCaptor<RegistrarObservacionesRequest> captor = ArgumentCaptor.forClass(RegistrarObservacionesRequest.class);
        verify(observacionService).registrar(eq(50L), captor.capture());
        assertEquals(2, captor.getValue().observaciones().size());
        assertNull(captor.getValue().observaciones().get(0).requiereAjuste());
        assertEquals(Boolean.FALSE, captor.getValue().observaciones().get(1).requiereAjuste());
    }

    @Test
    @DisplayName("Criterio 1: sin observaciones responde 400")
    void registrar_sinObservaciones_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/propuestas/50/observaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":[]}"))
                .andExpect(status().isBadRequest());
        verify(observacionService, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("Criterio 1: una observación con descripción vacía responde 400")
    void registrar_descripcionVacia_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/propuestas/50/observaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":[{\"descripcion\":\"   \"}]}"))
                .andExpect(status().isBadRequest());
        verify(observacionService, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("GET /propuestas/{id}/observaciones devuelve la situación de la propuesta")
    void listar_retorna200() throws Exception {
        when(observacionService.listar(50L)).thenReturn(resumen());

        mockMvc.perform(get("/api/v1/propuestas/50/observaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeAprobarse").value(false))
                .andExpect(jsonPath("$.versionActual").value(1));
    }

    @Test
    @DisplayName("Criterio 2: PATCH .../resolver da por resuelta la observación")
    void resolver_retorna200() throws Exception {
        when(observacionService.resolver(50L, 60L)).thenReturn(new ObservacionResponse(60L, "Ampliar la metodología",
                true, 1, 2, 102L, true, ObservacionResponse.RESUELTA, "Elena Ruiz", true,
                LocalDateTime.of(2026, 11, 8, 10, 0), "María Rodríguez", LocalDateTime.of(2026, 11, 9, 9, 0)));

        mockMvc.perform(patch("/api/v1/propuestas/50/observaciones/60/resolver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("resuelta"))
                .andExpect(jsonPath("$.versionAtencionNumero").value(2));
    }

    @Test
    @DisplayName("Criterio 3: POST /propuestas/{id}/version-definitiva aprueba la versión")
    void aprobar_retorna200() throws Exception {
        when(observacionService.aprobarVersionDefinitiva(50L)).thenReturn(new VersionDefinitivaResponse(50L,
                "IA en la Educación", "aprobada", 102L, 2, null, LocalDateTime.of(2026, 11, 9, 9, 0),
                "María Rodríguez", 1, "La versión 2 quedó aprobada como definitiva."));

        mockMvc.perform(post("/api/v1/propuestas/50/version-definitiva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroVersion").value(2))
                .andExpect(jsonPath("$.estadoPropuesta").value("aprobada"));
    }

    @Test
    @DisplayName("Criterio 4: con ajustes pendientes responde 409 con la lista de pendientes")
    void aprobar_conPendientes_retorna409() throws Exception {
        when(observacionService.aprobarVersionDefinitiva(50L)).thenThrow(new VersionDefinitivaNoAprobableException(
                VersionDefinitivaNoAprobableException.OBSERVACIONES_PENDIENTES,
                "No se puede aprobar la versión definitiva: la propuesta tiene 1 ajuste(s) sin resolver.",
                List.of(pendiente(61L, ObservacionResponse.ATENDIDA))));

        mockMvc.perform(post("/api/v1/propuestas/50/version-definitiva"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OBSERVACIONES_PENDIENTES"))
                .andExpect(jsonPath("$.pendientes[0].id").value(61))
                .andExpect(jsonPath("$.pendientes[0].estado").value("atendida"));
    }

    @Test
    @DisplayName("Criterio 3: GET /eventos/{id}/propuestas-programables lista las propuestas definitivas")
    void listarProgramables_retorna200() throws Exception {
        when(observacionService.listarProgramables(2L, true)).thenReturn(List.of(new PropuestaProgramableResponse(
                50L, "IA en la Educación", 5L, "Convocatoria 2026", null, null, 2, LocalDateTime.of(2026, 11, 9, 9, 0),
                List.of("Carlos Gómez"), 10L, false, List.of())));

        mockMvc.perform(get("/api/v1/eventos/2/propuestas-programables").param("soloSinProgramar", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].propuestaId").value(50))
                .andExpect(jsonPath("$[0].programada").value(false));
        verify(observacionService).listarProgramables(2L, true);
    }
}
