package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.actividad.ActividadRequest;
import com.sigea.demosigea_backend.dto.actividad.ActividadResponse;
import com.sigea.demosigea_backend.dto.actividad.ConflictoActividadResponse;
import com.sigea.demosigea_backend.dto.actividad.ConflictoActividadResponse.TipoConflicto;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.ProgramacionActividadException;
import com.sigea.demosigea_backend.service.ActividadService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del controlador de la agenda (HU-21): validación de datos obligatorios y respuestas 409.
 */
@ExtendWith(MockitoExtension.class)
class ActividadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ActividadService actividadService;

    @InjectMocks
    private ActividadController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String VALIDA = """
            {"nombre":"Taller IA","tipoActividadId":5,"fecha":"2027-03-10","horaInicio":"09:00",
             "horaFin":"11:00","salaId":10,"ponenteId":20}""";

    @Test
    @DisplayName("Criterio 1: POST programa la actividad y responde 201")
    void crear_retorna201() throws Exception {
        when(actividadService.crear(eq(1L), any(ActividadRequest.class))).thenReturn(new ActividadResponse(
                99L, 1L, "Taller IA", 5L, "Taller", LocalDate.of(2027, 3, 10), LocalTime.of(9, 0),
                LocalTime.of(11, 0), 10L, "Auditorio", 20L, "Ana Pérez", null, null, null, false, "programada", List.of(),
                120, false));

        mockMvc.perform(post("/api/v1/eventos/1/actividades").contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.salaNombre").value("Auditorio"));
    }

    @Test
    @DisplayName("Criterio 4: sin fecha, horas ni sala responde 400 indicando los campos y no llama al servicio")
    void crear_sinDatosObligatorios_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/eventos/1/actividades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Taller IA\",\"tipoActividadId\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.fecha").value("La fecha de la actividad es obligatoria."))
                .andExpect(jsonPath("$.erroresValidacion.horaInicio").exists())
                .andExpect(jsonPath("$.erroresValidacion.horaFin").exists())
                .andExpect(jsonPath("$.erroresValidacion.salaId").value("La sala es obligatoria."));

        verify(actividadService, never()).crear(any(), any());
    }

    @Test
    @DisplayName("Criterio 2: un cruce responde 409 CONFLICTO_PROGRAMACION con la lista de conflictos")
    void crear_conCruce_retorna409() throws Exception {
        ConflictoActividadResponse c = new ConflictoActividadResponse(TipoConflicto.SALA, 1L, "Conferencia",
                LocalDate.of(2027, 3, 10), LocalTime.of(9, 0), LocalTime.of(11, 0), "Auditorio",
                "La sala «Auditorio» ya está ocupada.");
        when(actividadService.crear(eq(1L), any(ActividadRequest.class))).thenThrow(new ProgramacionActividadException(
                ProgramacionActividadException.CONFLICTO_PROGRAMACION, "Hay cruces.", List.of(c), List.of()));

        mockMvc.perform(post("/api/v1/eventos/1/actividades").contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_PROGRAMACION"))
                .andExpect(jsonPath("$.conflictos[0].tipo").value("SALA"))
                .andExpect(jsonPath("$.conflictos[0].actividadId").value(1));
    }
}
