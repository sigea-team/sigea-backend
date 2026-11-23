package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.historial.HistorialParticipacionResponse;
import com.sigea.demosigea_backend.dto.historial.RolParticipacion;
import com.sigea.demosigea_backend.dto.periodo.FiltroPeriodo;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.service.HistorialParticipacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HistorialParticipacionControllerTest {

    private static final String URL = "/api/v1/historial-participacion/personas";

    private MockMvc mockMvc;

    @Mock
    private HistorialParticipacionService historialService;

    @InjectMocks
    private HistorialParticipacionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static HistorialParticipacionResponse sinHistorial() {
        return new HistorialParticipacionResponse(7L, "Ana Pérez", "1090", "ana@ufps.edu.co", false, true,
                "La persona no tiene participación registrada", 0, new EnumMap<>(RolParticipacion.class), List.of());
    }

    @Test
    @DisplayName("Criterios 2 y 3: GET con filtros retorna 200 y pasa los filtros al servicio")
    void consultar_conFiltros() throws Exception {
        when(historialService.consultar(eq(7L), any(FiltroPeriodo.class))).thenReturn(sinHistorial());

        mockMvc.perform(get(URL + "/7").param("semestre", "2026-1").param("fechaDesde", "2026-01-01")
                        .param("tipoEventoId", "2").param("tipoActividad", "Taller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primeraParticipacion").value(true))
                .andExpect(jsonPath("$.tieneHistorial").value(false));

        ArgumentCaptor<FiltroPeriodo> captor = ArgumentCaptor.forClass(FiltroPeriodo.class);
        verify(historialService).consultar(eq(7L), captor.capture());
        assertEquals(new FiltroPeriodo("2026-1", LocalDate.of(2026, 1, 1), null, 2L, "Taller"), captor.getValue());
    }

    @Test
    @DisplayName("Persona inexistente → 404")
    void consultar_noExiste() throws Exception {
        when(historialService.consultar(eq(99L), any())).thenThrow(new RecursoNoEncontradoException("No existe"));

        mockMvc.perform(get(URL + "/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Filtro inválido → 400 SOLICITUD_INVALIDA")
    void consultar_filtroInvalido() throws Exception {
        when(historialService.consultar(eq(7L), any())).thenThrow(new SolicitudInvalidaException("Formato"));

        mockMvc.perform(get(URL + "/7").param("semestre", "2026-9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    @DisplayName("Fecha con formato inválido → 400")
    void consultar_fechaInvalida() throws Exception {
        mockMvc.perform(get(URL + "/7").param("fechaDesde", "01/01/2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Consulta por documento retorna 200")
    void consultarPorDocumento() throws Exception {
        when(historialService.consultarPorIdentificacion(eq("1090"), isNull(), any(FiltroPeriodo.class)))
                .thenReturn(sinHistorial());

        mockMvc.perform(get(URL).param("documento", "1090"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personaId").value(7));
    }
}
