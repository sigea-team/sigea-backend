package com.sigea.demosigea_backend.security;

import com.sigea.demosigea_backend.model.EstadoUsuario;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = obtenerTokenDeRequest(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            String email = tokenProvider.getEmailFromToken(token);

            Optional<Usuario> usuarioOpt = usuarioRepository.findByPersona_CorreoIgnoreCase(email);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                // Revalidar que la cuenta siga activa y el correo verificado durante la vigencia del token
                if (usuario.getEstado() == EstadoUsuario.activo && Boolean.TRUE.equals(usuario.getCorreoVerificado())) {
                    List<String> tokenRoles = tokenProvider.getRolesFromToken(token);
                    List<String> rolesToUse = (tokenRoles != null && !tokenRoles.isEmpty())
                            ? tokenRoles
                            : usuario.getRoles().stream().map(Rol::getNombre).toList();

                    // Aplicar convención única: asegurar prefijo "ROLE_"
                    List<SimpleGrantedAuthority> authorities = rolesToUse.stream()
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String obtenerTokenDeRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
