package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.encuesta.ConteoResultadoResponse;
import com.sigea.demosigea_backend.dto.encuesta.EncuestaRequest;
import com.sigea.demosigea_backend.dto.encuesta.EncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.PreguntaEncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResultadoPreguntaResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResultadosEncuestaResponse;
import com.sigea.demosigea_backend.exception.EncuestaException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.model.EstadoEncuesta;
import com.sigea.demosigea_backend.model.TipoRespuestaEncuesta;
import com.sigea.demosigea_backend.service.EncuestaService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EncuestaControllerTest {

    private static final String URL = "/api/v1/eventos/1/encuestas";
    private static final String CUERPO = """
            {"titulo":"¿Cómo te pareció el taller?","actividadId":201,"anonima":true,
             "preguntas":[{"texto":"Satisfacción","tipoRespuesta":"escala"},
                          {"texto":"¿Lo recomendarías?","tipoRespuesta":"opcion","opciones":["Sí","No"]}]}
            """;

    private MockMvc mockMvc;

    @Mock
    private EncuestaService encuestaService;

    @InjectMocks
    private EncuestaController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static EncuestaResponse encuesta(EstadoEncuesta estado) {
        return new EncuestaResponse(7L, 1L, "Semana de Ingeniería", 201L, "Taller de Spring Boot", "actividad",
                "¿Cómo te pareció el taller?", null, true, estado, estado == EstadoEncuesta.publicada,
                LocalDateTime.of(2026, 11, 19, 9, 0), estado == EstadoEncuesta.borrador ? null : LocalDateTime.of(2026, 11, 20, 9, 0),
                null, 2, 0,
                List.of(new PreguntaEncuestaResponse(40L, 1, "Satisfacción", TipoRespuestaEncuesta.escala, true, 1, 5, null)));
    }

    @Test
    @DisplayName("Criterio 1: POST crea la encuesta en borrador (201)")
    void crea() throws Exception {
        when(encuestaService.crear(eq(1L), any(EncuestaRequest.class))).thenReturn(encuesta(EstadoEncuesta.borrador));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.encuestaId").value(7))
                .andExpect(jsonPath("$.estado").value("borrador"))
                .andExpect(jsonPath("$.anonima").value(true))
                .andExpect(jsonPath("$.preguntas[0].tipoRespuesta").value("escala"))
                .andExpect(jsonPath("$.preguntas[0].opciones").doesNotExist());
    }

    @Test
    @DisplayName("POST sin título responde 400 VALIDACION_FALLIDA sin llamar al servicio")
    void sinTitulo() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"  \",\"preguntas\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
        verify(encuestaService, never()).crear(anyLong(), any());
    }

    @Test
    @DisplayName("Configuración inválida → 400 ENCUESTA_CONFIGURACION_INVALIDA con erroresValidacion")
    void configuracionInvalida() throws Exception {
        when(encuestaService.crear(eq(1L), any(EncuestaRequest.class))).thenThrow(new EncuestaException(
                EncuestaException.CONFIGURACION_INVALIDA, HttpStatus.BAD_REQUEST, "Revise la configuración de la encuesta.",
                Map.of("preguntas[1].opciones", "Una pregunta de opción necesita entre 2 y 20 opciones.")));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENCUESTA_CONFIGURACION_INVALIDA"))
                .andExpect(jsonPath("$.erroresValidacion['preguntas[1].opciones']").exists())
                .andExpect(jsonPath("$.path").value(URL));
    }

    @Test
    @DisplayName("PUT de una encuesta publicada → 409 ENCUESTA_NO_EDITABLE")
    void noEditable() throws Exception {
        when(encuestaService.actualizar(eq(1L), eq(7L), any(EncuestaRequest.class))).thenThrow(new EncuestaException(
                EncuestaException.NO_EDITABLE, HttpStatus.CONFLICT, "Ya fue publicada."));

        mockMvc.perform(put(URL + "/7").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ENCUESTA_NO_EDITABLE"))
                .andExpect(jsonPath("$.erroresValidacion").doesNotExist());
    }

    @Test
    @DisplayName("Criterio 1: POST /publicar publica la encuesta (200)")
    void publica() throws Exception {
        when(encuestaService.publicar(1L, 7L)).thenReturn(encuesta(EstadoEncuesta.publicada));

        mockMvc.perform(post(URL + "/7/publicar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("publicada"))
                .andExpect(jsonPath("$.recibeRespuestas").value(true))
                .andExpect(jsonPath("$.totalHabilitados").value(2));
    }

    @Test
    @DisplayName("Publicar sin preguntas → 409 ENCUESTA_INCOMPLETA con el detalle")
    void publicaIncompleta() throws Exception {
        when(encuestaService.publicar(1L, 7L)).thenThrow(new EncuestaException(EncuestaException.INCOMPLETA,
                HttpStatus.CONFLICT, "La encuesta no se puede publicar todavía.",
                Map.of("preguntas", "Agregue al menos una pregunta antes de publicar.")));

        mockMvc.perform(post(URL + "/7/publicar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ENCUESTA_INCOMPLETA"))
                .andExpect(jsonPath("$.erroresValidacion.preguntas").exists());
    }

    @Test
    @DisplayName("POST /cerrar cierra la encuesta y DELETE responde 204")
    void cierraYElimina() throws Exception {
        when(encuestaService.cerrar(1L, 7L)).thenReturn(encuesta(EstadoEncuesta.cerrada));

        mockMvc.perform(post(URL + "/7/cerrar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("cerrada"));
        mockMvc.perform(delete(URL + "/8"))
                .andExpect(status().isNoContent());
        verify(encuestaService).eliminar(1L, 8L);
    }

    @Test
    @DisplayName("Criterio 4: GET /resultados devuelve los resultados consolidados")
    void resultados() throws Exception {
        ResultadoPreguntaResponse escala = new ResultadoPreguntaResponse(40L, 1, "Satisfacción", TipoRespuestaEncuesta.escala,
                true, 2, 0, 1, 5, new BigDecimal("3.50"),
                List.of(new ConteoResultadoResponse(5, null, "5", 1, new BigDecimal("50.0"))), null);
        when(encuestaService.resultados(1L, 7L)).thenReturn(new ResultadosEncuestaResponse(7L, "¿Cómo te pareció el taller?",
                1L, "Semana de Ingeniería", 201L, "Taller de Spring Boot", "actividad", true, EstadoEncuesta.publicada,
                2, 2, new BigDecimal("100.0"), LocalDateTime.of(2026, 11, 21, 8, 0), List.of(escala)));

        mockMvc.perform(get(URL + "/7/resultados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRespuestas").value(2))
                .andExpect(jsonPath("$.tasaRespuesta").value(100.0))
                .andExpect(jsonPath("$.preguntas[0].promedio").value(3.5))
                .andExpect(jsonPath("$.preguntas[0].distribucion[0].valor").value(5))
                .andExpect(jsonPath("$.preguntas[0].distribucion[0].opcionId").doesNotExist())
                .andExpect(jsonPath("$.preguntas[0].respuestasTexto").doesNotExist());
    }

    @Test
    @DisplayName("Criterio 2: respuestas por participante de una anónima → 409 ENCUESTA_ANONIMA")
    void respuestasDeAnonima() throws Exception {
        when(encuestaService.respuestasPorParticipante(1L, 7L)).thenThrow(new EncuestaException(
                EncuestaException.ANONIMA, HttpStatus.CONFLICT, "La encuesta es anónima."));

        mockMvc.perform(get(URL + "/7/respuestas"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ENCUESTA_ANONIMA"));
    }
}
