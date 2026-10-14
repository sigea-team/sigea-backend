package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoAprobadoRequest;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoAprobadoResponse;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoResumenResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoPresupuestoAprobado;
import com.sigea.demosigea_backend.service.PresupuestoAprobadoService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PresupuestoAprobadoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PresupuestoAprobadoService presupuestoService;

    @InjectMocks
    private PresupuestoAprobadoController presupuestoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(presupuestoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PresupuestoAprobadoResponse respuesta(Long id, BigDecimal valor, boolean oficial) {
        return new PresupuestoAprobadoResponse(
                id,
                1L,
                "Congreso TIC",
                valor,
                new BigDecimal("45000000.00"),
                valor.subtract(new BigDecimal("45000000.00")),
                "https://archivos.ufps.edu.co/soporte.pdf",
                LocalDateTime.of(2026, 10, 11, 12, 0),
                2L,
                "Carlos Gómez",
                oficial ? EstadoPresupuestoAprobado.aprobado : EstadoPresupuestoAprobado.historico,
                oficial
        );
    }

    // ------------------------------------------------------------------
    // Criterio 1 y Criterio 2: POST /api/v1/eventos/{eventoId}/presupuesto-aprobado
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 1: POST retorna 201 Created con la versión oficial guardada")
    void registrar_exitoso_retorna201() throws Exception {
        when(presupuestoService.registrarOActualizar(eq(1L), any(PresupuestoAprobadoRequest.class)))
                .thenReturn(respuesta(10L, new BigDecimal("50000000.00"), true));

        mockMvc.perform(post("/api/v1/eventos/1/presupuesto-aprobado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorTotalAprobado\":50000000.00,\"evidenciaUrl\":\"https://archivos.ufps.edu.co/soporte.pdf\",\"aprobadoPorId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.valorTotalAprobado").value(50000000.00))
                .andExpect(jsonPath("$.valorPreliminarProyectado").value(45000000.00))
                .andExpect(jsonPath("$.diferenciaConPreliminar").value(5000000.00))
                .andExpect(jsonPath("$.evidenciaUrl").value("https://archivos.ufps.edu.co/soporte.pdf"))
                .andExpect(jsonPath("$.estado").value("aprobado"))
                .andExpect(jsonPath("$.esOficial").value(true));
    }

    // ------------------------------------------------------------------
    // Criterio 3: Exigir evidencia documental antes de guardar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 3: POST sin evidenciaUrl retorna 400 Bad Request con VALIDACION_FALLIDA")
    void registrar_sinEvidencia_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/eventos/1/presupuesto-aprobado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorTotalAprobado\":50000000.00,\"evidenciaUrl\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.evidenciaUrl").exists());

        verify(presupuestoService, never()).registrarOActualizar(any(), any());
    }

    @Test
    @DisplayName("Validación: POST con monto negativo retorna 400 Bad Request")
    void registrar_montoNegativo_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/eventos/1/presupuesto-aprobado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorTotalAprobado\":-100.00,\"evidenciaUrl\":\"https://soporte.pdf\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));

        verify(presupuestoService, never()).registrarOActualizar(any(), any());
    }

    @Test
    @DisplayName("Criterio 1: POST sin presupuesto preliminar previo retorna 409 Conflict")
    void registrar_sinPreliminar_retorna409() throws Exception {
        when(presupuestoService.registrarOActualizar(eq(1L), any(PresupuestoAprobadoRequest.class)))
                .thenThrow(new OperacionNoPermitidaException("No se puede registrar el presupuesto aprobado porque el evento no cuenta con un presupuesto preliminar registrado."));

        mockMvc.perform(post("/api/v1/eventos/1/presupuesto-aprobado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorTotalAprobado\":50000000.00,\"evidenciaUrl\":\"https://soporte.pdf\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
    }

    @Test
    @DisplayName("Validación: Evento no encontrado retorna 404 Not Found")
    void registrar_eventoNoExiste_retorna404() throws Exception {
        when(presupuestoService.registrarOActualizar(eq(999L), any(PresupuestoAprobadoRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("No se encontró el evento con ID: 999"));

        mockMvc.perform(post("/api/v1/eventos/999/presupuesto-aprobado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorTotalAprobado\":50000000.00,\"evidenciaUrl\":\"https://soporte.pdf\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
    }

    // ------------------------------------------------------------------
    // Consultas: GET /api/v1/eventos/{eventoId}/presupuesto-aprobado
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET oficial: Retorna 200 OK con la versión vigente")
    void obtenerOficial_retorna200() throws Exception {
        when(presupuestoService.obtenerOficial(1L))
                .thenReturn(respuesta(10L, new BigDecimal("50000000.00"), true));

        mockMvc.perform(get("/api/v1/eventos/1/presupuesto-aprobado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.esOficial").value(true));
    }

    @Test
    @DisplayName("GET oficial: Retorna 404 si el evento aún no tiene presupuesto aprobado")
    void obtenerOficial_noExiste_retorna404() throws Exception {
        when(presupuestoService.obtenerOficial(1L))
                .thenThrow(new RecursoNoEncontradoException("El evento con ID 1 no tiene un presupuesto aprobado oficial"));

        mockMvc.perform(get("/api/v1/eventos/1/presupuesto-aprobado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
    }

    // ------------------------------------------------------------------
    // Consultas: GET /api/v1/eventos/{eventoId}/presupuesto-aprobado/historial
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 2: GET /historial retorna 200 OK con la lista de versiones históricas y vigentes")
    void listarHistorial_retorna200() throws Exception {
        when(presupuestoService.listarHistorial(1L))
                .thenReturn(List.of(
                        respuesta(11L, new BigDecimal("52000000.00"), true),
                        respuesta(10L, new BigDecimal("50000000.00"), false)
                ));

        mockMvc.perform(get("/api/v1/eventos/1/presupuesto-aprobado/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].esOficial").value(true))
                .andExpect(jsonPath("$[1].id").value(10))
                .andExpect(jsonPath("$[1].esOficial").value(false));
    }

    // ------------------------------------------------------------------
    // Consultas: GET /api/v1/eventos/{eventoId}/presupuesto-aprobado/resumen
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /resumen retorna 200 OK con comparativa presupuestal")
    void obtenerResumen_retorna200() throws Exception {
        PresupuestoResumenResponse resumen = new PresupuestoResumenResponse(
                1L,
                "Congreso TIC",
                new BigDecimal("45000000.00"),
                5,
                respuesta(10L, new BigDecimal("50000000.00"), true),
                List.of(respuesta(10L, new BigDecimal("50000000.00"), true))
        );

        when(presupuestoService.obtenerResumen(1L)).thenReturn(resumen);

        mockMvc.perform(get("/api/v1/eventos/1/presupuesto-aprobado/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoId").value(1))
                .andExpect(jsonPath("$.totalPreliminarProyectado").value(45000000.00))
                .andExpect(jsonPath("$.cantidadRubrosPreliminares").value(5))
                .andExpect(jsonPath("$.presupuestoOficial.id").value(10));
    }
}
