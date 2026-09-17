package com.sigea.demosigea_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigea.demosigea_backend.dto.LoginRequest;
import com.sigea.demosigea_backend.dto.LoginResponse;
import com.sigea.demosigea_backend.dto.ReenviarVerificacionRequest;
import com.sigea.demosigea_backend.dto.ReenviarVerificacionResponse;
import com.sigea.demosigea_backend.dto.RegistroRequest;
import com.sigea.demosigea_backend.dto.RegistroResponse;
import com.sigea.demosigea_backend.dto.VerificarCorreoRequest;
import com.sigea.demosigea_backend.dto.VerificarCorreoResponse;
import com.sigea.demosigea_backend.exception.CorreoNoVerificadoException;
import com.sigea.demosigea_backend.exception.GlobalExceptionHandler;
import com.sigea.demosigea_backend.exception.RecursoDuplicadoException;
import com.sigea.demosigea_backend.service.AuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Criterio 1: Registro exitoso con datos obligatorios retorna 201 Created y mensaje de verificación")
    void registrar_Exitoso_Retorna201() throws Exception {
        RegistroRequest request = new RegistroRequest(
                "Carlos", "Gómez", "CC", "12345678",
                "carlos@correo.com", "Password123*", "cgomez", null, null
        );

        RegistroResponse response = new RegistroResponse(
                1L, "cgomez", "carlos@correo.com",
                "Cuenta creada exitosamente. Se ha enviado un enlace de verificación a su correo electrónico.",
                true
        );

        when(authService.registrar(any(RegistroRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.nombreUsuario").value("cgomez"))
                .andExpect(jsonPath("$.correo").value("carlos@correo.com"))
                .andExpect(jsonPath("$.requiereVerificacion").value(true));
    }

    @Test
    @DisplayName("Criterio 2: Registro con correo o documento existente retorna 409 Conflict")
    void registrar_Duplicado_Retorna409() throws Exception {
        RegistroRequest request = new RegistroRequest(
                "Carlos", "Gómez", "CC", "12345678",
                "carlos@correo.com", "Password123*", null, null, null
        );

        when(authService.registrar(any(RegistroRequest.class)))
                .thenThrow(new RecursoDuplicadoException("Ya existe una cuenta registrada con el correo electrónico ingresado."));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("CUENTA_YA_EXISTE"))
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta registrada con el correo electrónico ingresado."));
    }

    @Test
    @DisplayName("Criterio 3: Contraseña que no cumple con política de seguridad retorna 400 Bad Request indicando requisitos")
    void registrar_ContrasenaInvalida_Retorna400ConRequisitos() throws Exception {
        // Contraseña débil (sin mayúscula, sin carácter especial, menor a 8)
        RegistroRequest request = new RegistroRequest(
                "Carlos", "Gómez", "CC", "12345678",
                "carlos@correo.com", "12345", null, null, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.erroresValidacion.contrasena").exists());
    }

    @Test
    @DisplayName("Criterio 4: Login de usuario no verificado es impedido con 403 Forbidden y código CORREO_NO_VERIFICADO")
    void login_CorreoNoVerificado_Retorna403() throws Exception {
        LoginRequest request = new LoginRequest("carlos@correo.com", "Password123*");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new CorreoNoVerificadoException(
                        "No se puede iniciar sesión: Su correo electrónico aún no ha sido verificado.",
                        "carlos@correo.com"
                ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.codigo").value("CORREO_NO_VERIFICADO"))
                .andExpect(jsonPath("$.correo").value("carlos@correo.com"))
                .andExpect(jsonPath("$.message").value("No se puede iniciar sesión: Su correo electrónico aún no ha sido verificado."));
    }

    @Test
    @DisplayName("Login exitoso con correo verificado retorna 200 OK y token JWT")
    void login_Exitoso_Retorna200YToken() throws Exception {
        LoginRequest request = new LoginRequest("carlos@correo.com", "Password123*");
        LoginResponse response = new LoginResponse(
                "jwt.sample.token", "Bearer", 1L, "cgomez",
                "carlos@correo.com", "Carlos Gómez", List.of("PARTICIPANTE")
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.sample.token"))
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.nombreUsuario").value("cgomez"));
    }

    @Test
    @DisplayName("Verificar correo vía GET retorna 200 OK")
    void verificarCorreo_Get_Retorna200() throws Exception {
        VerificarCorreoResponse response = new VerificarCorreoResponse(
                "¡Correo verificado con éxito!", true
        );

        when(authService.verificarCorreo("token123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificado").value(true));
    }

    @Test
    @DisplayName("Reenviar verificación retorna 200 OK con confirmación")
    void reenviarVerificacion_Retorna200() throws Exception {
        ReenviarVerificacionRequest request = new ReenviarVerificacionRequest("carlos@correo.com");
        ReenviarVerificacionResponse response = new ReenviarVerificacionResponse(
                "Se ha enviado un nuevo enlace de verificación a su correo electrónico.",
                "carlos@correo.com"
        );

        when(authService.reenviarVerificacion(any(ReenviarVerificacionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("carlos@correo.com"));
    }
}
