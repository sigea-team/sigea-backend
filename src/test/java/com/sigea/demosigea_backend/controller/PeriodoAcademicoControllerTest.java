package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.periodo.ComparacionSemestresResponse;
import com.sigea.demosigea_backend.dto.periodo.ConsultaPeriodoResponse;
import com.sigea.demosigea_backend.dto.periodo.FiltroPeriodo;
import com.sigea.demosigea_backend.dto.periodo.IndicadoresPeriodoResponse;
import com.sigea.demosigea_backend.dto.periodo.VariacionIndicadorResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.service.PeriodoAcademicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PeriodoAcademicoControllerTest {

    private static final String URL = "/api/v1/periodos-academicos";

    private MockMvc mockMvc;

    @Mock
    private PeriodoAcademicoService periodoService;

    @InjectMocks
    private PeriodoAcademicoController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static IndicadoresPeriodoResponse indicadores(String semestre, long eventos) {
        return new IndicadoresPeriodoResponse(new FiltroPeriodo(semestre, null, null, null, null), eventos > 0,
                eventos > 0 ? null : "No hay información disponible para el semestre " + semestre + ".",
                eventos, Map.of(), Map.of(), 0, Map.of(), 0, 0, Map.of(), 0, BigDecimal.ZERO, 0, 0, 0, 0, 0, null);
    }

    @Test
    @DisplayName("Lista los semestres registrados")
    void listarSemestres() throws Exception {
        when(periodoService.listarSemestres()).thenReturn(List.of("2026-1", "2025-2"));

        mockMvc.perform(get(URL + "/semestres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("2026-1"));
    }

    @Test
    @DisplayName("Criterio 4: semestre sin eventos responde 200 con hayInformacion=false")
    void consultarEventos_sinInformacion() throws Exception {
        FiltroPeriodo filtro = new FiltroPeriodo("2019-1", null, null, null, null);
        when(periodoService.consultarEventos(filtro)).thenReturn(new ConsultaPeriodoResponse(filtro, false,
                "No hay información disponible para el semestre 2019-1.", 0, List.of()));

        mockMvc.perform(get(URL + "/eventos").param("semestre", "2019-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayInformacion").value(false))
                .andExpect(jsonPath("$.mensaje").value("No hay información disponible para el semestre 2019-1."));
    }

    @Test
    @DisplayName("Criterio 3: indicadores por semestre")
    void calcularIndicadores() throws Exception {
        when(periodoService.calcularIndicadores(any(FiltroPeriodo.class))).thenReturn(indicadores("2026-1", 3));

        mockMvc.perform(get(URL + "/indicadores").param("semestre", "2026-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEventos").value(3))
                .andExpect(jsonPath("$.filtros.semestre").value("2026-1"));
    }

    @Test
    @DisplayName("Criterio 5: comparación de dos semestres")
    void comparar() throws Exception {
        when(periodoService.comparar(eq("2025-2"), eq("2026-1"), isNull(), isNull()))
                .thenReturn(new ComparacionSemestresResponse("2025-2", "2026-1",
                        indicadores("2025-2", 2), indicadores("2026-1", 3),
                        List.of(new VariacionIndicadorResponse("totalEventos", "Eventos realizados",
                                BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.ONE,
                                new BigDecimal("50.00"), "AUMENTO")),
                        null));

        mockMvc.perform(get(URL + "/comparacion").param("semestreA", "2025-2").param("semestreB", "2026-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodoA.totalEventos").value(2))
                .andExpect(jsonPath("$.periodoB.totalEventos").value(3))
                .andExpect(jsonPath("$.comparativo[0].tendencia").value("AUMENTO"));
    }

    @Test
    @DisplayName("Criterio 5: semestres iguales → 400 SOLICITUD_INVALIDA")
    void comparar_invalido() throws Exception {
        when(periodoService.comparar(eq("2026-1"), eq("2026-1"), isNull(), isNull()))
                .thenThrow(new SolicitudInvalidaException("Seleccione dos semestres distintos para comparar."));

        mockMvc.perform(get(URL + "/comparacion").param("semestreA", "2026-1").param("semestreB", "2026-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }
}
