package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.asistencia.CodigoQrResponse;
import com.sigea.demosigea_backend.dto.asistencia.ReenvioQrResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.CodigoQr;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.CodigoQrRepository;
import com.sigea.demosigea_backend.repository.InscripcionEventoRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Generación, consulta y reenvío del código QR de cada inscripción al evento (HU-24, RF44).
 * <ul>
 *   <li>Criterio 1: al confirmarse la inscripción se genera un código único y se envía por correo.</li>
 *   <li>Criterio 4: el reenvío manda el mismo código; nunca se genera uno distinto.</li>
 * </ul>
 * El código es aleatorio (128 bits de {@link SecureRandom}) y no contiene datos personales: el QR solo
 * sirve si el sistema lo encuentra en {@code codigos_qr}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodigoQrService {

    /** Prefijo que identifica los códigos de SIGEA al leerlos en el escáner. */
    public static final String PREFIJO = "SIGEA-";

    private static final String ENTIDAD = "codigos_qr";
    private static final int BYTES_ALEATORIOS = 16;
    private static final int INTENTOS_GENERACION = 5;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final CodigoQrRepository codigoQrRepository;
    private final InscripcionEventoRepository inscripcionEventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final GeneradorImagenQr generadorImagenQr;
    private final AuditoriaService auditoriaService;

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 1: generación al confirmar la inscripción
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Genera el código de una inscripción recién confirmada y programa su envío por correo para después
     * del commit (si la transacción se revierte, no se envía nada). Si la inscripción ya tenía código,
     * se conserva.
     *
     * @param inscripcion inscripción ya guardada.
     * @return el código de la inscripción.
     */
    @Transactional
    public String generarAlConfirmarInscripcion(InscripcionEvento inscripcion) {
        CodigoQr qr = obtenerOCrear(inscripcion);
        Persona persona = inscripcion.getPersona();
        if (persona != null && persona.getCorreo() != null && !persona.getCorreo().isBlank()) {
            String correo = persona.getCorreo();
            String nombre = nombreDe(persona);
            String eventoNombre = inscripcion.getEvento() != null ? inscripcion.getEvento().getNombre() : "";
            String codigo = qr.getCodigo();
            despuesDelCommit(() -> enviarCorreo(correo, nombre, eventoNombre, codigo, false));
        }
        return qr.getCodigo();
    }

    /**
     * Devuelve el código de la inscripción y lo crea si no existe (inscripciones anteriores a HU-24 que
     * no pasaron por el changeset de relleno).
     */
    @Transactional
    public CodigoQr obtenerOCrear(InscripcionEvento inscripcion) {
        if (inscripcion == null || inscripcion.getId() == null) {
            throw new IllegalArgumentException("La inscripción debe estar guardada para generar su código QR.");
        }
        return codigoQrRepository.findByInscripcion_Id(inscripcion.getId())
                .orElseGet(() -> crear(inscripcion));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Consulta del participante y del organizador
    // ──────────────────────────────────────────────────────────────────────────────

    /** Código QR de la inscripción del usuario autenticado en el evento. */
    @Transactional
    public CodigoQrResponse consultarMiCodigo(Long eventoId) {
        InscripcionEvento inscripcion = miInscripcion(eventoId);
        return CodigoQrResponse.fromEntity(obtenerOCrear(inscripcion));
    }

    /** Imagen PNG del código QR del usuario autenticado en el evento. */
    @Transactional
    public byte[] imagenMiCodigo(Long eventoId, int tamano) {
        InscripcionEvento inscripcion = miInscripcion(eventoId);
        return generadorImagenQr.generarPng(obtenerOCrear(inscripcion).getCodigo(), tamano);
    }

    /** Código QR de una inscripción del evento (organizador). */
    @Transactional
    public CodigoQrResponse consultarCodigo(Long eventoId, Long inscripcionId) {
        InscripcionEvento inscripcion = inscripcionDelEvento(eventoId, inscripcionId);
        return CodigoQrResponse.fromEntity(obtenerOCrear(inscripcion));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 4: reenvío sin generar un código distinto
    // ──────────────────────────────────────────────────────────────────────────────

    /** El participante autenticado pide que se le reenvíe su QR. */
    @Transactional
    public ReenvioQrResponse reenviarMiCodigo(Long eventoId) {
        return reenviar(miInscripcion(eventoId), "participante");
    }

    /** El organizador reenvía el QR de una inscripción del evento (por ejemplo, desde la mesa de registro). */
    @Transactional
    public ReenvioQrResponse reenviarCodigo(Long eventoId, Long inscripcionId) {
        return reenviar(inscripcionDelEvento(eventoId, inscripcionId), "organizador");
    }

    private ReenvioQrResponse reenviar(InscripcionEvento inscripcion, String solicitadoPor) {
        Persona persona = inscripcion.getPersona();
        if (persona == null || persona.getCorreo() == null || persona.getCorreo().isBlank()) {
            throw new OperacionNoPermitidaException(
                    "El participante no tiene un correo registrado al cual reenviar el código QR.");
        }
        CodigoQr qr = obtenerOCrear(inscripcion);
        String eventoNombre = inscripcion.getEvento() != null ? inscripcion.getEvento().getNombre() : "";

        boolean enviado = enviarCorreo(persona.getCorreo(), nombreDe(persona), eventoNombre,
                qr.getCodigo(), true);

        auditoriaService.registrar(TipoOperacionAuditoria.CODIGO_QR_REENVIADO, ENTIDAD, qr.getId(),
                DetalleAuditoria.de(
                        "inscripcionId", inscripcion.getId(),
                        "eventoId", inscripcion.getEvento() != null ? inscripcion.getEvento().getId() : null,
                        "personaId", persona.getId(),
                        "solicitadoPor", solicitadoPor,
                        "enviado", enviado
                ));

        String mensaje = enviado
                ? "Se reenvió el código QR a " + persona.getCorreo() + ". Es el mismo código de la inscripción."
                : "No se pudo enviar el correo en este momento. El código QR no cambió; puede consultarlo en su inscripción.";
        return new ReenvioQrResponse(inscripcion.getId(), persona.getCorreo(), qr.getCodigo(), enviado,
                LocalDateTime.now(), mensaje);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Auxiliares
    // ──────────────────────────────────────────────────────────────────────────────

    private CodigoQr crear(InscripcionEvento inscripcion) {
        CodigoQr qr = CodigoQr.builder()
                .inscripcion(inscripcion)
                .codigo(nuevoCodigoUnico())
                .fechaGeneracion(LocalDateTime.now())
                .vigente(Boolean.TRUE)
                .build();
        CodigoQr guardado = codigoQrRepository.save(qr);
        log.info("Código QR {} generado para la inscripción {}", guardado.getId(), inscripcion.getId());
        return guardado;
    }

    /** Código aleatorio que no esté en uso. La restricción UNIQUE (codigo) de la BD es la última garantía. */
    String nuevoCodigoUnico() {
        for (int intento = 0; intento < INTENTOS_GENERACION; intento++) {
            String candidato = generarCodigo();
            if (!codigoQrRepository.existsByCodigo(candidato)) {
                return candidato;
            }
        }
        throw new IllegalStateException("No se pudo generar un código QR único. Intente de nuevo.");
    }

    static String generarCodigo() {
        byte[] bytes = new byte[BYTES_ALEATORIOS];
        ALEATORIO.nextBytes(bytes);
        return PREFIJO + HexFormat.of().formatHex(bytes);
    }

    private boolean enviarCorreo(String correo, String nombre, String eventoNombre, String codigo, boolean reenvio) {
        byte[] imagen = null;
        try {
            imagen = generadorImagenQr.generarPng(codigo);
        } catch (RuntimeException ex) {
            log.error("No se pudo generar la imagen del QR para {}: {}. Se envía solo el código en texto.", correo, ex.getMessage());
        }
        try {
            return emailService.enviarCodigoQrInscripcion(correo, nombre, eventoNombre, codigo, imagen, reenvio);
        } catch (RuntimeException ex) {
            log.error("No se pudo enviar el código QR a {}: {}", correo, ex.getMessage());
            return false;
        }
    }

    private static void despuesDelCommit(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accion.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }

    private InscripcionEvento miInscripcion(Long eventoId) {
        Persona persona = personaAutenticada();
        return inscripcionEventoRepository.findByEvento_IdAndPersona_Id(eventoId, persona.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró una inscripción activa en este evento para el participante."));
    }

    private InscripcionEvento inscripcionDelEvento(Long eventoId, Long inscripcionId) {
        return inscripcionEventoRepository.findByIdAndEvento_IdConPersona(inscripcionId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la inscripción con ID " + inscripcionId + " en este evento."));
    }

    private Persona personaAutenticada() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debe iniciar sesión para consultar su código QR.");
        }
        Usuario usuario = usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("No se encontró el usuario autenticado."));
        if (usuario.getPersona() == null) {
            throw new OperacionNoPermitidaException("El usuario autenticado no tiene una persona asociada.");
        }
        return usuario.getPersona();
    }

    /** Nombre completo de una persona para los correos. */
    private static String nombreDe(Persona p) {
        String nombre = ((p.getNombres() != null ? p.getNombres() : "") + " "
                + (p.getApellidos() != null ? p.getApellidos() : "")).trim();
        return nombre.isEmpty() ? p.getCorreo() : nombre;
    }
}
