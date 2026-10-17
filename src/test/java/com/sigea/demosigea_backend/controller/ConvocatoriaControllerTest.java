package com.sigea.demosigea_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sigea.demosigea_backend.dto.convocatoria.CambioFechaCierreResponse;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaRequest;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.exception.ConvocatoriaCerradaException;
import com.sigea.demosigea_backend.exception.ConvocatoriaIncompletaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                .andExpect(jsonPath("$.erroresValidacion.eventoId").exists())
                .andExpect(jsonPath("$.erroresValidacion.titulo").exists());

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
    @DisplayName("Criterio 4: GET /api/v1/convocatorias/{id}/validar-envio retorna 409 OPERACION_NO_PERMITIDA si la convocatoria no está publicada")
    void validarEnvio_estadoNoPublicado_retorna409() throws Exception {
        doThrow(new com.sigea.demosigea_backend.exception.OperacionNoPermitidaException("La convocatoria no está publicada."))
                .when(convocatoriaService).validarRecepcionPropuestas(10L);

        mockMvc.perform(get("/api/v1/convocatorias/10/validar-envio"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
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

    // ---------------- HU-11 (RF14): Publicar convocatorias ----------------

    @Test
    @DisplayName("HU-11 Criterio 1: POST /api/v1/convocatorias/{id}/publicar retorna 200 con estado publicada")
    void publicar_retorna200() throws Exception {
        when(convocatoriaService.publicar(10L)).thenReturn(respuestaEjemplo(10L, EstadoConvocatoria.publicada));

        mockMvc.perform(post("/api/v1/convocatorias/10/publicar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("publicada"))
                .andExpect(jsonPath("$.estaAbierta").value(true));
    }

    @Test
    @DisplayName("HU-11 Criterio 3: publicar sin fecha límite retorna 400 CONVOCATORIA_INCOMPLETA indicando el campo")
    void publicar_sinFechaCierre_retorna400() throws Exception {
        when(convocatoriaService.publicar(10L)).thenThrow(new ConvocatoriaIncompletaException(
                "No es posible publicar la convocatoria sin fecha límite de recepción de propuestas.",
                Map.of("fechaCierre", "La fecha límite de recepción de propuestas es obligatoria para publicarla.")));

        mockMvc.perform(post("/api/v1/convocatorias/10/publicar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONVOCATORIA_INCOMPLETA"))
                .andExpect(jsonPath("$.erroresValidacion.fechaCierre").exists());
    }

    @Test
    @DisplayName("HU-11: publicar una convocatoria que no está en borrador retorna 409")
    void publicar_noEnBorrador_retorna409() throws Exception {
        when(convocatoriaService.publicar(10L))
                .thenThrow(new OperacionNoPermitidaException("Solo se pueden publicar convocatorias en estado borrador."));

        mockMvc.perform(post("/api/v1/convocatorias/10/publicar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
    }

    @Test
    @DisplayName("HU-11 Criterio 2: PATCH /api/v1/convocatorias/{id}/fecha-cierre actualiza y reporta usuarios notificados")
    void actualizarFechaCierre_retorna200() throws Exception {
        LocalDateTime nueva = CIERRE.plusDays(15);
        ConvocatoriaResponse actualizada = new ConvocatoriaResponse(10L, 1L, "Congreso de Prueba",
                "Convocatoria Ponencias 2026", "Descripción amplia", "Requisitos de envío",
                APERTURA, nueva, EstadoConvocatoria.publicada, true);
        when(convocatoriaService.actualizarFechaCierre(eq(10L), any(LocalDateTime.class)))
                .thenReturn(new CambioFechaCierreResponse(actualizada, CIERRE, 2));

        mockMvc.perform(patch("/api/v1/convocatorias/10/fecha-cierre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fechaCierre", nueva.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuariosNotificados").value(2))
                .andExpect(jsonPath("$.convocatoria.estado").value("publicada"));
    }

    @Test
    @DisplayName("HU-11 Criterio 2: PATCH sin la nueva fecha límite retorna 400 VALIDACION_FALLIDA")
    void actualizarFechaCierre_sinFecha_retorna400() throws Exception {
        mockMvc.perform(patch("/api/v1/convocatorias/10/fecha-cierre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));

        verify(convocatoriaService, never()).actualizarFechaCierre(any(), any());
    }

    @Test
    @DisplayName("HU-11 Criterio 2: PATCH sobre una convocatoria expirada retorna 400 CONVOCATORIA_CERRADA")
    void actualizarFechaCierre_expirada_retorna400() throws Exception {
        LocalDateTime nueva = CIERRE.plusDays(1);
        when(convocatoriaService.actualizarFechaCierre(eq(10L), any()))
                .thenThrow(new ConvocatoriaCerradaException("La convocatoria ya expiró."));

        mockMvc.perform(patch("/api/v1/convocatorias/10/fecha-cierre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fechaCierre", nueva.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONVOCATORIA_CERRADA"));
    }
}
