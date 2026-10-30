package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.propuesta.ActualizarPropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorizacionEdicionResponse;
import com.sigea.demosigea_backend.dto.propuesta.DocumentoPropuestaResponse;
import com.sigea.demosigea_backend.dto.propuesta.EnvioPropuestaResponse;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaResponse;
import com.sigea.demosigea_backend.dto.propuesta.VersionPropuestaResponse;
import com.sigea.demosigea_backend.exception.ArchivoInvalidoException;
import com.sigea.demosigea_backend.exception.ConvocatoriaCerradaException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.PropuestaBloqueadaException;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.service.VersionPropuestaService;
import com.sigea.demosigea_backend.service.VersionPropuestaService.ArchivoDescarga;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VersionPropuestaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VersionPropuestaService versionPropuestaService;

    @InjectMocks
    private VersionPropuestaController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static MockMultipartFile datos(String json) {
        return new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile pdf(String nombre) {
        return new MockMultipartFile("documentos", nombre, "application/pdf", "%PDF-1.7".getBytes());
    }

    private static VersionPropuestaResponse version(int numero) {
        DocumentoPropuestaResponse doc = new DocumentoPropuestaResponse(7L, "ponencia.pdf", "application/pdf",
                2048L, true, LocalDateTime.now(), "/api/v1/propuestas/50/documentos/7");
        return new VersionPropuestaResponse(100L + numero, 50L, numero, LocalDateTime.now(), 4L, "Carlos Gómez",
                "carlos@ufps.edu.co", "IA en la Educación", "Resumen", "IA", null, null, null, false, List.of(doc));
    }

    private static EnvioPropuestaResponse envio(int numero) {
        PropuestaResponse propuesta = new PropuestaResponse(50L, 1L, "Convocatoria 2026", "IA en la Educación",
                "Resumen", "IA", null, null, EstadoPropuesta.recibida, LocalDateTime.now(), List.of());
        return new EnvioPropuestaResponse("La propuesta fue recibida correctamente (versión " + numero + ").",
                propuesta, version(numero));
    }

    private static final String DATOS_VALIDOS = """
            {"convocatoriaId": 1, "titulo": "IA en la Educación", "resumen": "Resumen", "palabrasClave": "IA"}
            """;

    // ------------------------------------------------------------------
    // Criterio 1
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 1: POST /envio con datos y documentos responde 201 y confirma la recepción")
    void enviar_DatosYDocumentos_Retorna201() throws Exception {
        when(versionPropuestaService.enviarPropuesta(any(PropuestaRequest.class), anyList())).thenReturn(envio(1));

        mockMvc.perform(multipart("/api/v1/propuestas/envio")
                        .file(datos(DATOS_VALIDOS))
                        .file(pdf("ponencia.pdf"))
                        .file(pdf("anexo.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value(containsString("recibida correctamente")))
                .andExpect(jsonPath("$.propuesta.id").value(50))
                .andExpect(jsonPath("$.version.numeroVersion").value(1))
                .andExpect(jsonPath("$.version.documentos[0].nombreOriginal").value("ponencia.pdf"));
    }

    // ------------------------------------------------------------------
    // Criterio 2
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 2: sin título obligatorio responde 400 VALIDACION_FALLIDA y no llama al servicio")
    void enviar_SinTitulo_Retorna400() throws Exception {
        mockMvc.perform(multipart("/api/v1/propuestas/envio")
                        .file(datos("{\"convocatoriaId\": 1, \"titulo\": \"\"}"))
                        .file(pdf("ponencia.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.titulo").exists());

        verify(versionPropuestaService, never()).enviarPropuesta(any(), any());
    }

    @Test
    @DisplayName("Criterio 2: sin la parte 'datos' responde 400 indicando la parte faltante")
    void enviar_SinDatos_Retorna400() throws Exception {
        mockMvc.perform(multipart("/api/v1/propuestas/envio").file(pdf("ponencia.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("datos")));
    }

    @Test
    @DisplayName("Criterio 2: archivo con formato no permitido responde 400 ARCHIVO_INVALIDO con el detalle")
    void enviar_ArchivoInvalido_Retorna400() throws Exception {
        when(versionPropuestaService.enviarPropuesta(any(PropuestaRequest.class), anyList()))
                .thenThrow(new ArchivoInvalidoException("Uno o varios documentos no cumplen las condiciones de carga.",
                        Map.of("documentos[0]", "foto.png: formato no permitido.")));

        mockMvc.perform(multipart("/api/v1/propuestas/envio")
                        .file(datos(DATOS_VALIDOS))
                        .file(new MockMultipartFile("documentos", "foto.png", "image/png", new byte[]{1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_INVALIDO"))
                .andExpect(jsonPath("$.erroresValidacion['documentos[0]']").value(containsString("formato no permitido")));
    }

    @Test
    @DisplayName("Criterio 2: con la convocatoria cerrada responde 400 CONVOCATORIA_CERRADA")
    void enviar_ConvocatoriaCerrada_Retorna400() throws Exception {
        when(versionPropuestaService.enviarPropuesta(any(PropuestaRequest.class), anyList()))
                .thenThrow(new ConvocatoriaCerradaException("La fecha de cierre de la convocatoria ya se cumplió."));

        mockMvc.perform(multipart("/api/v1/propuestas/envio")
                        .file(datos(DATOS_VALIDOS))
                        .file(pdf("ponencia.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONVOCATORIA_CERRADA"));
    }

    // ------------------------------------------------------------------
    // Criterio 3
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 3: POST /{id}/versiones con un documento nuevo responde 201 con la versión 2")
    void crearVersion_ReemplazaDocumento_Retorna201() throws Exception {
        when(versionPropuestaService.crearNuevaVersion(eq(50L), any(ActualizarPropuestaRequest.class), anyList()))
                .thenReturn(envio(2));

        mockMvc.perform(multipart("/api/v1/propuestas/50/versiones")
                        .file(datos("{\"notaCambio\": \"Documento corregido\", \"documentosConservados\": []}"))
                        .file(pdf("ponencia-v2.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version.numeroVersion").value(2));
    }

    @Test
    @DisplayName("Criterio 3: se puede crear una versión enviando solo documentos, sin la parte 'datos'")
    void crearVersion_SoloDocumentos_Retorna201() throws Exception {
        when(versionPropuestaService.crearNuevaVersion(eq(50L), isNull(), anyList())).thenReturn(envio(2));

        mockMvc.perform(multipart("/api/v1/propuestas/50/versiones").file(pdf("ponencia-v2.pdf")))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------
    // Criterio 4
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 4: GET /{id}/versiones devuelve el historial con fecha y autor del cambio")
    void listarVersiones_Retorna200() throws Exception {
        when(versionPropuestaService.listarVersiones(50L)).thenReturn(List.of(version(1), version(2)));

        mockMvc.perform(get("/api/v1/propuestas/50/versiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].numeroVersion").value(2))
                .andExpect(jsonPath("$[1].autorCambioNombre").value("Carlos Gómez"))
                .andExpect(jsonPath("$[1].fechaCreacion").exists());
    }

    @Test
    @DisplayName("Criterio 4: GET /{id}/documentos/{documentoId} descarga el archivo con su nombre original")
    void descargarDocumento_Retorna200() throws Exception {
        when(versionPropuestaService.descargarDocumento(50L, 7L)).thenReturn(new ArchivoDescarga(
                new ByteArrayResource("%PDF-1.7".getBytes()), "ponencia.pdf", "application/pdf", 8L));

        mockMvc.perform(get("/api/v1/propuestas/50/documentos/7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("ponencia.pdf")));
    }

    // ------------------------------------------------------------------
    // Criterio 5
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 5: modificar una propuesta con versión definitiva responde 409 PROPUESTA_BLOQUEADA")
    void crearVersion_Definitiva_Retorna409() throws Exception {
        when(versionPropuestaService.crearNuevaVersion(eq(50L), any(), anyList()))
                .thenThrow(new PropuestaBloqueadaException("La propuesta ya fue aprobada en versión definitiva."));

        mockMvc.perform(multipart("/api/v1/propuestas/50/versiones").file(pdf("nueva.pdf")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PROPUESTA_BLOQUEADA"));
    }

    @Test
    @DisplayName("Criterio 5: PATCH /{id}/autorizar-edicion concede la autorización explícita")
    void autorizarEdicion_Retorna200() throws Exception {
        when(versionPropuestaService.autorizarEdicion(50L, "Corrección de forma"))
                .thenReturn(new AutorizacionEdicionResponse(50L, true, "Corrección de forma", "Autorizada"));

        mockMvc.perform(patch("/api/v1/propuestas/50/autorizar-edicion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Corrección de forma\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edicionAutorizada").value(true));
    }

    @Test
    @DisplayName("Criterio 5: autorizar sin motivo responde 400")
    void autorizarEdicion_SinMotivo_Retorna400() throws Exception {
        mockMvc.perform(patch("/api/v1/propuestas/50/autorizar-edicion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"\"}"))
                .andExpect(status().isBadRequest());

        verify(versionPropuestaService, never()).autorizarEdicion(any(), any());
    }
}
