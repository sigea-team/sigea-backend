package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.brevo.BrevoEmailAddress;
import com.sigea.demosigea_backend.dto.brevo.BrevoSendEmailRequest;
import com.sigea.demosigea_backend.dto.brevo.BrevoSendEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Servicio encargado de la gestión y envío de correos electrónicos en la plataforma SIGEA
 * haciendo uso de la API REST v3 de Brevo.
 * <p>
 * Proporciona mecanismos para notificaciones transaccionales como la verificación de cuenta,
 * recuperación de contraseña y notificación de actualización de credenciales mediante plantillas HTML.
 * Incluye un mecanismo de tolerancia a fallos (fallback) registrando los datos en los logs del sistema
 * si la API Key no está configurada o si ocurre algún error durante el despacho del correo.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 2.0
 * @since 2026
 */
@Slf4j
@Service
public class EmailService {

    private final RestClient restClient;

    /**
     * API Key de autenticación para los servicios REST de Brevo.
     */
    @Value("${brevo.api.key:}")
    private String apiKey;

    /**
     * Endpoint para el envío de correos transaccionales en Brevo API.
     */
    @Value("${brevo.api.url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    /**
     * Dirección de correo electrónico utilizada como remitente.
     */
    @Value("${app.mail.from:sigeaufps@gmail.com}")
    private String remitente;

    /**
     * Nombre descriptivo del remitente en el mensaje.
     */
    @Value("${app.mail.sender-name:SIGEA - Notificaciones}")
    private String senderName;

    /**
     * URL base del servidor backend utilizada para construir enlaces de verificación GET.
     */
    @Value("${app.mail.backend-url:${app.mail.frontend-url:http://localhost:8080}}")
    private String baseUrl;

    /**
     * Ruta del endpoint para la verificación de correo electrónico.
     */
    @Value("${app.mail.verification-path:/api/v1/auth/verify-email}")
    private String verificationPath;

    /**
     * Ruta del frontend para restablecer la contraseña.
     */
    @Value("${app.mail.reset-password-path:/reset-password}")
    private String resetPasswordPath;

    /**
     * URL base del frontend para restablecimiento de contraseña.
     */
    @Value("${app.mail.reset-password-base-url:http://localhost:5173/#}")
    private String resetPasswordBaseUrl;

    /**
     * Horas de vigencia del token de recuperación.
     */
    @Value("${app.mail.reset-token-expiration-hours:1}")
    private int resetTokenExpirationHours;

    public EmailService(RestClient restClient) {
        this.restClient = restClient != null ? restClient : RestClient.create();
    }

    public EmailService() {
        this(RestClient.create());
    }

    /**
     * Envía un correo electrónico con formato HTML para la activación de la cuenta de un nuevo usuario.
     *
     * @param destinatario Dirección de correo electrónico del usuario.
     * @param nombre       Nombre del destinatario para personalizar el mensaje.
     * @param token        Token único asociado al proceso de verificación.
     */
    public void enviarCorreoVerificacion(String destinatario, String nombre, String token) {
        String enlaceVerificacion = baseUrl + verificationPath + "?token=" + token;

        log.info("================================================================================");
        log.info("📧 [CORREO DE VERIFICACIÓN]");
        log.info("Destinatario: {} ({})", nombre, destinatario);
        log.info("Token: {}", token);
        log.info("Enlace de Verificación: {}", enlaceVerificacion);
        log.info("================================================================================");

        String contenidoHtml = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 10px; background-color: #ffffff;">
                    <h2 style="color: #2563eb; text-align: center; margin-bottom: 20px;">Bienvenido(a) a SIGEA</h2>
                    <p style="font-size: 16px; color: #334155;">Hola <strong>%s</strong>,</p>
                    <p style="font-size: 15px; color: #475569; line-height: 1.6;">
                        Gracias por registrarte en la plataforma de gestión de eventos académicos SIGEA.
                        Para activar tu cuenta y acceder a la plataforma, por favor confirma tu dirección de correo electrónico haciendo clic en el siguiente botón:
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block; font-size: 15px;">
                            Verificar mi correo electrónico
                        </a>
                    </div>
                    <p style="font-size: 13px; color: #64748b; line-height: 1.5;">
                        Si el botón no funciona, copia y pega el siguiente enlace en tu navegador web:<br/>
                        <a href="%s" style="color: #2563eb; word-break: break-all;">%s</a>
                    </p>
                    <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 25px 0;" />
                    <p style="font-size: 12px; color: #94a3b8; text-align: center;">
                        Este enlace expirará en 24 horas. Si no solicitaste esta cuenta, puedes ignorar este mensaje.
                    </p>
                </div>
                """.formatted(nombre, enlaceVerificacion, enlaceVerificacion, enlaceVerificacion);

        despacharCorreoBrevo(destinatario, nombre, "SIGEA - Verificación de Cuenta", contenidoHtml);
    }

    /**
     * Envía el enlace de recuperación de contraseña (HU-32, Criterio 1).
     *
     * @param destinatario Correo electrónico del usuario.
     * @param nombre       Nombre del destinatario, para personalizar el mensaje.
     * @param token        Token de recuperación.
     */
    public void enviarCorreoRecuperacion(String destinatario, String nombre, String token) {
        String enlaceRecuperacion = resetPasswordBaseUrl + resetPasswordPath + "?token=" + token;

        log.info("================================================================================");
        log.info("🔑 [CORREO DE RECUPERACIÓN DE CONTRASEÑA]");
        log.info("Destinatario: {} ({})", nombre, destinatario);
        log.info("Token: {}", token);
        log.info("Enlace de Recuperación: {}", enlaceRecuperacion);
        log.info("================================================================================");

        String contenidoHtml = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 10px; background-color: #ffffff;">
                    <h2 style="color: #2563eb; text-align: center; margin-bottom: 20px;">Recuperación de contraseña</h2>
                    <p style="font-size: 16px; color: #334155;">Hola <strong>%s</strong>,</p>
                    <p style="font-size: 15px; color: #475569; line-height: 1.6;">
                        Recibimos una solicitud para restablecer la contraseña de tu cuenta en SIGEA.
                        Haz clic en el siguiente botón para crear una nueva contraseña:
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block; font-size: 15px;">
                            Restablecer mi contraseña
                        </a>
                    </div>
                    <p style="font-size: 13px; color: #64748b; line-height: 1.5;">
                        Si el botón no funciona, copia y pega el siguiente enlace en tu navegador web:<br/>
                        <a href="%s" style="color: #2563eb; word-break: break-all;">%s</a>
                    </p>
                    <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 25px 0;" />
                    <p style="font-size: 12px; color: #94a3b8; text-align: center;">
                        Este enlace expirará en %d hora(s). Si tú no solicitaste este cambio, puedes ignorar este mensaje: tu contraseña actual seguirá funcionando.
                    </p>
                </div>
                """.formatted(nombre, enlaceRecuperacion, enlaceRecuperacion, enlaceRecuperacion, resetTokenExpirationHours);

        despacharCorreoBrevo(destinatario, nombre, "SIGEA - Recuperación de contraseña", contenidoHtml);
    }

    /**
     * Notifica por correo que la contraseña de la cuenta acaba de cambiar (HU-32, Criterio 2).
     *
     * @param destinatario Correo electrónico del usuario.
     * @param nombre       Nombre del destinatario, para personalizar el mensaje.
     */
    public void enviarNotificacionCambioContrasena(String destinatario, String nombre) {
        log.info("================================================================================");
        log.info("🔒 [NOTIFICACIÓN DE CAMBIO DE CONTRASEÑA]");
        log.info("Destinatario: {} ({})", nombre, destinatario);
        log.info("================================================================================");

        String contenidoHtml = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 10px; background-color: #ffffff;">
                    <h2 style="color: #2563eb; text-align: center; margin-bottom: 20px;">Tu contraseña fue actualizada</h2>
                    <p style="font-size: 16px; color: #334155;">Hola <strong>%s</strong>,</p>
                    <p style="font-size: 15px; color: #475569; line-height: 1.6;">
                        Te confirmamos que la contraseña de tu cuenta en SIGEA fue cambiada exitosamente.
                    </p>
                    <p style="font-size: 13px; color: #64748b; line-height: 1.5;">
                        Si tú no realizaste este cambio, contacta al administrador del sistema de inmediato.
                    </p>
                </div>
                """.formatted(nombre);

        despacharCorreoBrevo(destinatario, nombre, "SIGEA - Tu contraseña fue actualizada", contenidoHtml);
    }

    /**
     * Ejecuta la petición HTTP POST a la API REST de Brevo para realizar el envío del correo electrónico.
     */
    private void despacharCorreoBrevo(String destinatario, String nombreDestinatario, String asunto, String contenidoHtml) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ API Key de Brevo no configurada. El correo para {} se registró únicamente en los logs.", destinatario);
            return;
        }

        try {
            BrevoSendEmailRequest request = new BrevoSendEmailRequest(
                    new BrevoEmailAddress(remitente, senderName),
                    List.of(new BrevoEmailAddress(destinatario, nombreDestinatario)),
                    asunto,
                    contenidoHtml
            );

            BrevoSendEmailResponse response = restClient.post()
                    .uri(brevoApiUrl)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", apiKey)
                    .body(request)
                    .retrieve()
                    .body(BrevoSendEmailResponse.class);

            if (response != null && response.messageId() != null) {
                log.info("✅ Correo enviado exitosamente vía Brevo API a {}. MessageId: {}", destinatario, response.messageId());
            } else {
                log.warn("⚠️ Brevo API respondió sin messageId para el correo a {}", destinatario);
            }
        } catch (Exception ex) {
            log.error("❌ Error al intentar enviar el correo vía Brevo API a {}: {}. El enlace/notificación sigue registrado en los logs.", destinatario, ex.getMessage());
        }
    }
}