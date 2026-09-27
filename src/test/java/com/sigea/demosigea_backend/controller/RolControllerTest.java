package com.sigea.demosigea_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigea.demosigea_backend.dto.permiso.PermisoResponse;
import com.sigea.demosigea_backend.dto.rol.RolRequest;
import com.sigea.demosigea_backend.dto.rol.RolResponse;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.service.RolService;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RolControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RolService rolService;

    @InjectMocks
    private RolController rolController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rolController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Criterio 1: POST /api/v1/roles crea un rol y retorna 201 Created")
    void crearRol_retorna201() throws Exception {
        RolRequest request = new RolRequest("EVALUADOR", "Rol para evaluadores", Set.of(1L, 2L));
        PermisoResponse perm = new PermisoResponse(1L, "ROLES_VER", "ROLES", "Ver roles");
        RolResponse response = new RolResponse(1L, "EVALUADOR", "Rol para evaluadores", 0, 0, List.of(perm), System.currentTimeMillis());

        when(rolService.crearRol(any(RolRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("EVALUADOR"))
                .andExpect(jsonPath("$.permisos[0].codigo").value("ROLES_VER"));
    }

    @Test
    @DisplayName("Criterio 2: PUT /api/v1/roles/{id} actualiza un rol y retorna 200 OK")
    void actualizarRol_retorna200() throws Exception {
        RolRequest request = new RolRequest("EVALUADOR_ACTUALIZADO", "Rol modificado", Set.of(2L));
        PermisoResponse perm = new PermisoResponse(2L, "PROPUESTAS_EVALUAR", "EVALUACION", "Evaluar");
        RolResponse response = new RolResponse(1L, "EVALUADOR_ACTUALIZADO", "Rol modificado", 2, 2, List.of(perm), System.currentTimeMillis());

        when(rolService.actualizarRol(eq(1L), any(RolRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("EVALUADOR_ACTUALIZADO"))
                .andExpect(jsonPath("$.usuariosActivosAsignados").value(2));
    }

    @Test
    @DisplayName("Criterio 3: DELETE /api/v1/roles/{id} con usuarios activos retorna 409 Conflict")
    void eliminarRol_conUsuariosActivos_retorna409() throws Exception {
        doThrow(new OperacionNoPermitidaException("No es posible eliminar el rol 'ADMIN' porque tiene 2 usuario(s) activo(s) asignado(s)."))
                .when(rolService).eliminarRol(1L);

        mockMvc.perform(delete("/api/v1/roles/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"))
                .andExpect(jsonPath("$.message").value("No es posible eliminar el rol 'ADMIN' porque tiene 2 usuario(s) activo(s) asignado(s)."));
    }

    @Test
    @DisplayName("Criterio 3: DELETE /api/v1/roles/{id} sin usuarios activos retorna 200 OK")
    void eliminarRol_sinUsuariosActivos_retorna200() throws Exception {
        doNothing().when(rolService).eliminarRol(2L);

        mockMvc.perform(delete("/api/v1/roles/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Rol eliminado exitosamente."));
    }

    @Test
    @DisplayName("GET /api/v1/roles lista los roles del sistema")
    void listarRoles_retorna200() throws Exception {
        RolResponse rol = new RolResponse(1L, "PARTICIPANTE", "Rol participante", 5, 5, List.of(), System.currentTimeMillis());
        when(rolService.listarRoles()).thenReturn(List.of(rol));

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("PARTICIPANTE"));
    }
}
