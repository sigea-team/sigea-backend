package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.rol.RolRequest;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoDuplicadoException;
import com.sigea.demosigea_backend.model.Permiso;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.PermisoRepository;
import com.sigea.demosigea_backend.repository.RolRepository;
import com.sigea.demosigea_backend.security.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU-03, Criterio 1: las operaciones críticas de roles (HU-02) quedan auditadas
 * únicamente cuando se completan con éxito.
 */
@ExtendWith(MockitoExtension.class)
class RolServiceAuditoriaTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PermisoRepository permisoRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private RolService rolService;

    private Permiso permiso(Long id, String codigo) {
        return Permiso.builder().id(id).codigo(codigo).modulo("ROLES").descripcion(codigo).build();
    }

    @Test
    @DisplayName("Crear rol registra ROL_CREADO con los datos del rol")
    void crearRol_registraAuditoria() {
        when(rolRepository.existsByNombreIgnoreCase("COORDINADOR")).thenReturn(false);
        when(permisoRepository.findAllById(anySet())).thenReturn(List.of(permiso(1L, "ROLES_VER")));
        when(rolRepository.save(any(Rol.class))).thenAnswer(inv -> {
            Rol r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        rolService.crearRol(new RolRequest("coordinador", "Coordinadores", Set.of(1L)));

        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ROL_CREADO), eq("roles"), eq(10L), any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    @DisplayName("Cambiar un rol registra ROL_ACTUALIZADO con el antes y el después")
    void actualizarRol_registraAntesYDespues() {
        Rol existente = Rol.builder().id(4L).nombre("EVALUADOR").descripcion("Evaluadores")
                .permisos(new HashSet<>(Set.of(permiso(1L, "ROLES_VER")))).build();
        when(rolRepository.findByIdWithPermisos(4L)).thenReturn(Optional.of(existente));
        when(rolRepository.existsByNombreIgnoreCaseAndIdNot("EVALUADOR_PAR", 4L)).thenReturn(false);
        when(permisoRepository.findAllById(anySet()))
                .thenReturn(List.of(permiso(1L, "ROLES_VER"), permiso(2L, "PROPUESTAS_EVALUAR")));
        when(rolRepository.save(any(Rol.class))).thenAnswer(inv -> inv.getArgument(0));

        rolService.actualizarRol(4L, new RolRequest("evaluador_par", "Evaluadores", Set.of(1L, 2L)));

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ROL_ACTUALIZADO), eq("roles"), eq(4L),
                captor.capture());
        Map<String, Object> detalle = captor.getValue();
        Map<String, Object> antes = (Map<String, Object>) detalle.get("antes");
        Map<String, Object> despues = (Map<String, Object>) detalle.get("despues");
        assertEquals("EVALUADOR", antes.get("nombre"));
        assertEquals(List.of("ROLES_VER"), antes.get("permisos"));
        assertEquals("EVALUADOR_PAR", despues.get("nombre"));
        assertEquals(List.of("PROPUESTAS_EVALUAR", "ROLES_VER"), despues.get("permisos"));
    }

    @Test
    @DisplayName("Si la operación falla (nombre duplicado) NO se registra auditoría")
    void actualizarRol_fallido_noAudita() {
        Rol existente = Rol.builder().id(4L).nombre("EVALUADOR").permisos(new HashSet<>()).build();
        when(rolRepository.findByIdWithPermisos(4L)).thenReturn(Optional.of(existente));
        when(rolRepository.existsByNombreIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class,
                () -> rolService.actualizarRol(4L, new RolRequest("ADMIN", null, Set.of())));

        verify(auditoriaService, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si no se puede eliminar (usuarios activos) NO se registra auditoría")
    void eliminarRol_fallido_noAudita() {
        Rol existente = Rol.builder().id(5L).nombre("DOCENTE").permisos(new HashSet<>()).build();
        when(rolRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(rolRepository.countUsuariosActivosByRolId(5L)).thenReturn(2L);

        assertThrows(OperacionNoPermitidaException.class, () -> rolService.eliminarRol(5L));

        verify(auditoriaService, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Eliminar rol registra ROL_ELIMINADO conservando lo eliminado")
    void eliminarRol_registraAuditoria() {
        Rol existente = Rol.builder().id(6L).nombre("TEMPORAL").permisos(new HashSet<>()).build();
        when(rolRepository.findById(6L)).thenReturn(Optional.of(existente));
        when(rolRepository.countUsuariosActivosByRolId(6L)).thenReturn(0L);

        rolService.eliminarRol(6L);

        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ROL_ELIMINADO), eq("roles"), eq(6L), any());
    }
}
