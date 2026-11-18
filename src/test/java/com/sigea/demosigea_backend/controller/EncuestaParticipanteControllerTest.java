package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.encuesta.EncuestaParticipanteResponse;
import com.sigea.demosigea_backend.dto.encuesta.EnvioEncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResponderEncuestaRequest;
import com.sigea.demosigea_backend.exception.EncuestaException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.model.EstadoEncuesta;
import com.sigea.demosigea_backend.service.RespuestaEncuestaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EncuestaParticipanteControllerTest {

    private static final String URL = "/api/v1/encuestas";
    private static final String RESPUESTAS = """
            {"respuestas":[{"preguntaId":40,"valorEscala":5},{"preguntaId":41,"opcionId":410},
                           {"preguntaId":42,"texto":"Muy buen taller"}]}
            """;

    private MockMvc mockMvc;

    @Mock
    private RespuestaEncuestaService respuestaEncuestaService;

    @InjectMocks
    private EncuestaParticipanteController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Criterio 1: GET /mias lista las encuestas habilitadas")
    void misEncuestas() throws Exception {
        when(respuestaEncuestaService.misEncuestas()).thenReturn(List.of(new EncuestaParticipanteResponse(7L, 1L,
                "Semana de Ingeniería", 201L, "Taller de Spring Boot", "actividad", "¿Cómo te pareció el taller?", null,
                true, EstadoEncuesta.publicada, null, true, false, null, 3, null)));

        mockMvc.perform(get(URL + "/mias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].encuestaId").value(7))
                .andExpect(jsonPath("$[0].recibeRespuestas").value(true))
                .andExpect(jsonPath("$[0].respondida").value(false))
                .andExpect(jsonPath("$[0].preguntas").doesNotExist());
    }

    @Test
    @DisplayName("Criterio 3: sin asistencia el formulario responde 403 ENCUESTA_NO_HABILITADA")
    void noHabilitada() throws Exception {
        when(respuestaEncuestaService.formulario(7L)).thenThrow(new EncuestaException(EncuestaException.NO_HABILITADA,
                HttpStatus.FORBIDDEN, "No tienes asistencia registrada en la actividad."));

        mockMvc.perform(get(URL + "/7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ENCUESTA_NO_HABILITADA"))
                .andExpect(jsonPath("$.message").value("No tienes asistencia registrada en la actividad."));
    }

    @Test
    @DisplayName("Criterio 2: POST /respuestas guarda las respuestas (201)")
    void responde() throws Exception {
        when(respuestaEncuestaService.responder(eq(7L), any(ResponderEncuestaRequest.class))).thenReturn(
                new EnvioEncuestaResponse(7L, "¿Cómo te pareció el taller?", true, 3,
                        LocalDateTime.of(2026, 11, 20, 10, 30), "Gracias por responder."));

        mockMvc.perform(post(URL + "/7/respuestas").contentType(MediaType.APPLICATION_JSON).content(RESPUESTAS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preguntasRespondidas").value(3))
                .andExpect(jsonPath("$.anonima").value(true));
    }

    @Test
    @DisplayName("POST sin la lista de respuestas → 400 VALIDACION_FALLIDA sin llamar al servicio")
    void sinRespuestas() throws Exception {
        mockMvc.perform(post(URL + "/7/respuestas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
        verify(respuestaEncuestaService, never()).responder(anyLong(), any());
    }

    @Test
    @DisplayName("Respuestas inválidas → 400 RESPUESTAS_INVALIDAS con el detalle por pregunta")
    void respuestasInvalidas() throws Exception {
        when(respuestaEncuestaService.responder(eq(7L), any(ResponderEncuestaRequest.class))).thenThrow(
                new EncuestaException(EncuestaException.RESPUESTAS_INVALIDAS, HttpStatus.BAD_REQUEST,
                        "Revise sus respuestas antes de enviar la encuesta.",
                        Map.of("pregunta_41", "Esta pregunta es obligatoria.")));

        mockMvc.perform(post(URL + "/7/respuestas").contentType(MediaType.APPLICATION_JSON).content(RESPUESTAS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("RESPUESTAS_INVALIDAS"))
                .andExpect(jsonPath("$.erroresValidacion.pregunta_41").value("Esta pregunta es obligatoria."));
    }

    @Test
    @DisplayName("Ya respondida → 409 ENCUESTA_YA_RESPONDIDA")
    void yaRespondida() throws Exception {
        when(respuestaEncuestaService.responder(eq(7L), any(ResponderEncuestaRequest.class))).thenThrow(
                new EncuestaException(EncuestaException.YA_RESPONDIDA, HttpStatus.CONFLICT, "Ya respondiste la encuesta."));

        mockMvc.perform(post(URL + "/7/respuestas").contentType(MediaType.APPLICATION_JSON).content(RESPUESTAS))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ENCUESTA_YA_RESPONDIDA"));
    }
}
