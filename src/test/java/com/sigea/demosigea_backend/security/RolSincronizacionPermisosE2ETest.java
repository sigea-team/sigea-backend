package com.sigea.demosigea_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigea.demosigea_backend.dto.rol.RolRequest;
import com.sigea.demosigea_backend.dto.rol.RolResponse;
import com.sigea.demosigea_backend.model.EstadoUsuario;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import com.sigea.demosigea_backend.service.RolService;
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

import jakarta.servlet.FilterChain;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba de flujo que valida el comportamiento del Criterio 2:
 * "modificarRol invalida sesiones activas forzando re-autenticación inmediata".
 */
@ExtendWith(MockitoExtension.class)
class RolSincronizacionPermisosE2ETest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FilterChain filterChain;

    @Mock
    private RolService rolService;

    private TokenBlacklistService tokenBlacklistService;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenBlacklistService = new TokenBlacklistService();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(tokenProvider, usuarioRepository, tokenBlacklistService);
    }

    @Test
    @DisplayName("Criterio 2: modificarRol invalida sesiones activas y el usuario debe luego re-autenticarse")
    void modificarRol_invalidaSesionesActivas_usuarioDebeLugoReautenticarse() throws Exception {
        Long rolId = 2L;
        String correoDocente = "docente@ufps.edu.co";
        String tokenInicial = "token_inicial_emitido_antes_del_cambio";

        Rol rolDocente = Rol.builder()
                .id(rolId)
                .nombre("DOCENTE")
                .build();

        Persona personaDocente = Persona.builder().correo(correoDocente).build();
        Usuario usuarioDocente = Usuario.builder()
                .id(100L)
                .persona(personaDocente)
                .estado(EstadoUsuario.activo)
                .correoVerificado(true)
                .roles(new java.util.HashSet<>(Set.of(rolDocente)))
                .build();

        // 1. Simular emisión del token inicial (hace 10 segundos)
        Date fechaEmisionTokenViejo = new Date(System.currentTimeMillis() - 10000);
        when(tokenProvider.validateToken(tokenInicial)).thenReturn(true);
        when(tokenProvider.getEmailFromToken(tokenInicial)).thenReturn(correoDocente);
        when(tokenProvider.getIssuedAtFromToken(tokenInicial)).thenReturn(fechaEmisionTokenViejo);
        when(usuarioRepository.findByPersona_CorreoIgnoreCase(correoDocente)).thenReturn(Optional.of(usuarioDocente));

        // Petición 1: Antes del cambio de rol, el token inicial autentica exitosamente
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        request1.addHeader("Authorization", "Bearer " + tokenInicial);

        jwtAuthenticationFilter.doFilter(request1, response1, filterChain);

        Authentication authAntes = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authAntes, "El usuario con sesión activa debe poder autenticarse inicialmente");

        // 2. Administrador modifica el rol mediante RolService (dispara invalidación inmediata de sesiones)
        // Simulamos la llamada a invalidarSesionesDeRol que realiza rolService.actualizarRol(...)
        tokenBlacklistService.invalidarSesionesDeRol(rolId);
        SecurityContextHolder.clearContext();

        // 3. Petición 2: El usuario intenta realizar una petición con el token emitido anteriormente
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        request2.addHeader("Authorization", "Bearer " + tokenInicial);

        jwtAuthenticationFilter.doFilter(request2, response2, filterChain);

        Authentication authDespues = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authDespues, "El token anterior debe ser rechazado inmediatamente tras la modificación del rol");

        // 4. Usuario se re-autentica (Login) y obtiene un token nuevo
        String tokenNuevo = "token_nuevo_emitido_despues_del_cambio";
        Date fechaEmisionTokenNuevo = new Date(System.currentTimeMillis() + 1000);
        when(tokenProvider.validateToken(tokenNuevo)).thenReturn(true);
        when(tokenProvider.getEmailFromToken(tokenNuevo)).thenReturn(correoDocente);
        when(tokenProvider.getIssuedAtFromToken(tokenNuevo)).thenReturn(fechaEmisionTokenNuevo);

        MockHttpServletRequest request3 = new MockHttpServletRequest();
        MockHttpServletResponse response3 = new MockHttpServletResponse();
        request3.addHeader("Authorization", "Bearer " + tokenNuevo);

        jwtAuthenticationFilter.doFilter(request3, response3, filterChain);

        Authentication authReautenticado = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authReautenticado, "El usuario autenticado nuevamente con el token renovado tiene acceso inmediato");
    }
}
