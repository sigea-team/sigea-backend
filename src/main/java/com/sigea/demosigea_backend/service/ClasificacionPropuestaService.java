package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.clasificacion.AjusteClasificacionRequest;
import com.sigea.demosigea_backend.dto.clasificacion.AplicacionClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.AplicacionClasificacionResponse.PropuestaOmitidaResponse;
import com.sigea.demosigea_backend.dto.clasificacion.ClasificacionConvocatoriaResponse;
import com.sigea.demosigea_backend.dto.clasificacion.HistorialClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.PropuestaClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.UmbralesClasificacionRequest;
import com.sigea.demosigea_backend.dto.clasificacion.UmbralesClasificacionResponse;
import com.sigea.demosigea_backend.exception.PropuestaNoEvaluadaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.ClasificacionResultado;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoAsignacion;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.HistorialClasificacion;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.ReglasClasificacion;
import com.sigea.demosigea_backend.model.ResultadoEvaluacion;
import com.sigea.demosigea_backend.model.TipoClasificacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.HistorialClasificacionRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.ResultadoEvaluacionRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de clasificación de propuestas según su resultado de evaluación (HU-18, RF27).
 * <p>
 * Criterios de aceptación:
 * <ul>
 *   <li><b>Criterio 1</b>: con la puntuación ponderada calculada (HU-17, RF26) y los umbrales de la convocatoria,
 *       el sistema asigna automáticamente la clasificación y el estado de la propuesta
 *       ({@code aprobada}, {@code ajustes} o {@code rechazada}).</li>
 *   <li><b>Criterio 2</b>: el administrador puede ajustar manualmente la clasificación; el motivo es obligatorio y
 *       queda en el historial ({@code historial_clasificaciones}).</li>
 *   <li><b>Criterio 3</b>: una propuesta sin evaluación completa (sin evaluadores, con evaluaciones pendientes o sin
 *       puntuación calculada) no se puede clasificar.</li>
 * </ul>
 * Correspondencia con la historia de usuario: aceptada = {@code aprobada}; aceptada con modificaciones =
 * {@code aprobada_con_ajustes} (estado de la propuesta {@code ajustes}); rechazada = {@code rechazada}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClasificacionPropuestaService {

    private static final String ENTIDAD_RESULTADO = "resultados_evaluacion";

    /** Grupo del listado para propuestas con puntuación calculada pero sin clasificación aplicada. */
    static final String GRUPO_SIN_CLASIFICAR = "sin_clasificar";

    /** Grupo del listado para propuestas cuya evaluación no está completa. */
    static final String GRUPO_EVALUACION_INCOMPLETA = "evaluacion_incompleta";

    private final PropuestaRepository propuestaRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final AsignacionEvaluacionRepository asignacionRepository;
    private final ResultadoEvaluacionRepository resultadoRepository;
    private final HistorialClasificacionRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    // ──────────────────────────────────────────────────────────────────────────────
    // Umbrales
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Consulta los umbrales de clasificación de una convocatoria.
     *
     * @param convocatoriaId ID de la convocatoria.
     * @return umbrales vigentes.
     */
    @Transactional(readOnly = true)
    public UmbralesClasificacionResponse obtenerUmbrales(Long convocatoriaId) {
        Convocatoria convocatoria = buscarConvocatoria(convocatoriaId);
        return new UmbralesClasificacionResponse(convocatoria.getId(),
                ReglasClasificacion.umbralAceptacion(convocatoria),
                ReglasClasificacion.umbralAjustes(convocatoria), 0);
    }

    /**
     * Define los umbrales de clasificación de una convocatoria (Criterio 1).
     * <p>
     * Los resultados todavía no aplicados y sin ajuste manual se reclasifican con los nuevos umbrales. Las
     * propuestas ya clasificadas no cambian hasta volver a aplicar la clasificación; la respuesta indica cuántas
     * cambiarían.
     * </p>
     *
     * @param convocatoriaId ID de la convocatoria.
     * @param request        nuevos umbrales.
     * @return umbrales guardados.
     * @throws SolicitudInvalidaException si el umbral de ajustes es mayor que el de aprobación.
     */
    @Transactional
    public UmbralesClasificacionResponse configurarUmbrales(Long convocatoriaId, UmbralesClasificacionRequest request) {
        String error = ReglasClasificacion.validarUmbrales(request.umbralAceptacion(), request.umbralAjustes());
        if (error != null) {
            throw new SolicitudInvalidaException(error);
        }
        Convocatoria convocatoria = buscarConvocatoria(convocatoriaId);
        BigDecimal aceptacionAnterior = ReglasClasificacion.umbralAceptacion(convocatoria);
        BigDecimal ajustesAnterior = ReglasClasificacion.umbralAjustes(convocatoria);

        convocatoria.setUmbralAceptacion(request.umbralAceptacion());
        convocatoria.setUmbralAjustes(request.umbralAjustes());
        convocatoriaRepository.save(convocatoria);

        long porReaplicar = 0;
        List<ResultadoEvaluacion> pendientesDeAplicar = new ArrayList<>();
        for (ResultadoEvaluacion resultado : resultadoRepository.findByPropuesta_Convocatoria_Id(convocatoriaId)) {
            if (Boolean.TRUE.equals(resultado.getAjusteManual()) || resultado.getPuntuacionPonderada() == null) {
                continue;
            }
            ClasificacionResultado nueva = ReglasClasificacion.clasificar(resultado.getPuntuacionPonderada(), convocatoria);
            if (resultado.getFechaClasificacion() == null) {
                if (nueva != resultado.getClasificacion()) {
                    resultado.setClasificacion(nueva);
                    pendientesDeAplicar.add(resultado);
                }
            } else if (nueva != resultado.getClasificacion()) {
                porReaplicar++;
            }
        }
        if (!pendientesDeAplicar.isEmpty()) {
            resultadoRepository.saveAll(pendientesDeAplicar);
        }

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("convocatoriaId", convocatoriaId);
        datos.put("umbralAceptacionAnterior", aceptacionAnterior.toPlainString());
        datos.put("umbralAjustesAnterior", ajustesAnterior.toPlainString());
        datos.put("umbralAceptacion", request.umbralAceptacion().toPlainString());
        datos.put("umbralAjustes", request.umbralAjustes().toPlainString());
        datos.put("propuestasPorReaplicar", porReaplicar);
        auditoriaService.registrar(TipoOperacionAuditoria.UMBRALES_CLASIFICACION_ACTUALIZADOS, "convocatorias",
                convocatoriaId, datos);

        return new UmbralesClasificacionResponse(convocatoriaId, request.umbralAceptacion(),
                request.umbralAjustes(), porReaplicar);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Consulta
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Lista las propuestas de una convocatoria agrupadas por su resultado (CU-31, paso 3).
     *
     * @param convocatoriaId ID de la convocatoria.
     * @return umbrales, totales por grupo y propuestas ordenadas por grupo y puntuación.
     */
    @Transactional(readOnly = true)
    public ClasificacionConvocatoriaResponse listarPorConvocatoria(Long convocatoriaId) {
        Convocatoria convocatoria = buscarConvocatoria(convocatoriaId);
        List<Propuesta> propuestas = propuestaRepository.findByConvocatoria_IdOrderByFechaEnvioDesc(convocatoriaId);

        List<Long> ids = propuestas.stream().map(Propuesta::getId).toList();
        Map<Long, List<AsignacionEvaluacion>> asignacionesPorPropuesta = ids.isEmpty() ? Map.of()
                : asignacionRepository.findDetalleByPropuestaIds(ids).stream()
                        .collect(Collectors.groupingBy(a -> a.getPropuesta().getId()));
        Map<Long, ResultadoEvaluacion> resultadosPorPropuesta = ids.isEmpty() ? Map.of()
                : resultadoRepository.findByPropuesta_IdIn(ids).stream()
                        .collect(Collectors.toMap(r -> r.getPropuesta().getId(), Function.identity(), (a, b) -> a));

        List<PropuestaClasificacionResponse> filas = propuestas.stream()
                .map(p -> construirRespuesta(p, convocatoria,
                        asignacionesPorPropuesta.getOrDefault(p.getId(), List.of()),
                        resultadosPorPropuesta.get(p.getId())))
                .sorted(Comparator.comparingInt((PropuestaClasificacionResponse f) -> ordenGrupo(grupo(f)))
                        .thenComparing(PropuestaClasificacionResponse::puntuacionPonderada,
                                Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder())))
                .toList();

        Map<String, Long> totales = new LinkedHashMap<>();
        for (String g : List.of(ClasificacionResultado.aprobada.name(), ClasificacionResultado.aprobada_con_ajustes.name(),
                ClasificacionResultado.rechazada.name(), GRUPO_SIN_CLASIFICAR, GRUPO_EVALUACION_INCOMPLETA)) {
            totales.put(g, 0L);
        }
        filas.forEach(f -> totales.merge(grupo(f), 1L, Long::sum));

        return new ClasificacionConvocatoriaResponse(convocatoriaId,
                ReglasClasificacion.umbralAceptacion(convocatoria),
                ReglasClasificacion.umbralAjustes(convocatoria), totales, filas);
    }

    /**
     * Situación de clasificación de una propuesta.
     *
     * @param propuestaId ID de la propuesta.
     * @return situación de clasificación.
     */
    @Transactional(readOnly = true)
    public PropuestaClasificacionResponse obtenerPorPropuesta(Long propuestaId) {
        Propuesta propuesta = buscarPropuesta(propuestaId, false);
        return construirRespuesta(propuesta, propuesta.getConvocatoria(),
                asignacionRepository.findByPropuesta_Id(propuestaId),
                resultadoRepository.findByPropuesta_Id(propuestaId).orElse(null));
    }

    /**
     * Historial de clasificaciones de una propuesta (Criterio 2).
     *
     * @param propuestaId ID de la propuesta.
     * @return movimientos del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public List<HistorialClasificacionResponse> historial(Long propuestaId) {
        if (!propuestaRepository.existsById(propuestaId)) {
            throw new RecursoNoEncontradoException("No se encontró la propuesta con ID: " + propuestaId);
        }
        return historialRepository.findDetalleByPropuestaId(propuestaId).stream()
                .map(HistorialClasificacionResponse::fromEntity)
                .toList();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 1 y 3: aplicar la clasificación
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Aplica la clasificación a una propuesta según los umbrales de su convocatoria (Criterios 1 y 3).
     * <p>
     * Si la clasificación fue ajustada manualmente se respeta ese ajuste. Si ya estaba aplicada y no cambió, no se
     * registra un movimiento nuevo.
     * </p>
     *
     * @param propuestaId ID de la propuesta.
     * @return situación de clasificación tras aplicarla.
     * @throws PropuestaNoEvaluadaException si la evaluación de la propuesta no está completa (Criterio 3).
     */
    @Transactional
    public PropuestaClasificacionResponse aplicar(Long propuestaId) {
        Propuesta propuesta = buscarPropuesta(propuestaId, true);
        List<AsignacionEvaluacion> asignaciones = asignacionRepository.findByPropuesta_Id(propuestaId);
        ResultadoEvaluacion resultado = resultadoRepository.findByPropuesta_Id(propuestaId).orElse(null);
        String motivo = motivoEvaluacionIncompleta(asignaciones, resultado);
        if (motivo != null) {
            throw new PropuestaNoEvaluadaException("La propuesta no se puede clasificar: " + motivo);
        }
        Usuario usuario = usuarioAutenticado().orElse(null);
        aplicarAutomatica(propuesta, resultado, usuario);
        return construirRespuesta(propuesta, propuesta.getConvocatoria(), asignaciones, resultado);
    }

    /**
     * Aplica la clasificación a todas las propuestas evaluadas de una convocatoria (Criterio 1). Las propuestas sin
     * evaluación completa no se clasifican y se informan como omitidas (Criterio 3).
     *
     * @param convocatoriaId ID de la convocatoria.
     * @return propuestas clasificadas y omitidas.
     */
    @Transactional
    public AplicacionClasificacionResponse aplicarConvocatoria(Long convocatoriaId) {
        Convocatoria convocatoria = buscarConvocatoria(convocatoriaId);
        List<Propuesta> propuestas = propuestaRepository.findByConvocatoria_IdOrderByFechaEnvioDesc(convocatoriaId);
        List<Long> ids = propuestas.stream().map(Propuesta::getId).toList();
        Map<Long, List<AsignacionEvaluacion>> asignacionesPorPropuesta = ids.isEmpty() ? Map.of()
                : asignacionRepository.findDetalleByPropuestaIds(ids).stream()
                        .collect(Collectors.groupingBy(a -> a.getPropuesta().getId()));
        Map<Long, ResultadoEvaluacion> resultadosPorPropuesta = new HashMap<>();
        if (!ids.isEmpty()) {
            resultadoRepository.findByPropuesta_IdIn(ids)
                    .forEach(r -> resultadosPorPropuesta.putIfAbsent(r.getPropuesta().getId(), r));
        }

        Usuario usuario = usuarioAutenticado().orElse(null);
        List<PropuestaClasificacionResponse> clasificadas = new ArrayList<>();
        List<PropuestaOmitidaResponse> omitidas = new ArrayList<>();
        for (Propuesta propuesta : propuestas) {
            List<AsignacionEvaluacion> asignaciones = asignacionesPorPropuesta.getOrDefault(propuesta.getId(), List.of());
            ResultadoEvaluacion resultado = resultadosPorPropuesta.get(propuesta.getId());
            String motivo = motivoEvaluacionIncompleta(asignaciones, resultado);
            if (motivo != null) {
                omitidas.add(new PropuestaOmitidaResponse(propuesta.getId(), propuesta.getTitulo(), motivo));
                continue;
            }
            aplicarAutomatica(propuesta, resultado, usuario);
            clasificadas.add(construirRespuesta(propuesta, convocatoria, asignaciones, resultado));
        }
        log.info("Clasificación aplicada en la convocatoria {}: {} clasificadas, {} omitidas.",
                convocatoriaId, clasificadas.size(), omitidas.size());
        return new AplicacionClasificacionResponse(convocatoriaId, clasificadas, omitidas);
    }

    /**
     * Aplica la clasificación por umbrales (o la manual vigente) al resultado y al estado de la propuesta.
     */
    private void aplicarAutomatica(Propuesta propuesta, ResultadoEvaluacion resultado, Usuario usuario) {
        Convocatoria convocatoria = propuesta.getConvocatoria();
        boolean manual = Boolean.TRUE.equals(resultado.getAjusteManual());
        ClasificacionResultado anterior = resultado.getFechaClasificacion() != null ? resultado.getClasificacion() : null;
        ClasificacionResultado nueva = manual
                ? resultado.getClasificacion()
                : ReglasClasificacion.clasificar(resultado.getPuntuacionPonderada(), convocatoria);
        EstadoPropuesta estadoNuevo = ReglasClasificacion.estadoPropuesta(nueva);

        if (anterior == nueva && propuesta.getEstado() == estadoNuevo) {
            return; // ya aplicada: no se duplica el historial
        }

        LocalDateTime ahora = LocalDateTime.now();
        resultado.setClasificacion(nueva);
        resultado.setFechaClasificacion(ahora);
        resultadoRepository.save(resultado);

        EstadoPropuesta estadoAnterior = propuesta.getEstado();
        propuesta.setEstado(estadoNuevo);
        propuestaRepository.save(propuesta);

        if (!manual) {
            historialRepository.save(HistorialClasificacion.builder()
                    .propuesta(propuesta)
                    .tipo(TipoClasificacion.automatica)
                    .clasificacionAnterior(anterior)
                    .clasificacionNueva(nueva)
                    .puntuacionPonderada(resultado.getPuntuacionPonderada())
                    .umbralAceptacion(ReglasClasificacion.umbralAceptacion(convocatoria))
                    .umbralAjustes(ReglasClasificacion.umbralAjustes(convocatoria))
                    .usuario(usuario)
                    .fechaHora(ahora)
                    .build());
        }

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("propuestaId", propuesta.getId());
        datos.put("puntuacionPonderada", resultado.getPuntuacionPonderada().toPlainString());
        datos.put("clasificacionAnterior", anterior != null ? anterior.name() : "ninguna");
        datos.put("clasificacion", nueva.name());
        datos.put("estadoAnterior", estadoAnterior != null ? estadoAnterior.name() : "ninguno");
        datos.put("estado", estadoNuevo.name());
        datos.put("ajusteManual", manual);
        auditoriaService.registrar(TipoOperacionAuditoria.PROPUESTA_CLASIFICADA, ENTIDAD_RESULTADO,
                resultado.getId(), datos, usuario);
        log.info("Propuesta {} clasificada como {} (puntuación {}).", propuesta.getId(), nueva,
                resultado.getPuntuacionPonderada());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 2: ajuste manual
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Ajusta manualmente la clasificación de una propuesta evaluada (Criterio 2). El motivo es obligatorio y queda
     * en el historial. La propuesta toma el estado de la nueva clasificación.
     *
     * @param propuestaId ID de la propuesta.
     * @param request     nueva clasificación y motivo.
     * @return situación de clasificación tras el ajuste.
     * @throws PropuestaNoEvaluadaException si la evaluación de la propuesta no está completa (Criterio 3).
     * @throws SolicitudInvalidaException   si falta el motivo o la propuesta ya tiene esa clasificación aplicada.
     */
    @Transactional
    public PropuestaClasificacionResponse ajustar(Long propuestaId, AjusteClasificacionRequest request) {
        String motivo = request.motivo() == null ? "" : request.motivo().strip();
        if (motivo.length() < 10) {
            throw new SolicitudInvalidaException(
                    "Para ajustar manualmente la clasificación debe registrar un motivo de al menos 10 caracteres.");
        }
        Propuesta propuesta = buscarPropuesta(propuestaId, true);
        List<AsignacionEvaluacion> asignaciones = asignacionRepository.findByPropuesta_Id(propuestaId);
        ResultadoEvaluacion resultado = resultadoRepository.findByPropuesta_Id(propuestaId).orElse(null);
        String incompleta = motivoEvaluacionIncompleta(asignaciones, resultado);
        if (incompleta != null) {
            throw new PropuestaNoEvaluadaException("La propuesta no se puede clasificar: " + incompleta);
        }

        ClasificacionResultado nueva = request.clasificacion();
        EstadoPropuesta estadoNuevo = ReglasClasificacion.estadoPropuesta(nueva);
        ClasificacionResultado anterior = resultado.getClasificacion();
        boolean yaAplicada = resultado.getFechaClasificacion() != null && anterior == nueva
                && propuesta.getEstado() == estadoNuevo;
        if (yaAplicada) {
            throw new SolicitudInvalidaException(String.format(
                    "La propuesta ya está clasificada como '%s'.", ReglasClasificacion.etiqueta(nueva)));
        }

        Convocatoria convocatoria = propuesta.getConvocatoria();
        ClasificacionResultado porUmbrales = ReglasClasificacion.clasificar(resultado.getPuntuacionPonderada(), convocatoria);
        Usuario usuario = usuarioAutenticado().orElse(null);
        LocalDateTime ahora = LocalDateTime.now();

        resultado.setClasificacion(nueva);
        resultado.setAjusteManual(true);
        resultado.setFechaClasificacion(ahora);
        resultadoRepository.save(resultado);

        EstadoPropuesta estadoAnterior = propuesta.getEstado();
        propuesta.setEstado(estadoNuevo);
        propuestaRepository.save(propuesta);

        historialRepository.save(HistorialClasificacion.builder()
                .propuesta(propuesta)
                .tipo(TipoClasificacion.manual)
                .clasificacionAnterior(anterior)
                .clasificacionNueva(nueva)
                .puntuacionPonderada(resultado.getPuntuacionPonderada())
                .umbralAceptacion(ReglasClasificacion.umbralAceptacion(convocatoria))
                .umbralAjustes(ReglasClasificacion.umbralAjustes(convocatoria))
                .motivo(motivo)
                .usuario(usuario)
                .fechaHora(ahora)
                .build());

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("propuestaId", propuestaId);
        datos.put("puntuacionPonderada", resultado.getPuntuacionPonderada().toPlainString());
        datos.put("clasificacionAnterior", anterior != null ? anterior.name() : "ninguna");
        datos.put("clasificacion", nueva.name());
        datos.put("clasificacionPorUmbrales", porUmbrales.name());
        datos.put("estadoAnterior", estadoAnterior != null ? estadoAnterior.name() : "ninguno");
        datos.put("estado", estadoNuevo.name());
        datos.put("motivo", motivo);
        auditoriaService.registrar(TipoOperacionAuditoria.CLASIFICACION_AJUSTADA, ENTIDAD_RESULTADO,
                resultado.getId(), datos, usuario);
        log.info("Clasificación de la propuesta {} ajustada manualmente de {} a {}.", propuestaId, anterior, nueva);

        return construirRespuesta(propuesta, convocatoria, asignaciones, resultado);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Apoyo
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Criterio 3: determina si la evaluación de una propuesta está completa.
     *
     * @param asignaciones asignaciones de la propuesta.
     * @param resultado    resultado calculado ({@code null} si no existe).
     * @return motivo por el que no está completa, o {@code null} si se puede clasificar.
     */
    static String motivoEvaluacionIncompleta(List<AsignacionEvaluacion> asignaciones, ResultadoEvaluacion resultado) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            return "no tiene evaluadores asignados.";
        }
        long pendientes = contarPendientes(asignaciones);
        if (pendientes > 0) {
            return String.format("tiene %d evaluación(es) pendiente(s) de %d asignada(s).", pendientes, asignaciones.size());
        }
        if (resultado == null || resultado.getPuntuacionPonderada() == null) {
            return "aún no tiene puntuación ponderada calculada.";
        }
        return null;
    }

    private static long contarPendientes(List<AsignacionEvaluacion> asignaciones) {
        return asignaciones.stream().filter(a -> a.getEstado() != EstadoAsignacion.completada).count();
    }

    private PropuestaClasificacionResponse construirRespuesta(Propuesta propuesta, Convocatoria convocatoria,
                                                              List<AsignacionEvaluacion> asignaciones,
                                                              ResultadoEvaluacion resultado) {
        boolean completa = motivoEvaluacionIncompleta(asignaciones, resultado) == null;
        BigDecimal puntuacion = resultado != null ? resultado.getPuntuacionPonderada() : null;
        ClasificacionResultado porUmbrales = puntuacion != null
                ? ReglasClasificacion.clasificar(puntuacion, convocatoria) : null;
        ClasificacionResultado vigente = completa ? resultado.getClasificacion() : null;
        boolean aplicada = completa && vigente != null && resultado.getFechaClasificacion() != null
                && propuesta.getEstado() == ReglasClasificacion.estadoPropuesta(vigente);
        return new PropuestaClasificacionResponse(
                propuesta.getId(),
                propuesta.getTitulo(),
                propuesta.getEstado() != null ? propuesta.getEstado().name() : null,
                completa,
                contarPendientes(asignaciones == null ? List.of() : asignaciones),
                puntuacion,
                porUmbrales != null ? porUmbrales.name() : null,
                vigente != null ? vigente.name() : null,
                ReglasClasificacion.etiqueta(vigente),
                resultado != null && Boolean.TRUE.equals(resultado.getAjusteManual()),
                aplicada,
                resultado != null ? resultado.getFechaClasificacion() : null
        );
    }

    /** Grupo del listado al que pertenece una fila. */
    static String grupo(PropuestaClasificacionResponse fila) {
        if (!fila.evaluacionCompleta()) {
            return GRUPO_EVALUACION_INCOMPLETA;
        }
        if (!fila.aplicada() || fila.clasificacion() == null) {
            return GRUPO_SIN_CLASIFICAR;
        }
        return fila.clasificacion();
    }

    private static int ordenGrupo(String grupo) {
        return switch (grupo) {
            case "aprobada" -> 0;
            case "aprobada_con_ajustes" -> 1;
            case "rechazada" -> 2;
            case GRUPO_SIN_CLASIFICAR -> 3;
            default -> 4;
        };
    }

    private Convocatoria buscarConvocatoria(Long convocatoriaId) {
        return convocatoriaRepository.findById(convocatoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la convocatoria con ID: " + convocatoriaId));
    }

    private Propuesta buscarPropuesta(Long propuestaId, boolean bloquear) {
        Optional<Propuesta> propuesta = bloquear
                ? propuestaRepository.findByIdParaActualizar(propuestaId)
                : propuestaRepository.findById(propuestaId);
        return propuesta.orElseThrow(() -> new RecursoNoEncontradoException(
                "No se encontró la propuesta con ID: " + propuestaId));
    }

    private Optional<Usuario> usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName());
    }
}
