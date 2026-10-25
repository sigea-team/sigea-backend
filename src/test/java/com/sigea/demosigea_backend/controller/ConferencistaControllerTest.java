package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.conferencista.ActualizarConferencistaRequest;
import com.sigea.demosigea_backend.dto.conferencista.AsociarConferencistaRequest;
import com.sigea.demosigea_backend.dto.conferencista.CoincidenciaConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistaEventoResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistaRegistradoResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistasPublicosEventoResponse;
import com.sigea.demosigea_backend.dto.conferencista.PerfilConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.TrayectoriaResponse;
import com.sigea.demosigea_backend.exception.ConferencistaYaAsociadoException;
import com.sigea.demosigea_backend.exception.DatosPersonaDuplicadosException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.service.ConferencistaService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
class ConferencistaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConferencistaService conferencistaService;

    @InjectMocks
    private ConferencistaController conferencistaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(conferencistaController, new ConferencistaPublicoController(conferencistaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String TRAYECTORIA = "{\"biografia\":\"Ingeniera de sistemas con 15 años de experiencia.\","
            + "\"cargoActual\":\"Profesora titular\",\"institucion\":\"UFPS\","
            + "\"estudios\":[{\"nivel\":\"doctorado\",\"titulo\":\"Doctora en Ingeniería\",\"institucion\":\"UNAL\",\"anio\":2018}],"
            + "\"publicaciones\":[{\"tipo\":\"articulo\",\"titulo\":\"Microservicios\",\"anio\":2024,\"enlace\":\"https://doi.org/10.1/x\"}]}";

    private static String actualizarJson() {
        return "{\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"1090123456\",\"nombres\":\"Ana María\","
                + "\"apellidos\":\"Pérez Gómez\",\"correo\":\"ana.perez@ufps.edu.co\",\"telefono\":\"3001234567\","
                + "\"trayectoria\":" + TRAYECTORIA + "}";
    }

    private ConferencistaEventoResponse respuesta() {
        return new ConferencistaEventoResponse(7L, 1L, 12L, "Ana María Pérez Gómez", "CC", "1090123456",
                "ana.perez@ufps.edu.co", "3001234567", TipoParticipacionConferencista.conferencista,
                "IA en la educación", LocalDateTime.of(2026, 10, 1, 9, 0), true, "Profesora titular", "UFPS",
                LocalDateTime.of(2026, 10, 1, 9, 0));
    }

    private CoincidenciaConferencistaResponse coincidencia() {
        return new CoincidenciaConferencistaResponse(12L, "Ana María Pérez Gómez", "CC", "1090123456",
                "ana.perez@ufps.edu.co", "documento", true, false, 2L);
    }

    // ---------------- Criterio 1 ----------------

    @Test
    @DisplayName("Criterio 1: GET lista los conferencistas del evento")
    void listar_retorna200() throws Exception {
        when(conferencistaService.listarPorEvento(1L)).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/v1/eventos/1/conferencistas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participacionId").value(7))
                .andExpect(jsonPath("$[0].tipoParticipacion").value("conferencista"))
                .andExpect(jsonPath("$[0].tienePerfil").value(true));
    }

    // ---------------- Criterio 3 ----------------

    @Test
    @DisplayName("Criterio 3: GET /conferencistas lista los registrados para vincularlos")
    void listarRegistrados_retorna200() throws Exception {
        when(conferencistaService.listarRegistrados("pérez", 1L)).thenReturn(List.of(new ConferencistaRegistradoResponse(
                12L, "Ana María Pérez Gómez", "CC", "1090123456", "ana.perez@ufps.edu.co", "Profesora titular", "UFPS",
                2L, false, LocalDateTime.of(2026, 9, 15, 10, 0))));

        mockMvc.perform(get("/api/v1/conferencistas").param("busqueda", "pérez").param("eventoId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personaId").value(12))
                .andExpect(jsonPath("$[0].eventosPrevios").value(2))
                .andExpect(jsonPath("$[0].yaAsociadoAlEvento").value(false));
    }

    @Test
    @DisplayName("Criterio 3: asociar persona existente → 201")
    void asociar_retorna201() throws Exception {
        when(conferencistaService.asociar(eq(1L), any(AsociarConferencistaRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/eventos/1/conferencistas/asociar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":12,\"tipoParticipacion\":\"conferencista\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participacionId").value(7));
    }

    @Test
    @DisplayName("Criterio 3: asociar sin persona → 400")
    void asociar_sinPersona_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/eventos/1/conferencistas/asociar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoParticipacion\":\"conferencista\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresValidacion.personaId").exists());
    }

    @Test
    @DisplayName("Criterio 3: ya asociado → 409 CONFERENCISTA_YA_ASOCIADO")
    void asociar_yaAsociado_retorna409() throws Exception {
        when(conferencistaService.asociar(eq(1L), any(AsociarConferencistaRequest.class)))
                .thenThrow(new ConferencistaYaAsociadoException("Ana María Pérez Gómez ya es conferencista del evento..."));

        mockMvc.perform(post("/api/v1/eventos/1/conferencistas/asociar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":12,\"tipoParticipacion\":\"conferencista\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFERENCISTA_YA_ASOCIADO"));
    }

    @Test
    @DisplayName("Criterio 3: búsqueda previa → 200 con la persona o 204 si no existe")
    void buscar_retorna200o204() throws Exception {
        when(conferencistaService.buscarCoincidencia("1090123456", null, 1L)).thenReturn(Optional.of(coincidencia()));
        when(conferencistaService.buscarCoincidencia("555", null, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/conferencistas/buscar").param("numeroDocumento", "1090123456").param("eventoId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personaId").value(12))
                .andExpect(jsonPath("$.tienePerfil").value(true));
        mockMvc.perform(get("/api/v1/conferencistas/buscar").param("numeroDocumento", "555").param("eventoId", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE retira al conferencista del evento → 204")
    void desasociar_retorna204() throws Exception {
        mockMvc.perform(delete("/api/v1/eventos/1/conferencistas/7"))
                .andExpect(status().isNoContent());
        verify(conferencistaService).desasociar(1L, 7L);
    }

    // ---------------- Criterio 4 ----------------

    @Test
    @DisplayName("Criterio 4: PUT actualiza datos y trayectoria → 200")
    void actualizar_retorna200() throws Exception {
        TrayectoriaResponse trayectoria = new TrayectoriaResponse("Nueva biografía", null, null, null, List.of(),
                List.of(), LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 10, 19, 9, 0));
        when(conferencistaService.actualizar(eq(12L), any(ActualizarConferencistaRequest.class)))
                .thenReturn(new PerfilConferencistaResponse(12L, "CC", "1090123456", "Ana María", "Pérez Gómez",
                        "Ana María Pérez Gómez", "ana.perez@ufps.edu.co", null, false, trayectoria, List.of()));

        mockMvc.perform(put("/api/v1/conferencistas/12").contentType(MediaType.APPLICATION_JSON)
                        .content(actualizarJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trayectoria.biografia").value("Nueva biografía"));
    }

    @Test
    @DisplayName("Criterio 4: documento de otra persona → 409 DATOS_PERSONA_DUPLICADOS en el campo")
    void actualizar_documentoDuplicado_retorna409() throws Exception {
        when(conferencistaService.actualizar(eq(12L), any(ActualizarConferencistaRequest.class)))
                .thenThrow(new DatosPersonaDuplicadosException("numeroDocumento", "El documento ya pertenece a otra persona."));

        mockMvc.perform(put("/api/v1/conferencistas/12").contentType(MediaType.APPLICATION_JSON)
                        .content(actualizarJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DATOS_PERSONA_DUPLICADOS"))
                .andExpect(jsonPath("$.erroresValidacion.numeroDocumento").exists());
    }

    @Test
    @DisplayName("Criterio 4: la vista pública no requiere sesión y devuelve la trayectoria vigente")
    void publico_retorna200() throws Exception {
        when(conferencistaService.listarPublico(1L)).thenReturn(new ConferencistasPublicosEventoResponse(1L,
                "Congreso de Ingeniería de Sistemas", "2026-2", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22),
                List.of()));

        mockMvc.perform(get("/api/v1/publico/eventos/1/conferencistas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoNombre").value("Congreso de Ingeniería de Sistemas"));
    }

    @Test
    @DisplayName("Criterio 4: evento no publicado → 404 en la vista pública")
    void publico_noPublicado_retorna404() throws Exception {
        when(conferencistaService.listarPublico(2L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró un evento publicado con ID: 2"));

        mockMvc.perform(get("/api/v1/publico/eventos/2/conferencistas"))
                .andExpect(status().isNotFound());
    }

    // ---------------- Perfil de ingreso "Conferencista" ----------------

    private PerfilConferencistaResponse miPerfil() {
        TrayectoriaResponse trayectoria = new TrayectoriaResponse("Mi biografía", null, null, null, List.of(),
                List.of(), LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 10, 19, 9, 0));
        return new PerfilConferencistaResponse(12L, "CC", "1090123456", "Ana María", "Pérez Gómez",
                "Ana María Pérez Gómez", "ana.perez@ufps.edu.co", null, true, trayectoria, List.of());
    }

    @Test
    @DisplayName("Mi perfil: GET usa el correo del usuario de la sesión")
    void miPerfil_retorna200() throws Exception {
        when(conferencistaService.obtenerMiPerfil("ana.perez@ufps.edu.co")).thenReturn(miPerfil());

        mockMvc.perform(get("/api/v1/conferencistas/mi-perfil").principal(() -> "ana.perez@ufps.edu.co"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personaId").value(12))
                .andExpect(jsonPath("$.trayectoria.biografia").value("Mi biografía"));
    }

    @Test
    @DisplayName("Mi perfil: aún no registrado como conferencista → 404")
    void miPerfil_noRegistrado_retorna404() throws Exception {
        when(conferencistaService.obtenerMiPerfil("nuevo@ufps.edu.co"))
                .thenThrow(new RecursoNoEncontradoException("Aún no estás registrado como conferencista."));

        mockMvc.perform(get("/api/v1/conferencistas/mi-perfil").principal(() -> "nuevo@ufps.edu.co"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Mi perfil: PUT actualiza la trayectoria del usuario de la sesión")
    void actualizarMiPerfil_retorna200() throws Exception {
        when(conferencistaService.actualizarMiPerfil(eq("ana.perez@ufps.edu.co"), any(ActualizarConferencistaRequest.class)))
                .thenReturn(miPerfil());

        mockMvc.perform(put("/api/v1/conferencistas/mi-perfil").principal(() -> "ana.perez@ufps.edu.co")
                        .contentType(MediaType.APPLICATION_JSON).content(actualizarJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneCuentaUsuario").value(true));
    }
}
