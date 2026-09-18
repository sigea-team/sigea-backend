package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.auth.LoginRequest;
import com.sigea.demosigea_backend.dto.auth.LoginResponse;
import com.sigea.demosigea_backend.dto.auth.ReenviarVerificacionRequest;
import com.sigea.demosigea_backend.dto.auth.ReenviarVerificacionResponse;
import com.sigea.demosigea_backend.dto.auth.RegistroRequest;
import com.sigea.demosigea_backend.dto.auth.RegistroResponse;
import com.sigea.demosigea_backend.dto.auth.VerificarCorreoResponse;
import com.sigea.demosigea_backend.exception.CorreoNoVerificadoException;
import com.sigea.demosigea_backend.exception.CredencialesInvalidasException;
import com.sigea.demosigea_backend.exception.RecursoDuplicadoException;
import com.sigea.demosigea_backend.exception.TokenInvalidoException;
import com.sigea.demosigea_backend.model.EstadoUsuario;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.TipoToken;
import com.sigea.demosigea_backend.model.TokenRecuperacion;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.RolRepository;
import com.sigea.demosigea_backend.repository.TokenRecuperacionRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import com.sigea.demosigea_backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "defaultRoleName", "PARTICIPANTE");
    }

    @Test
    @DisplayName("Criterio 1: Registro exitoso crea cuenta y solicita verificación de correo")
    void registrar_Exitoso() {
        RegistroRequest request = new RegistroRequest(
                "Carlos", "Gómez", "CC", "12345678",
                "carlos@correo.com", "Password123*", null, null, null
        );

        when(personaRepository.existsByCorreoIgnoreCase("carlos@correo.com")).thenReturn(false);
        when(personaRepository.existsByNumeroDocumento("12345678")).thenReturn(false);
        when(usuarioRepository.existsByNombreUsuarioIgnoreCase("carlos")).thenReturn(false);

        Rol rol = Rol.builder().id(1L).nombre("PARTICIPANTE").build();
        when(rolRepository.findByNombreIgnoreCase("PARTICIPANTE")).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode("Password123*")).thenReturn("hash_123");

        Persona personaGuardada = Persona.builder().id(10L).correo("carlos@correo.com").nombres("Carlos").build();
        when(personaRepository.save(any(Persona.class))).thenReturn(personaGuardada);

        Usuario usuarioGuardado = Usuario.builder()
                .id(20L)
                .persona(personaGuardada)
                .nombreUsuario("carlos")
                .correoVerificado(false)
                .build();
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        RegistroResponse response = authService.registrar(request);

        assertNotNull(response);
        assertEquals(20L, response.usuarioId());
        assertEquals("carlos", response.nombreUsuario());
        assertEquals("carlos@correo.com", response.correo());
        assertTrue(response.requiereVerificacion());

        verify(tokenRecuperacionRepository).save(any(TokenRecuperacion.class));
        verify(emailService).enviarCorreoVerificacion(eq("carlos@correo.com"), eq("Carlos"), anyString());
    }

    @Test
    @DisplayName("Criterio 2: Registro con correo existente es rechazado")
    void registrar_CorreoDuplicado_LanzaExcepcion() {
        RegistroRequest request = new RegistroRequest(
                "Carlos", "Gómez", "CC", "12345678",
                "carlos@correo.com", "Password123*", null, null, null
        );

        when(personaRepository.existsByCorreoIgnoreCase("carlos@correo.com")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class, () -> authService.registrar(request));
        assertTrue(ex.getMessage().contains("correo electrónico"));

        verify(personaRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 2: Registro con documento existente es rechazado")
    void registrar_DocumentoDuplicado_LanzaExcepcion() {
        RegistroRequest request = new RegistroRequest(
                "Carlos", "Gómez", "CC", "12345678",
                "carlos@correo.com", "Password123*", null, null, null
        );

        when(personaRepository.existsByCorreoIgnoreCase("carlos@correo.com")).thenReturn(false);
        when(personaRepository.existsByNumeroDocumento("12345678")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class, () -> authService.registrar(request));
        assertTrue(ex.getMessage().contains("documento"));

        verify(personaRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 4: Login con correo no verificado es impedido (403/CorreoNoVerificadoException)")
    void login_CorreoNoVerificado_ImpideAcceso() {
        LoginRequest request = new LoginRequest("carlos@correo.com", "Password123*");

        Persona persona = Persona.builder().id(1L).correo("carlos@correo.com").nombres("Carlos").build();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .persona(persona)
                .nombreUsuario("carlos")
                .contrasenaHash("hash_pass")
                .correoVerificado(false)
                .estado(EstadoUsuario.activo)
                .build();

        when(usuarioRepository.findByIdentificador("carlos@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123*", "hash_pass")).thenReturn(true);

        CorreoNoVerificadoException ex = assertThrows(CorreoNoVerificadoException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("no ha sido verificado"));
        assertEquals("carlos@correo.com", ex.getCorreo());
    }

    @Test
    @DisplayName("Login exitoso cuando el correo ya fue verificado")
    void login_CorreoVerificado_Exitoso() {
        LoginRequest request = new LoginRequest("carlos@correo.com", "Password123*");

        Persona persona = Persona.builder().id(1L).correo("carlos@correo.com").nombres("Carlos").apellidos("Gómez").build();
        Rol rol = Rol.builder().id(1L).nombre("PARTICIPANTE").build();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .persona(persona)
                .nombreUsuario("carlos")
                .contrasenaHash("hash_pass")
                .correoVerificado(true)
                .estado(EstadoUsuario.activo)
                .roles(new HashSet<>(Set.of(rol)))
                .build();

        when(usuarioRepository.findByIdentificador("carlos@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123*", "hash_pass")).thenReturn(true);
        when(jwtTokenProvider.generateToken(eq(1L), eq("carlos"), eq("carlos@correo.com"), any())).thenReturn("jwt_token_sample");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt_token_sample", response.token());
        assertEquals("Bearer", response.tipoToken());
        assertEquals("carlos", response.nombreUsuario());
    }

    @Test
    @DisplayName("Verificación de correo con token válido activa al usuario")
    void verificarCorreo_TokenValido_Exitoso() {
        Usuario usuario = Usuario.builder().id(1L).correoVerificado(false).build();
        TokenRecuperacion token = TokenRecuperacion.builder()
                .id(1L)
                .token("valid_token_123")
                .usuario(usuario)
                .tipo(TipoToken.verificacion)
                .fechaExpiracion(LocalDateTime.now().plusHours(2))
                .usado(false)
                .build();

        when(tokenRecuperacionRepository.findByTokenAndTipo("valid_token_123", TipoToken.verificacion))
                .thenReturn(Optional.of(token));

        VerificarCorreoResponse response = authService.verificarCorreo("valid_token_123");

        assertTrue(response.verificado());
        assertTrue(token.getUsado());
        assertTrue(usuario.getCorreoVerificado());
        verify(tokenRecuperacionRepository).save(token);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Reenvío de verificación genera nuevo token y despacha correo")
    void reenviarVerificacion_Exitoso() {
        ReenviarVerificacionRequest request = new ReenviarVerificacionRequest("carlos@correo.com");
        Persona persona = Persona.builder().id(1L).correo("carlos@correo.com").nombres("Carlos").build();
        Usuario usuario = Usuario.builder().id(1L).persona(persona).correoVerificado(false).build();

        when(usuarioRepository.findByIdentificador("carlos@correo.com")).thenReturn(Optional.of(usuario));
        when(tokenRecuperacionRepository.findByUsuarioAndTipoAndUsadoFalse(usuario, TipoToken.verificacion))
                .thenReturn(Collections.emptyList());

        ReenviarVerificacionResponse response = authService.reenviarVerificacion(request);

        assertNotNull(response);
        assertEquals("carlos@correo.com", response.correo());
        verify(tokenRecuperacionRepository).save(any(TokenRecuperacion.class));
        verify(emailService).enviarCorreoVerificacion(eq("carlos@correo.com"), eq("Carlos"), anyString());
    }
}
