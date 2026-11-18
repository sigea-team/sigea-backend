package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.asistencia.AsistenciaResponse;
import com.sigea.demosigea_backend.dto.asistencia.AsistenciasActividadResponse;
import com.sigea.demosigea_backend.exception.AsistenciaRechazadaException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.model.CategoriaParticipante;
import com.sigea.demosigea_backend.model.MetodoRegistroAsistencia;
import com.sigea.demosigea_backend.service.AsistenciaService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AsistenciaControllerTest {

    private static final String URL = "/api/v1/eventos/1/actividades/201/asistencias";
    private static final String CODIGO = "SIGEA-11112222333344445555666677778888";

    private MockMvc mockMvc;

    @Mock
    private AsistenciaService asistenciaService;

    @InjectMocks
    private AsistenciaController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AsistenciaResponse asistencia(MetodoRegistroAsistencia metodo) {
        return new AsistenciaResponse(900L, 501L, 201L, "Taller de Spring Boot", 10L, "María Pérez", "1090123456",
                CategoriaParticipante.estudiante, LocalDateTime.of(2026, 11, 20, 9, 3), metodo);
    }

    @Test
    @DisplayName("Criterio 2: POST con el código leído registra la asistencia (201)")
    void registraPorQr() throws Exception {
        when(asistenciaService.registrarPorQr(1L, 201L, CODIGO)).thenReturn(asistencia(MetodoRegistroAsistencia.qr));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + CODIGO + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.asistenciaId").value(900))
                .andExpect(jsonPath("$.participanteNombre").value("María Pérez"))
                .andExpect(jsonPath("$.metodoRegistro").value("qr"))
                .andExpect(jsonPath("$.fechaHoraRegistro").exists());
    }

    @Test
    @DisplayName("POST sin código responde 400 sin llamar al servicio")
    void codigoVacio() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
        verify(asistenciaService, never()).registrarPorQr(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Criterio 3: asistencia ya registrada → 409 ASISTENCIA_DUPLICADA con la asistencia previa")
    void duplicada() throws Exception {
        when(asistenciaService.registrarPorQr(eq(1L), eq(201L), anyString())).thenThrow(new AsistenciaRechazadaException(
                AsistenciaRechazadaException.ASISTENCIA_DUPLICADA, HttpStatus.CONFLICT,
                "La asistencia ya estaba registrada.", asistencia(MetodoRegistroAsistencia.qr)));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + CODIGO + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ASISTENCIA_DUPLICADA"))
                .andExpect(jsonPath("$.message").value("La asistencia ya estaba registrada."))
                .andExpect(jsonPath("$.asistenciaExistente.asistenciaId").value(900))
                .andExpect(jsonPath("$.path").value(URL));
    }

    @Test
    @DisplayName("Criterio 3: QR de otro evento → 409 QR_OTRO_EVENTO sin asistencia previa")
    void qrOtroEvento() throws Exception {
        when(asistenciaService.registrarPorQr(eq(1L), eq(201L), anyString())).thenThrow(new AsistenciaRechazadaException(
                AsistenciaRechazadaException.QR_OTRO_EVENTO, HttpStatus.CONFLICT, "Es de otro evento."));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + CODIGO + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("QR_OTRO_EVENTO"))
                .andExpect(jsonPath("$.asistenciaExistente").doesNotExist());
    }

    @Test
    @DisplayName("Criterio 3: QR desconocido → 404 QR_INVALIDO")
    void qrInvalido() throws Exception {
        when(asistenciaService.registrarPorQr(eq(1L), eq(201L), anyString())).thenThrow(new AsistenciaRechazadaException(
                AsistenciaRechazadaException.QR_INVALIDO, HttpStatus.NOT_FOUND, "No existe."));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"XYZ\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("QR_INVALIDO"));
    }

    @Test
    @DisplayName("POST /manual registra la asistencia de una inscripción (201)")
    void registraManual() throws Exception {
        when(asistenciaService.registrarManual(1L, 201L, 501L)).thenReturn(asistencia(MetodoRegistroAsistencia.manual));

        mockMvc.perform(post(URL + "/manual").contentType(MediaType.APPLICATION_JSON).content("{\"inscripcionId\":501}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metodoRegistro").value("manual"));
    }

    @Test
    @DisplayName("POST /manual sin inscripción responde 400")
    void manualSinInscripcion() throws Exception {
        mockMvc.perform(post(URL + "/manual").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(asistenciaService, never()).registrarManual(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("GET lista las asistencias con totales")
    void lista() throws Exception {
        when(asistenciaService.listarPorActividad(1L, 201L)).thenReturn(new AsistenciasActividadResponse(
                201L, "Taller de Spring Boot", LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), LocalTime.of(11, 0),
                30L, 1L, List.of(asistencia(MetodoRegistroAsistencia.qr))));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscritos").value(30))
                .andExpect(jsonPath("$.asistentes").value(1))
                .andExpect(jsonPath("$.asistencias[0].numeroDocumento").value("1090123456"));
    }
}
