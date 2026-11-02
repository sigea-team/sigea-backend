package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.usuario.UsuarioRolesResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Permiso;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.RolRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Asignación de roles a usuarios por parte del administrador (HU-02).
 * <p>
 * Los permisos de un usuario salen de sus roles: por ejemplo, quien tenga un rol con el permiso
 * {@code PROPUESTAS_EVALUAR} puede ingresar con el perfil «Evaluador». Como el filtro JWT recarga los
 * roles desde la base de datos en cada petición, el cambio aplica de inmediato en el backend.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioRolService {

    private static final String ENTIDAD = "usuarios_roles";

    /** Nombres de rol que el sistema trata como administrador. */
    static final Set<String> ROLES_ADMINISTRADOR = Set.of("ADMIN", "ADMINISTRADOR", "ROLE_ADMIN");

    /** Permiso que habilita la gestión de roles de usuarios. */
    static final String PERMISO_GESTIONAR_ROLES = "USUARIOS_GESTIONAR_ROLES";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Lista los usuarios con sus roles, filtrando opcionalmente por nombre, correo o documento.
     *
     * @param busqueda texto a buscar (opcional, sin distinguir mayúsculas).
     * @return usuarios que coinciden.
     */
    @Transactional(readOnly = true)
    public List<UsuarioRolesResponse> listarUsuarios(String busqueda) {
        String q = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        return usuarioRepository.findAllConPersonaOrdenados().stream()
                .filter(u -> q.isEmpty() || coincide(u, q))
                .map(UsuarioRolesResponse::fromEntity)
                .toList();
    }

    /**
     * Reemplaza los roles de un usuario.
     *
     * @param usuarioId ID del usuario.
     * @param rolesIds  IDs de los roles que tendrá.
     * @return el usuario con sus roles actualizados.
     * @throws RecursoNoEncontradoException  si el usuario o algún rol no existen.
     * @throws OperacionNoPermitidaException si el administrador intenta quitarse a sí mismo la gestión de roles.
     */
    @Transactional
    public UsuarioRolesResponse actualizarRoles(Long usuarioId, Set<Long> rolesIds) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario con ID: " + usuarioId));

        List<Rol> encontrados = rolRepository.findAllById(rolesIds);
        if (encontrados.size() != rolesIds.size()) {
            Set<Long> faltantes = new HashSet<>(rolesIds);
            faltantes.removeAll(encontrados.stream().map(Rol::getId).collect(Collectors.toSet()));
            throw new RecursoNoEncontradoException("Los siguientes IDs de roles no existen: " + faltantes);
        }

        if (esUsuarioAutenticado(usuario) && !puedeGestionarRoles(encontrados)) {
            throw new OperacionNoPermitidaException(
                    "No puede quitarse a sí mismo el rol de administrador o el permiso de gestionar roles: "
                            + "perdería el acceso a esta pantalla.");
        }

        List<String> antes = nombres(usuario.getRoles());
        usuario.getRoles().clear();
        usuario.getRoles().addAll(encontrados);
        Usuario guardado = usuarioRepository.save(usuario);
        List<String> despues = nombres(guardado.getRoles());

        log.info("Roles del usuario {} actualizados: {} -> {}", usuarioId, antes, despues);
        auditoriaService.registrar(TipoOperacionAuditoria.USUARIO_ROLES_ACTUALIZADOS, ENTIDAD, usuarioId,
                DetalleAuditoria.de("antes", Map.of("roles", antes), "despues", Map.of("roles", despues)));

        return UsuarioRolesResponse.fromEntity(guardado);
    }

    private static boolean coincide(Usuario u, String q) {
        Persona p = u.getPersona();
        if (p == null) {
            return false;
        }
        String texto = String.join(" ",
                p.getNombres() == null ? "" : p.getNombres(),
                p.getApellidos() == null ? "" : p.getApellidos(),
                p.getCorreo() == null ? "" : p.getCorreo(),
                p.getNumeroDocumento() == null ? "" : p.getNumeroDocumento()).toLowerCase(Locale.ROOT);
        return texto.contains(q);
    }

    private static List<String> nombres(Collection<Rol> roles) {
        return roles == null ? List.of() : roles.stream().map(Rol::getNombre).sorted().toList();
    }

    static boolean puedeGestionarRoles(Collection<Rol> roles) {
        return roles.stream().anyMatch(r ->
                ROLES_ADMINISTRADOR.contains(r.getNombre().toUpperCase(Locale.ROOT))
                        || (r.getPermisos() != null && r.getPermisos().stream()
                        .map(Permiso::getCodigo)
                        .anyMatch(PERMISO_GESTIONAR_ROLES::equals)));
    }

    private static boolean esUsuarioAutenticado(Usuario usuario) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || usuario.getPersona() == null) {
            return false;
        }
        return auth.getName() != null && auth.getName().equalsIgnoreCase(usuario.getPersona().getCorreo());
    }
}
