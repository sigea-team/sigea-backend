package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.asistencia.CodigoQrResponse;
import com.sigea.demosigea_backend.dto.asistencia.ReenvioQrResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.service.CodigoQrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CodigoQrControllerTest {

    private static final String CODIGO = "SIGEA-11112222333344445555666677778888";

    private MockMvc mockMvc;

    @Mock
    private CodigoQrService codigoQrService;

    @InjectMocks
    private CodigoQrController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CodigoQrResponse codigo() {
        return new CodigoQrResponse(77L, 501L, 1L, "Semana de Ingeniería", 10L, "María Pérez",
                "maria.perez@ufps.edu.co", CODIGO, LocalDateTime.of(2026, 11, 15, 10, 0), true);
    }

    @Test
    @DisplayName("Criterio 1: GET mi-inscripcion/qr devuelve el código de la inscripción")
    void miCodigo() throws Exception {
        when(codigoQrService.consultarMiCodigo(1L)).thenReturn(codigo());

        mockMvc.perform(get("/api/v1/eventos/1/inscripciones/mi-inscripcion/qr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(CODIGO))
                .andExpect(jsonPath("$.vigente").value(true));
    }

    @Test
    @DisplayName("GET mi-inscripcion/qr sin inscripción → 404")
    void miCodigoSinInscripcion() throws Exception {
        when(codigoQrService.consultarMiCodigo(1L)).thenThrow(new RecursoNoEncontradoException("No inscrito"));

        mockMvc.perform(get("/api/v1/eventos/1/inscripciones/mi-inscripcion/qr"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET mi-inscripcion/qr/imagen devuelve el PNG sin caché")
    void imagen() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        when(codigoQrService.imagenMiCodigo(1L, 500)).thenReturn(png);

        mockMvc.perform(get("/api/v1/eventos/1/inscripciones/mi-inscripcion/qr/imagen").param("tamano", "500"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    @DisplayName("GET imagen sin tamaño usa el tamaño por defecto")
    void imagenTamanoPorDefecto() throws Exception {
        when(codigoQrService.imagenMiCodigo(1L, 320)).thenReturn(new byte[]{1});

        mockMvc.perform(get("/api/v1/eventos/1/inscripciones/mi-inscripcion/qr/imagen"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Criterio 4: POST mi-inscripcion/qr/reenviar reenvía el mismo código")
    void reenviarMiCodigo() throws Exception {
        when(codigoQrService.reenviarMiCodigo(1L)).thenReturn(new ReenvioQrResponse(501L, "maria.perez@ufps.edu.co",
                CODIGO, true, LocalDateTime.now(), "Se reenvió el código QR."));

        mockMvc.perform(post("/api/v1/eventos/1/inscripciones/mi-inscripcion/qr/reenviar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(CODIGO))
                .andExpect(jsonPath("$.enviado").value(true));
    }

    @Test
    @DisplayName("Criterio 4: el organizador reenvía el QR de una inscripción")
    void organizadorReenvia() throws Exception {
        when(codigoQrService.reenviarCodigo(1L, 501L)).thenReturn(new ReenvioQrResponse(501L, "maria.perez@ufps.edu.co",
                CODIGO, true, LocalDateTime.now(), "Se reenvió el código QR."));

        mockMvc.perform(post("/api/v1/eventos/1/inscripciones/501/qr/reenviar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscripcionId").value(501));
    }

    @Test
    @DisplayName("Criterio 4: sin correo registrado → 409 OPERACION_NO_PERMITIDA")
    void reenvioSinCorreo() throws Exception {
        when(codigoQrService.reenviarCodigo(1L, 501L)).thenThrow(new OperacionNoPermitidaException("Sin correo"));

        mockMvc.perform(post("/api/v1/eventos/1/inscripciones/501/qr/reenviar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
    }

    @Test
    @DisplayName("El organizador consulta el QR de una inscripción")
    void organizadorConsulta() throws Exception {
        when(codigoQrService.consultarCodigo(1L, 501L)).thenReturn(codigo());

        mockMvc.perform(get("/api/v1/eventos/1/inscripciones/501/qr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participanteNombre").value("María Pérez"));
    }
}
