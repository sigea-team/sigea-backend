package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.asignacion.AsignacionResponse;
import com.sigea.demosigea_backend.dto.asignacion.AsignarEvaluadoresRequest;
import com.sigea.demosigea_backend.dto.asignacion.EvaluadorDisponibleResponse;
import com.sigea.demosigea_backend.dto.asignacion.HistorialAsignacionResponse;
import com.sigea.demosigea_backend.dto.asignacion.PropuestaAsignacionResponse;
import com.sigea.demosigea_backend.dto.asignacion.ReasignarEvaluadorRequest;
import com.sigea.demosigea_backend.exception.AsignacionDuplicadaException;
import com.sigea.demosigea_backend.exception.ConfirmacionRequeridaException;
import com.sigea.demosigea_backend.exception.ConflictoInteresException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Afiliacion;
import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.ComiteEvaluador;
import com.sigea.demosigea_backend.model.EstadoAsignacion;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.HistorialAsignacion;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.PropuestaAutor;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.TipoOperacionAsignacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.ComiteEvaluadorRepository;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.EvaluacionRepository;
import com.sigea.demosigea_backend.repository.HistorialAsignacionRepository;
import com.sigea.demosigea_backend.repository.PropuestaAutorRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Asignación de propuestas a los evaluadores del comité (HU-16, RF24).
 * <ul>
 *   <li><b>Criterio 1</b>: asigna una propuesta a uno o más evaluadores, notifica a cada uno en su campana y la
 *       asignación aparece en su bandeja ({@link #miBandeja()}).</li>
 *   <li><b>Criterio 2</b>: detecta conflictos de interés. Si el evaluador es autor, coautor o ponente de la
 *       propuesta, <b>impide</b> la asignación; si comparte afiliación con un autor, <b>advierte</b> y exige que el
 *       organizador confirme ({@code confirmarConflicto = true}).</li>
 *   <li><b>Criterio 3</b>: reasigna a otro evaluador actualizando la asignación y dejando cada movimiento en el
 *       historial inmutable {@code historial_asignaciones}.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsignacionEvaluacionService {

    /** Una propuesta solo se asigna mientras está recibida o en evaluación. */
    static final Set<EstadoPropuesta> ESTADOS_ASIGNABLES = EnumSet.of(EstadoPropuesta.recibida,
            EstadoPropuesta.en_evaluacion);

    private static final String ENTIDAD = "asignaciones_evaluacion";

    /**
     * Índice único (propuesta_id, comite_evaluador_id) que ya existe en {@code asignaciones_evaluacion}
     * (ver {@code schema.sql}). Protege ante peticiones simultáneas que pasen la validación del servicio.
     */
    static final String INDICE_ASIGNACION_UNICA = "asignaciones_evaluacion_propuesta_id_comite_evaluador_id_idx";

    /** SQLState de PostgreSQL para violación de unicidad. */
    static final String SQLSTATE_VIOLACION_UNICIDAD = "23505";

    /** Nivel del conflicto de interés entre un evaluador y una propuesta (Criterio 2). */
    enum NivelConflicto { NINGUNO, ADVERTENCIA, BLOQUEANTE }

    /** Resultado de evaluar el conflicto de interés. */
    record Conflicto(NivelConflicto nivel, String detalle) {
        static final Conflicto NINGUNO = new Conflicto(NivelConflicto.NINGUNO, null);
    }

    private final AsignacionEvaluacionRepository asignacionRepository;
    private final HistorialAsignacionRepository historialRepository;
    private final ComiteEvaluadorRepository comiteEvaluadorRepository;
    private final PropuestaRepository propuestaRepository;
    private final PropuestaAutorRepository propuestaAutorRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;
    private final ConvocatoriaRepository convocatoriaRepository;

    // ------------------------------------------------------------------
    // Criterio 1 y 2: asignar
    // ------------------------------------------------------------------

    /**
     * Asigna la propuesta a uno o más evaluadores del comité de su convocatoria. La operación es atómica: si algún
     * evaluador no es válido, no se guarda ninguna asignación.
     *
     * @param propuestaId ID de la propuesta.
     * @param request     evaluadores y confirmación de advertencias de conflicto.
     * @return asignaciones creadas.
     * @throws RecursoNoEncontradoException   si la propuesta o algún evaluador no existe.
     * @throws OperacionNoPermitidaException  si la propuesta no está recibida ni en evaluación.
     * @throws SolicitudInvalidaException     si el evaluador no pertenece al comité de la convocatoria.
     * @throws AsignacionDuplicadaException   si la propuesta ya está asignada a alguno de los evaluadores.
     * @throws ConflictoInteresException      si algún evaluador es autor de la propuesta (Criterio 2).
     * @throws ConfirmacionRequeridaException si hay advertencias de conflicto sin confirmar (Criterio 2).
     */
    @Transactional
    public List<AsignacionResponse> asignar(Long propuestaId, AsignarEvaluadoresRequest request) {
        Propuesta propuesta = buscarPropuestaAsignable(propuestaId);
        List<PropuestaAutor> autores = propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(propuestaId);

        // 1. Validar todos los evaluadores antes de guardar nada
        Map<ComiteEvaluador, Conflicto> evaluadores = new LinkedHashMap<>();
        for (Long comiteEvaluadorId : new LinkedHashSet<>(request.comiteEvaluadorIds())) {
            ComiteEvaluador evaluador = buscarEvaluadorDeConvocatoria(comiteEvaluadorId, propuesta);
            if (asignacionRepository.existsByPropuesta_IdAndComiteEvaluador_Id(propuestaId, evaluador.getId())) {
                throw new AsignacionDuplicadaException(String.format(
                        "La propuesta '%s' ya está asignada a %s.", propuesta.getTitulo(), nombre(evaluador)));
            }
            evaluadores.put(evaluador, verificarConflicto(evaluador, autores));
        }
        exigirConfirmacionSiHayAdvertencias(evaluadores.values().stream().toList(), request.conflictoConfirmado());

        // 2. Guardar, registrar historial, notificar y auditar
        Usuario actor = usuarioAutenticado().orElse(null);
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<AsignacionResponse> respuestas = new ArrayList<>();
        for (Map.Entry<ComiteEvaluador, Conflicto> entrada : evaluadores.entrySet()) {
            ComiteEvaluador evaluador = entrada.getKey();
            Conflicto conflicto = entrada.getValue();

            AsignacionEvaluacion asignacion = guardarControlandoDuplicado(AsignacionEvaluacion.builder()
                    .propuesta(propuesta)
                    .comiteEvaluador(evaluador)
                    .fechaAsignacion(ahora)
                    .estado(EstadoAsignacion.pendiente)
                    .build(), propuesta, evaluador);

            registrarHistorial(asignacion, TipoOperacionAsignacion.asignacion, null, evaluador, conflicto,
                    null, actor, ahora);
            notificarAsignacion(evaluador, asignacion, propuesta);

            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("propuestaId", propuestaId);
            datos.put("comiteEvaluadorId", evaluador.getId());
            datos.put("conflictoAdvertido", conflicto.nivel() == NivelConflicto.ADVERTENCIA);
            auditoriaService.registrar(TipoOperacionAuditoria.PROPUESTA_ASIGNADA, ENTIDAD, asignacion.getId(), datos);

            respuestas.add(AsignacionResponse.fromEntity(asignacion, advertencias(conflicto)));
        }

        if (propuesta.getEstado() == EstadoPropuesta.recibida) {
            propuesta.setEstado(EstadoPropuesta.en_evaluacion);
            propuestaRepository.save(propuesta);
        }

        log.info("Propuesta {} asignada a {} evaluador(es)", propuestaId, respuestas.size());
        return respuestas;
    }

    // ------------------------------------------------------------------
    // Criterio 3: reasignar
    // ------------------------------------------------------------------

    /**
     * Reasigna una asignación a otro evaluador del comité. La asignación conserva su ID, vuelve a
     * {@code pendiente} y el movimiento queda en el historial.
     *
     * @param asignacionId ID de la asignación.
     * @param request      nuevo evaluador, motivo y confirmación de advertencias.
     * @return la asignación actualizada.
     * @throws RecursoNoEncontradoException   si la asignación o el evaluador no existe.
     * @throws OperacionNoPermitidaException  si la evaluación ya fue completada o tiene calificaciones, o si la
     *                                        propuesta ya no está en evaluación.
     * @throws SolicitudInvalidaException     si el evaluador es el mismo o no pertenece al comité.
     * @throws AsignacionDuplicadaException   si el nuevo evaluador ya tiene asignada la propuesta.
     * @throws ConflictoInteresException      si el nuevo evaluador es autor de la propuesta (Criterio 2).
     * @throws ConfirmacionRequeridaException si hay una advertencia de conflicto sin confirmar (Criterio 2).
     */
    @Transactional
    public AsignacionResponse reasignar(Long asignacionId, ReasignarEvaluadorRequest request) {
        AsignacionEvaluacion asignacion = asignacionRepository.findById(asignacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la asignación de evaluación con ID: " + asignacionId));
        Propuesta propuesta = asignacion.getPropuesta();
        validarPropuestaAsignable(propuesta);

        if (asignacion.getEstado() == EstadoAsignacion.completada
                || evaluacionRepository.existsByAsignacion_Id(asignacionId)) {
            throw new OperacionNoPermitidaException(
                    "No es posible reasignar: el evaluador ya registró calificaciones para esta propuesta.");
        }

        ComiteEvaluador anterior = asignacion.getComiteEvaluador();
        if (Objects.equals(anterior.getId(), request.comiteEvaluadorId())) {
            throw new SolicitudInvalidaException("La propuesta ya está asignada a ese evaluador. Elija otro evaluador.");
        }

        ComiteEvaluador nuevo = buscarEvaluadorDeConvocatoria(request.comiteEvaluadorId(), propuesta);
        if (asignacionRepository.existsByPropuesta_IdAndComiteEvaluador_Id(propuesta.getId(), nuevo.getId())) {
            throw new AsignacionDuplicadaException(String.format(
                    "La propuesta '%s' ya está asignada a %s.", propuesta.getTitulo(), nombre(nuevo)));
        }

        List<PropuestaAutor> autores = propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(propuesta.getId());
        Conflicto conflicto = verificarConflicto(nuevo, autores);
        exigirConfirmacionSiHayAdvertencias(List.of(conflicto), request.conflictoConfirmado());

        LocalDateTime ahora = LocalDateTime.now(clock);
        asignacion.setComiteEvaluador(nuevo);
        asignacion.setEstado(EstadoAsignacion.pendiente);
        asignacion.setFechaAsignacion(ahora);
        asignacion = guardarControlandoDuplicado(asignacion, propuesta, nuevo);

        String motivo = request.motivo() != null && !request.motivo().isBlank() ? request.motivo().trim() : null;
        registrarHistorial(asignacion, TipoOperacionAsignacion.reasignacion, anterior, nuevo, conflicto, motivo,
                usuarioAutenticado().orElse(null), ahora);

        notificarAsignacion(nuevo, asignacion, propuesta);
        notificarRetiro(anterior, asignacion, propuesta);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("propuestaId", propuesta.getId());
        datos.put("evaluadorAnteriorId", anterior.getId());
        datos.put("evaluadorNuevoId", nuevo.getId());
        datos.put("conflictoAdvertido", conflicto.nivel() == NivelConflicto.ADVERTENCIA);
        if (motivo != null) {
            datos.put("motivo", motivo);
        }
        auditoriaService.registrar(TipoOperacionAuditoria.PROPUESTA_REASIGNADA, ENTIDAD, asignacion.getId(), datos);

        log.info("Asignación {} reasignada del evaluador {} al {}", asignacionId, anterior.getId(), nuevo.getId());
        return AsignacionResponse.fromEntity(asignacion, advertencias(conflicto));
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Propuestas de una convocatoria con el resumen de sus asignaciones (vista del organizador, Criterio 1).
     *
     * @param convocatoriaId ID de la convocatoria.
     * @return propuestas de la más reciente a la más antigua.
     * @throws RecursoNoEncontradoException si la convocatoria no existe.
     */
    @Transactional(readOnly = true)
    public List<PropuestaAsignacionResponse> propuestasDeConvocatoria(Long convocatoriaId) {
        if (!convocatoriaRepository.existsById(convocatoriaId)) {
            throw new RecursoNoEncontradoException("No se encontró la convocatoria con ID: " + convocatoriaId);
        }
        return propuestaRepository.findByConvocatoria_IdOrderByFechaEnvioDesc(convocatoriaId).stream()
                .map(propuesta -> {
                    List<String> autores = propuestaAutorRepository
                            .findByPropuesta_IdOrderByOrdenAsc(propuesta.getId()).stream()
                            .map(autor -> nombre(autor.getPersona()))
                            .toList();
                    List<AsignacionEvaluacion> asignaciones =
                            asignacionRepository.findDetalleByPropuestaId(propuesta.getId());
                    int completadas = (int) asignaciones.stream()
                            .filter(a -> a.getEstado() == EstadoAsignacion.completada)
                            .count();
                    return new PropuestaAsignacionResponse(
                            propuesta.getId(),
                            propuesta.getTitulo(),
                            propuesta.getEstado() != null ? propuesta.getEstado().name() : null,
                            propuesta.getFechaEnvio(),
                            autores,
                            asignaciones.stream().map(a -> nombre(a.getComiteEvaluador())).toList(),
                            asignaciones.size(),
                            completadas);
                })
                .toList();
    }

    /** Asignaciones vigentes de una propuesta (vista del organizador). */
    @Transactional(readOnly = true)
    public List<AsignacionResponse> listarPorPropuesta(Long propuestaId) {
        buscarPropuesta(propuestaId);
        return asignacionRepository.findDetalleByPropuestaId(propuestaId).stream()
                .map(AsignacionResponse::fromEntity)
                .toList();
    }

    /** Historial de asignaciones y reasignaciones de una propuesta (Criterio 3). */
    @Transactional(readOnly = true)
    public List<HistorialAsignacionResponse> historialPorPropuesta(Long propuestaId) {
        buscarPropuesta(propuestaId);
        return historialRepository.findHistorialDePropuesta(propuestaId).stream()
                .map(HistorialAsignacionResponse::fromEntity)
                .toList();
    }

    /**
     * Evaluadores del comité de la convocatoria de la propuesta, con su carga de trabajo, si ya la tienen asignada
     * y el conflicto de interés que tendrían (Criterios 1 y 2). Permite al frontend advertir antes de asignar.
     */
    @Transactional(readOnly = true)
    public List<EvaluadorDisponibleResponse> evaluadoresDisponibles(Long propuestaId) {
        Propuesta propuesta = buscarPropuesta(propuestaId);
        List<PropuestaAutor> autores = propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(propuestaId);
        return comiteEvaluadorRepository.findByConvocatoriaConPersona(propuesta.getConvocatoriaId()).stream()
                .map(ce -> {
                    Conflicto conflicto = evaluarConflicto(ce, autores);
                    Persona persona = ce.getPersona();
                    return new EvaluadorDisponibleResponse(
                            ce.getId(),
                            persona.getId(),
                            nombre(ce),
                            persona.getCorreo(),
                            ce.getAreaExperticia(),
                            asignacionRepository.countByComiteEvaluador_IdAndEstadoNot(ce.getId(),
                                    EstadoAsignacion.completada),
                            asignacionRepository.existsByPropuesta_IdAndComiteEvaluador_Id(propuestaId, ce.getId()),
                            conflicto.nivel().name(),
                            conflicto.detalle());
                })
                .toList();
    }

    /**
     * Bandeja del evaluador autenticado (Criterio 1). Por defecto muestra solo el trabajo actual (asignaciones
     * pendientes o en proceso); con {@code incluirCompletadas = true} incluye también las completadas, para que el
     * evaluador pueda volver a una evaluación enviada mientras el periodo siga abierto (HU-17, Criterio 4).
     *
     * @param incluirCompletadas si se incluyen las asignaciones completadas.
     * @throws AccessDeniedException si no hay sesión iniciada.
     */
    @Transactional(readOnly = true)
    public List<AsignacionResponse> miBandeja(boolean incluirCompletadas) {
        Usuario usuario = usuarioAutenticado()
                .orElseThrow(() -> new AccessDeniedException("Debe iniciar sesión para consultar su bandeja."));
        Long personaId = usuario.getPersona().getId();
        List<AsignacionEvaluacion> asignaciones = incluirCompletadas
                ? asignacionRepository.findBandejaByPersonaId(personaId)
                : asignacionRepository.findBandejaByPersonaIdAndEstadoNot(personaId, EstadoAsignacion.completada);
        return asignaciones.stream()
                .map(AsignacionResponse::fromEntity)
                .toList();
    }

    // ------------------------------------------------------------------
    // Conflicto de interés (Criterio 2)
    // ------------------------------------------------------------------

    /**
     * Evalúa el conflicto de interés entre el evaluador y los autores de la propuesta:
     * <ul>
     *   <li>{@code BLOQUEANTE}: el evaluador figura como autor, coautor o ponente.</li>
     *   <li>{@code ADVERTENCIA}: el evaluador comparte afiliación institucional con algún autor.</li>
     * </ul>
     */
    Conflicto evaluarConflicto(ComiteEvaluador evaluador, List<PropuestaAutor> autores) {
        Persona personaEvaluador = evaluador.getPersona();
        for (PropuestaAutor autor : autores) {
            if (Objects.equals(autor.getPersona().getId(), personaEvaluador.getId())) {
                String rol = autor.getRol() != null ? autor.getRol().getDescripcion().toLowerCase() : "autor";
                return new Conflicto(NivelConflicto.BLOQUEANTE, String.format(
                        "%s figura como %s de la propuesta. Un evaluador no puede evaluar una propuesta propia.",
                        nombre(evaluador), rol));
            }
        }
        Afiliacion afiliacion = personaEvaluador.getAfiliacion();
        if (afiliacion != null && afiliacion.getId() != null) {
            for (PropuestaAutor autor : autores) {
                Afiliacion afiliacionAutor = autor.getPersona().getAfiliacion();
                if (afiliacionAutor != null && Objects.equals(afiliacionAutor.getId(), afiliacion.getId())) {
                    return new Conflicto(NivelConflicto.ADVERTENCIA, String.format(
                            "%s comparte la afiliación '%s' con el autor %s.", nombre(evaluador),
                            afiliacion.getNombreAfiliacion(), nombre(autor.getPersona())));
                }
            }
        }
        return Conflicto.NINGUNO;
    }

    private Conflicto verificarConflicto(ComiteEvaluador evaluador, List<PropuestaAutor> autores) {
        Conflicto conflicto = evaluarConflicto(evaluador, autores);
        if (conflicto.nivel() == NivelConflicto.BLOQUEANTE) {
            throw new ConflictoInteresException(conflicto.detalle());
        }
        return conflicto;
    }

    private void exigirConfirmacionSiHayAdvertencias(List<Conflicto> conflictos, boolean confirmado) {
        List<String> advertencias = conflictos.stream()
                .filter(c -> c.nivel() == NivelConflicto.ADVERTENCIA)
                .map(Conflicto::detalle)
                .toList();
        if (!advertencias.isEmpty() && !confirmado) {
            throw new ConfirmacionRequeridaException("Posible conflicto de interés: " + String.join(" ", advertencias)
                    + " Confirme la asignación (confirmarConflicto = true) para continuar.");
        }
    }

    private static List<String> advertencias(Conflicto conflicto) {
        return conflicto.nivel() == NivelConflicto.ADVERTENCIA ? List.of(conflicto.detalle()) : List.of();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Propuesta buscarPropuesta(Long propuestaId) {
        return propuestaRepository.findById(propuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la propuesta con ID: " + propuestaId));
    }

    private Propuesta buscarPropuestaAsignable(Long propuestaId) {
        Propuesta propuesta = buscarPropuesta(propuestaId);
        validarPropuestaAsignable(propuesta);
        return propuesta;
    }

    private void validarPropuestaAsignable(Propuesta propuesta) {
        if (!ESTADOS_ASIGNABLES.contains(propuesta.getEstado())) {
            throw new OperacionNoPermitidaException(String.format(
                    "La propuesta '%s' está en estado '%s'. Solo se asignan propuestas recibidas o en evaluación.",
                    propuesta.getTitulo(), propuesta.getEstado()));
        }
    }

    private ComiteEvaluador buscarEvaluadorDeConvocatoria(Long comiteEvaluadorId, Propuesta propuesta) {
        ComiteEvaluador evaluador = comiteEvaluadorRepository.findDetalleById(comiteEvaluadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el evaluador del comité con ID: " + comiteEvaluadorId));
        Long convocatoriaEvaluador = evaluador.getConvocatoria() != null ? evaluador.getConvocatoria().getId() : null;
        if (!Objects.equals(convocatoriaEvaluador, propuesta.getConvocatoriaId())) {
            throw new SolicitudInvalidaException(String.format(
                    "%s no pertenece al comité evaluador de la convocatoria de la propuesta.", nombre(evaluador)));
        }
        return evaluador;
    }

    /**
     * Guarda con flush inmediato: si dos peticiones simultáneas pasaron la validación, el índice
     * {@value #INDICE_ASIGNACION_UNICA} lo detecta aquí y se responde 409 en vez de 500.
     */
    private AsignacionEvaluacion guardarControlandoDuplicado(AsignacionEvaluacion asignacion, Propuesta propuesta,
                                                             ComiteEvaluador evaluador) {
        try {
            return asignacionRepository.saveAndFlush(asignacion);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionAsignacionUnica(ex)) {
                log.warn("El índice {} rechazó una asignación duplicada: propuesta {}, evaluador {}. Se responde 409.",
                        INDICE_ASIGNACION_UNICA, propuesta.getId(), evaluador.getId());
                throw new AsignacionDuplicadaException(String.format(
                        "La propuesta '%s' ya está asignada a %s.", propuesta.getTitulo(), nombre(evaluador)));
            }
            throw ex;
        }
    }

    /**
     * Determina si la excepción corresponde al índice {@value #INDICE_ASIGNACION_UNICA}, usando el SQLState y el
     * nombre de la restricción (no el texto del mensaje).
     */
    static boolean esViolacionAsignacionUnica(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause() == causa ? null : causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                String restriccion = violacion.getConstraintName();
                return SQLSTATE_VIOLACION_UNICIDAD.equals(violacion.getSQLState())
                        && restriccion != null
                        && INDICE_ASIGNACION_UNICA.equalsIgnoreCase(restriccion.replace("\"", ""));
            }
        }
        return false;
    }

    private void registrarHistorial(AsignacionEvaluacion asignacion, TipoOperacionAsignacion tipo,
                                    ComiteEvaluador anterior, ComiteEvaluador nuevo, Conflicto conflicto,
                                    String motivo, Usuario actor, LocalDateTime fecha) {
        historialRepository.save(HistorialAsignacion.builder()
                .asignacion(asignacion)
                .propuesta(asignacion.getPropuesta())
                .tipoOperacion(tipo)
                .evaluadorAnterior(anterior)
                .evaluadorNuevo(nuevo)
                .conflictoAdvertido(conflicto.nivel() == NivelConflicto.ADVERTENCIA)
                .motivo(motivo)
                .usuario(actor)
                .fechaHora(fecha)
                .build());
    }

    private void notificarAsignacion(ComiteEvaluador evaluador, AsignacionEvaluacion asignacion, Propuesta propuesta) {
        usuarioDe(evaluador).ifPresent(usuario -> notificacionService.notificarAUsuario(usuario,
                TipoNotificacion.PROPUESTA_ASIGNADA,
                "Nueva propuesta para evaluar: " + propuesta.getTitulo(),
                String.format("Se te asignó la propuesta '%s'%s. La encuentras en tu bandeja de evaluaciones.",
                        propuesta.getTitulo(), deConvocatoria(propuesta)),
                ENTIDAD, asignacion.getId()));
    }

    private void notificarRetiro(ComiteEvaluador evaluador, AsignacionEvaluacion asignacion, Propuesta propuesta) {
        usuarioDe(evaluador).ifPresent(usuario -> notificacionService.notificarAUsuario(usuario,
                TipoNotificacion.ASIGNACION_RETIRADA,
                "Propuesta reasignada: " + propuesta.getTitulo(),
                String.format("La propuesta '%s'%s fue reasignada a otro evaluador y ya no está en tu bandeja.",
                        propuesta.getTitulo(), deConvocatoria(propuesta)),
                ENTIDAD, asignacion.getId()));
    }

    private Optional<Usuario> usuarioDe(ComiteEvaluador evaluador) {
        Optional<Usuario> usuario = usuarioRepository.findByPersona_Id(evaluador.getPersona().getId());
        if (usuario.isEmpty()) {
            log.warn("El evaluador {} (persona {}) no tiene cuenta de usuario; no se crea notificación.",
                    evaluador.getId(), evaluador.getPersona().getId());
        }
        return usuario;
    }

    private static String deConvocatoria(Propuesta propuesta) {
        return propuesta.getConvocatoria() != null && propuesta.getConvocatoria().getTitulo() != null
                ? " de la convocatoria '" + propuesta.getConvocatoria().getTitulo() + "'"
                : "";
    }

    private static String nombre(ComiteEvaluador evaluador) {
        return nombre(evaluador.getPersona());
    }

    private static String nombre(Persona persona) {
        return (persona.getNombres() + " " + persona.getApellidos()).trim();
    }

    private Optional<Usuario> usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName());
    }
}
