package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.evento.EdicionesEventoResponse;
import com.sigea.demosigea_backend.dto.evento.EventoRequest;
import com.sigea.demosigea_backend.dto.evento.EventoResponse;
import com.sigea.demosigea_backend.dto.evento.NuevaEdicionRequest;
import com.sigea.demosigea_backend.exception.ConfirmacionRequeridaException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.ModalidadEvento;
import com.sigea.demosigea_backend.service.EventoService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventoService eventoService;

    @InjectMocks
    private EventoController eventoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private EventoResponse respuesta(Long id, Long baseId, EstadoEvento estado) {
        return new EventoResponse(id, "Congreso", null, null, "Congreso", 1L, ModalidadEvento.presencial,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22), "2026-2", estado, baseId, baseId != null);
    }

    @Test
    @DisplayName("Criterio 1: POST /api/v1/eventos retorna 201 y estado en_configuracion")
    void crear_retorna201() throws Exception {
        when(eventoService.crear(any(EventoRequest.class)))
                .thenReturn(respuesta(1L, null, EstadoEvento.en_configuracion));

        String body = """
                {"nombre":"Congreso","tipoEventoId":1,"modalidad":"presencial",
                 "fechaInicio":"2026-10-20","fechaFin":"2026-10-22"}
                """;

        mockMvc.perform(post("/api/v1/eventos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("en_configuracion"))
                .andExpect(header().string("Location", "http://localhost/api/v1/eventos/1"));
    }

    @Test
    @DisplayName("Criterio 5: datos obligatorios incompletos → 400 con los campos faltantes")
    void crear_datosIncompletos_retorna400() throws Exception {
        String body = """
                {"nombre":"","fechaInicio":"2026-10-20"}
                """;

        mockMvc.perform(post("/api/v1/eventos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.nombre").exists())
                .andExpect(jsonPath("$.erroresValidacion.tipoEventoId").exists())
                .andExpect(jsonPath("$.erroresValidacion.modalidad").exists())
                .andExpect(jsonPath("$.erroresValidacion.fechaFin").exists());
        verify(eventoService, never()).crear(any());
    }

    @Test
    @DisplayName("Criterio 5: fecha fin anterior a la de inicio → 400")
    void crear_rangoFechasInvalido_retorna400() throws Exception {
        String body = """
                {"nombre":"Congreso","tipoEventoId":1,"modalidad":"virtual",
                 "fechaInicio":"2026-10-22","fechaFin":"2026-10-20"}
                """;

        mockMvc.perform(post("/api/v1/eventos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.rangoFechasValido").exists());
    }

    @Test
    @DisplayName("Criterio 3: POST /api/v1/eventos/{id}/ediciones retorna 201 vinculado al base")
    void crearEdicion_retorna201() throws Exception {
        when(eventoService.crearEdicion(eq(1L), any(NuevaEdicionRequest.class)))
                .thenReturn(respuesta(2L, 1L, EstadoEvento.en_configuracion));

        String body = """
                {"fechaInicio":"2027-10-19","fechaFin":"2027-10-21"}
                """;

        mockMvc.perform(post("/api/v1/eventos/1/ediciones").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventoBaseId").value(1))
                .andExpect(jsonPath("$.esEdicion").value(true))
                .andExpect(header().string("Location", "http://localhost/api/v1/eventos/2"));
    }

    @Test
    @DisplayName("Criterio 4: GET /api/v1/eventos/{id}/ediciones retorna la lista ordenada con estado")
    void listarEdiciones_retorna200() throws Exception {
        EdicionesEventoResponse response = new EdicionesEventoResponse(1L, "Congreso", 2, List.of(
                respuesta(1L, null, EstadoEvento.cerrado),
                respuesta(2L, 1L, EstadoEvento.en_configuracion)));
        when(eventoService.listarEdiciones(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/eventos/1/ediciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEdiciones").value(2))
                .andExpect(jsonPath("$.ediciones[0].estado").value("cerrado"))
                .andExpect(jsonPath("$.ediciones[1].estado").value("en_configuracion"));
    }

    @Test
    @DisplayName("Criterio 5: DELETE sin confirmar sobre evento con información → 409 CONFIRMACION_REQUERIDA")
    void eliminar_requiereConfirmacion_retorna409() throws Exception {
        doThrow(new ConfirmacionRequeridaException("Requiere confirmación"))
                .when(eventoService).eliminar(1L, false);

        mockMvc.perform(delete("/api/v1/eventos/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFIRMACION_REQUERIDA"));
    }

    @Test
    @DisplayName("Criterio 5: DELETE con confirmar=true retorna 200")
    void eliminar_confirmado_retorna200() throws Exception {
        mockMvc.perform(delete("/api/v1/eventos/1").param("confirmar", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoId").value("1"));
        verify(eventoService).eliminar(1L, true);
    }

    @Test
    @DisplayName("Modalidad inexistente en el JSON → 400")
    void crear_modalidadInvalida_retorna400() throws Exception {
        String body = """
                {"nombre":"Congreso","tipoEventoId":1,"modalidad":"mixta",
                 "fechaInicio":"2026-10-20","fechaFin":"2026-10-22"}
                """;

        mockMvc.perform(post("/api/v1/eventos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Criterio 5: confirmar con un valor distinto de true (p. ej. yes) se rechaza sin eliminar")
    void eliminar_confirmarAmbiguo_retorna409() throws Exception {
        mockMvc.perform(delete("/api/v1/eventos/1").param("confirmar", "yes"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFIRMACION_REQUERIDA"));
        verify(eventoService, never()).eliminar(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Criterio 5: confirmar=false equivale a no confirmar")
    void eliminar_confirmarFalse_noConfirma() throws Exception {
        mockMvc.perform(delete("/api/v1/eventos/1").param("confirmar", "false"));
        verify(eventoService).eliminar(1L, false);
    }

    @Test
    @DisplayName("Publicar: POST /api/v1/eventos/{id}/publicar retorna 200 con estado habilitado")
    void publicar_retorna200() throws Exception {
        when(eventoService.publicar(1L)).thenReturn(respuesta(1L, null, EstadoEvento.habilitado));

        mockMvc.perform(post("/api/v1/eventos/1/publicar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("habilitado"));
    }

    @Test
    @DisplayName("Listar sin autenticación filtra eventos en configuración y solo devuelve publicados")
    void listar_sinAutenticacion_filtraConfiguracion() throws Exception {
        when(eventoService.listar(isNull(), isNull())).thenReturn(List.of(
                respuesta(1L, null, EstadoEvento.habilitado),
                respuesta(2L, null, EstadoEvento.en_configuracion),
                respuesta(3L, null, EstadoEvento.en_ejecucion),
                respuesta(4L, null, EstadoEvento.cerrado)
        ));

        mockMvc.perform(get("/api/v1/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[2].id").value(4));
    }

    @Test
    @DisplayName("Consultar por ID sin autenticación sobre evento en configuración retorna 404")
    void obtenerPorId_sinAutenticacion_enConfiguracion_retorna404() throws Exception {
        when(eventoService.obtenerPorId(2L)).thenReturn(respuesta(2L, null, EstadoEvento.en_configuracion));

        mockMvc.perform(get("/api/v1/eventos/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Consultar por ID sin autenticación sobre evento habilitado retorna 200")
    void obtenerPorId_sinAutenticacion_habilitado_retorna200() throws Exception {
        when(eventoService.obtenerPorId(1L)).thenReturn(respuesta(1L, null, EstadoEvento.habilitado));

        mockMvc.perform(get("/api/v1/eventos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("habilitado"));
    }
}
