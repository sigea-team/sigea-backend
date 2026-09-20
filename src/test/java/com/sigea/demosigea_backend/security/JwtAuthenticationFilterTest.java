package com.sigea.demosigea_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigea.demosigea_backend.model.EstadoUsuario;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Token válido de usuario activo asigna authorities con convención ROLE_")
    void doFilterInternal_UsuarioActivo_AsignaRolesConConvencionRole() throws Exception {
        request.addHeader("Authorization", "Bearer valid_token");

        when(tokenProvider.validateToken("valid_token")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("valid_token")).thenReturn("juan");
        when(tokenProvider.getRolesFromToken("valid_token")).thenReturn(List.of("ADMIN", "PARTICIPANTE"));

        Usuario usuario = Usuario.builder()
                .nombreUsuario("juan")
                .estado(EstadoUsuario.activo)
                .correoVerificado(true)
                .build();

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("juan")).thenReturn(Optional.of(usuario));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("juan", auth.getName());
        assertEquals(2, auth.getAuthorities().size());

        List<String> authorityNames = auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        assertTrue(authorityNames.contains("ROLE_ADMIN"));
        assertTrue(authorityNames.contains("ROLE_PARTICIPANTE"));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Usuario bloqueado no es autenticado aunque el token sea válido")
    void doFilterInternal_UsuarioBloqueado_NoAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer valid_token");

        when(tokenProvider.validateToken("valid_token")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("valid_token")).thenReturn("juan");

        Usuario usuario = Usuario.builder()
                .nombreUsuario("juan")
                .estado(EstadoUsuario.bloqueado)
                .correoVerificado(true)
                .build();

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("juan")).thenReturn(Optional.of(usuario));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Usuario con correo no verificado no es autenticado")
    void doFilterInternal_CorreoNoVerificado_NoAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer valid_token");

        when(tokenProvider.validateToken("valid_token")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("valid_token")).thenReturn("juan");

        Usuario usuario = Usuario.builder()
                .nombreUsuario("juan")
                .estado(EstadoUsuario.activo)
                .correoVerificado(false)
                .build();

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("juan")).thenReturn(Optional.of(usuario));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Handlers 401 y 403 devuelven JSON ErrorResponse estructurado")
    void testSecurityHandlersFormat() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint(objectMapper);
        CustomAccessDeniedHandler accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res401 = new MockHttpServletResponse();
        MockHttpServletResponse res403 = new MockHttpServletResponse();

        entryPoint.commence(req, res401, null);
        assertEquals(401, res401.getStatus());
        assertTrue(res401.getContentAsString().contains("NO_AUTORIZADO"));

        accessDeniedHandler.handle(req, res403, null);
        assertEquals(403, res403.getStatus());
        assertTrue(res403.getContentAsString().contains("ACCESO_DENEGADO"));
    }
}
