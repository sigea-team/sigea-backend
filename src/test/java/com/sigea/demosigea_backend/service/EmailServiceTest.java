package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.brevo.BrevoSendEmailRequest;
import com.sigea.demosigea_backend.dto.brevo.BrevoSendEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "brevoApiUrl", "https://api.brevo.com/v3/smtp/email");
        ReflectionTestUtils.setField(emailService, "remitente", "sigeaufps@gmail.com");
        ReflectionTestUtils.setField(emailService, "senderName", "SIGEA - Notificaciones");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(emailService, "verificationPath", "/api/v1/auth/verify-email");
        ReflectionTestUtils.setField(emailService, "resetPasswordPath", "/reset-password");
        ReflectionTestUtils.setField(emailService, "resetPasswordBaseUrl", "http://localhost:5173/#");
        ReflectionTestUtils.setField(emailService, "resetTokenExpirationHours", 1);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(BrevoSendEmailResponse.class)).thenReturn(new BrevoSendEmailResponse("msg-test-id"));
    }

    @Test
    @DisplayName("Debe ejecutar el envio de correo de verificacion sin lanzar excepciones")
    void enviarCorreoVerificacion_exito() {
        assertDoesNotThrow(() ->
                emailService.enviarCorreoVerificacion("usuario@ejemplo.com", "Juan Perez", "token-123")
        );
    }

    @Test
    @DisplayName("Debe ejecutar el envio de correo de recuperacion sin lanzar excepciones")
    void enviarCorreoRecuperacion_exito() {
        assertDoesNotThrow(() ->
                emailService.enviarCorreoRecuperacion("usuario@ejemplo.com", "Juan Perez", "token-456")
        );
    }

    @Test
    @DisplayName("Debe ejecutar el envio de notificacion de cambio de contrasena sin lanzar excepciones")
    void enviarNotificacionCambioContrasena_exito() {
        assertDoesNotThrow(() ->
                emailService.enviarNotificacionCambioContrasena("usuario@ejemplo.com", "Juan Perez")
        );
    }

    @Test
    @DisplayName("Debe registrar en logs sin fallar cuando no hay API Key")
    void enviarCorreo_sinApiKey() {
        ReflectionTestUtils.setField(emailService, "apiKey", "");

        assertDoesNotThrow(() ->
                emailService.enviarCorreoVerificacion("usuario@ejemplo.com", "Juan Perez", "token-789")
        );
    }

    @Test
    @DisplayName("HU-12: el enlace de invitación usa la URL del frontend y la ruta de invitación")
    void enlaceInvitacionConferencista_formato() {
        assertEquals("http://localhost:5173/#/conferencista/invitacion?token=tok-1",
                emailService.enlaceInvitacionConferencista("tok-1"));
    }

    @Test
    @DisplayName("HU-12: si la URL base del front se configura sin '#', el enlace igual queda con /#/ para el HashRouter")
    void enlaceInvitacionConferencista_baseSinHash() {
        ReflectionTestUtils.setField(emailService, "resetPasswordBaseUrl", "https://sigea-team.github.io/sigea-front/");
        assertEquals("https://sigea-team.github.io/sigea-front/#/conferencista/invitacion?token=tok-3",
                emailService.enlaceInvitacionConferencista("tok-3"));
    }

    @Test
    @DisplayName("HU-12: debe ejecutar el envío de la invitación a conferencista sin lanzar excepciones")
    void enviarInvitacionConferencista_exito() {
        assertDoesNotThrow(() -> emailService.enviarInvitacionConferencista("conf@ejemplo.com", "Ana Pérez",
                "Congreso de Ingeniería de Sistemas", emailService.enlaceInvitacionConferencista("tok-2"),
                java.time.LocalDateTime.of(2026, 10, 22, 9, 30)));
    }
    @Test
    @DisplayName("HU-24: envía el código QR con la imagen adjunta en Base64 y el código en el texto")
    void enviarCodigoQrInscripcion_conAdjunto() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};

        boolean enviado = emailService.enviarCodigoQrInscripcion("maria@ejemplo.com", "María Pérez",
                "Semana de Ingeniería", "SIGEA-abc123", png, false);

        assertTrue(enviado);
        ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
        verify(requestBodySpec).body(cuerpo.capture());
        BrevoSendEmailRequest request =
                (BrevoSendEmailRequest) cuerpo.getValue();
        assertEquals(1, request.attachment().size());
        assertEquals(Base64.getEncoder().encodeToString(png), request.attachment().get(0).content());
        assertEquals("codigo-qr-sigea.png", request.attachment().get(0).name());
        assertTrue(request.htmlContent().contains("SIGEA-abc123"));
        assertTrue(request.subject().contains("Semana de Ingeniería"));
    }

    @Test
    @DisplayName("HU-24: el reenvío cambia el asunto pero conserva el código")
    void enviarCodigoQrInscripcion_reenvio() {
        emailService.enviarCodigoQrInscripcion("maria@ejemplo.com", "María Pérez", "Semana de Ingeniería",
                "SIGEA-abc123", null, true);

        ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
        verify(requestBodySpec).body(cuerpo.capture());
        BrevoSendEmailRequest request =
                (BrevoSendEmailRequest) cuerpo.getValue();
        assertTrue(request.subject().startsWith("SIGEA - Reenvío"));
        assertTrue(request.htmlContent().contains("SIGEA-abc123"));
        assertNull(request.attachment());
    }

    @Test
    @DisplayName("HU-24: sin API Key el envío del QR devuelve false")
    void enviarCodigoQrInscripcion_sinApiKey() {
        ReflectionTestUtils.setField(emailService, "apiKey", "");

        assertFalse(emailService.enviarCodigoQrInscripcion("maria@ejemplo.com", "María Pérez", "Evento",
                "SIGEA-abc123", null, false));
    }
}
