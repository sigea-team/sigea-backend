package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.indicador.DashboardIndicadoresResponse;
import com.sigea.demosigea_backend.dto.indicador.GrupoIndicadoresResponse;
import com.sigea.demosigea_backend.dto.indicador.IndicadorEventoResponse;
import com.sigea.demosigea_backend.dto.indicador.MedidaSerieResponse;
import com.sigea.demosigea_backend.dto.indicador.PuntoSerieResponse;
import com.sigea.demosigea_backend.dto.indicador.SerieIndicadorResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.service.IndicadorEventoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IndicadorEventoControllerTest {

    private static final String URL = "/api/v1/eventos/5/indicadores";

    private MockMvc mockMvc;

    @Mock
    private IndicadorEventoService indicadorEventoService;

    @InjectMocks
    private IndicadorEventoController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Criterio 1: indicadores para la tabla y series para las gráficas, sin caché")
    void generar() throws Exception {
        when(indicadorEventoService.generar(5L)).thenReturn(new DashboardIndicadoresResponse(5L, "Congreso",
                EstadoEvento.en_ejecucion, false, LocalDateTime.of(2026, 11, 26, 1, 0), true, null,
                List.of(new GrupoIndicadoresResponse("ASISTENCIA", "Asistencia", true, null)),
                List.of(new IndicadorEventoResponse("TASA_ASISTENCIA", "Tasa de asistencia de los inscritos",
                                "ASISTENCIA", new BigDecimal("75.00"), "%", true, null),
                        new IndicadorEventoResponse("SATISFACCION_PROMEDIO", "Satisfacción promedio", "SATISFACCION",
                                null, "%", false, "No hay respuestas a encuestas de satisfacción del evento.")),
                List.of(new SerieIndicadorResponse("INSCRIPCIONES_POR_CATEGORIA", "Inscritos por categoría",
                        "circular", true, null, List.of(new MedidaSerieResponse("inscritos", "Inscritos", "personas")),
                        List.of(new PuntoSerieResponse("estudiante", "Estudiante",
                                Map.of("inscritos", BigDecimal.valueOf(3))))))));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.eventoId").value(5))
                .andExpect(jsonPath("$.hayDatos").value(true))
                .andExpect(jsonPath("$.indicadores[0].codigo").value("TASA_ASISTENCIA"))
                .andExpect(jsonPath("$.indicadores[0].valor").value(75.00))
                .andExpect(jsonPath("$.indicadores[1].disponible").value(false))
                .andExpect(jsonPath("$.indicadores[1].motivo")
                        .value("No hay respuestas a encuestas de satisfacción del evento."))
                .andExpect(jsonPath("$.series[0].puntos[0].valores.inscritos").value(3));
    }

    @Test
    @DisplayName("Criterio 2: sin datos responde 200 con el aviso")
    void sinDatos() throws Exception {
        when(indicadorEventoService.generar(5L)).thenReturn(new DashboardIndicadoresResponse(5L, "Congreso",
                EstadoEvento.en_configuracion, false, LocalDateTime.of(2026, 11, 26, 1, 0), false,
                "El evento todavía no tiene información.", List.of(), List.of(), List.of()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayDatos").value(false))
                .andExpect(jsonPath("$.mensaje").value("El evento todavía no tiene información."));
    }

    @Test
    @DisplayName("Evento inexistente: 404")
    void eventoInexistente() throws Exception {
        when(indicadorEventoService.generar(5L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró el evento con ID: 5"));

        mockMvc.perform(get(URL)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Permisos: administrador/organizador (REPORTES_VER, EVENTOS_EDITAR o admin)")
    void permisos() throws Exception {
        PreAuthorize anotacion = IndicadorEventoController.class.getMethod("generar", Long.class)
                .getAnnotation(PreAuthorize.class);
        assertEquals("hasAnyAuthority('REPORTES_VER', 'EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')", anotacion.value());
    }
}
