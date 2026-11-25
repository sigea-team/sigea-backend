package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.certificado.CertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.ConfiguracionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.EmisionCertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.GeneracionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.HabilitacionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.MisCertificadosEventoResponse;
import com.sigea.demosigea_backend.dto.certificado.VerificacionCertificadoResponse;
import com.sigea.demosigea_backend.exception.CertificadoException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.TipoCertificado;
import com.sigea.demosigea_backend.service.CertificadoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link CertificadoController} y {@link CertificadoPublicoController} (HU-26).
 */
@ExtendWith(MockitoExtension.class)
class CertificadoControllerTest {

    private static final String CODIGO = "CERT-3F9A1C07B2D44E5A9C61B0D2";

    private MockMvc mockMvc;

    @Mock
    private CertificadoService certificadoService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CertificadoController(certificadoService),
                        new CertificadoPublicoController(certificadoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CertificadoResponse certificado() {
        return new CertificadoResponse(21L, 3L, "Semana de Ingeniería", LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12), TipoCertificado.asistente, "Asistente", 12L, "María Pérez", "CC",
                "1090123456", new BigDecimal("85.71"), "Asistió a 6 de las 7 actividades.",
                LocalDateTime.of(2026, 11, 24, 18, 30), CODIGO, "/api/v1/certificados/21/pdf");
    }

    @Test
    @DisplayName("Criterio 1: POST certificados/mios emite el certificado → 201")
    void solicitarEmite() throws Exception {
        when(certificadoService.solicitar(3L, TipoCertificado.asistente))
                .thenReturn(new EmisionCertificadoResponse(true, "Se generó tu certificado de asistente.", certificado()));

        mockMvc.perform(post("/api/v1/eventos/3/certificados/mios")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"asistente\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generadoAhora").value(true))
                .andExpect(jsonPath("$.certificado.tipo").value("asistente"))
                .andExpect(jsonPath("$.certificado.codigoVerificacion").value(CODIGO))
                .andExpect(jsonPath("$.certificado.urlDescarga").value("/api/v1/certificados/21/pdf"));
    }

    @Test
    @DisplayName("Criterio 3: si ya existía responde 200 con el mismo certificado")
    void solicitarExistente() throws Exception {
        when(certificadoService.solicitar(3L, TipoCertificado.asistente))
                .thenReturn(new EmisionCertificadoResponse(false, "Ya había sido generado.", certificado()));

        mockMvc.perform(post("/api/v1/eventos/3/certificados/mios")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"asistente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generadoAhora").value(false))
                .andExpect(jsonPath("$.certificado.certificadoId").value(21));
    }

    @Test
    @DisplayName("Criterio 2: sin los requisitos responde 409 CERTIFICADO_REQUISITOS_NO_CUMPLIDOS con el motivo")
    void solicitarSinRequisitos() throws Exception {
        when(certificadoService.solicitar(3L, TipoCertificado.asistente)).thenThrow(new CertificadoException(
                CertificadoException.REQUISITOS_NO_CUMPLIDOS, HttpStatus.CONFLICT, "No cumples los requisitos."));

        mockMvc.perform(post("/api/v1/eventos/3/certificados/mios")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"asistente\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(CertificadoException.REQUISITOS_NO_CUMPLIDOS))
                .andExpect(jsonPath("$.message").value("No cumples los requisitos."));
    }

    @Test
    @DisplayName("Sin tipo → 400 y no llama al servicio")
    void solicitarSinTipo() throws Exception {
        mockMvc.perform(post("/api/v1/eventos/3/certificados/mios")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(certificadoService, never()).solicitar(any(), any());
    }

    @Test
    @DisplayName("GET certificados/{id}/pdf descarga el PDF como adjunto y sin caché")
    void descargar() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes();
        when(certificadoService.descargar(21L))
                .thenReturn(new CertificadoService.ArchivoCertificado("certificado-asistente-" + CODIGO + ".pdf", pdf));

        mockMvc.perform(get("/api/v1/certificados/21/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"certificado-asistente-" + CODIGO + ".pdf\""));
    }

    @Test
    @DisplayName("GET certificados/mios lista los del participante")
    void listarMios() throws Exception {
        when(certificadoService.listarMios()).thenReturn(List.of(certificado()));

        mockMvc.perform(get("/api/v1/certificados/mios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventoNombre").value("Semana de Ingeniería"));
    }

    @Test
    @DisplayName("GET certificados/mios/eventos lista el estado de sus certificados por evento")
    void listarMisEventos() throws Exception {
        when(certificadoService.listarMisEventos()).thenReturn(List.of(new MisCertificadosEventoResponse(3L,
                "Semana de Ingeniería", LocalDate.of(2026, 11, 12), true, false, new BigDecimal("80.00"), true,
                List.of())));

        mockMvc.perform(get("/api/v1/certificados/mios/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventoId").value(3))
                .andExpect(jsonPath("$[0].certificadosHabilitados").value(true));
    }

    @Test
    @DisplayName("POST habilitar devuelve la fecha y los certificados generados; plantilla incompleta → 409")
    void habilitar() throws Exception {
        when(certificadoService.habilitar(3L)).thenReturn(new HabilitacionCertificadosResponse(3L,
                LocalDateTime.of(2026, 12, 4, 10, 15), List.of(certificado()), 0, List.of()));

        mockMvc.perform(post("/api/v1/eventos/3/certificados/habilitar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generados.length()").value(1))
                .andExpect(jsonPath("$.fechaHabilitacion").exists());

        when(certificadoService.habilitar(4L)).thenThrow(new CertificadoException(
                CertificadoException.PLANTILLA_INCOMPLETA, HttpStatus.CONFLICT, "Suba la imagen de fondo."));
        mockMvc.perform(post("/api/v1/eventos/4/certificados/habilitar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(CertificadoException.PLANTILLA_INCOMPLETA));
    }

    @Test
    @DisplayName("CU-23: POST generar devuelve emitidos, ya emitidos y excluidos")
    void generarDelEvento() throws Exception {
        when(certificadoService.generarDelEvento(3L))
                .thenReturn(new GeneracionCertificadosResponse(3L, List.of(certificado()), 2, List.of()));

        mockMvc.perform(post("/api/v1/eventos/3/certificados/generar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generados.length()").value(1))
                .andExpect(jsonPath("$.yaEmitidos").value(2));
    }

    @Test
    @DisplayName("PUT configuracion valida el porcentaje (mayor que 0 y hasta 100)")
    void configuracion() throws Exception {
        when(certificadoService.actualizarConfiguracion(eq(3L), any()))
                .thenReturn(new ConfiguracionCertificadosResponse(3L, new BigDecimal("75.00"), true, null, 0));

        mockMvc.perform(put("/api/v1/eventos/3/certificados/configuracion")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"porcentajeMinimoAsistencia\":75}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeMinimoAsistencia").value(75.00));

        mockMvc.perform(put("/api/v1/eventos/3/certificados/configuracion")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"porcentajeMinimoAsistencia\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/eventos/3/certificados/configuracion")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"porcentajeMinimoAsistencia\":100.5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Verificación pública por código; código inexistente → 404")
    void verificar() throws Exception {
        when(certificadoService.verificar(CODIGO)).thenReturn(new VerificacionCertificadoResponse(CODIGO,
                "María Pérez", "CC ******3456", TipoCertificado.asistente, "Asistente", "Semana de Ingeniería",
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12), LocalDateTime.of(2026, 11, 24, 18, 30)));
        when(certificadoService.verificar("CERT-X")).thenThrow(new RecursoNoEncontradoException("No existe."));

        mockMvc.perform(get("/api/v1/publico/certificados/" + CODIGO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documento").value("CC ******3456"));
        mockMvc.perform(get("/api/v1/publico/certificados/CERT-X"))
                .andExpect(status().isNotFound());
    }
}
