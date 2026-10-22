package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.conferencista.CoincidenciaConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.CompletarInvitacionRequest;
import com.sigea.demosigea_backend.dto.conferencista.DatosRegistradosResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionCompletadaResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionPublicaResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitarConferencistaRequest;
import com.sigea.demosigea_backend.dto.conferencista.TrayectoriaResponse;
import com.sigea.demosigea_backend.exception.ConferencistaExistenteException;
import com.sigea.demosigea_backend.exception.ConferencistaYaAsociadoException;
import com.sigea.demosigea_backend.exception.InvitacionNoDisponibleException;
import com.sigea.demosigea_backend.exception.InvitacionPendienteException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoInvitacion;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.InvitacionConferencista;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.InvitacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PerfilConferencistaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Invitación de conferencistas nuevos por correo (HU-12).
 * <ul>
 *   <li>El administrador vincula a los conferencistas ya registrados desde la lista
 *       ({@link ConferencistaService#listarRegistrados}); para uno nuevo solo indica nombre y correo
 *       ({@link #invitar}).</li>
 *   <li>El invitado recibe un enlace vigente {@value #HORAS_VIGENCIA_POR_DEFECTO} horas y registra él
 *       mismo su documento, teléfono y trayectoria ({@link #completar}); al hacerlo queda vinculado al
 *       evento de inmediato (Criterio 1).</li>
 *   <li>Si el correo ya pertenece a un conferencista registrado no se envía invitación: se ofrece
 *       vincularlo (Criterio 3).</li>
 * </ul>
 * <p>
 * El envío del correo nunca hace fallar la operación ({@link EmailService} solo registra el error en el
 * log); por eso el administrador también recibe el enlace para copiarlo y enviarlo por otro medio.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitacionConferencistaService {

    static final String ENTIDAD_INVITACION = "invitaciones_conferencista";

    /** Vigencia del enlace si no se configura {@code app.conferencistas.invitacion-horas} (3 días). */
    static final long HORAS_VIGENCIA_POR_DEFECTO = 72;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final InvitacionConferencistaRepository invitacionRepository;
    private final PersonaRepository personaRepository;
    private final PerfilConferencistaRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConferencistaService conferencistaService;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;

    @Value("${app.conferencistas.invitacion-horas:72}")
    private long horasVigencia = HORAS_VIGENCIA_POR_DEFECTO;

    // ------------------------------------------------------------------
    // Administrador
    // ------------------------------------------------------------------

    /**
     * Invitaciones pendientes del evento (vigentes y vencidas), con su enlace.
     *
     * @param eventoId ID del evento.
     */
    @Transactional(readOnly = true)
    public List<InvitacionResponse> listarPendientes(Long eventoId) {
        conferencistaService.buscarEvento(eventoId);
        LocalDateTime ahora = LocalDateTime.now();
        return invitacionRepository
                .findByEvento_IdAndEstadoOrderByFechaEnvioDescIdDesc(eventoId, EstadoInvitacion.pendiente)
                .stream()
                .map(i -> respuesta(i, ahora))
                .toList();
    }

    /**
     * Invita a un conferencista nuevo: guarda la invitación y le envía el enlace por correo.
     * Si ya había una invitación vencida para ese correo en el evento, se renueva.
     *
     * @param eventoId ID del evento (en configuración o habilitado).
     * @param request  Nombre y correo.
     * @return La invitación con su enlace.
     * @throws ConferencistaExistenteException  si el correo ya es de un conferencista registrado (Criterio 3).
     * @throws ConferencistaYaAsociadoException si esa persona ya está en el evento.
     * @throws InvitacionPendienteException     si ya hay una invitación vigente para ese correo.
     */
    @Transactional
    public InvitacionResponse invitar(Long eventoId, InvitarConferencistaRequest request) {
        Evento evento = conferencistaService.buscarEvento(eventoId);
        conferencistaService.validarAsociacionModificable(evento);
        String correo = ConferencistaService.normalizarCorreo(request.correo());
        String nombre = request.nombre().trim().replaceAll("\\s+", " ");

        Optional<CoincidenciaConferencistaResponse> coincidencia =
                conferencistaService.coincidencia(null, correo, eventoId);
        if (coincidencia.isPresent()) {
            CoincidenciaConferencistaResponse c = coincidencia.get();
            if (Boolean.TRUE.equals(c.yaAsociadoAlEvento())) {
                throw new ConferencistaYaAsociadoException(String.format(
                        "%s ya es conferencista del evento '%s'.", c.nombreCompleto(), evento.getNombre()));
            }
            if (c.tienePerfil()) {
                throw new ConferencistaExistenteException(ConferencistaService.mensajeExistente(c), c);
            }
            // Existe la persona (p. ej. un participante con cuenta) pero sin trayectoria: se le invita a completarla.
        }

        LocalDateTime ahora = LocalDateTime.now();
        Optional<InvitacionConferencista> pendiente = invitacionRepository
                .findFirstByEvento_IdAndCorreoIgnoreCaseAndEstado(eventoId, correo, EstadoInvitacion.pendiente);
        if (pendiente.isPresent() && pendiente.get().estaVigente(ahora)) {
            throw new InvitacionPendienteException(String.format(
                    "Ya se envió una invitación a %s que sigue vigente hasta el %s. Puedes reenviarla desde la lista "
                            + "de invitaciones pendientes.", correo, pendiente.get().getFechaExpiracion().toLocalDate()));
        }

        InvitacionConferencista invitacion = pendiente.orElseGet(() -> InvitacionConferencista.builder()
                .evento(evento)
                .correo(correo)
                .invitadoPor(usuarioAutenticado().orElse(null))
                .build());
        invitacion.setNombre(nombre);
        invitacion.renovar(generarToken(), ahora, horasVigencia);
        invitacion = invitacionRepository.save(invitacion);

        enviarCorreo(invitacion, evento);
        auditoriaService.registrar(TipoOperacionAuditoria.CONFERENCISTA_INVITADO, ENTIDAD_INVITACION, invitacion.getId(),
                DetalleAuditoria.de("evento", evento.getNombre(), "nombre", nombre, "correo", correo,
                        "renovada", pendiente.isPresent(), "vence", invitacion.getFechaExpiracion()));
        log.info("Invitación {} enviada a {} para el evento {}", invitacion.getId(), correo, eventoId);
        return respuesta(invitacion, ahora);
    }

    /**
     * Reenvía la invitación con un enlace nuevo (el anterior deja de servir) y una vigencia nueva.
     *
     * @param eventoId     ID del evento.
     * @param invitacionId ID de la invitación pendiente.
     */
    @Transactional
    public InvitacionResponse reenviar(Long eventoId, Long invitacionId) {
        InvitacionConferencista invitacion = buscarPendiente(eventoId, invitacionId);
        Evento evento = invitacion.getEvento();
        conferencistaService.validarAsociacionModificable(evento);
        LocalDateTime ahora = LocalDateTime.now();
        invitacion.renovar(generarToken(), ahora, horasVigencia);
        invitacion = invitacionRepository.save(invitacion);

        enviarCorreo(invitacion, evento);
        auditoriaService.registrar(TipoOperacionAuditoria.CONFERENCISTA_INVITADO, ENTIDAD_INVITACION, invitacion.getId(),
                DetalleAuditoria.de("evento", evento.getNombre(), "nombre", invitacion.getNombre(),
                        "correo", invitacion.getCorreo(), "reenvio", true, "vence", invitacion.getFechaExpiracion()));
        return respuesta(invitacion, ahora);
    }

    /**
     * Cancela una invitación pendiente; su enlace deja de servir.
     *
     * @param eventoId     ID del evento.
     * @param invitacionId ID de la invitación pendiente.
     */
    @Transactional
    public void cancelar(Long eventoId, Long invitacionId) {
        InvitacionConferencista invitacion = buscarPendiente(eventoId, invitacionId);
        invitacion.setEstado(EstadoInvitacion.cancelada);
        invitacionRepository.save(invitacion);
        auditoriaService.registrar(TipoOperacionAuditoria.INVITACION_CONFERENCISTA_CANCELADA, ENTIDAD_INVITACION,
                invitacion.getId(), DetalleAuditoria.de("evento", invitacion.getEvento().getNombre(),
                        "nombre", invitacion.getNombre(), "correo", invitacion.getCorreo()));
    }

    // ------------------------------------------------------------------
    // Conferencista invitado (público, con el token del enlace)
    // ------------------------------------------------------------------

    /**
     * Datos de la invitación para el formulario del conferencista. Si la persona ya existe en SIGEA,
     * se devuelven sus datos para precargarlo; el documento y el teléfono van enmascarados porque el
     * enlace no exige sesión.
     *
     * @param token Token del enlace.
     * @throws RecursoNoEncontradoException    si el enlace no existe.
     * @throws InvitacionNoDisponibleException si venció, ya se usó, se canceló o el evento ya no recibe conferencistas.
     */
    @Transactional(readOnly = true)
    public InvitacionPublicaResponse consultar(String token) {
        InvitacionConferencista invitacion = invitacionUtilizable(token);
        Evento evento = invitacion.getEvento();
        DatosRegistradosResponse datos = personaRepository.findByCorreoIgnoreCase(invitacion.getCorreo())
                .map(p -> new DatosRegistradosResponse(
                        p.getTipoDocumento(),
                        DatosRegistradosResponse.enmascarar(p.getNumeroDocumento()),
                        p.getNombres(),
                        p.getApellidos(),
                        DatosRegistradosResponse.enmascarar(p.getTelefono()),
                        TrayectoriaResponse.de(perfilRepository.findByPersona_Id(p.getId()).orElse(null))))
                .orElse(null);
        return new InvitacionPublicaResponse(evento.getId(), evento.getNombre(), evento.getSemestre(),
                evento.getFechaInicio(), evento.getFechaFin(), invitacion.getNombre(), invitacion.getCorreo(),
                invitacion.getFechaExpiracion(), datos);
    }

    /**
     * El conferencista registra sus datos y trayectoria desde el enlace y queda vinculado al evento.
     *
     * @param token   Token del enlace.
     * @param request Datos personales, tema y trayectoria.
     * @return Confirmación con el evento.
     */
    @Transactional
    public InvitacionCompletadaResponse completar(String token, CompletarInvitacionRequest request) {
        InvitacionConferencista invitacion = invitacionUtilizable(token);
        Evento evento = invitacion.getEvento();
        ParticipacionConferencista participacion = conferencistaService.registrarDesdeInvitacion(
                evento, invitacion.getCorreo(), invitacion.getTipoParticipacion(), request);
        Persona persona = participacion.getPersona();
        invitacion.completar(persona, LocalDateTime.now());
        invitacionRepository.save(invitacion);
        log.info("Invitación {} completada: persona {}, evento {}", invitacion.getId(), persona.getId(), evento.getId());
        return new InvitacionCompletadaResponse(evento.getId(), evento.getNombre(),
                ConferencistaService.nombreCompleto(persona), ConferencistaService.esPublico(evento));
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /** Invitación del enlace, solo si todavía se puede usar. */
    private InvitacionConferencista invitacionUtilizable(String token) {
        if (ConferencistaService.estaVacio(token)) {
            throw new RecursoNoEncontradoException("El enlace de invitación no es válido.");
        }
        InvitacionConferencista invitacion = invitacionRepository.findByToken(token.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El enlace de invitación no es válido. Revisa que lo hayas copiado completo o pide uno nuevo "
                                + "al organizador del evento."));
        switch (invitacion.getEstado()) {
            case completada -> throw new InvitacionNoDisponibleException(InvitacionNoDisponibleException.COMPLETADA,
                    "Esta invitación ya fue utilizada: tus datos ya están registrados.");
            case cancelada -> throw new InvitacionNoDisponibleException(InvitacionNoDisponibleException.CANCELADA,
                    "El organizador canceló esta invitación.");
            default -> {
                // pendiente: se revisa la vigencia
            }
        }
        if (invitacion.estaVencida(LocalDateTime.now())) {
            throw new InvitacionNoDisponibleException(InvitacionNoDisponibleException.VENCIDA,
                    "Este enlace venció el " + invitacion.getFechaExpiracion().toLocalDate()
                            + ". Pide al organizador del evento que te reenvíe la invitación.");
        }
        if (!ConferencistaService.ESTADOS_ASOCIACION_MODIFICABLE.contains(invitacion.getEvento().getEstado())) {
            throw new InvitacionNoDisponibleException(InvitacionNoDisponibleException.EVENTO_CERRADO,
                    "El evento '" + invitacion.getEvento().getNombre() + "' ya no recibe conferencistas. "
                            + "Comunícate con el organizador.");
        }
        return invitacion;
    }

    private InvitacionConferencista buscarPendiente(Long eventoId, Long invitacionId) {
        InvitacionConferencista invitacion = invitacionRepository.findByIdAndEvento_Id(invitacionId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "No se encontró la invitación %d en el evento %d.", invitacionId, eventoId)));
        if (invitacion.getEstado() != EstadoInvitacion.pendiente) {
            throw new OperacionNoPermitidaException("La invitación ya no está pendiente (estado: "
                    + invitacion.getEstado() + ").");
        }
        return invitacion;
    }

    private void enviarCorreo(InvitacionConferencista invitacion, Evento evento) {
        emailService.enviarInvitacionConferencista(invitacion.getCorreo(), invitacion.getNombre(), evento.getNombre(),
                emailService.enlaceInvitacionConferencista(invitacion.getToken()), invitacion.getFechaExpiracion());
    }

    private InvitacionResponse respuesta(InvitacionConferencista i, LocalDateTime ahora) {
        return new InvitacionResponse(
                i.getId(),
                i.getEvento().getId(),
                i.getNombre(),
                i.getCorreo(),
                i.estaVencida(ahora) ? "vencida" : "pendiente",
                i.getFechaEnvio(),
                i.getFechaExpiracion(),
                emailService.enlaceInvitacionConferencista(i.getToken()));
    }

    /** 32 bytes aleatorios en Base64 URL (43 caracteres), imposibles de adivinar. */
    static String generarToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Optional<Usuario> usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName());
    }

    /** Para pruebas: cambia la vigencia del enlace. */
    void setHorasVigencia(long horas) {
        this.horasVigencia = horas;
    }

    /** Vigencia configurada del enlace en horas. */
    long getHorasVigencia() {
        return horasVigencia;
    }
}
