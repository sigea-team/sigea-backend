package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.conferencista.CoincidenciaConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.CompletarInvitacionRequest;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionCompletadaResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionPublicaResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitarConferencistaRequest;
import com.sigea.demosigea_backend.exception.ConferencistaExistenteException;
import com.sigea.demosigea_backend.exception.DatosConferencistaInvalidosException;
import com.sigea.demosigea_backend.exception.DatosPersonaDuplicadosException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.InvitacionNoDisponibleException;
import com.sigea.demosigea_backend.exception.InvitacionPendienteException;
import com.sigea.demosigea_backend.service.InvitacionConferencistaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
class InvitacionConferencistaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InvitacionConferencistaService invitacionService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new InvitacionConferencistaController(invitacionService),
                        new InvitacionConferencistaPublicaController(invitacionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String BASE = "/api/v1/eventos/1/conferencistas/invitaciones";
    private static final String PUBLICA = "/api/v1/publico/invitaciones-conferencista/abc";

    private static final String TRAYECTORIA = "{\"biografia\":\"Ingeniera de sistemas con 15 años de experiencia.\","
            + "\"estudios\":[{\"nivel\":\"doctorado\",\"titulo\":\"Doctora en Ingeniería\",\"institucion\":\"UNAL\",\"anio\":2018}],"
            + "\"publicaciones\":[]}";

    private static String completarJson(String trayectoria) {
        return "{\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"1090123456\",\"nombres\":\"Ana María\","
                + "\"apellidos\":\"Pérez Gómez\",\"telefono\":\"3001234567\",\"tema\":\"IA\",\"trayectoria\":" + trayectoria + "}";
    }

    private InvitacionResponse invitacion() {
        return new InvitacionResponse(4L, 1L, "Ana María Pérez", "ana.perez@unal.edu.co", "pendiente",
                LocalDateTime.of(2026, 10, 19, 9, 0), LocalDateTime.of(2026, 10, 22, 9, 0),
                "http://localhost:5173/#/conferencista/invitacion?token=abc");
    }

    // ---------------- Administrador ----------------

    @Test
    @DisplayName("GET lista las invitaciones pendientes con su enlace")
    void listar_retorna200() throws Exception {
        when(invitacionService.listarPendientes(1L)).thenReturn(List.of(invitacion()));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("ana.perez@unal.edu.co"))
                .andExpect(jsonPath("$[0].estado").value("pendiente"))
                .andExpect(jsonPath("$[0].enlace").exists());
    }

    @Test
    @DisplayName("POST invitar con nombre y correo → 201 con Location")
    void invitar_retorna201() throws Exception {
        when(invitacionService.invitar(eq(1L), any(InvitarConferencistaRequest.class))).thenReturn(invitacion());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana María Pérez\",\"correo\":\"ana.perez@unal.edu.co\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(header().string("Location", "http://localhost" + BASE + "/4"));
    }

    @Test
    @DisplayName("Criterio 2: invitar sin nombre o con correo inválido → 400 sin llamar al servicio")
    void invitar_datosInvalidos_retorna400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \",\"correo\":\"ana@\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.nombre").exists())
                .andExpect(jsonPath("$.erroresValidacion.correo").exists());
        verify(invitacionService, never()).invitar(anyLong(), any());
    }

    @Test
    @DisplayName("Criterio 3: invitar un correo de conferencista registrado → 409 con la persona para vincularla")
    void invitar_registrado_retorna409() throws Exception {
        when(invitacionService.invitar(eq(1L), any(InvitarConferencistaRequest.class)))
                .thenThrow(new ConferencistaExistenteException("Ana ya está registrado...",
                        new CoincidenciaConferencistaResponse(12L, "Ana María Pérez Gómez", "CC", "1090123456",
                                "ana.perez@unal.edu.co", "correo", true, false, 2L)));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"ana.perez@unal.edu.co\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFERENCISTA_EXISTENTE"))
                .andExpect(jsonPath("$.coincidencia.personaId").value(12));
    }

    @Test
    @DisplayName("Invitar con una invitación vigente → 409 INVITACION_PENDIENTE")
    void invitar_pendiente_retorna409() throws Exception {
        when(invitacionService.invitar(eq(1L), any(InvitarConferencistaRequest.class)))
                .thenThrow(new InvitacionPendienteException("Ya se envió una invitación..."));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"ana.perez@unal.edu.co\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INVITACION_PENDIENTE"));
    }

    @Test
    @DisplayName("Reenviar → 200 y cancelar → 204")
    void reenviarYCancelar() throws Exception {
        when(invitacionService.reenviar(1L, 4L)).thenReturn(invitacion());

        mockMvc.perform(post(BASE + "/4/reenviar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("pendiente"));
        mockMvc.perform(delete(BASE + "/4"))
                .andExpect(status().isNoContent());
        verify(invitacionService).cancelar(1L, 4L);
    }

    // ---------------- Enlace del conferencista (público) ----------------

    @Test
    @DisplayName("GET público: datos de la invitación vigente")
    void consultar_retorna200() throws Exception {
        when(invitacionService.consultar("abc")).thenReturn(new InvitacionPublicaResponse(1L, "Congreso 2026", "2026-2",
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22), "Ana María Pérez", "ana.perez@unal.edu.co",
                LocalDateTime.of(2026, 10, 22, 9, 0), null));

        mockMvc.perform(get(PUBLICA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoNombre").value("Congreso 2026"))
                .andExpect(jsonPath("$.correo").value("ana.perez@unal.edu.co"));
    }

    @Test
    @DisplayName("GET público: enlace vencido → 410 INVITACION_VENCIDA")
    void consultar_vencida_retorna410() throws Exception {
        when(invitacionService.consultar("abc")).thenThrow(new InvitacionNoDisponibleException(
                InvitacionNoDisponibleException.VENCIDA, "Este enlace venció."));

        mockMvc.perform(get(PUBLICA))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.codigo").value("INVITACION_VENCIDA"));
    }

    @Test
    @DisplayName("Criterio 1: POST público registra los datos del conferencista → 200")
    void completar_retorna200() throws Exception {
        when(invitacionService.completar(eq("abc"), any(CompletarInvitacionRequest.class)))
                .thenReturn(new InvitacionCompletadaResponse(1L, "Congreso 2026", "Ana María Pérez Gómez", true));

        mockMvc.perform(post(PUBLICA).contentType(MediaType.APPLICATION_JSON).content(completarJson(TRAYECTORIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Ana María Pérez Gómez"))
                .andExpect(jsonPath("$.vistaPublicaDisponible").value(true));
    }

    @Test
    @DisplayName("Criterio 2: sin nombres ni biografía → 400 VALIDACION_FALLIDA y no se registra nada")
    void completar_datosIncompletos_retorna400() throws Exception {
        String json = "{\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"1090123456\",\"nombres\":\"\",\"apellidos\":\"Pérez\","
                + "\"trayectoria\":{\"biografia\":\"  \"}}";

        mockMvc.perform(post(PUBLICA).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.nombres").exists())
                .andExpect(jsonPath("$.erroresValidacion.biografia").exists());
        verify(invitacionService, never()).completar(anyString(), any());
    }

    @Test
    @DisplayName("Criterio 2: sin trayectoria → 400")
    void completar_sinTrayectoria_retorna400() throws Exception {
        String json = "{\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"1090123456\",\"nombres\":\"Ana\",\"apellidos\":\"Pérez\"}";

        mockMvc.perform(post(PUBLICA).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.trayectoria").exists());
    }

    @Test
    @DisplayName("Criterio 2: estudio incompleto o enlace sin http(s) → 400 con el campo anidado")
    void completar_estudioIncompleto_retorna400() throws Exception {
        String trayectoria = "{\"biografia\":\"Bio\",\"enlacePerfil\":\"orcid.org/123\","
                + "\"estudios\":[{\"nivel\":\"maestria\",\"titulo\":\"\",\"institucion\":\"UFPS\"}]}";

        mockMvc.perform(post(PUBLICA).contentType(MediaType.APPLICATION_JSON).content(completarJson(trayectoria)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion['trayectoria.estudios[0].titulo']").exists())
                .andExpect(jsonPath("$.erroresValidacion['trayectoria.enlacePerfil']").exists());
    }

    @Test
    @DisplayName("Criterio 2: reglas del servicio (año futuro) → 400 con el campo")
    void completar_anioFuturo_retorna400() throws Exception {
        when(invitacionService.completar(eq("abc"), any(CompletarInvitacionRequest.class)))
                .thenThrow(new DatosConferencistaInvalidosException("Datos inválidos",
                        Map.of("trayectoria.estudios[0].anio", "El año del estudio no puede ser posterior a 2026")));

        mockMvc.perform(post(PUBLICA).contentType(MediaType.APPLICATION_JSON).content(completarJson(TRAYECTORIA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion['trayectoria.estudios[0].anio']").exists());
    }

    @Test
    @DisplayName("Criterio 3: documento registrado con otro correo → 409 DATOS_PERSONA_DUPLICADOS")
    void completar_documentoDuplicado_retorna409() throws Exception {
        when(invitacionService.completar(eq("abc"), any(CompletarInvitacionRequest.class)))
                .thenThrow(new DatosPersonaDuplicadosException("numeroDocumento", "Este número de documento ya está registrado."));

        mockMvc.perform(post(PUBLICA).contentType(MediaType.APPLICATION_JSON).content(completarJson(TRAYECTORIA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DATOS_PERSONA_DUPLICADOS"))
                .andExpect(jsonPath("$.erroresValidacion.numeroDocumento").exists());
    }
}
