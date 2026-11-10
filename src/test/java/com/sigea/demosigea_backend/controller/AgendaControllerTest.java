package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.actividad.ActividadResponse;
import com.sigea.demosigea_backend.dto.agenda.ActividadAgendaPublicaResponse;
import com.sigea.demosigea_backend.dto.agenda.ActividadIncompletaResponse;
import com.sigea.demosigea_backend.dto.agenda.ActividadPendienteResponse;
import com.sigea.demosigea_backend.dto.agenda.ActividadPorProgramarRequest;
import com.sigea.demosigea_backend.dto.agenda.AgendaPublicaResponse;
import com.sigea.demosigea_backend.dto.agenda.AsignacionPropuestaResponse;
import com.sigea.demosigea_backend.dto.agenda.DiaAgendaPublicaResponse;
import com.sigea.demosigea_backend.dto.agenda.GeneracionAgendaResponse;
import com.sigea.demosigea_backend.dto.agenda.PublicacionAgendaResponse;
import com.sigea.demosigea_backend.exception.AgendaNoPublicableException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.ModalidadEvento;
import com.sigea.demosigea_backend.service.ActividadService;
import com.sigea.demosigea_backend.service.AgendaService;
import com.sigea.demosigea_backend.service.GeneradorAgenda.MotivoPendiente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los controladores de HU-22: generación, publicación, consulta pública y registro de actividades por
 * programar.
 */
@ExtendWith(MockitoExtension.class)
class AgendaControllerTest {

    private static final LocalDate DIA = LocalDate.of(2027, 3, 10);

    private MockMvc mockMvc;

    @Mock
    private AgendaService agendaService;

    @Mock
    private ActividadService actividadService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AgendaController(agendaService),
                        new AgendaPublicaController(agendaService), new ActividadController(actividadService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Criterios 1 y 2: POST /generar devuelve la propuesta sin aplicar, con asignadas y pendientes")
    void proponer() throws Exception {
        when(agendaService.proponer(1L, false)).thenReturn(new GeneracionAgendaResponse(1L, false, 2,
                List.of(new AsignacionPropuestaResponse(2L, "Taller", DIA, LocalTime.of(9, 0), LocalTime.of(11, 0),
                        10L, "Auditorio", 20L, "Ana Pérez")),
                List.of(new ActividadPendienteResponse(3L, "Charla", 60, null, null,
                        MotivoPendiente.SIN_ESPACIO_LIBRE, "Todos los horarios chocan."))));

        mockMvc.perform(post("/api/v1/eventos/1/agenda/generar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada").value(false))
                .andExpect(jsonPath("$.asignadas[0].salaNombre").value("Auditorio"))
                .andExpect(jsonPath("$.pendientes[0].actividadId").value(3))
                .andExpect(jsonPath("$.pendientes[0].motivo").value("SIN_ESPACIO_LIBRE"));
    }

    @Test
    @DisplayName("Criterio 1: POST /generar/aplicar guarda la propuesta y acepta reubicar las automáticas")
    void aplicar() throws Exception {
        when(agendaService.aplicar(1L, true)).thenReturn(new GeneracionAgendaResponse(1L, true, 0, List.of(), List.of()));

        mockMvc.perform(post("/api/v1/eventos/1/agenda/generar/aplicar").param("reubicarAutomaticas", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada").value(true));
        verify(agendaService).aplicar(1L, true);
    }

    @Test
    @DisplayName("Criterio 3: POST /publicar devuelve los días publicados")
    void publicar() throws Exception {
        when(agendaService.publicar(1L)).thenReturn(new PublicacionAgendaResponse(1L,
                LocalDateTime.of(2027, 3, 1, 10, 0), List.of(DIA, DIA.plusDays(1)), 5));

        mockMvc.perform(post("/api/v1/eventos/1/agenda/publicar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diasPublicados.length()").value(2))
                .andExpect(jsonPath("$.totalActividades").value(5));
    }

    @Test
    @DisplayName("Criterio 4: publicar con actividades sin sala u horario responde 409 AGENDA_INCOMPLETA con la lista")
    void publicarIncompleta() throws Exception {
        when(agendaService.publicar(1L)).thenThrow(new AgendaNoPublicableException(
                AgendaNoPublicableException.AGENDA_INCOMPLETA, "Hay 1 actividad(es) sin sala u horario asignado.",
                List.of(new ActividadIncompletaResponse(4L, "Panel", List.of("fecha", "horario", "sala"))), List.of()));

        mockMvc.perform(post("/api/v1/eventos/1/agenda/publicar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("AGENDA_INCOMPLETA"))
                .andExpect(jsonPath("$.incompletas[0].actividadId").value(4))
                .andExpect(jsonPath("$.incompletas[0].faltantes[2]").value("sala"))
                .andExpect(jsonPath("$.path").value("/api/v1/eventos/1/agenda/publicar"));
    }

    @Test
    @DisplayName("Criterio 3: la agenda publicada se consulta sin autenticación, organizada por día")
    void agendaPublica() throws Exception {
        when(agendaService.agendaPublica(1L)).thenReturn(new AgendaPublicaResponse(1L, "Simposio",
                LocalDateTime.of(2027, 3, 1, 10, 0), List.of(new DiaAgendaPublicaResponse(DIA, List.of(
                new ActividadAgendaPublicaResponse(2L, "Taller", "Taller", LocalTime.of(9, 0), LocalTime.of(11, 0),
                        "Auditorio", "Edificio A", "Ana Pérez", ModalidadEvento.presencial, null, false))))));

        mockMvc.perform(get("/api/v1/publico/eventos/1/agenda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoNombre").value("Simposio"))
                .andExpect(jsonPath("$.dias[0].actividades[0].nombre").value("Taller"));
    }

    @Test
    @DisplayName("Criterio 3: sin agenda publicada la consulta pública responde 404")
    void agendaPublicaNoPublicada() throws Exception {
        when(agendaService.agendaPublica(1L)).thenThrow(new RecursoNoEncontradoException("Aún no está publicada."));
        mockMvc.perform(get("/api/v1/publico/eventos/1/agenda")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Criterio 1: registrar una actividad por programar responde 201")
    void registrarPorProgramar() throws Exception {
        when(actividadService.registrarPorProgramar(eq(1L), any(ActividadPorProgramarRequest.class)))
                .thenReturn(new ActividadResponse(9L, 1L, "Panel", 5L, "Panel", null, null, null, null, null, null,
                        null, null, null, null, false, "por_programar", List.of(), 90, false));

        mockMvc.perform(post("/api/v1/eventos/1/actividades/por-programar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Panel\",\"tipoActividadId\":5,\"duracionMinutos\":90}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("por_programar"))
                .andExpect(jsonPath("$.duracionMinutos").value(90));
    }

    @Test
    @DisplayName("registrar una actividad por programar sin duración responde 400 y no llama al servicio")
    void registrarPorProgramarSinDuracion() throws Exception {
        mockMvc.perform(post("/api/v1/eventos/1/actividades/por-programar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Panel\",\"tipoActividadId\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.duracionMinutos")
                        .value("La duración de la actividad es obligatoria."));
        verify(actividadService, never()).registrarPorProgramar(any(), any());
    }

    @Test
    @DisplayName("devolver una actividad a por programar responde 200")
    void desprogramar() throws Exception {
        when(actividadService.desprogramar(1L, 9L)).thenReturn(new ActividadResponse(9L, 1L, "Panel", 5L, "Panel",
                null, null, null, null, null, null, null, null, null, null, false, "por_programar", List.of(), 90,
                false));
        mockMvc.perform(post("/api/v1/eventos/1/actividades/9/desprogramar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("por_programar"));
    }
}
