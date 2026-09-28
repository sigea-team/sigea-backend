package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.auth.RestablecerContrasenaRequest;
import com.sigea.demosigea_backend.dto.auth.RestablecerContrasenaResponse;
import com.sigea.demosigea_backend.dto.auth.SolicitarRecuperacionRequest;
import com.sigea.demosigea_backend.dto.auth.SolicitarRecuperacionResponse;
import com.sigea.demosigea_backend.exception.TokenInvalidoException;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoToken;
import com.sigea.demosigea_backend.model.TokenRecuperacion;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AfiliacionRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.RolRepository;
import com.sigea.demosigea_backend.repository.TokenRecuperacionRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import com.sigea.demosigea_backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del flujo de recuperación de contraseña (HU-32).
 * Cubre los 4 criterios de aceptación contra {@link AuthService}.
 */
@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private AfiliacionRepository afiliacionRepository;
    @Mock
    private TokenRecuperacionRepository tokenRecuperacionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "resetTokenExpirationHours", 1);

        Persona persona = Persona.builder()
                .id(1L)
                .nombres("Ana")
                .apellidos("Pérez")
                .correo("ana.perez@ufps.edu.co")
                .build();

        usuario = Usuario.builder()
                .id(1L)
                .persona(persona)
                .contrasenaHash("hashViejo")
                .build();
    }

    // --- Criterio 1: solicitar recuperación con correo registrado ---

    @Test
    @DisplayName("Criterio 1: correo registrado -> genera token 'recuperacion' y envía correo")
    void solicitarRecuperacion_correoRegistrado_generaTokenYEnviaCorreo() {
        when(usuarioRepository.findByPersona_CorreoIgnoreCase("ana.perez@ufps.edu.co"))
                .thenReturn(Optional.of(usuario));
        when(tokenRecuperacionRepository.findByUsuarioAndTipoAndUsadoFalse(usuario, TipoToken.recuperacion))
                .thenReturn(Collections.emptyList());

        SolicitarRecuperacionResponse response =
                authService.solicitarRecuperacion(new SolicitarRecuperacionRequest("ana.perez@ufps.edu.co"));

        assertNotNull(response.mensaje());
        verify(tokenRecuperacionRepository).save(any(TokenRecuperacion.class));
        verify(emailService).enviarCorreoRecuperacion(eq("ana.perez@ufps.edu.co"), eq("Ana"), anyString());
    }

    // --- Criterio 4: mismo mensaje genérico si el correo NO existe ---

    @Test
    @DisplayName("Criterio 4: correo no registrado -> mismo mensaje genérico, sin generar token ni enviar correo")
    void solicitarRecuperacion_correoNoRegistrado_soloMensajeGenerico() {
        when(usuarioRepository.findByPersona_CorreoIgnoreCase("nadie@ufps.edu.co"))
                .thenReturn(Optional.empty());

        SolicitarRecuperacionResponse response =
                authService.solicitarRecuperacion(new SolicitarRecuperacionRequest("nadie@ufps.edu.co"));

        assertNotNull(response.mensaje());
        verify(tokenRecuperacionRepository, never()).save(any(TokenRecuperacion.class));
        verify(emailService, never()).enviarCorreoRecuperacion(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Criterio 4: el mensaje es literalmente el mismo exista o no el correo")
    void solicitarRecuperacion_mensajeEsIdenticoEnAmbosCasos() {
        when(usuarioRepository.findByPersona_CorreoIgnoreCase("ana.perez@ufps.edu.co"))
                .thenReturn(Optional.of(usuario));
        when(tokenRecuperacionRepository.findByUsuarioAndTipoAndUsadoFalse(usuario, TipoToken.recuperacion))
                .thenReturn(Collections.emptyList());
        String mensajeConCuenta =
                authService.solicitarRecuperacion(new SolicitarRecuperacionRequest("ana.perez@ufps.edu.co")).mensaje();

        when(usuarioRepository.findByPersona_CorreoIgnoreCase("nadie@ufps.edu.co"))
                .thenReturn(Optional.empty());
        String mensajeSinCuenta =
                authService.solicitarRecuperacion(new SolicitarRecuperacionRequest("nadie@ufps.edu.co")).mensaje();

        assertEquals(mensajeConCuenta, mensajeSinCuenta);
    }

    // --- Criterio 2: token válido + contraseña válida -> actualiza y notifica ---

    @Test
    @DisplayName("Criterio 2: token válido -> actualiza contraseña, marca token usado y notifica por correo")
    void restablecerContrasena_tokenValido_actualizaYNotifica() {
        TokenRecuperacion token = TokenRecuperacion.builder()
                .id(10L)
                .usuario(usuario)
                .token("token-valido")
                .tipo(TipoToken.recuperacion)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(30))
                .usado(false)
                .build();

        when(tokenRecuperacionRepository.findByTokenAndTipo("token-valido", TipoToken.recuperacion))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NuevaSegura123*")).thenReturn("hashNuevo");

        RestablecerContrasenaResponse response = authService.restablecerContrasena(
                new RestablecerContrasenaRequest("token-valido", "NuevaSegura123*"));

        assertEquals("ana.perez@ufps.edu.co", response.correo());
        assertEquals("hashNuevo", usuario.getContrasenaHash());
        assertEquals(true, token.getUsado());
        verify(usuarioRepository).save(usuario);
        verify(tokenRecuperacionRepository).save(token);
        verify(emailService).enviarNotificacionCambioContrasena("ana.perez@ufps.edu.co", "Ana");
    }

    // --- Criterio 3: token inexistente / ya usado / expirado ---

    @Test
    @DisplayName("Criterio 3: token inexistente -> TokenInvalidoException, no toca la contraseña")
    void restablecerContrasena_tokenInexistente_lanzaTokenInvalido() {
        when(tokenRecuperacionRepository.findByTokenAndTipo("token-fantasma", TipoToken.recuperacion))
                .thenReturn(Optional.empty());

        assertThrows(TokenInvalidoException.class, () -> authService.restablecerContrasena(
                new RestablecerContrasenaRequest("token-fantasma", "NuevaSegura123*")));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Criterio 3: token ya usado -> TokenInvalidoException")
    void restablecerContrasena_tokenYaUsado_lanzaTokenInvalido() {
        TokenRecuperacion token = TokenRecuperacion.builder()
                .usuario(usuario)
                .token("token-usado")
                .tipo(TipoToken.recuperacion)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(30))
                .usado(true)
                .build();

        when(tokenRecuperacionRepository.findByTokenAndTipo("token-usado", TipoToken.recuperacion))
                .thenReturn(Optional.of(token));

        assertThrows(TokenInvalidoException.class, () -> authService.restablecerContrasena(
                new RestablecerContrasenaRequest("token-usado", "NuevaSegura123*")));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Criterio 3: token expirado -> TokenInvalidoException")
    void restablecerContrasena_tokenExpirado_lanzaTokenInvalido() {
        TokenRecuperacion token = TokenRecuperacion.builder()
                .usuario(usuario)
                .token("token-expirado")
                .tipo(TipoToken.recuperacion)
                .fechaExpiracion(LocalDateTime.now().minusMinutes(1))
                .usado(false)
                .build();

        when(tokenRecuperacionRepository.findByTokenAndTipo("token-expirado", TipoToken.recuperacion))
                .thenReturn(Optional.of(token));

        assertThrows(TokenInvalidoException.class, () -> authService.restablecerContrasena(
                new RestablecerContrasenaRequest("token-expirado", "NuevaSegura123*")));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // --- Detalle interno del Criterio 1: invalida tokens previos sin usar antes de crear uno nuevo ---

    @Test
    @DisplayName("Al solicitar de nuevo, invalida los tokens de recuperación previos sin usar")
    void solicitarRecuperacion_invalidaTokensPreviosSinUsar() {
        TokenRecuperacion tokenViejo = TokenRecuperacion.builder()
                .usuario(usuario).token("viejo").tipo(TipoToken.recuperacion)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(10)).usado(false).build();

        when(usuarioRepository.findByPersona_CorreoIgnoreCase("ana.perez@ufps.edu.co"))
                .thenReturn(Optional.of(usuario));
        when(tokenRecuperacionRepository.findByUsuarioAndTipoAndUsadoFalse(usuario, TipoToken.recuperacion))
                .thenReturn(List.of(tokenViejo));

        authService.solicitarRecuperacion(new SolicitarRecuperacionRequest("ana.perez@ufps.edu.co"));

        assertEquals(true, tokenViejo.getUsado());
        verify(tokenRecuperacionRepository).saveAll(List.of(tokenViejo));
        verify(tokenRecuperacionRepository, times(1)).save(any(TokenRecuperacion.class)); // el nuevo token
    }
}
