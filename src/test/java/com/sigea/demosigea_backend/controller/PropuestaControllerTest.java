package com.sigea.demosigea_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigea.demosigea_backend.dto.propuesta.ActualizarAutorRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorResponse;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaResponse;
import com.sigea.demosigea_backend.exception.ConvocatoriaCerradaException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RecursoDuplicadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.RolAutor;
import com.sigea.demosigea_backend.service.PropuestaService;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PropuestaControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PropuestaService propuestaService;

    @InjectMocks
    private PropuestaController propuestaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(propuestaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AutorResponse autorRespuesta(Long personaId, String nombres, RolAutor rol, int orden) {
        return new AutorResponse(
                personaId, nombres, "Pérez", nombres + " Pérez",
                nombres.toLowerCase() + "@ufps.edu.co", "UFPS", 1L,
                rol, rol.getDescripcion(), orden, "CC", "1090" + personaId, "+573001234567"
        );
    }

    private PropuestaResponse propuestaRespuesta(Long id, String titulo, List<AutorResponse> autores) {
        return new PropuestaResponse(
                id, 1L, "Convocatoria 2026", titulo, "Resumen...", "Palabras",
                2L, "Línea 1", EstadoPropuesta.recibida, LocalDateTime.now(), autores
        );
    }

    // ------------------------------------------------------------------
    // Criterio 1: Asociar autor/ponente al enviar una propuesta
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 1: POST /api/v1/propuestas registra propuesta y asocia datos del autor/ponente (201 Created)")
    void crearPropuesta_ConAutor_Retorna201YAsociaAutor() throws Exception {
        AutorResponse autorResp = autorRespuesta(10L, "Carlos", RolAutor.autor_principal, 1);
        PropuestaResponse propuestaResp = propuestaRespuesta(50L, "IA en la Educación", List.of(autorResp));

        when(propuestaService.crearPropuesta(any(PropuestaRequest.class))).thenReturn(propuestaResp);

        String json = """
                {
                  "convocatoriaId": 1,
                  "titulo": "IA en la Educación",
                  "resumen": "Resumen del trabajo",
                  "palabrasClave": "IA, Educación",
                  "lineaTematicaId": 2,
                  "autores": [
                    {
                      "nombre": "Carlos",
                      "apellidos": "Pérez",
                      "correo": "carlos@ufps.edu.co",
                      "filiacion": "UFPS",
                      "rol": "autor_principal",
                      "orden": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/propuestas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.titulo").value("IA en la Educación"))
                .andExpect(jsonPath("$.autores[0].personaId").value(10))
                .andExpect(jsonPath("$.autores[0].nombres").value("Carlos"))
                .andExpect(jsonPath("$.autores[0].rol").value("autor_principal"))
                .andExpect(jsonPath("$.autores[0].orden").value(1));
    }

    @Test
    @DisplayName("Criterio 1: POST /api/v1/propuestas/{id}/autores asocia directamente un autor a una propuesta existente (201 Created)")
    void agregarAutor_APropuestaExistente_Retorna201YAsocia() throws Exception {
        AutorResponse autorResp = autorRespuesta(15L, "Diana", RolAutor.ponente, 2);
        when(propuestaService.agregarAutor(eq(50L), any(AutorRequest.class))).thenReturn(autorResp);

        String json = """
                {
                  "nombre": "Diana",
                  "apellidos": "Pérez",
                  "correo": "diana@ufps.edu.co",
                  "filiacion": "UFPS",
                  "rol": "ponente"
                }
                """;

        mockMvc.perform(post("/api/v1/propuestas/50/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personaId").value(15))
                .andExpect(jsonPath("$.nombres").value("Diana"))
                .andExpect(jsonPath("$.rol").value("ponente"))
                .andExpect(jsonPath("$.orden").value(2));
    }

    // ------------------------------------------------------------------
    // Criterio 2: Múltiples autores con roles diferenciados
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 2: POST /api/v1/propuestas guarda diferenciadamente autores con roles (autor principal, coautor, ponente)")
    void crearPropuesta_VariosAutoresRolesDiferenciados_Retorna201ConRolesDiferenciados() throws Exception {
        AutorResponse a1 = autorRespuesta(1L, "Andrés", RolAutor.autor_principal, 1);
        AutorResponse a2 = autorRespuesta(2L, "Beatriz", RolAutor.coautor, 2);
        AutorResponse a3 = autorRespuesta(3L, "Camilo", RolAutor.ponente, 3);
        PropuestaResponse propuestaResp = propuestaRespuesta(60L, "Proyecto Múltiples Autores", List.of(a1, a2, a3));

        when(propuestaService.crearPropuesta(any(PropuestaRequest.class))).thenReturn(propuestaResp);

        String json = """
                {
                  "convocatoriaId": 1,
                  "titulo": "Proyecto Múltiples Autores",
                  "autores": [
                    {
                      "nombre": "Andrés",
                      "correo": "andres@ufps.edu.co",
                      "filiacion": "UFPS",
                      "rol": "autor_principal",
                      "orden": 1
                    },
                    {
                      "nombre": "Beatriz",
                      "correo": "beatriz@ufps.edu.co",
                      "filiacion": "UFPS",
                      "rol": "coautor",
                      "orden": 2
                    },
                    {
                      "nombre": "Camilo",
                      "correo": "camilo@ufps.edu.co",
                      "filiacion": "UFPS",
                      "rol": "ponente",
                      "orden": 3
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/propuestas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autores.length()").value(3))
                .andExpect(jsonPath("$.autores[0].rol").value("autor_principal"))
                .andExpect(jsonPath("$.autores[1].rol").value("coautor"))
                .andExpect(jsonPath("$.autores[2].rol").value("ponente"));
    }

    @Test
    @DisplayName("Criterio 2: PUT /api/v1/propuestas/{id}/autores/{personaId} actualiza rol u orden de autor")
    void actualizarAutor_ModificaRol_Retorna200() throws Exception {
        AutorResponse actualizado = autorRespuesta(5L, "Laura", RolAutor.ponente, 1);
        when(propuestaService.actualizarAutor(eq(10L), eq(5L), any(ActualizarAutorRequest.class)))
                .thenReturn(actualizado);

        String json = """
                {
                  "rol": "ponente",
                  "orden": 1
                }
                """;

        mockMvc.perform(put("/api/v1/propuestas/10/autores/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ponente"))
                .andExpect(jsonPath("$.orden").value(1));
    }

    // ------------------------------------------------------------------
    // Criterio 3: Rechazar registro sin datos obligatorios (nombre, correo, filiación)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 3: POST /api/v1/propuestas/{id}/autores sin nombre, correo ni filiación retorna 400 y señala campos faltantes")
    void agregarAutor_SinCamposObligatorios_Retorna400YDetallaCamposFaltantes() throws Exception {
        String json = """
                {
                  "nombre": "",
                  "correo": "",
                  "filiacion": "",
                  "rol": "autor_principal"
                }
                """;

        mockMvc.perform(post("/api/v1/propuestas/10/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.nombre", notNullValue()))
                .andExpect(jsonPath("$.erroresValidacion.correo", notNullValue()))
                .andExpect(jsonPath("$.erroresValidacion.filiacion", notNullValue()));
    }

    @Test
    @DisplayName("Criterio 3: POST /api/v1/propuestas/{id}/autores con correo ausente señala campo correo")
    void agregarAutor_SinCorreo_Retorna400YSenalaCorreo() throws Exception {
        String json = """
                {
                  "nombre": "Juan Pérez",
                  "filiacion": "UFPS",
                  "rol": "coautor"
                }
                """;

        mockMvc.perform(post("/api/v1/propuestas/10/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.correo").value("El correo es obligatorio"));
    }

    @Test
    @DisplayName("Criterio 3: POST /api/v1/propuestas/{id}/autores con filiación ausente señala campo filiacion")
    void agregarAutor_SinFiliacion_Retorna400YSenalaFiliacion() throws Exception {
        String json = """
                {
                  "nombre": "Juan Pérez",
                  "correo": "juan@ufps.edu.co",
                  "rol": "coautor"
                }
                """;

        mockMvc.perform(post("/api/v1/propuestas/10/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.filiacion").value("La filiación es obligatoria"));
    }

    @Test
    @DisplayName("Criterio 3: POST /api/v1/propuestas/{id}/autores con formato de correo inválido señala error de formato")
    void agregarAutor_CorreoInvalido_Retorna400() throws Exception {
        String json = """
                {
                  "nombre": "Juan Pérez",
                  "correo": "correo-no-valido",
                  "filiacion": "UFPS",
                  "rol": "coautor"
                }
                """;

        mockMvc.perform(post("/api/v1/propuestas/10/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.correo", notNullValue()));
    }

    @Test
    @DisplayName("Criterio 3: POST /api/v1/propuestas con autor en lista sin datos obligatorios señala los campos faltantes")
    void crearPropuesta_ConAutorSinDatosObligatorios_Retorna400YSenalaCamposFaltantes() throws Exception {
        String json = """
                {
                  "convocatoriaId": 1,
                  "titulo": "Propuesta con datos incompletos",
                  "autores": [
                    {
                      "nombre": "",
                      "correo": "",
                      "filiacion": "",
                      "rol": "autor_principal"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/propuestas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.nombre", notNullValue()))
                .andExpect(jsonPath("$.erroresValidacion.correo", notNullValue()))
                .andExpect(jsonPath("$.erroresValidacion.filiacion", notNullValue()));
    }

    // ------------------------------------------------------------------
    // Casos de Reglas de Negocio (400, 404, 409)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/propuestas sobre convocatoria cerrada retorna 400 CONVOCATORIA_CERRADA")
    void crearPropuesta_ConvocatoriaCerrada_Retorna400() throws Exception {
        when(propuestaService.crearPropuesta(any(PropuestaRequest.class)))
                .thenThrow(new ConvocatoriaCerradaException("La fecha de cierre ya se cumplió."));

        String json = """
                {
                  "convocatoriaId": 1,
                  "titulo": "Propuesta Tardía",
                  "autores": []
                }
                """;

        mockMvc.perform(post("/api/v1/propuestas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CONVOCATORIA_CERRADA"));
    }

    @Test
    @DisplayName("POST /api/v1/propuestas/{id}/autores con persona ya registrada retorna 409 CUENTA_YA_EXISTE")
    void agregarAutor_PersonaDuplicada_Retorna409() throws Exception {
        when(propuestaService.agregarAutor(eq(10L), any(AutorRequest.class)))
                .thenThrow(new RecursoDuplicadoException("La persona ya se encuentra registrada en la propuesta."));

        String json = """
                {
                  "nombre": "Ana",
                  "correo": "ana@ufps.edu.co",
                  "filiacion": "UFPS",
                  "rol": "coautor"
                }
                """;

        mockMvc.perform(post("/api/v1/propuestas/10/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("ya se encuentra registrada")));
    }

    @Test
    @DisplayName("GET /api/v1/propuestas/{id} inexistente retorna 404 NOT_FOUND")
    void obtenerPropuesta_Inexistente_Retorna404() throws Exception {
        when(propuestaService.obtenerPropuesta(999L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la propuesta"));

        mockMvc.perform(get("/api/v1/propuestas/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("GET /api/v1/propuestas/{id}/autores retorna lista de autores (200 OK)")
    void listarAutores_PropuestaExistente_Retorna200() throws Exception {
        AutorResponse a1 = autorRespuesta(1L, "Carlos", RolAutor.autor_principal, 1);
        when(propuestaService.listarAutores(10L)).thenReturn(List.of(a1));

        mockMvc.perform(get("/api/v1/propuestas/10/autores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personaId").value(1))
                .andExpect(jsonPath("$[0].nombres").value("Carlos"));
    }

    @Test
    @DisplayName("DELETE /api/v1/propuestas/{id}/autores/{personaId} desvincula autor (204 No Content)")
    void eliminarAutor_Existente_Retorna204() throws Exception {
        mockMvc.perform(delete("/api/v1/propuestas/10/autores/5"))
                .andExpect(status().isNoContent());

        verify(propuestaService).eliminarAutor(10L, 5L);
    }
}
