package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.inscripcion.CupoActividadResponse;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionActividadResponse;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionEventoRequest;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionEventoResponse;
import com.sigea.demosigea_backend.dto.inscripcion.MiInscripcionResponse;
import com.sigea.demosigea_backend.exception.ConflictoHorarioInscripcionException;
import com.sigea.demosigea_backend.exception.CupoAgotadoException;
import com.sigea.demosigea_backend.exception.InscripcionDuplicadaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.CodigoQr;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.InscripcionActividad;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.AsistenciaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.InscripcionActividadRepository;
import com.sigea.demosigea_backend.repository.InscripcionEventoRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Servicio de negocio para la gestión de inscripciones a eventos y actividades (HU-23).
 * HU-24: al confirmar la inscripción al evento se genera su código QR ({@link CodigoQrService}).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InscripcionService {

    private static final String ENTIDAD_EVENTO = "inscripciones_evento";
    private static final String ENTIDAD_ACTIVIDAD = "inscripciones_actividad";

    private static final Set<String> ROLES_ADMIN_O_GESTION = Set.of(
            "INSCRIPCIONES_GESTIONAR", "ROLE_ADMIN", "ADMIN"
    );

    private final InscripcionEventoRepository inscripcionEventoRepository;
    private final InscripcionActividadRepository inscripcionActividadRepository;
    private final EventoRepository eventoRepository;
    private final ActividadRepository actividadRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final CodigoQrService codigoQrService;
    private final AsistenciaRepository asistenciaRepository;

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 1 y Criterio 4: Inscripción al Evento
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Inscribe a un participante en el evento según su categoría (HU-23, Criterio 1).
     * Valida que las inscripciones estén abiertas y que no exista inscripción previa (Criterio 4).
     */
    @Transactional
    public InscripcionEventoResponse inscribirAEvento(Long eventoId, InscripcionEventoRequest request) {
        Evento evento = buscarEvento(eventoId);
        validarInscripcionesAbiertas(evento);

        Persona persona = resolverPersona(request.personaId());

        if (inscripcionEventoRepository.existsByEvento_IdAndPersona_Id(eventoId, persona.getId())) {
            throw new InscripcionDuplicadaException(
                    "El participante ya se encuentra inscrito previamente en el evento «" + evento.getNombre() + "».");
        }

        InscripcionEvento inscripcion = InscripcionEvento.builder()
                .evento(evento)
                .persona(persona)
                .categoriaParticipante(request.categoria())
                .fechaInscripcion(LocalDateTime.now())
                .build();

        InscripcionEvento guardada;
        try {
            guardada = inscripcionEventoRepository.save(inscripcion);
            inscripcionEventoRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new InscripcionDuplicadaException(
                    "El participante ya se encuentra inscrito previamente en el evento «" + evento.getNombre() + "».");
        }

        log.info("Inscripción {} registrada para persona {} en evento {} ({})",
                guardada.getId(), persona.getId(), eventoId, request.categoria());

        auditoriaService.registrar(TipoOperacionAuditoria.INSCRIPCION_EVENTO_REGISTRADA, ENTIDAD_EVENTO, guardada.getId(),
                DetalleAuditoria.de(
                        "eventoId", eventoId,
                        "eventoNombre", evento.getNombre(),
                        "personaId", persona.getId(),
                        "categoria", request.categoria().name()
                ));

        // HU-24, Criterio 1: código QR único de la inscripción, enviado por correo después del commit
        String codigoQr = codigoQrService.generarAlConfirmarInscripcion(guardada);

        return InscripcionEventoResponse.fromEntity(guardada, codigoQr);
    }

    /**
     * Consulta la inscripción del usuario autenticado en el evento junto con sus actividades inscritas y su código QR.
     */
    @Transactional
    public MiInscripcionResponse obtenerMiInscripcion(Long eventoId) {
        Persona persona = personaAutenticada();
        InscripcionEvento inscripcion = inscripcionEventoRepository.findByEvento_IdAndPersona_Id(eventoId, persona.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró una inscripción activa en este evento para el participante."));

        List<InscripcionActividad> actividades = inscripcionActividadRepository.findByInscripcion_Id(inscripcion.getId());
        List<InscripcionActividadResponse> actividadesResponse = actividades.stream()
                .map(InscripcionActividadResponse::fromEntity)
                .toList();

        // HU-24: el código se crea aquí si la inscripción es anterior a HU-24 (por eso la transacción no es de solo lectura)
        CodigoQr codigoQr = codigoQrService.obtenerOCrear(inscripcion);

        return MiInscripcionResponse.fromEntity(inscripcion, actividadesResponse,
                codigoQr != null ? codigoQr.getCodigo() : null);
    }

    /**
     * Cancela la inscripción del usuario autenticado al evento, liberando en cascada todas sus actividades (Criterio 5).
     */
    @Transactional
    public void cancelarMiInscripcionEvento(Long eventoId) {
        Persona persona = personaAutenticada();
        InscripcionEvento inscripcion = inscripcionEventoRepository.findByEvento_IdAndPersona_Id(eventoId, persona.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró una inscripción activa en este evento para el participante."));

        // HU-24: la asistencia registrada respalda certificados e indicadores; no se borra al cancelar
        if (asistenciaRepository.existsByInscripcion_Id(inscripcion.getId())) {
            throw new OperacionNoPermitidaException(
                    "No se puede cancelar la inscripción al evento porque ya tiene asistencia registrada en alguna actividad.");
        }

        Long inscripcionId = inscripcion.getId();
        inscripcionEventoRepository.delete(inscripcion);

        log.info("Inscripción al evento {} cancelada para persona {}", eventoId, persona.getId());

        auditoriaService.registrar(TipoOperacionAuditoria.INSCRIPCION_EVENTO_CANCELADA, ENTIDAD_EVENTO, inscripcionId,
                DetalleAuditoria.de("eventoId", eventoId, "personaId", persona.getId()));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterios 2, 3, 4 y 5: Inscripción a Actividades Puntuales
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Inscribe al participante en una actividad puntual (Criterio 2), validando:
     * - Estar previamente inscrito al evento.
     * - No superar el aforo de la sala (Criterio 2 y 4).
     * - No tener cruce o traslape de horario con otra actividad a la que esté inscrito (Criterio 3).
     * - No duplicar la inscripción en la misma actividad (Criterio 4).
     */
    @Transactional
    public InscripcionActividadResponse inscribirAActividad(Long eventoId, Long actividadId) {
        Persona persona = personaAutenticada();

        InscripcionEvento inscripcion = inscripcionEventoRepository.findByEvento_IdAndPersona_Id(eventoId, persona.getId())
                .orElseThrow(() -> new OperacionNoPermitidaException(
                        "Debe estar inscrito al evento antes de poder inscribirse a sus actividades."));

        // Lock transaccional con SELECT FOR UPDATE sobre la actividad para evitar condiciones de carrera en el aforo
        Actividad actividad = actividadRepository.findByIdAndEvento_IdParaInscripcion(actividadId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la actividad con ID " + actividadId + " en este evento."));

        if (actividad.estaPorProgramar() || actividad.estaCancelada() || actividad.getSala() == null) {
            throw new OperacionNoPermitidaException(
                    "La actividad «" + actividad.getNombre() + "» no está disponible para inscripción.");
        }

        if (inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(inscripcion.getId(), actividadId)) {
            throw new InscripcionDuplicadaException(
                    "El participante ya se encuentra inscrito previamente en la actividad «" + actividad.getNombre() + "».");
        }

        // Criterio 2 y 4: Validar aforo máximo
        int aforoMaximo = actividad.getSala().getAforoMaximo();
        long inscritosActuales = inscripcionActividadRepository.countByActividad_Id(actividadId);
        if (inscritosActuales >= aforoMaximo) {
            throw new CupoAgotadoException(
                    "La actividad «" + actividad.getNombre() + "» ha alcanzado su cupo máximo de " + aforoMaximo + " participantes.");
        }

        // Criterio 3: Validar horario traslapado
        validarConflictoHorario(persona.getId(), actividad);

        InscripcionActividad inscripcionActividad = InscripcionActividad.builder()
                .inscripcion(inscripcion)
                .actividad(actividad)
                .fechaInscripcion(LocalDateTime.now())
                .build();

        InscripcionActividad guardada;
        try {
            guardada = inscripcionActividadRepository.save(inscripcionActividad);
            inscripcionActividadRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new InscripcionDuplicadaException(
                    "El participante ya se encuentra inscrito previamente en la actividad «" + actividad.getNombre() + "».");
        }

        log.info("Inscripción {} registrada para participante {} en actividad {} (sala {})",
                guardada.getId(), persona.getId(), actividadId, actividad.getSala().getNombre());

        auditoriaService.registrar(TipoOperacionAuditoria.INSCRIPCION_ACTIVIDAD_REGISTRADA, ENTIDAD_ACTIVIDAD, guardada.getId(),
                DetalleAuditoria.de(
                        "eventoId", eventoId,
                        "actividadId", actividadId,
                        "actividadNombre", actividad.getNombre(),
                        "personaId", persona.getId()
                ));

        return InscripcionActividadResponse.fromEntity(guardada);
    }

    /**
     * Cancela la inscripción a una actividad puntual, liberando el cupo de forma inmediata (Criterio 5).
     */
    @Transactional
    public void cancelarInscripcionActividad(Long eventoId, Long actividadId) {
        Persona persona = personaAutenticada();

        InscripcionEvento inscripcion = inscripcionEventoRepository.findByEvento_IdAndPersona_Id(eventoId, persona.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró su inscripción en el evento."));

        InscripcionActividad ia = inscripcionActividadRepository.findByInscripcion_IdAndActividad_Id(inscripcion.getId(), actividadId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encuentra inscrito en la actividad seleccionada."));

        // HU-24: no se cancela una actividad a la que el participante ya asistió
        if (asistenciaRepository.existsByInscripcion_IdAndActividad_Id(inscripcion.getId(), actividadId)) {
            throw new OperacionNoPermitidaException(
                    "No se puede cancelar la inscripción a la actividad porque ya tiene la asistencia registrada.");
        }

        Long iaId = ia.getId();
        inscripcionActividadRepository.delete(ia);
        inscripcionActividadRepository.flush();

        log.info("Inscripción de persona {} a actividad {} cancelada. Cupo liberado.", persona.getId(), actividadId);

        auditoriaService.registrar(TipoOperacionAuditoria.INSCRIPCION_ACTIVIDAD_CANCELADA, ENTIDAD_ACTIVIDAD, iaId,
                DetalleAuditoria.de("eventoId", eventoId, "actividadId", actividadId, "personaId", persona.getId()));
    }

    /**
     * Consulta el aforo y cupos disponibles de una actividad (Criterios 2, 4 y 5).
     */
    @Transactional(readOnly = true)
    public CupoActividadResponse consultarCupo(Long eventoId, Long actividadId) {
        Actividad actividad = buscarActividad(eventoId, actividadId);

        Integer aforoMaximo = actividad.getSala() != null ? actividad.getSala().getAforoMaximo() : 0;
        long inscritos = inscripcionActividadRepository.countByActividad_Id(actividadId);
        long cuposDisponibles = Math.max(0, aforoMaximo - inscritos);
        boolean cupoAgotado = (aforoMaximo > 0) && (inscritos >= aforoMaximo);

        boolean estaInscrito = false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Optional<Usuario> usuario = usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName());
            if (usuario.isPresent() && usuario.get().getPersona() != null) {
                Long personaId = usuario.get().getPersona().getId();
                Optional<InscripcionEvento> inscripcion = inscripcionEventoRepository.findByEvento_IdAndPersona_Id(eventoId, personaId);
                if (inscripcion.isPresent()) {
                    estaInscrito = inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(inscripcion.get().getId(), actividadId);
                }
            }
        }

        return new CupoActividadResponse(
                actividad.getId(),
                actividad.getNombre(),
                aforoMaximo,
                inscritos,
                cuposDisponibles,
                cupoAgotado,
                estaInscrito
        );
    }

    /**
     * Lista las actividades a las que el usuario autenticado está inscrito en el evento.
     */
    @Transactional(readOnly = true)
    public List<InscripcionActividadResponse> listarMisActividades(Long eventoId) {
        Persona persona = personaAutenticada();
        return inscripcionActividadRepository.findByPersonaIdAndEventoId(persona.getId(), eventoId).stream()
                .map(InscripcionActividadResponse::fromEntity)
                .toList();
    }

    /**
     * Lista todas las inscripciones del evento (para organizadores/administración).
     */
    @Transactional(readOnly = true)
    public List<InscripcionEventoResponse> listarInscripcionesEvento(Long eventoId) {
        buscarEvento(eventoId);
        return inscripcionEventoRepository.findByEvento_IdOrderByFechaInscripcionDesc(eventoId).stream()
                .map(InscripcionEventoResponse::fromEntity)
                .toList();
    }

    /**
     * Lista los participantes inscritos en una actividad específica (para organizadores/administración).
     */
    @Transactional(readOnly = true)
    public List<InscripcionActividadResponse> listarInscritosActividad(Long eventoId, Long actividadId) {
        buscarActividad(eventoId, actividadId);
        return inscripcionActividadRepository.findByActividad_IdOrderByFechaInscripcionAsc(actividadId).stream()
                .map(InscripcionActividadResponse::fromEntity)
                .toList();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Validaciones y Auxiliares
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Criterio 3: Valida si la nueva actividad se traslapa en fecha y horario con alguna actividad ya inscrita.
     * Respeta el flag permiteSimultaneidad: si la nueva actividad o la actividad existente permite
     * simultaneidad, no se genera conflicto de horario.
     */
    private void validarConflictoHorario(Long personaId, Actividad nueva) {
        if (nueva.isPermiteSimultaneidad()) {
            return;
        }
        if (nueva.getFecha() == null || nueva.getHoraInicio() == null || nueva.getHoraFin() == null) {
            return;
        }

        List<InscripcionActividad> inscritasEnFecha = inscripcionActividadRepository.findActivasPorPersonaYFecha(
                personaId, nueva.getFecha());

        for (InscripcionActividad ia : inscritasEnFecha) {
            Actividad existente = ia.getActividad();
            if (existente == null || Objects.equals(existente.getId(), nueva.getId())) {
                continue;
            }

            if (existente.isPermiteSimultaneidad()) {
                continue;
            }

            if (hayTraslape(existente, nueva)) {
                throw new ConflictoHorarioInscripcionException(String.format(
                        "Conflicto de horario: ya se encuentra inscrito en la actividad «%s» (%s - %s), la cual se traslapa con «%s» (%s - %s). Debe cancelar la inscripción anterior para inscribirse a esta.",
                        existente.getNombre(), existente.getHoraInicio(), existente.getHoraFin(),
                        nueva.getNombre(), nueva.getHoraInicio(), nueva.getHoraFin()
                ));
            }
        }
    }

    /**
     * Determina si dos actividades coinciden en horario en el mismo día.
     * Si alguna de las actividades permite simultaneidad, no se considera traslape conflictivo.
     */
    private boolean hayTraslape(Actividad a1, Actividad a2) {
        if (a1.isPermiteSimultaneidad() || a2.isPermiteSimultaneidad()) {
            return false;
        }
        if (!Objects.equals(a1.getFecha(), a2.getFecha())) {
            return false;
        }
        if (a1.getHoraInicio() == null || a1.getHoraFin() == null || a2.getHoraInicio() == null || a2.getHoraFin() == null) {
            return false;
        }
        return a1.getHoraInicio().isBefore(a2.getHoraFin()) && a1.getHoraFin().isAfter(a2.getHoraInicio());
    }

    private void validarInscripcionesAbiertas(Evento evento) {
        if (evento.getEstado() == EstadoEvento.en_configuracion) {
            throw new OperacionNoPermitidaException(
                    "Las inscripciones no están abiertas porque el evento «" + evento.getNombre() + "» se encuentra en configuración.");
        }
        if (evento.getEstado() == EstadoEvento.cerrado) {
            throw new OperacionNoPermitidaException(
                    "Las inscripciones están cerradas porque el evento «" + evento.getNombre() + "» ha concluido.");
        }
        if (evento.getFechaFin() != null && LocalDate.now().isAfter(evento.getFechaFin())) {
            throw new OperacionNoPermitidaException(
                    "Las inscripciones están cerradas porque la fecha del evento ya expiró.");
        }
    }

    private Persona resolverPersona(Long personaIdExplicit) {
        if (personaIdExplicit != null) {
            if (!tieneRolAdminOGestion()) {
                throw new AccessDeniedException("Solo los administradores pueden inscribir a otros participantes.");
            }
            return personaRepository.findById(personaIdExplicit)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la persona con ID: " + personaIdExplicit));
        }
        return personaAutenticada();
    }

    private Persona personaAutenticada() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debe iniciar sesión para realizar inscripciones.");
        }
        Usuario usuario = usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("No se encontró el usuario autenticado."));
        if (usuario.getPersona() == null) {
            throw new OperacionNoPermitidaException("El usuario autenticado no tiene una persona asociada.");
        }
        return usuario.getPersona();
    }

    private boolean tieneRolAdminOGestion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ROLES_ADMIN_O_GESTION.contains(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Evento buscarEvento(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId));
    }

    private Actividad buscarActividad(Long eventoId, Long actividadId) {
        return actividadRepository.findByIdAndEvento_Id(actividadId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la actividad con ID " + actividadId + " en este evento."));
    }
}
