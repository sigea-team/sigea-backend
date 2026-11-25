package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.certificado.FirmanteCertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.PlantillaCertificadoResponse;
import com.sigea.demosigea_backend.exception.ArchivoInvalidoException;
import com.sigea.demosigea_backend.exception.CertificadoException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.model.TipoCertificado;
import com.sigea.demosigea_backend.service.PlantillaCertificadoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link PlantillaCertificadoController} (HU-26: fondo y firmantes).
 */
@ExtendWith(MockitoExtension.class)
class PlantillaCertificadoControllerTest {

    private static final String URL = "/api/v1/eventos/3/certificados/plantilla";

    private MockMvc mockMvc;

    @Mock
    private PlantillaCertificadoService plantillaService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PlantillaCertificadoController(plantillaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static PlantillaCertificadoResponse plantilla() {
        return new PlantillaCertificadoResponse(3L, true, URL + "/fondo",
                List.of(new FirmanteCertificadoResponse(4L, "Judith Rodríguez", "Directora", 1,
                        URL + "/firmantes/4/firma")), 4, true, null, URL + "/vista-previa");
    }

    @Test
    @DisplayName("PUT fondo (multipart) sube la imagen")
    void subirFondo() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        when(plantillaService.subirFondo(3L, png)).thenReturn(plantilla());

        mockMvc.perform(multipart(URL + "/fondo").file(new MockMultipartFile("archivo", "fondo.png", "image/png", png))
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneFondo").value(true))
                .andExpect(jsonPath("$.firmantes[0].nombre").value("Judith Rodríguez"));
    }

    @Test
    @DisplayName("POST firmantes con nombre, cargo y firma; imagen inválida → 400 ARCHIVO_INVALIDO")
    void agregarFirmante() throws Exception {
        byte[] firma = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        when(plantillaService.agregarFirmante(3L, "Judith Rodríguez", "Directora", firma)).thenReturn(plantilla());

        mockMvc.perform(multipart(URL + "/firmantes")
                        .file(new MockMultipartFile("firma", "firma.jpg", "image/jpeg", firma))
                        .param("nombre", "Judith Rodríguez").param("cargo", "Directora"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firmantes[0].orden").value(1));

        when(plantillaService.agregarFirmante(eq(3L), eq("X"), eq("Y"), any()))
                .thenThrow(new ArchivoInvalidoException("Solo se aceptan imágenes PNG o JPG.", Map.of("firma", "PNG o JPG")));
        mockMvc.perform(multipart(URL + "/firmantes")
                        .file(new MockMultipartFile("firma", "firma.pdf", "application/pdf", new byte[]{'%'}))
                        .param("nombre", "X").param("cargo", "Y"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_INVALIDO"));
    }

    @Test
    @DisplayName("Con certificados emitidos → 409 PLANTILLA_CERTIFICADO_BLOQUEADA")
    void bloqueada() throws Exception {
        when(plantillaService.reordenarFirmantes(3L, List.of(5L, 4L))).thenThrow(new CertificadoException(
                CertificadoException.PLANTILLA_BLOQUEADA, HttpStatus.CONFLICT, "Ya se emitieron certificados."));

        mockMvc.perform(put(URL + "/firmantes/orden").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firmanteIds\":[5,4]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(CertificadoException.PLANTILLA_BLOQUEADA));
    }

    @Test
    @DisplayName("PUT firmante sin imagen conserva la firma")
    void actualizarSinImagen() throws Exception {
        when(plantillaService.actualizarFirmante(eq(3L), eq(4L), eq("Judith"), eq("Directora"), isNull()))
                .thenReturn(plantilla());

        mockMvc.perform(multipart(URL + "/firmantes/4").param("nombre", "Judith").param("cargo", "Directora")
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        }))
                .andExpect(status().isOk());
        verify(plantillaService).actualizarFirmante(eq(3L), eq(4L), eq("Judith"), eq("Directora"), isNull());
    }

    @Test
    @DisplayName("GET vista-previa devuelve un PDF")
    void vistaPrevia() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes();
        when(plantillaService.vistaPrevia(3L, TipoCertificado.ponente)).thenReturn(pdf);

        mockMvc.perform(get(URL + "/vista-previa").param("tipo", "ponente"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));
    }
}
