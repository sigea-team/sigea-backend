package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.observacion.ObservacionRequest;
import com.sigea.demosigea_backend.dto.observacion.ObservacionResponse;
import com.sigea.demosigea_backend.dto.observacion.ObservacionesPropuestaResponse;
import com.sigea.demosigea_backend.dto.observacion.PropuestaProgramableResponse;
import com.sigea.demosigea_backend.dto.observacion.RegistrarObservacionesRequest;
import com.sigea.demosigea_backend.dto.observacion.VersionDefinitivaResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.PropuestaBloqueadaException;
import com.sigea.demosigea_backend.exception.PropuestaNoEvaluadaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.exception.VersionDefinitivaNoAprobableException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.ComiteEvaluador;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.ObservacionPropuesta;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.PropuestaAutor;
import com.sigea.demosigea_backend.model.ResultadoEvaluacion;
import com.sigea.demosigea_backend.model.RolAutor;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.model.VersionPropuesta;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.ObservacionPropuestaRepository;
import com.sigea.demosigea_backend.repository.PropuestaAutorRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.ResultadoEvaluacionRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import com.sigea.demosigea_backend.repository.VersionPropuestaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Observaciones y ajustes requeridos a una propuesta y aprobación de su versión definitiva (HU-19: RF28, RF29;
 * CU-16).
 * <ul>
 *   <li><b>Criterio 1:</b> el evaluador asignado o el administrador registra observaciones y ajustes requeridos sobre
 *       la versión actual de una propuesta clasificada como aprobada con ajustes (estado {@code ajustes}); se guardan
 *       y se notifica a sus autores. Una propuesta aprobada sin modificaciones no lleva ajustes: su versión
 *       definitiva se aprueba directamente.</li>
 *   <li><b>Criterio 2:</b> cuando el autor sube una nueva versión ({@link VersionPropuestaService#crearNuevaVersion}),
 *       los ajustes sin resolver quedan vinculados a esa versión ({@code version_atencion_id}). Luego el
 *       evaluador o el administrador los da por resueltos.</li>
 *   <li><b>Criterio 3:</b> el administrador aprueba la versión actual como definitiva: queda marcada
 *       ({@code es_definitiva}), la propuesta pasa a {@code aprobada}, no se puede modificar (HU-14, Criterio 5) y
 *       aparece entre las propuestas disponibles para programar en la agenda del evento.</li>
 *   <li><b>Criterio 4:</b> si quedan ajustes requeridos sin resolver, la aprobación se rechaza (409) con la lista de
 *       pendientes.</li>
 * </ul>
 * <p>
 * Todas las escrituras bloquean primero la fila de la propuesta (igual que la creación de versiones de HU-14), así
 * que registrar, vincular, resolver y aprobar no se intercalan.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObservacionPropuestaService {

    private static final String ENTIDAD_OBSERVACION = "observaciones_propuesta";
    private static final String ENTIDAD_VERSION = "versiones_propuesta";
    private static final String ENTIDAD_PROPUESTA = "propuestas";

    /**
     * Autoridades del administrador en HU-19: registrar y resolver observaciones de cualquier propuesta y aprobar su
     * versión definitiva (además del {@code @PreAuthorize} del controlador). {@code PROPUESTAS_CLASIFICAR} (HU-18) no
     * basta: quien solo clasifica debe estar asignado a la propuesta para gestionar sus observaciones.
     */
    private static final Set<String> AUTORIDADES_ADMIN = Set.of("ROLE_ADMIN", "ADMIN", "CONVOCATORIAS_GESTIONAR",
            "PROPUESTAS_APROBAR_DEFINITIVA");

    /** Estados de una propuesta clasificada que admite observaciones y aprobación definitiva. */
    private static final Set<EstadoPropuesta> ESTADOS_APROBABLES = EnumSet.of(EstadoPropuesta.aprobada,
            EstadoPropuesta.ajustes);

    private final PropuestaRepository propuestaRepository;
    private final VersionPropuestaRepository versionRepository;
    private final ObservacionPropuestaRepository observacionRepository;
    private final ResultadoEvaluacionRepository resultadoRepository;
    private final AsignacionEvaluacionRepository asignacionRepository;
    private final PropuestaAutorRepository propuestaAutorRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final ActividadRepository actividadRepository;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    /** Motivo por el que una propuesta no se puede aprobar todavía. */
    private record Impedimento(String codigo, String mensaje, List<ObservacionPropuesta> pendientes) {
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 1: registrar observaciones y ajustes
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Registra observaciones y ajustes requeridos sobre la versión actual de la propuesta y notifica a sus autores.
     *
     * @param propuestaId ID de la propuesta.
     * @param request     observaciones a registrar.
     * @return situación de las observaciones de la propuesta.
     * @throws AccessDeniedException        si el usuario no es evaluador asignado ni administrador.
     * @throws PropuestaNoEvaluadaException si la propuesta aún no ha sido evaluada y clasificada.
     * @throws OperacionNoPermitidaException si la propuesta fue aprobada sin modificaciones o rechazada.
     * @throws PropuestaBloqueadaException  si ya tiene versión definitiva.
     */
    @Transactional
    public ObservacionesPropuestaResponse registrar(Long propuestaId, RegistrarObservacionesRequest request) {
        if (request == null || request.observaciones() == null || request.observaciones().isEmpty()) {
            throw new SolicitudInvalidaException("Debe registrar al menos una observación.");
        }
        Usuario usuario = usuarioAutenticado();
        Propuesta propuesta = bloquearPropuesta(propuestaId);
        ComiteEvaluador evaluador = evaluadorAsignado(propuestaId, usuario).orElse(null);
        if (evaluador == null && !esAdministrador()) {
            throw new AccessDeniedException(
                    "Solo los evaluadores asignados a la propuesta o el administrador pueden registrar observaciones.");
        }
        exigirClasificada(propuesta, "registrar observaciones");
        if (versionRepository.existsByPropuesta_IdAndEsDefinitivaTrue(propuestaId)) {
            throw new PropuestaBloqueadaException("La propuesta ya tiene versión definitiva aprobada y no admite "
                    + "nuevas observaciones.");
        }
        if (propuesta.getEstado() == EstadoPropuesta.aprobada) {
            // Aceptada sin modificaciones: no lleva ajustes; su versión definitiva se aprueba directamente.
            throw new OperacionNoPermitidaException("La propuesta fue aceptada sin modificaciones y no requiere "
                    + "ajustes; su versión definitiva se puede aprobar directamente.");
        }
        VersionPropuesta version = versionActual(propuestaId);

        LocalDateTime ahora = LocalDateTime.now();
        List<ObservacionPropuesta> nuevas = new ArrayList<>();
        for (ObservacionRequest item : request.observaciones()) {
            String descripcion = item == null || item.descripcion() == null ? "" : item.descripcion().strip();
            if (descripcion.isEmpty()) {
                throw new SolicitudInvalidaException("La descripción de cada observación es obligatoria.");
            }
            nuevas.add(ObservacionPropuesta.builder()
                    .version(version)
                    .comiteEvaluador(evaluador)
                    .usuario(usuario)
                    .descripcion(descripcion)
                    .requiereAjuste(item.requiereAjuste() == null || item.requiereAjuste())
                    .fecha(ahora)
                    .resuelto(false)
                    .build());
        }
        List<ObservacionPropuesta> guardadas = observacionRepository.saveAll(nuevas);
        long ajustes = guardadas.stream().filter(o -> Boolean.TRUE.equals(o.getRequiereAjuste())).count();

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("propuestaId", propuestaId);
        datos.put("versionId", version.getId());
        datos.put("numeroVersion", version.getNumeroVersion());
        datos.put("observaciones", guardadas.size());
        datos.put("ajustesRequeridos", ajustes);
        datos.put("observacionIds", guardadas.stream().map(ObservacionPropuesta::getId).toList());
        datos.put("registradaComoEvaluador", evaluador != null);
        auditoriaService.registrar(TipoOperacionAuditoria.OBSERVACIONES_PROPUESTA_REGISTRADAS, ENTIDAD_PROPUESTA,
                propuestaId, datos, usuario);

        String mensaje = String.format("Se registraron %d observación(es) sobre la versión %d de tu propuesta '%s'%s. ",
                guardadas.size(), version.getNumeroVersion(), propuesta.getTitulo(), deConvocatoria(propuesta))
                + (ajustes > 0
                ? (ajustes == 1 ? "1 requiere ajustes" : ajustes + " requieren ajustes")
                + ": sube una nueva versión que los atienda para que pueda aprobarse su versión definitiva."
                : "Ninguna requiere ajustes.");
        int notificados = notificarAutores(propuesta, TipoNotificacion.OBSERVACIONES_PROPUESTA_REGISTRADAS,
                "Observaciones a tu propuesta: " + propuesta.getTitulo(), mensaje);
        log.info("Propuesta {}: {} observación(es) ({} ajustes) registradas por el usuario {}; {} autor(es) notificados.",
                propuestaId, guardadas.size(), ajustes, usuario.getId(), notificados);

        return resumen(propuesta);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Consulta
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Observaciones de la propuesta y si su versión actual se puede aprobar como definitiva. La consultan el
     * administrador, los evaluadores asignados y los autores.
     *
     * @param propuestaId ID de la propuesta.
     * @return situación de las observaciones.
     */
    @Transactional(readOnly = true)
    public ObservacionesPropuestaResponse listar(Long propuestaId) {
        Usuario usuario = usuarioAutenticado();
        Propuesta propuesta = propuestaRepository.findById(propuestaId)
                .orElseThrow(() -> noEncontrada(propuestaId));
        if (!esAdministrador() && evaluadorAsignado(propuestaId, usuario).isEmpty()
                && !esAutorOEmisor(propuesta, usuario)) {
            throw new AccessDeniedException("No tiene acceso a las observaciones de esta propuesta.");
        }
        return resumen(propuesta);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 2: dar por resuelto un ajuste atendido
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Da por resuelta una observación. Un ajuste requerido solo se puede resolver cuando el autor subió una versión que
     * lo atiende (Criterio 2); un comentario se puede cerrar en cualquier momento.
     *
     * @param propuestaId   ID de la propuesta.
     * @param observacionId ID de la observación.
     * @return la observación resuelta.
     */
    @Transactional
    public ObservacionResponse resolver(Long propuestaId, Long observacionId) {
        Usuario usuario = usuarioAutenticado();
        Propuesta propuesta = bloquearPropuesta(propuestaId);
        if (!esAdministrador() && evaluadorAsignado(propuestaId, usuario).isEmpty()) {
            throw new AccessDeniedException(
                    "Solo los evaluadores asignados a la propuesta o el administrador pueden resolver observaciones.");
        }
        ObservacionPropuesta observacion = observacionRepository.findByIdAndVersion_Propuesta_Id(observacionId, propuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "No se encontró la observación ID %d en la propuesta ID %d.", observacionId, propuestaId)));
        if (versionRepository.existsByPropuesta_IdAndEsDefinitivaTrue(propuestaId)) {
            throw new PropuestaBloqueadaException("La propuesta ya tiene versión definitiva aprobada; sus observaciones "
                    + "no se pueden modificar.");
        }
        if (Boolean.TRUE.equals(observacion.getResuelto())) {
            throw new OperacionNoPermitidaException("La observación ya fue resuelta.");
        }
        if (Boolean.TRUE.equals(observacion.getRequiereAjuste()) && observacion.getVersionAtencion() == null) {
            throw new OperacionNoPermitidaException("El autor aún no ha subido una versión que atienda este ajuste; "
                    + "no se puede dar por resuelto.");
        }

        observacion.setResuelto(true);
        observacion.setFechaResolucion(LocalDateTime.now());
        observacion.setResueltoPor(usuario);
        ObservacionPropuesta guardada = observacionRepository.save(observacion);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("propuestaId", propuestaId);
        datos.put("requiereAjuste", Boolean.TRUE.equals(guardada.getRequiereAjuste()));
        datos.put("numeroVersion", guardada.getVersion() != null ? guardada.getVersion().getNumeroVersion() : null);
        datos.put("versionAtencion", guardada.getVersionAtencion() != null
                ? guardada.getVersionAtencion().getNumeroVersion() : null);
        auditoriaService.registrar(TipoOperacionAuditoria.OBSERVACION_PROPUESTA_RESUELTA, ENTIDAD_OBSERVACION,
                guardada.getId(), datos, usuario);
        log.info("Propuesta {}: observación {} resuelta por el usuario {}.", propuesta.getId(), observacionId,
                usuario.getId());
        return ObservacionResponse.fromEntity(guardada);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterios 3 y 4: aprobar la versión definitiva
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Aprueba la versión actual de la propuesta como definitiva (Criterio 3). Si quedan ajustes sin resolver, se
     * rechaza con la lista de pendientes (Criterio 4). Solo el administrador (también restringido en el controlador).
     * <p>
     * Si la propuesta ya tenía una versión definitiva y el administrador autorizó modificarla (HU-14, Criterio 5),
     * la marca pasa a la nueva versión.
     * </p>
     *
     * @param propuestaId ID de la propuesta.
     * @return la versión definitiva aprobada.
     * @throws VersionDefinitivaNoAprobableException si hay ajustes sin resolver o sin registrar.
     */
    @Transactional
    public VersionDefinitivaResponse aprobarVersionDefinitiva(Long propuestaId) {
        Usuario usuario = usuarioAutenticado();
        if (!esAdministrador()) {
            throw new AccessDeniedException("Solo el administrador puede aprobar la versión definitiva de una propuesta.");
        }
        Propuesta propuesta = bloquearPropuesta(propuestaId);
        exigirClasificada(propuesta, "aprobar su versión definitiva");
        VersionPropuesta actual = versionActual(propuestaId);
        if (Boolean.TRUE.equals(actual.getEsDefinitiva())) {
            throw new OperacionNoPermitidaException(String.format(
                    "La versión %d ya es la versión definitiva de la propuesta.", actual.getNumeroVersion()));
        }
        List<ObservacionPropuesta> observaciones = observacionRepository.findDePropuesta(propuestaId);
        Impedimento impedimento = impedimento(propuesta, observaciones);
        if (impedimento != null) {
            throw new VersionDefinitivaNoAprobableException(impedimento.codigo(), impedimento.mensaje(),
                    impedimento.pendientes().stream().map(ObservacionResponse::fromEntity).toList());
        }

        LocalDateTime ahora = LocalDateTime.now();
        Integer definitivaAnterior = null;
        Optional<VersionPropuesta> anterior = versionRepository.findByPropuesta_IdAndEsDefinitivaTrue(propuestaId);
        if (anterior.isPresent()) {
            // Índice único parcial: se desmarca y se escribe antes de marcar la nueva.
            VersionPropuesta previa = anterior.get();
            definitivaAnterior = previa.getNumeroVersion();
            previa.setEsDefinitiva(false);
            previa.setFechaDefinitiva(null);
            previa.setAprobadaPor(null);
            versionRepository.saveAndFlush(previa);
        }
        actual.setEsDefinitiva(true);
        actual.setFechaDefinitiva(ahora);
        actual.setAprobadaPor(usuario);
        VersionPropuesta definitiva = versionRepository.save(actual);

        EstadoPropuesta estadoAnterior = propuesta.getEstado();
        propuesta.setEstado(EstadoPropuesta.aprobada);
        propuesta.setEdicionAutorizada(false);
        propuestaRepository.save(propuesta);

        long ajustesResueltos = observaciones.stream()
                .filter(o -> Boolean.TRUE.equals(o.getRequiereAjuste()) && Boolean.TRUE.equals(o.getResuelto()))
                .count();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("propuestaId", propuestaId);
        datos.put("numeroVersion", definitiva.getNumeroVersion());
        datos.put("estadoAnterior", estadoAnterior != null ? estadoAnterior.name() : "ninguno");
        datos.put("estado", EstadoPropuesta.aprobada.name());
        datos.put("versionDefinitivaAnterior", definitivaAnterior != null ? definitivaAnterior : "ninguna");
        datos.put("ajustesResueltos", ajustesResueltos);
        auditoriaService.registrar(TipoOperacionAuditoria.VERSION_DEFINITIVA_APROBADA, ENTIDAD_VERSION,
                definitiva.getId(), datos, usuario);

        notificarAutores(propuesta, TipoNotificacion.VERSION_DEFINITIVA_APROBADA,
                "Versión definitiva aprobada: " + propuesta.getTitulo(),
                String.format("La versión %d de tu propuesta '%s'%s fue aprobada como definitiva. Ya no admite "
                                + "modificaciones y queda disponible para programarse en la agenda del evento.",
                        definitiva.getNumeroVersion(), propuesta.getTitulo(), deConvocatoria(propuesta)));
        log.info("Propuesta {}: versión {} aprobada como definitiva por el usuario {}.", propuestaId,
                definitiva.getNumeroVersion(), usuario.getId());

        return new VersionDefinitivaResponse(propuestaId, propuesta.getTitulo(), EstadoPropuesta.aprobada.name(),
                definitiva.getId(), definitiva.getNumeroVersion(), definitivaAnterior, ahora,
                ObservacionResponse.nombre(usuario), ajustesResueltos,
                String.format("La versión %d de la propuesta '%s' quedó aprobada como definitiva, bloqueada para edición "
                        + "y disponible para programar en la agenda.", definitiva.getNumeroVersion(),
                        propuesta.getTitulo()));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 3: propuestas disponibles para la agenda
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Propuestas de las convocatorias del evento con versión definitiva aprobada, es decir, las que se pueden
     * programar en la agenda (HU-21/HU-22 solo aceptan estas propuestas).
     *
     * @param eventoId        ID del evento.
     * @param soloSinProgramar si es {@code true}, omite las que ya tienen una actividad no cancelada.
     * @return propuestas en orden de aprobación.
     */
    @Transactional(readOnly = true)
    public List<PropuestaProgramableResponse> listarProgramables(Long eventoId, boolean soloSinProgramar) {
        if (!eventoRepository.existsById(eventoId)) {
            throw new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId);
        }
        List<VersionPropuesta> definitivas = versionRepository.findDefinitivasDelEvento(eventoId);
        if (definitivas.isEmpty()) {
            return List.of();
        }
        List<Long> ids = definitivas.stream().map(v -> v.getPropuesta().getId()).toList();
        Map<Long, List<PropuestaAutor>> autores = propuestaAutorRepository.findByPropuestaIdsConPersona(ids).stream()
                .collect(Collectors.groupingBy(a -> a.getPropuesta().getId()));
        Map<Long, List<Long>> actividades = actividadRepository.findByEvento_IdAndPropuestaIsNotNull(eventoId).stream()
                .filter(a -> !a.estaCancelada())
                .collect(Collectors.groupingBy(a -> a.getPropuesta().getId(),
                        Collectors.mapping(Actividad::getId, Collectors.toList())));

        List<PropuestaProgramableResponse> respuesta = new ArrayList<>();
        for (VersionPropuesta v : definitivas) {
            Propuesta p = v.getPropuesta();
            List<Long> vinculadas = actividades.getOrDefault(p.getId(), List.of());
            if (soloSinProgramar && !vinculadas.isEmpty()) {
                continue;
            }
            List<PropuestaAutor> deLaPropuesta = autores.getOrDefault(p.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(a -> a.getOrden() == null ? Integer.MAX_VALUE : a.getOrden()))
                    .toList();
            respuesta.add(new PropuestaProgramableResponse(
                    p.getId(),
                    p.getTitulo(),
                    p.getConvocatoria() != null ? p.getConvocatoria().getId() : null,
                    p.getConvocatoria() != null ? p.getConvocatoria().getTitulo() : null,
                    p.getLineaTematica() != null ? p.getLineaTematica().getId() : null,
                    p.getLineaTematica() != null ? p.getLineaTematica().getNombre() : null,
                    v.getNumeroVersion(),
                    v.getFechaDefinitiva(),
                    deLaPropuesta.stream().map(a -> nombre(a.getPersona())).filter(Objects::nonNull).toList(),
                    ponenteSugerido(deLaPropuesta),
                    !vinculadas.isEmpty(),
                    vinculadas));
        }
        return respuesta;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Reglas
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * La propuesta debe estar evaluada y con la clasificación aplicada (HU-18) como aprobada o aprobada con ajustes.
     */
    private void exigirClasificada(Propuesta propuesta, String accion) {
        if (propuesta.getEstado() == EstadoPropuesta.rechazada) {
            throw new OperacionNoPermitidaException("La propuesta fue rechazada; no se puede " + accion + ".");
        }
        ResultadoEvaluacion resultado = resultadoRepository.findByPropuesta_Id(propuesta.getId()).orElse(null);
        if (resultado == null || resultado.getFechaClasificacion() == null
                || !ESTADOS_APROBABLES.contains(propuesta.getEstado())) {
            throw new PropuestaNoEvaluadaException("La propuesta aún no ha sido evaluada y clasificada; no se puede "
                    + accion + ".");
        }
    }

    /**
     * Criterio 4: ajustes requeridos sin resolver, o propuesta con ajustes solicitados en la clasificación sin ningún
     * ajuste registrado. {@code null} si se puede aprobar.
     */
    private static Impedimento impedimento(Propuesta propuesta, List<ObservacionPropuesta> observaciones) {
        List<ObservacionPropuesta> pendientes = observaciones.stream()
                .filter(ObservacionPropuesta::esAjustePendiente)
                .toList();
        if (!pendientes.isEmpty()) {
            long sinAtender = pendientes.stream().filter(o -> o.getVersionAtencion() == null).count();
            String detalle = sinAtender > 0
                    ? sinAtender + " aún sin una versión del autor que los atienda"
                    : "todos atendidos por el autor, pendientes de revisión";
            return new Impedimento(VersionDefinitivaNoAprobableException.OBSERVACIONES_PENDIENTES,
                    String.format("No se puede aprobar la versión definitiva: la propuesta tiene %d ajuste(s) sin "
                            + "resolver (%s).", pendientes.size(), detalle),
                    pendientes);
        }
        boolean tieneAjustes = observaciones.stream().anyMatch(o -> Boolean.TRUE.equals(o.getRequiereAjuste()));
        if (propuesta.getEstado() == EstadoPropuesta.ajustes && !tieneAjustes) {
            return new Impedimento(VersionDefinitivaNoAprobableException.AJUSTES_SIN_REGISTRAR,
                    "No se puede aprobar la versión definitiva: la propuesta fue clasificada como aprobada con ajustes "
                            + "y todavía no se han registrado los ajustes requeridos.",
                    List.of());
        }
        return null;
    }

    private ObservacionesPropuestaResponse resumen(Propuesta propuesta) {
        Long propuestaId = propuesta.getId();
        List<ObservacionPropuesta> observaciones = observacionRepository.findDePropuesta(propuestaId);
        VersionPropuesta actual = versionRepository.findTopByPropuesta_IdOrderByNumeroVersionDesc(propuestaId)
                .orElse(null);
        VersionPropuesta definitiva = versionRepository.findByPropuesta_IdAndEsDefinitivaTrue(propuestaId)
                .orElse(null);

        List<ObservacionPropuesta> pendientes = observaciones.stream().filter(ObservacionPropuesta::esAjustePendiente)
                .toList();
        long porAtender = pendientes.stream().filter(o -> o.getVersionAtencion() == null).count();

        String motivo = motivoNoAprobable(propuesta, actual, observaciones);
        return new ObservacionesPropuestaResponse(
                propuestaId,
                propuesta.getTitulo(),
                propuesta.getEstado() != null ? propuesta.getEstado().name() : null,
                actual != null ? actual.getNumeroVersion() : null,
                definitiva != null ? definitiva.getNumeroVersion() : null,
                definitiva != null ? definitiva.getFechaDefinitiva() : null,
                pendientes.size(),
                porAtender,
                motivo == null,
                motivo,
                observaciones.stream().map(ObservacionResponse::fromEntity).toList());
    }

    private String motivoNoAprobable(Propuesta propuesta, VersionPropuesta actual,
                                     List<ObservacionPropuesta> observaciones) {
        try {
            exigirClasificada(propuesta, "aprobar su versión definitiva");
        } catch (OperacionNoPermitidaException | PropuestaNoEvaluadaException ex) {
            return ex.getMessage();
        }
        if (actual == null) {
            return "La propuesta no tiene versiones registradas.";
        }
        if (Boolean.TRUE.equals(actual.getEsDefinitiva())) {
            return String.format("La versión %d ya es la versión definitiva.", actual.getNumeroVersion());
        }
        Impedimento impedimento = impedimento(propuesta, observaciones);
        return impedimento != null ? impedimento.mensaje() : null;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Auxiliares
    // ──────────────────────────────────────────────────────────────────────────────

    private VersionPropuesta versionActual(Long propuestaId) {
        return versionRepository.findTopByPropuesta_IdOrderByNumeroVersionDesc(propuestaId)
                .orElseThrow(() -> new OperacionNoPermitidaException(
                        "La propuesta no tiene versiones registradas."));
    }

    /** Miembro del comité evaluador asignado a la propuesta que corresponde al usuario, si lo hay. */
    private Optional<ComiteEvaluador> evaluadorAsignado(Long propuestaId, Usuario usuario) {
        Long personaId = usuario.getPersona() != null ? usuario.getPersona().getId() : null;
        if (personaId == null) {
            return Optional.empty();
        }
        return asignacionRepository.findDetalleByPropuestaId(propuestaId).stream()
                .map(AsignacionEvaluacion::getComiteEvaluador)
                .filter(c -> c != null && c.getPersona() != null && personaId.equals(c.getPersona().getId()))
                .findFirst();
    }

    private boolean esAutorOEmisor(Propuesta propuesta, Usuario usuario) {
        Long personaId = usuario.getPersona() != null ? usuario.getPersona().getId() : null;
        if (personaId != null && propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(propuesta.getId(), personaId)) {
            return true;
        }
        return versionRepository.findFirstByPropuesta_IdOrderByNumeroVersionAsc(propuesta.getId())
                .map(VersionPropuesta::getCreadoPor)
                .map(creador -> Objects.equals(creador.getId(), usuario.getId()))
                .orElse(false);
    }

    /**
     * Notifica en la campana a los autores con cuenta y a quien envió la propuesta (una vez por usuario).
     *
     * @return número de usuarios notificados.
     */
    private int notificarAutores(Propuesta propuesta, TipoNotificacion tipo, String titulo, String mensaje) {
        Map<Long, Usuario> destinatarios = new LinkedHashMap<>();
        for (PropuestaAutor autor : propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(propuesta.getId())) {
            if (autor.getPersona() == null || autor.getPersona().getId() == null) {
                continue;
            }
            usuarioRepository.findByPersona_Id(autor.getPersona().getId())
                    .ifPresent(u -> destinatarios.putIfAbsent(u.getId(), u));
        }
        versionRepository.findFirstByPropuesta_IdOrderByNumeroVersionAsc(propuesta.getId())
                .map(VersionPropuesta::getCreadoPor)
                .ifPresent(u -> destinatarios.putIfAbsent(u.getId(), u));
        if (destinatarios.isEmpty()) {
            log.warn("La propuesta {} no tiene autores con cuenta de usuario; no se crea notificación.",
                    propuesta.getId());
        }
        destinatarios.values().forEach(u -> notificacionService.notificarAUsuario(u, tipo, titulo, mensaje,
                ENTIDAD_PROPUESTA, propuesta.getId()));
        return destinatarios.size();
    }

    private static Long ponenteSugerido(List<PropuestaAutor> autores) {
        return autores.stream()
                .filter(a -> a.getRol() == RolAutor.ponente)
                .findFirst()
                .or(() -> autores.stream().filter(a -> a.getRol() == RolAutor.autor_principal).findFirst())
                .or(() -> autores.stream().findFirst())
                .map(PropuestaAutor::getPersona)
                .map(Persona::getId)
                .orElse(null);
    }

    private static String nombre(Persona persona) {
        if (persona == null) {
            return null;
        }
        String completo = ((persona.getNombres() == null ? "" : persona.getNombres()) + " "
                + (persona.getApellidos() == null ? "" : persona.getApellidos())).trim();
        return completo.isEmpty() ? null : completo;
    }

    private static String deConvocatoria(Propuesta propuesta) {
        return propuesta.getConvocatoria() != null && propuesta.getConvocatoria().getTitulo() != null
                ? " de la convocatoria '" + propuesta.getConvocatoria().getTitulo() + "'"
                : "";
    }

    private Propuesta bloquearPropuesta(Long propuestaId) {
        return propuestaRepository.findByIdParaActualizar(propuestaId).orElseThrow(() -> noEncontrada(propuestaId));
    }

    private static RecursoNoEncontradoException noEncontrada(Long propuestaId) {
        return new RecursoNoEncontradoException("No se encontró la propuesta con ID: " + propuestaId);
    }

    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debe iniciar sesión para gestionar observaciones de propuestas.");
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("No se encontró el usuario autenticado."));
    }

    private boolean esAdministrador() {
        return tieneAutoridad(AUTORIDADES_ADMIN);
    }

    private static boolean tieneAutoridad(Set<String> autoridades) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority autoridad : auth.getAuthorities()) {
            if (autoridades.contains(autoridad.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
