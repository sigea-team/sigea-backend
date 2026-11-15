package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.inscripcion.CupoActividadResponse;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionActividadResponse;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionEventoRequest;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionEventoResponse;
import com.sigea.demosigea_backend.dto.inscripcion.MiInscripcionResponse;
import com.sigea.demosigea_backend.exception.ConflictoHorarioInscripcionException;
import com.sigea.demosigea_backend.exception.CupoAgotadoException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.InscripcionDuplicadaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.CategoriaParticipante;
import com.sigea.demosigea_backend.service.InscripcionService;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InscripcionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InscripcionService inscripcionService;

    @InjectMocks
    private InscripcionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Criterio 1: POST /inscripciones registra con éxito la categoría del participante (201)")
    void inscribirEvento_exitoso_retorna201() throws Exception {
        InscripcionEventoResponse resp = new InscripcionEventoResponse(
                501L, 1L, "Semana de Ingeniería", 10L, "Carlos Gómez", "carlos.gomez@ufps.edu.co",
                CategoriaParticipante.estudiante, LocalDateTime.now(), 0
        );

        when(inscripcionService.inscribirAEvento(eq(1L), any(InscripcionEventoRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/eventos/1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\":\"estudiante\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inscripcionId").value(501))
                .andExpect(jsonPath("$.categoria").value("estudiante"))
                .andExpect(jsonPath("$.personaNombre").value("Carlos Gómez"));
    }

    @Test
    @DisplayName("Criterio 1: POST /inscripciones valida que la categoría no sea nula (400)")
    void inscribirEvento_categoriaNula_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/eventos/1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Criterio 1: POST /inscripciones rechaza si el evento no tiene inscripciones abiertas (409)")
    void inscribirEvento_noAbiertas_retorna409() throws Exception {
        when(inscripcionService.inscribirAEvento(eq(1L), any(InscripcionEventoRequest.class)))
                .thenThrow(new OperacionNoPermitidaException("Las inscripciones no están abiertas porque el evento se encuentra en configuración."));

        mockMvc.perform(post("/api/v1/eventos/1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\":\"docente\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"))
                .andExpect(jsonPath("$.message").value("Las inscripciones no están abiertas porque el evento se encuentra en configuración."));
    }

    @Test
    @DisplayName("Criterio 2: POST /actividades/{id}/inscripciones registra inscripción puntual a actividad (201)")
    void inscribirActividad_exitoso_retorna201() throws Exception {
        InscripcionActividadResponse resp = new InscripcionActividadResponse(
                701L, 501L, 201L, "Taller de Spring Boot",
                LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), LocalTime.of(11, 0),
                "Aula 101", LocalDateTime.now()
        );

        when(inscripcionService.inscribirAActividad(1L, 201L)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/eventos/1/actividades/201/inscripciones"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inscripcionActividadId").value(701))
                .andExpect(jsonPath("$.actividadNombre").value("Taller de Spring Boot"))
                .andExpect(jsonPath("$.salaNombre").value("Aula 101"));
    }

    @Test
    @DisplayName("Criterio 2 y 4: POST /actividades/{id}/inscripciones rechaza cuando el aforo está lleno (409 CUPO_AGOTADO)")
    void inscribirActividad_cupoAgotado_retorna409() throws Exception {
        when(inscripcionService.inscribirAActividad(1L, 201L))
                .thenThrow(new CupoAgotadoException("La actividad «Taller de Spring Boot» ha alcanzado su cupo máximo de 2 participantes."));

        mockMvc.perform(post("/api/v1/eventos/1/actividades/201/inscripciones"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CUPO_AGOTADO"))
                .andExpect(jsonPath("$.message").value("La actividad «Taller de Spring Boot» ha alcanzado su cupo máximo de 2 participantes."));
    }

    @Test
    @DisplayName("Criterio 3: POST /actividades/{id}/inscripciones rechaza por conflicto de horario traslapado (409 CONFLICTO_HORARIO_INSCRIPCION)")
    void inscribirActividad_conflictoHorario_retorna409() throws Exception {
        when(inscripcionService.inscribirAActividad(1L, 202L))
                .thenThrow(new ConflictoHorarioInscripcionException(
                        "Conflicto de horario: ya se encuentra inscrito en la actividad «Taller de Spring Boot» (09:00 - 11:00), la cual se traslapa con «Conferencia de Ciberseguridad» (10:00 - 12:00). Debe cancelar la inscripción anterior para inscribirse a esta."));

        mockMvc.perform(post("/api/v1/eventos/1/actividades/202/inscripciones"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_HORARIO_INSCRIPCION"))
                .andExpect(jsonPath("$.message").value("Conflicto de horario: ya se encuentra inscrito en la actividad «Taller de Spring Boot» (09:00 - 11:00), la cual se traslapa con «Conferencia de Ciberseguridad» (10:00 - 12:00). Debe cancelar la inscripción anterior para inscribirse a esta."));
    }

    @Test
    @DisplayName("Criterio 4: POST /inscripciones rechaza inscripción duplicada al evento (409 INSCRIPCION_DUPLICADA)")
    void inscribirEvento_duplicado_retorna409() throws Exception {
        when(inscripcionService.inscribirAEvento(eq(1L), any(InscripcionEventoRequest.class)))
                .thenThrow(new InscripcionDuplicadaException("El participante ya se encuentra inscrito previamente en el evento «Semana de Ingeniería»."));

        mockMvc.perform(post("/api/v1/eventos/1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\":\"particular\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INSCRIPCION_DUPLICADA"))
                .andExpect(jsonPath("$.message").value("El participante ya se encuentra inscrito previamente en el evento «Semana de Ingeniería»."));
    }

    @Test
    @DisplayName("Criterio 4: POST /actividades/{id}/inscripciones rechaza inscripción duplicada a actividad (409 INSCRIPCION_DUPLICADA)")
    void inscribirActividad_duplicado_retorna409() throws Exception {
        when(inscripcionService.inscribirAActividad(1L, 201L))
                .thenThrow(new InscripcionDuplicadaException("El participante ya se encuentra inscrito previamente en la actividad «Taller de Spring Boot»."));

        mockMvc.perform(post("/api/v1/eventos/1/actividades/201/inscripciones"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INSCRIPCION_DUPLICADA"));
    }

    @Test
    @DisplayName("Criterio 5: DELETE /actividades/{id}/inscripciones/mi-inscripcion cancela y libera cupo (204)")
    void cancelarActividad_exitoso_retorna204() throws Exception {
        doNothing().when(inscripcionService).cancelarInscripcionActividad(1L, 201L);

        mockMvc.perform(delete("/api/v1/eventos/1/actividades/201/inscripciones/mi-inscripcion"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Criterio 5: GET /actividades/{id}/cupos consulta aforo y cupos disponibles (200)")
    void consultarCupos_retorna200() throws Exception {
        CupoActividadResponse resp = new CupoActividadResponse(
                201L, "Taller de Spring Boot", 50, 45L, 5L, false, true
        );

        when(inscripcionService.consultarCupo(1L, 201L)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/eventos/1/actividades/201/cupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividadId").value(201))
                .andExpect(jsonPath("$.aforoMaximo").value(50))
                .andExpect(jsonPath("$.inscritos").value(45))
                .andExpect(jsonPath("$.cuposDisponibles").value(5))
                .andExpect(jsonPath("$.cupoAgotado").value(false))
                .andExpect(jsonPath("$.estaInscrito").value(true));
    }

    @Test
    @DisplayName("GET /inscripciones/mi-inscripcion obtiene el detalle completo con actividades (200)")
    void obtenerMiInscripcion_retorna200() throws Exception {
        InscripcionActividadResponse act = new InscripcionActividadResponse(
                701L, 501L, 201L, "Taller de Spring Boot",
                LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), LocalTime.of(11, 0),
                "Aula 101", LocalDateTime.now()
        );

        MiInscripcionResponse resp = new MiInscripcionResponse(
                501L, 1L, "Semana de Ingeniería", 10L, "Carlos Gómez", "carlos.gomez@ufps.edu.co",
                CategoriaParticipante.estudiante, LocalDateTime.now(), List.of(act)
        );

        when(inscripcionService.obtenerMiInscripcion(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/eventos/1/inscripciones/mi-inscripcion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscripcionId").value(501))
                .andExpect(jsonPath("$.actividades.length()").value(1))
                .andExpect(jsonPath("$.actividades[0].actividadNombre").value("Taller de Spring Boot"));
    }
}
