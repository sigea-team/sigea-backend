package com.sigea.demosigea_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de la gestión y envío de correos electrónicos en la plataforma SIGEA.
 * <p>
 * Proporciona mecanismos para notificaciones transaccionales como la verificación de cuenta
 * mediante plantillas HTML. Incluye un mecanismo de tolerancia a fallos (*fallback*)
 * registrando los datos en los logs del sistema si el servidor SMTP no está disponible.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
public class EmailService {

    /**
     * Emisor de mensajes de correo electrónico mediante el protocolo SMTP.
     * Configurado como opcional para permitir la ejecución de la aplicación sin un servidor de correo.
     */
    private final JavaMailSender mailSender;

    /**
     * Dirección de correo electrónico utilizada como remitente en las notificaciones enviadas.
     */
    @Value("${app.mail.from:no-reply@sigea.com}")
    private String remitente;

    /**
     * URL base del cliente o servidor utilizada para construir enlaces dentro de los correos.
     */
    @Value("${app.mail.frontend-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Ruta del *endpoint* para la verificación de correo electrónico.
     */
    @Value("${app.mail.verification-path:/api/v1/auth/verify-email}")
    private String verificationPath;

    /**
     * Ruta a la que apunta el enlace de recuperación de contraseña (HU-32).
     * <p>
     * A diferencia de la verificación de correo (que resuelve directo contra un
     * GET del backend), restablecer la contraseña requiere que el usuario escriba
     * una nueva clave, así que este enlace debe apuntar a una PÁGINA del frontend
     * (no a un endpoint del backend). Por ahora, mientras no exista ese frontend,
     * queda apuntando a {@code app.mail.frontend-url + reset-password-path} igual
     * que el resto de enlaces de este servicio; hay que actualizar
     * {@code app.mail.frontend-url} cuando el frontend real esté desplegado.
     * </p>
     */
    @Value("${app.mail.reset-password-path:/reset-password}")
    private String resetPasswordPath;

    /**
     * URL base del FRONTEND (React), distinta de {@code baseUrl} porque esa
     * apunta al backend (usada para el link GET de verificación). El enlace
     * de recuperación sí debe abrir una página del frontend.
     */
    @Value("${app.mail.reset-password-base-url:http://localhost:5173}")
    private String resetPasswordBaseUrl;

    /**
     * Horas de vigencia del token de recuperación, solo para mostrarlas en el
     * texto del correo (el valor real de expiración lo calcula {@code AuthService}
     * al crear el token; ver {@code app.mail.reset-token-expiration-hours}).
     */
    @Value("${app.mail.reset-token-expiration-hours:1}")
    private int resetTokenExpirationHours;

    /**
     * Construye una nueva instancia del servicio inyectando opcionalmente el cliente SMTP.
     *
     * @param mailSender Instancia de {@link JavaMailSender} para la gestión SMTP, puede ser {@code null}.
     */
    @Autowired
    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo electrónico con formato HTML para la activación de la cuenta de un nuevo usuario.
     * <p>
     * Construye la URL de confirmación adjuntando el token generado y despacha el mensaje.
     * En caso de fallo o ausencia de configuración SMTP, el enlace se imprime en los logs.
     * </p>
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

        if (mailSender == null) {
            log.warn("JavaMailSender no está configurado en el contexto. El correo se registró en log.");
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente, "SIGEA - Notificaciones");
            helper.setTo(destinatario);
            helper.setSubject("SIGEA - Verificación de Cuenta");

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

            helper.setText(contenidoHtml, true);
            mailSender.send(mensaje);
            log.info("✅ Correo de verificación enviado exitosamente a {}", destinatario);

        } catch (MessagingException ex) {
            log.warn("⚠️ No se pudo enviar el correo de verificación vía SMTP a {}: {}. El enlace está disponible en consola.", destinatario, ex.getMessage());
        } catch (Exception ex) {
            log.warn("⚠️ Error al intentar conectar con el servidor SMTP: {}. El usuario fue registrado y puede verificar con el token.", ex.getMessage());
        }
    }

    /**
     * Envía el enlace de recuperación de contraseña (HU-32, Criterio 1).
     * <p>
     * Igual que {@link #enviarCorreoVerificacion}, si no hay un {@link JavaMailSender}
     * configurado o falla el envío SMTP, el enlace queda disponible en los logs para
     * poder probar el flujo completo en desarrollo.
     * </p>
     *
     * @param destinatario Correo electrónico del usuario.
     * @param nombre       Nombre del destinatario, para personalizar el mensaje.
     * @param token        Token de recuperación (tipo {@code recuperacion} en {@code tokens_recuperacion}).
     */
    public void enviarCorreoRecuperacion(String destinatario, String nombre, String token) {
        String enlaceRecuperacion = resetPasswordBaseUrl + resetPasswordPath + "?token=" + token;

        log.info("================================================================================");
        log.info("🔑 [CORREO DE RECUPERACIÓN DE CONTRASEÑA]");
        log.info("Destinatario: {} ({})", nombre, destinatario);
        log.info("Token: {}", token);
        log.info("Enlace de Recuperación: {}", enlaceRecuperacion);
        log.info("================================================================================");

        if (mailSender == null) {
            log.warn("JavaMailSender no está configurado en el contexto. El enlace se registró en log.");
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente, "SIGEA - Notificaciones");
            helper.setTo(destinatario);
            helper.setSubject("SIGEA - Recuperación de contraseña");

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

            helper.setText(contenidoHtml, true);
            mailSender.send(mensaje);
            log.info("✅ Correo de recuperación enviado exitosamente a {}", destinatario);

        } catch (MessagingException ex) {
            log.warn("⚠️ No se pudo enviar el correo de recuperación vía SMTP a {}: {}. El enlace está disponible en consola.", destinatario, ex.getMessage());
        } catch (Exception ex) {
            log.warn("⚠️ Error al intentar conectar con el servidor SMTP: {}. El enlace está disponible en consola.", ex.getMessage());
        }
    }

    /**
     * Notifica por correo que la contraseña de la cuenta acaba de cambiar
     * (HU-32, Criterio 2). Es una notificación informativa, sin enlace ni token:
     * si el usuario no reconoce el cambio, sabe que debe actuar de inmediato.
     *
     * @param destinatario Correo electrónico del usuario.
     * @param nombre       Nombre del destinatario, para personalizar el mensaje.
     */
    public void enviarNotificacionCambioContrasena(String destinatario, String nombre) {
        log.info("================================================================================");
        log.info("🔒 [NOTIFICACIÓN DE CAMBIO DE CONTRASEÑA]");
        log.info("Destinatario: {} ({})", nombre, destinatario);
        log.info("================================================================================");

        if (mailSender == null) {
            log.warn("JavaMailSender no está configurado en el contexto. La notificación se registró en log.");
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente, "SIGEA - Notificaciones");
            helper.setTo(destinatario);
            helper.setSubject("SIGEA - Tu contraseña fue actualizada");

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

            helper.setText(contenidoHtml, true);
            mailSender.send(mensaje);
            log.info("✅ Notificación de cambio de contraseña enviada exitosamente a {}", destinatario);

        } catch (MessagingException ex) {
            log.warn("⚠️️ No se pudo enviar la notificación de cambio de contraseña vía SMTP a {}: {}.", destinatario, ex.getMessage());
        } catch (Exception ex) {
            log.warn("⚠️ Error al intentar conectar con el servidor SMTP: {}.", ex.getMessage());
        }
    }
}