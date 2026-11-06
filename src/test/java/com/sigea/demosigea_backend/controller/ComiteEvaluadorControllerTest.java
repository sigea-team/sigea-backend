package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.comiteevaluador.EvaluadorRequest;
import com.sigea.demosigea_backend.dto.comiteevaluador.EvaluadorResponse;
import com.sigea.demosigea_backend.exception.EvaluadorDuplicadoException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.service.ComiteEvaluadorService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los endpoints del comité evaluador (HU-15).
 */
@ExtendWith(MockitoExtension.class)
class ComiteEvaluadorControllerTest {

    private static final String URL = "/api/v1/convocatorias/10/comite-evaluador";

    private MockMvc mockMvc;

    @Mock
    private ComiteEvaluadorService comiteService;

    @InjectMocks
    private ComiteEvaluadorController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static EvaluadorResponse ejemplo() {
        return new EvaluadorResponse(3L, 10L, 21L, "Ana", "Pérez", "ana.perez@ufps.edu.co", "1090123456", "UFPS",
                "Ingeniería de software", true, 0);
    }

    @Test
    @DisplayName("Criterio 3: POST registra al evaluador con su área y responde 201")
    void registrar_retorna201() throws Exception {
        when(comiteService.registrar(eq(10L), any(EvaluadorRequest.class))).thenReturn(ejemplo());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana.perez@ufps.edu.co\",\"areaExperticia\":\"Ingeniería de software\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comiteEvaluadorId").value(3))
                .andExpect(jsonPath("$.areaExperticia").value("Ingeniería de software"));
    }

    @Test
    @DisplayName("Criterio 3: sin área de experticia o con correo inválido responde 400")
    void registrar_datosInvalidos_retorna400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana.perez@ufps.edu.co\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.areaExperticia").exists());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-es-correo\",\"areaExperticia\":\"Redes\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.correo").exists());

        verify(comiteService, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("Criterio 4: correo ya registrado en el comité responde 409 EVALUADOR_DUPLICADO")
    void registrar_duplicado_retorna409() throws Exception {
        when(comiteService.registrar(eq(10L), any(EvaluadorRequest.class)))
                .thenThrow(new EvaluadorDuplicadoException("Ya existe un evaluador con el correo ana.perez@ufps.edu.co."));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana.perez@ufps.edu.co\",\"areaExperticia\":\"Redes\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVALUADOR_DUPLICADO"));
    }

    @Test
    @DisplayName("GET lista los evaluadores del comité")
    void listar_retorna200() throws Exception {
        when(comiteService.listar(10L)).thenReturn(List.of(ejemplo()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tieneCuenta").value(true));
    }

    @Test
    @DisplayName("DELETE retira al evaluador (204) o responde 409 si tiene propuestas asignadas")
    void retirar() throws Exception {
        mockMvc.perform(delete(URL + "/3")).andExpect(status().isNoContent());
        verify(comiteService).retirar(10L, 3L);

        doThrow(new OperacionNoPermitidaException("Tiene propuestas asignadas."))
                .when(comiteService).retirar(10L, 5L);
        mockMvc.perform(delete(URL + "/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
    }
}
