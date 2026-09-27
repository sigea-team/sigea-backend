package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.rol.RolRequest;
import com.sigea.demosigea_backend.dto.rol.RolResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoDuplicadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Permiso;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.repository.PermisoRepository;
import com.sigea.demosigea_backend.repository.RolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PermisoRepository permisoRepository;

    @InjectMocks
    private RolService rolService;

    @Test
    @DisplayName("Criterio 1: Debe crear un rol y asignarle permisos correctamente")
    void crearRol_exitoso() {
        Permiso permiso = Permiso.builder()
                .id(1L)
                .codigo("ROLES_VER")
                .modulo("ROLES")
                .descripcion("Ver roles")
                .build();

        RolRequest request = new RolRequest("COORDINADOR", "Rol para coordinadores", Set.of(1L));

        when(rolRepository.existsByNombreIgnoreCase("COORDINADOR")).thenReturn(false);
        when(permisoRepository.findAllById(anySet())).thenReturn(List.of(permiso));
        when(rolRepository.save(any(Rol.class))).thenAnswer(invocation -> {
            Rol r = invocation.getArgument(0);
            r.setId(10L);
            return r;
        });

        RolResponse response = rolService.crearRol(request);

        assertNotNull(response);
        assertEquals("COORDINADOR", response.nombre());
        assertEquals(1, response.permisos().size());
        verify(rolRepository).save(any(Rol.class));
    }

    @Test
    @DisplayName("Criterio 1: No debe permitir crear un rol con nombre duplicado")
    void crearRol_nombreDuplicado() {
        RolRequest request = new RolRequest("ADMINISTRADOR", "Rol duplicado", Set.of());
        when(rolRepository.existsByNombreIgnoreCase("ADMINISTRADOR")).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> rolService.crearRol(request));
        verify(rolRepository, never()).save(any(Rol.class));
    }

    @Test
    @DisplayName("Criterio 2: Debe modificar los permisos de un rol existente")
    void actualizarRol_exitoso() {
        Rol rolExistente = Rol.builder()
                .id(5L)
                .nombre("EVALUADOR")
                .descripcion("Antigua descripción")
                .permisos(new HashSet<>())
                .build();

        Permiso permisoNuevo = Permiso.builder()
                .id(2L)
                .codigo("PROPUESTAS_EVALUAR")
                .modulo("EVALUACION")
                .build();

        RolRequest request = new RolRequest("EVALUADOR_SENIOR", "Nueva descripción", Set.of(2L));

        when(rolRepository.findByIdWithPermisos(5L)).thenReturn(Optional.of(rolExistente));
        when(rolRepository.existsByNombreIgnoreCaseAndIdNot("EVALUADOR_SENIOR", 5L)).thenReturn(false);
        when(permisoRepository.findAllById(Set.of(2L))).thenReturn(List.of(permisoNuevo));
        when(rolRepository.save(any(Rol.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rolRepository.countUsuariosActivosByRolId(5L)).thenReturn(3L);
        when(rolRepository.countTotalUsuariosByRolId(5L)).thenReturn(3L);

        RolResponse response = rolService.actualizarRol(5L, request);

        assertEquals("EVALUADOR_SENIOR", response.nombre());
        assertEquals(1, response.permisos().size());
        assertEquals("PROPUESTAS_EVALUAR", response.permisos().get(0).codigo());
        assertEquals(3L, response.usuariosActivosAsignados());
    }

    @Test
    @DisplayName("Criterio 3: Debe impedir la eliminación si el rol tiene usuarios activos asignados")
    void eliminarRol_conUsuariosActivos_lanzaExcepcion() {
        Rol rol = Rol.builder().id(2L).nombre("DOCENTE").build();

        when(rolRepository.findById(2L)).thenReturn(Optional.of(rol));
        when(rolRepository.countUsuariosActivosByRolId(2L)).thenReturn(4L);

        OperacionNoPermitidaException ex = assertThrows(
                OperacionNoPermitidaException.class,
                () -> rolService.eliminarRol(2L)
        );

        assertNotNull(ex.getMessage());
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("4 usuario(s) activo(s)"));
        verify(rolRepository, never()).delete(any(Rol.class));
    }

    @Test
    @DisplayName("Criterio 3: Debe eliminar el rol si no tiene usuarios activos asignados")
    void eliminarRol_sinUsuariosActivos_exitoso() {
        Rol rol = Rol.builder().id(3L).nombre("ROL_TEMPORAL").build();

        when(rolRepository.findById(3L)).thenReturn(Optional.of(rol));
        when(rolRepository.countUsuariosActivosByRolId(3L)).thenReturn(0L);

        rolService.eliminarRol(3L);

        verify(rolRepository).delete(rol);
    }
}
