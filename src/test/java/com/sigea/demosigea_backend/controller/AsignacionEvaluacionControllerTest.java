package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.asignacion.AsignacionResponse;
import com.sigea.demosigea_backend.dto.asignacion.AsignarEvaluadoresRequest;
import com.sigea.demosigea_backend.dto.asignacion.EvaluadorDisponibleResponse;
import com.sigea.demosigea_backend.dto.asignacion.HistorialAsignacionResponse;
import com.sigea.demosigea_backend.dto.asignacion.ReasignarEvaluadorRequest;
import com.sigea.demosigea_backend.exception.AsignacionDuplicadaException;
import com.sigea.demosigea_backend.exception.ConfirmacionRequeridaException;
import com.sigea.demosigea_backend.exception.ConflictoInteresException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.service.AsignacionEvaluacionService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints de asignación de propuestas a evaluadores (HU-16).
 */
@ExtendWith(MockitoExtension.class)
class AsignacionEvaluacionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AsignacionEvaluacionService asignacionService;

    @InjectMocks
    private AsignacionEvaluacionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AsignacionResponse ejemplo(Long comiteEvaluadorId, String nombre) {
        return new AsignacionResponse(12L, 40L, "Analítica educativa", "en_evaluacion", 10L,
                "Convocatoria de Ponencias 2026", comiteEvaluadorId, 21L, nombre, "ana@ufps.edu.co",
                "Ingeniería de software", LocalDateTime.of(2026, 10, 25, 10, 0), "pendiente", List.of());
    }

    @Test
    @DisplayName("POST /api/v1/asignaciones/propuestas/{id} asigna evaluadores y responde 201")
    void asignar_retorna201() throws Exception {
        when(asignacionService.asignar(eq(40L), any(AsignarEvaluadoresRequest.class)))
                .thenReturn(List.of(ejemplo(3L, "Ana Pérez"), ejemplo(5L, "Luis Gómez")));

        mockMvc.perform(post("/api/v1/asignaciones/propuestas/40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comiteEvaluadorIds\":[3,5]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].evaluadorNombre").value("Ana Pérez"))
                .andExpect(jsonPath("$[1].comiteEvaluadorId").value(5));
    }

    @Test
    @DisplayName("POST sin evaluadores responde 400 VALIDACION_FALLIDA")
    void asignar_sinEvaluadores_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/asignaciones/propuestas/40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comiteEvaluadorIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
        verify(asignacionService, never()).asignar(any(), any());
    }

    @Test
    @DisplayName("HU-16 Criterio 2: evaluador autor de la propuesta responde 409 CONFLICTO_INTERES")
    void asignar_conflictoBloqueante_retorna409() throws Exception {
        when(asignacionService.asignar(eq(40L), any(AsignarEvaluadoresRequest.class)))
                .thenThrow(new ConflictoInteresException("Ana Pérez figura como autor principal de la propuesta."));

        mockMvc.perform(post("/api/v1/asignaciones/propuestas/40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comiteEvaluadorIds\":[3]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_INTERES"));
    }

    @Test
    @DisplayName("HU-16 Criterio 2: advertencia sin confirmar responde 409 CONFIRMACION_REQUERIDA")
    void asignar_advertenciaSinConfirmar_retorna409() throws Exception {
        when(asignacionService.asignar(eq(40L), any(AsignarEvaluadoresRequest.class)))
                .thenThrow(new ConfirmacionRequeridaException("Posible conflicto de interés"));

        mockMvc.perform(post("/api/v1/asignaciones/propuestas/40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comiteEvaluadorIds\":[7],\"confirmarConflicto\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFIRMACION_REQUERIDA"));
    }

    @Test
    @DisplayName("Asignación repetida responde 409 ASIGNACION_DUPLICADA")
    void asignar_duplicada_retorna409() throws Exception {
        when(asignacionService.asignar(eq(40L), any(AsignarEvaluadoresRequest.class)))
                .thenThrow(new AsignacionDuplicadaException("La propuesta ya está asignada a Ana Pérez."));

        mockMvc.perform(post("/api/v1/asignaciones/propuestas/40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comiteEvaluadorIds\":[3]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ASIGNACION_DUPLICADA"));
    }

    @Test
    @DisplayName("HU-16 Criterio 3: PUT /api/v1/asignaciones/{id}/reasignar responde 200")
    void reasignar_retorna200() throws Exception {
        when(asignacionService.reasignar(eq(12L), any(ReasignarEvaluadorRequest.class)))
                .thenReturn(ejemplo(5L, "Luis Gómez"));

        mockMvc.perform(put("/api/v1/asignaciones/12/reasignar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comiteEvaluadorId\":5,\"motivo\":\"Sin disponibilidad\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comiteEvaluadorId").value(5))
                .andExpect(jsonPath("$.estado").value("pendiente"));
    }

    @Test
    @DisplayName("PUT reasignar sin evaluador responde 400")
    void reasignar_sinEvaluador_retorna400() throws Exception {
        mockMvc.perform(put("/api/v1/asignaciones/12/reasignar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU-16 Criterio 3: GET historial de la propuesta")
    void historial_retorna200() throws Exception {
        when(asignacionService.historialPorPropuesta(40L)).thenReturn(List.of(new HistorialAsignacionResponse(
                8L, 12L, 40L, "reasignacion", 3L, "Ana Pérez", 5L, "Luis Gómez", false,
                "Sin disponibilidad", "María Rodríguez", LocalDateTime.of(2026, 10, 25, 10, 0))));

        mockMvc.perform(get("/api/v1/asignaciones/propuestas/40/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoOperacion").value("reasignacion"))
                .andExpect(jsonPath("$[0].evaluadorAnteriorNombre").value("Ana Pérez"));
    }

    @Test
    @DisplayName("GET asignaciones vigentes de la propuesta")
    void listarPorPropuesta_retorna200() throws Exception {
        when(asignacionService.listarPorPropuesta(40L)).thenReturn(List.of(ejemplo(3L, "Ana Pérez")));

        mockMvc.perform(get("/api/v1/asignaciones/propuestas/40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].asignacionId").value(12));
    }

    @Test
    @DisplayName("HU-16 Criterio 2: GET evaluadores disponibles con nivel de conflicto")
    void evaluadoresDisponibles_retorna200() throws Exception {
        when(asignacionService.evaluadoresDisponibles(40L)).thenReturn(List.of(new EvaluadorDisponibleResponse(
                7L, 27L, "Sofía Mora", "sofia@ufps.edu.co", "Datos", 1L, false, "ADVERTENCIA",
                "Sofía Mora comparte la afiliación 'UFPS' con el autor Carlos Ruiz.")));

        mockMvc.perform(get("/api/v1/asignaciones/propuestas/40/evaluadores-disponibles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conflicto").value("ADVERTENCIA"));
    }

    @Test
    @DisplayName("HU-16 Criterio 1: GET mis-asignaciones devuelve por defecto solo el trabajo actual")
    void miBandeja_retorna200() throws Exception {
        when(asignacionService.miBandeja(false)).thenReturn(List.of(ejemplo(3L, "Ana Pérez")));

        mockMvc.perform(get("/api/v1/asignaciones/mis-asignaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].propuestaTitulo").value("Analítica educativa"));
    }

    @Test
    @DisplayName("HU-16 Criterio 1: GET mis-asignaciones?incluirCompletadas=true incluye las completadas")
    void miBandeja_incluirCompletadas_retorna200() throws Exception {
        when(asignacionService.miBandeja(true)).thenReturn(List.of(ejemplo(3L, "Ana Pérez")));

        mockMvc.perform(get("/api/v1/asignaciones/mis-asignaciones").param("incluirCompletadas", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
