package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.usuario.UsuarioRolesResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoUsuario;
import com.sigea.demosigea_backend.model.Permiso;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.RolRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link UsuarioRolService} (HU-02: asignación de roles a usuarios).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioRolServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private UsuarioRolService service;

    private final Rol rolAdmin = Rol.builder().id(1L).nombre("ADMIN").permisos(new HashSet<>()).build();
    private final Rol rolEvaluador = Rol.builder().id(3L).nombre("EVALUADOR")
            .permisos(new HashSet<>(Set.of(Permiso.builder().id(9L).codigo("PROPUESTAS_EVALUAR").build())))
            .build();
    private final Rol rolParticipante = Rol.builder().id(4L).nombre("PARTICIPANTE").permisos(new HashSet<>()).build();

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private static Usuario usuario(Long id, String nombres, String correo, Rol... roles) {
        Persona persona = Persona.builder().id(id).nombres(nombres).apellidos("Prueba").correo(correo)
                .numeroDocumento("10" + id).build();
        return Usuario.builder().id(id).persona(persona).estado(EstadoUsuario.activo)
                .roles(new HashSet<>(Set.of(roles))).build();
    }

    private static void autenticar(String correo) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                correo, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    @DisplayName("Lista usuarios con sus roles y permisos, filtrando por nombre o correo")
    void listarUsuarios_filtraYExponePermisos() {
        when(usuarioRepository.findAllConPersonaOrdenados()).thenReturn(List.of(
                usuario(1L, "Ana", "ana@ufps.edu.co", rolEvaluador),
                usuario(2L, "Luis", "luis@ufps.edu.co", rolParticipante)));

        List<UsuarioRolesResponse> todos = service.listarUsuarios(null);
        List<UsuarioRolesResponse> filtrados = service.listarUsuarios("  ANA@ufps ");

        assertEquals(2, todos.size());
        assertEquals(1, filtrados.size());
        assertEquals("Ana Prueba", filtrados.get(0).nombreCompleto());
        assertEquals(List.of("PROPUESTAS_EVALUAR"), filtrados.get(0).permisos());
        assertEquals("EVALUADOR", filtrados.get(0).roles().get(0).nombre());
    }

    @Test
    @DisplayName("El administrador asigna el rol de evaluador a otro usuario y queda auditado")
    void actualizarRoles_asignaEvaluador() {
        autenticar("admin@ufps.edu.co");
        Usuario luis = usuario(2L, "Luis", "luis@ufps.edu.co", rolParticipante);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(luis));
        when(rolRepository.findAllById(Set.of(3L, 4L))).thenReturn(List.of(rolEvaluador, rolParticipante));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioRolesResponse r = service.actualizarRoles(2L, Set.of(3L, 4L));

        assertEquals(List.of("EVALUADOR", "PARTICIPANTE"), r.roles().stream().map(UsuarioRolesResponse.RolAsignado::nombre).toList());
        assertEquals(List.of("PROPUESTAS_EVALUAR"), r.permisos());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.USUARIO_ROLES_ACTUALIZADOS),
                eq("usuarios_roles"), eq(2L), anyMap());
    }

    @Test
    @DisplayName("Rechaza roles inexistentes sin modificar al usuario")
    void actualizarRoles_rolInexistente_lanza404() {
        Usuario luis = usuario(2L, "Luis", "luis@ufps.edu.co", rolParticipante);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(luis));
        when(rolRepository.findAllById(Set.of(3L, 77L))).thenReturn(List.of(rolEvaluador));

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizarRoles(2L, Set.of(3L, 77L)));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("El administrador no puede quitarse a sí mismo la gestión de roles")
    void actualizarRoles_autoQuitarseAdmin_lanza409() {
        autenticar("admin@ufps.edu.co");
        Usuario admin = usuario(1L, "Admin", "admin@ufps.edu.co", rolAdmin);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(rolRepository.findAllById(Set.of(3L))).thenReturn(List.of(rolEvaluador));

        assertThrows(OperacionNoPermitidaException.class, () -> service.actualizarRoles(1L, Set.of(3L)));
        verify(usuarioRepository, never()).save(any());
    }
}
