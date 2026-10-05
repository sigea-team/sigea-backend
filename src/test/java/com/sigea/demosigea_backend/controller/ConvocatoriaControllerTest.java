package com.sigea.demosigea_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaRequest;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.exception.ConvocatoriaCerradaException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import com.sigea.demosigea_backend.service.ConvocatoriaService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConvocatoriaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ConvocatoriaService convocatoriaService;

    @InjectMocks
    private ConvocatoriaController convocatoriaController;

    private static final LocalDateTime APERTURA = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final LocalDateTime CIERRE = LocalDateTime.of(2026, 11, 15, 23, 59);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(convocatoriaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ConvocatoriaResponse respuestaEjemplo(Long id, EstadoConvocatoria estado) {
        return new ConvocatoriaResponse(
                id,
                1L,
                "Congreso de Prueba",
                "Convocatoria Ponencias 2026",
                "Descripción amplia",
                "Requisitos de envío",
                APERTURA,
                CIERRE,
                estado,
                estado == EstadoConvocatoria.publicada
        );
    }

    @Test
    @DisplayName("Criterio 1: POST /api/v1/convocatorias registra la convocatoria y retorna 201 Created con estado borrador")
    void crear_retorna201() throws Exception {
        ConvocatoriaRequest request = new ConvocatoriaRequest(
                1L, "Convocatoria Ponencias 2026", "Descripción", "Requisitos", APERTURA, CIERRE
        );

        when(convocatoriaService.crear(any(ConvocatoriaRequest.class)))
                .thenReturn(respuestaEjemplo(10L, EstadoConvocatoria.borrador));

        mockMvc.perform(post("/api/v1/convocatorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/convocatorias/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estado").value("borrador"))
                .andExpect(jsonPath("$.titulo").value("Convocatoria Ponencias 2026"));
    }

    @Test
    @DisplayName("Criterio 2: POST con campos obligatorios vacíos retorna 400 VALIDACION_FALLIDA")
    void crear_camposIncompletos_retorna400() throws Exception {
        ConvocatoriaRequest requestIncompleto = new ConvocatoriaRequest(
                null, "", null, null, null, null
        );

        mockMvc.perform(post("/api/v1/convocatorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestIncompleto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.errores.eventoId").exists())
                .andExpect(jsonPath("$.errores.titulo").exists());

        verify(convocatoriaService, never()).crear(any());
    }

    @Test
    @DisplayName("Criterio 2: POST con fecha de cierre anterior a la de apertura retorna 400 SOLICITUD_INVALIDA")
    void crear_fechaCierreInvalida_retorna400() throws Exception {
        ConvocatoriaRequest requestFechasInvalidas = new ConvocatoriaRequest(
                1L, "Título", "Desc", "Req", APERTURA, APERTURA
        );

        when(convocatoriaService.crear(any(ConvocatoriaRequest.class)))
                .thenThrow(new SolicitudInvalidaException("La fecha de cierre debe ser estrictamente posterior a la fecha de apertura."));

        mockMvc.perform(post("/api/v1/convocatorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestFechasInvalidas)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    @DisplayName("Criterio 3: PUT /api/v1/convocatorias/{id} actualiza contenido en borrador y retorna 200 OK")
    void actualizar_retorna200() throws Exception {
        ConvocatoriaRequest cambios = new ConvocatoriaRequest(
                1L, "Título Actualizado", "Nueva desc", "Nuevos req", APERTURA, CIERRE
        );

        when(convocatoriaService.actualizar(eq(10L), any(ConvocatoriaRequest.class)))
                .thenReturn(respuestaEjemplo(10L, EstadoConvocatoria.borrador));

        mockMvc.perform(put("/api/v1/convocatorias/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estado").value("borrador"));
    }

    @Test
    @DisplayName("Criterio 4: GET /api/v1/convocatorias/{id}/validar-envio retorna 400 CONVOCATORIA_CERRADA si la fecha transcurrió")
    void validarEnvio_convocatoriaCerrada_retorna400() throws Exception {
        doThrow(new ConvocatoriaCerradaException("La fecha de cierre de la convocatoria ya se cumplió."))
                .when(convocatoriaService).validarRecepcionPropuestas(10L);

        mockMvc.perform(get("/api/v1/convocatorias/10/validar-envio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONVOCATORIA_CERRADA"));
    }

    @Test
    @DisplayName("Criterio 4: GET /api/v1/convocatorias/{id}/validar-envio retorna 200 OK cuando está en vigencia")
    void validarEnvio_convocatoriaAbierta_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/convocatorias/10/validar-envio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/convocatorias lista todas las convocatorias")
    void listar_retorna200() throws Exception {
        when(convocatoriaService.listar(null, null))
                .thenReturn(List.of(respuestaEjemplo(10L, EstadoConvocatoria.borrador)));

        mockMvc.perform(get("/api/v1/convocatorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/convocatorias/{id} no existente retorna 404")
    void obtenerPorId_noExiste_retorna404() throws Exception {
        when(convocatoriaService.obtenerPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la convocatoria con ID: 99"));

        mockMvc.perform(get("/api/v1/convocatorias/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/convocatorias/{id} elimina correctamente")
    void eliminar_retorna200() throws Exception {
        mockMvc.perform(delete("/api/v1/convocatorias/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convocatoriaId").value("10"));

        verify(convocatoriaService).eliminar(10L);
    }
}
