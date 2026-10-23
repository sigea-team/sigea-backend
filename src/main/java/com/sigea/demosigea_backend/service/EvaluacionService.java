package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.evaluacion.AsignacionEvaluacionResponse;
import com.sigea.demosigea_backend.dto.evaluacion.CalificacionCriterioRequest;
import com.sigea.demosigea_backend.dto.evaluacion.EvaluacionResponse;
import com.sigea.demosigea_backend.dto.evaluacion.RegistrarEvaluacionRequest;
import com.sigea.demosigea_backend.dto.evaluacion.ResultadoEvaluacionResponse;
import com.sigea.demosigea_backend.exception.EvaluacionCerradaException;
import com.sigea.demosigea_backend.exception.EvaluacionIncompletaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.ClasificacionResultado;
import com.sigea.demosigea_backend.model.CriterioRubrica;
import com.sigea.demosigea_backend.model.EstadoAsignacion;
import com.sigea.demosigea_backend.model.Evaluacion;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.ResultadoEvaluacion;
import com.sigea.demosigea_backend.model.RubricaEvaluacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.CriterioRubricaRepository;
import com.sigea.demosigea_backend.repository.EvaluacionRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.ResultadoEvaluacionRepository;
import com.sigea.demosigea_backend.repository.RubricaEvaluacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para la gestión de evaluaciones de propuestas (HU-17).
 * <p>
 * Implementa los cuatro criterios de aceptación:
 * <ul>
 *   <li><b>Criterio 1</b>: Registro de calificaciones y cálculo automático de puntuación ponderada
 *       cuando todas las asignaciones de la propuesta están completadas.</li>
 *   <li><b>Criterio 2</b>: Validación de completitud de criterios antes de guardar la evaluación.</li>
 *   <li><b>Criterio 3</b>: Indicación de resultado incompleto cuando hay asignaciones pendientes.</li>
 *   <li><b>Criterio 4</b>: Bloqueo de modificación cuando el periodo de evaluación está cerrado.</li>
 * </ul>
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;
    private final AsignacionEvaluacionRepository asignacionRepository;
    private final CriterioRubricaRepository criterioRubricaRepository;
    private final RubricaEvaluacionRepository rubricaRepository;
    private final ResultadoEvaluacionRepository resultadoRepository;
    private final PropuestaRepository propuestaRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Registra o actualiza la evaluación completa de una propuesta por parte de un evaluador.
     * <p>
     * Aplica los cuatro criterios de aceptación del HU-17:
     * <ol>
     *   <li>Valida que la evaluación no esté cerrada (Criterio 4).</li>
     *   <li>Valida que todos los criterios de la rúbrica sean calificados (Criterio 2).</li>
     *   <li>Guarda las calificaciones y marca la asignación como completada.</li>
     *   <li>Intenta calcular la puntuación ponderada si todas las asignaciones están completas (Criterio 1).</li>
     * </ol>
     * </p>
     *
     * @param request solicitud con el ID de la asignación y las calificaciones por criterio.
     * @return DTO con el resumen de la asignación y todas las calificaciones registradas.
     * @throws RecursoNoEncontradoException  si la asignación o la rúbrica no existen.
     * @throws EvaluacionCerradaException    si la asignación ya estaba completada y el periodo está cerrado (Criterio 4).
     * @throws EvaluacionIncompletaException si faltan criterios por calificar en la solicitud (Criterio 2).
     */
    @Transactional
    public AsignacionEvaluacionResponse registrarEvaluacion(RegistrarEvaluacionRequest request) {
        // 1. Obtener la asignación
        AsignacionEvaluacion asignacion = asignacionRepository.findById(request.asignacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la asignación de evaluación con ID: " + request.asignacionId()));

        Long convocatoriaId = asignacion.getComiteEvaluador().getConvocatoria().getId();

        // 2. Criterio 4: si ya fue completada, verificar si el periodo está cerrado
        if (asignacion.getEstado() == EstadoAsignacion.completada) {
            LocalDateTime fechaCierre = asignacion.getComiteEvaluador().getConvocatoria().getFechaCierre();
            if (fechaCierre != null && LocalDateTime.now().isAfter(fechaCierre)) {
                throw new EvaluacionCerradaException(
                        "La evaluación ya fue enviada y el periodo de evaluación está cerrado. No se puede modificar.");
            }
            log.info("Recalculando evaluación cerrada para asignación {} dentro del periodo abierto.", asignacion.getId());
        }

        // 3. Obtener la rúbrica activa de la convocatoria
        RubricaEvaluacion rubrica = rubricaRepository.findByConvocatoria_IdAndActivoTrue(convocatoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una rúbrica activa para la convocatoria ID: " + convocatoriaId));

        List<CriterioRubrica> criterios = criterioRubricaRepository.findByRubrica_Id(rubrica.getId());

        // 4. Criterio 2: verificar que todos los criterios de la rúbrica estén calificados
        Set<Long> criteriosEnviados = request.calificaciones().stream()
                .map(CalificacionCriterioRequest::criterioId)
                .collect(Collectors.toSet());

        List<String> criteriosFaltantes = criterios.stream()
                .filter(c -> !criteriosEnviados.contains(c.getId()))
                .map(CriterioRubrica::getNombre)
                .collect(Collectors.toList());

        if (!criteriosFaltantes.isEmpty()) {
            throw new EvaluacionIncompletaException(
                    "Hay criterios sin calificar. Todos los criterios de la rúbrica deben ser calificados.",
                    criteriosFaltantes);
        }

        // 5. Persistir cada calificación (upsert)
        Map<Long, CriterioRubrica> criteriosPorId = criterios.stream()
                .collect(Collectors.toMap(CriterioRubrica::getId, c -> c));

        for (CalificacionCriterioRequest calReq : request.calificaciones()) {
            CriterioRubrica criterio = criteriosPorId.get(calReq.criterioId());
            if (criterio == null) {
                throw new RecursoNoEncontradoException(
                        "El criterio ID " + calReq.criterioId() + " no pertenece a la rúbrica activa.");
            }

            Optional<Evaluacion> existente = evaluacionRepository
                    .findByAsignacion_IdAndCriterio_Id(asignacion.getId(), criterio.getId());

            if (existente.isPresent()) {
                Evaluacion ev = existente.get();
                ev.setCalificacion(calReq.calificacion());
                ev.setObservaciones(calReq.observaciones());
                ev.setFechaEvaluacion(LocalDateTime.now());
                evaluacionRepository.save(ev);
            } else {
                Evaluacion nueva = Evaluacion.builder()
                        .asignacion(asignacion)
                        .criterio(criterio)
                        .calificacion(calReq.calificacion())
                        .observaciones(calReq.observaciones())
                        .build();
                evaluacionRepository.save(nueva);
            }
        }

        // 6. Marcar la asignación como completada y persistir
        asignacion.setEstado(EstadoAsignacion.completada);
        asignacionRepository.save(asignacion);

        Long propuestaId = asignacion.getPropuesta().getId();

        // 7. Criterio 1: intentar calcular la puntuación ponderada si todas las asignaciones están completas
        intentarCalcularPuntuacionPonderada(propuestaId);

        // 8. Registrar auditoría
        auditoriaService.registrar(
                TipoOperacionAuditoria.EVALUACION_REGISTRADA,
                "asignaciones_evaluacion",
                asignacion.getId(),
                Map.of("asignacionId", asignacion.getId(), "propuestaId", propuestaId));

        // 9. Construir y devolver la respuesta
        List<EvaluacionResponse> evaluacionesResp = evaluacionRepository
                .findByAsignacion_Id(asignacion.getId())
                .stream()
                .map(EvaluacionResponse::fromEntity)
                .collect(Collectors.toList());

        return AsignacionEvaluacionResponse.fromEntity(asignacion, evaluacionesResp);
    }

    /**
     * Intenta calcular la puntuación ponderada de una propuesta verificando primero
     * que todas sus asignaciones estén en estado {@code completada} (Criterio 1 y 3).
     * <p>
     * Si alguna asignación no está completada, no se calcula el resultado (Criterio 3).
     * Si todas están completadas, calcula el promedio ponderado por criterio entre todos
     * los evaluadores y determina la clasificación final.
     * </p>
     *
     * @param propuestaId ID de la propuesta cuyo resultado se intentará calcular.
     */
    private void intentarCalcularPuntuacionPonderada(Long propuestaId) {
        List<AsignacionEvaluacion> asignaciones = asignacionRepository.findByPropuesta_Id(propuestaId);

        // Criterio 3: si alguna asignación no está completada, no calcular
        boolean hayPendientes = asignaciones.stream()
                .anyMatch(a -> a.getEstado() != EstadoAsignacion.completada);

        if (hayPendientes) {
            log.info("Propuesta ID {} aún tiene evaluaciones pendientes. No se calculará la puntuación ponderada.",
                    propuestaId);
            return;
        }

        if (asignaciones.isEmpty()) {
            log.warn("Propuesta ID {} no tiene asignaciones de evaluación. No se puede calcular puntuación.", propuestaId);
            return;
        }

        // Obtener todas las evaluaciones de la propuesta (todos los evaluadores)
        List<Evaluacion> todasEvaluaciones = evaluacionRepository.findByAsignacion_Propuesta_Id(propuestaId);

        // Obtener la rúbrica desde la primera asignación
        Long convocatoriaId = asignaciones.get(0).getComiteEvaluador().getConvocatoria().getId();
        RubricaEvaluacion rubrica = rubricaRepository.findByConvocatoria_IdAndActivoTrue(convocatoriaId)
                .orElse(null);

        if (rubrica == null) {
            log.warn("No se encontró rúbrica activa para la convocatoria ID {}. No se calculará la puntuación.", convocatoriaId);
            return;
        }

        List<CriterioRubrica> criterios = criterioRubricaRepository.findByRubrica_Id(rubrica.getId());

        // Agrupar calificaciones por criterio_id
        Map<Long, List<BigDecimal>> calificacionesPorCriterio = todasEvaluaciones.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCriterio().getId(),
                        Collectors.mapping(Evaluacion::getCalificacion, Collectors.toList())
                ));

        // Calcular puntuación ponderada: sum(promedio_i * (peso_i / 100))
        BigDecimal puntuacionPonderada = BigDecimal.ZERO;
        for (CriterioRubrica criterio : criterios) {
            List<BigDecimal> califs = calificacionesPorCriterio.getOrDefault(criterio.getId(), List.of());
            if (califs.isEmpty()) {
                continue;
            }
            BigDecimal suma = califs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal promedio = suma.divide(BigDecimal.valueOf(califs.size()), 4, RoundingMode.HALF_UP);
            BigDecimal contribucion = promedio.multiply(
                    criterio.getPesoPorcentual().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            puntuacionPonderada = puntuacionPonderada.add(contribucion);
        }
        puntuacionPonderada = puntuacionPonderada.setScale(2, RoundingMode.HALF_UP);

        // Determinar clasificación
        ClasificacionResultado clasificacion;
        if (puntuacionPonderada.compareTo(new BigDecimal("7.0")) >= 0) {
            clasificacion = ClasificacionResultado.aprobada;
        } else if (puntuacionPonderada.compareTo(new BigDecimal("5.0")) >= 0) {
            clasificacion = ClasificacionResultado.aprobada_con_ajustes;
        } else {
            clasificacion = ClasificacionResultado.rechazada;
        }

        // Upsert del resultado
        Propuesta propuesta = asignaciones.get(0).getPropuesta();
        ResultadoEvaluacion resultado = resultadoRepository.findByPropuesta_Id(propuestaId)
                .orElse(ResultadoEvaluacion.builder().propuesta(propuesta).build());

        resultado.setPuntuacionPonderada(puntuacionPonderada);
        resultado.setClasificacion(clasificacion);
        resultado.setFechaCalculo(LocalDateTime.now());
        resultadoRepository.save(resultado);

        log.info("Puntuación ponderada calculada para propuesta ID {}: {} ({})",
                propuestaId, puntuacionPonderada, clasificacion);

        // Registrar auditoría del cálculo
        auditoriaService.registrar(
                TipoOperacionAuditoria.PUNTUACION_CALCULADA,
                "resultados_evaluacion",
                resultado.getId(),
                Map.of("propuestaId", propuestaId,
                        "puntuacionPonderada", puntuacionPonderada.toPlainString(),
                        "clasificacion", clasificacion.name()));
    }

    /**
     * Obtiene el detalle de una asignación de evaluación con sus calificaciones registradas.
     *
     * @param asignacionId ID de la asignación de evaluación.
     * @return DTO con el resumen de la asignación y las calificaciones por criterio.
     * @throws RecursoNoEncontradoException si la asignación no existe.
     */
    @Transactional(readOnly = true)
    public AsignacionEvaluacionResponse obtenerAsignacion(Long asignacionId) {
        AsignacionEvaluacion asignacion = asignacionRepository.findById(asignacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la asignación de evaluación con ID: " + asignacionId));

        List<EvaluacionResponse> evaluacionesResp = evaluacionRepository
                .findByAsignacion_Id(asignacionId)
                .stream()
                .map(EvaluacionResponse::fromEntity)
                .collect(Collectors.toList());

        return AsignacionEvaluacionResponse.fromEntity(asignacion, evaluacionesResp);
    }

    /**
     * Obtiene el resultado de evaluación de una propuesta.
     * <p>
     * Si todas las asignaciones están completadas y el resultado fue calculado, devuelve
     * el DTO con {@code completo=true} y la puntuación ponderada. Si hay asignaciones
     * pendientes, devuelve {@code completo=false} (Criterio 3).
     * </p>
     *
     * @param propuestaId ID de la propuesta.
     * @return DTO con el resultado de evaluación y la indicación de si está completo.
     * @throws RecursoNoEncontradoException si la propuesta no existe.
     */
    @Transactional(readOnly = true)
    public ResultadoEvaluacionResponse obtenerResultado(Long propuestaId) {
        Propuesta propuesta = propuestaRepository.findById(propuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la propuesta con ID: " + propuestaId));

        boolean completo = !asignacionRepository.existsByPropuesta_IdAndEstadoNot(
                propuestaId, EstadoAsignacion.completada);

        Optional<ResultadoEvaluacion> resultadoOpt = resultadoRepository.findByPropuesta_Id(propuestaId);

        if (resultadoOpt.isPresent() && completo) {
            ResultadoEvaluacion resultado = resultadoOpt.get();
            return new ResultadoEvaluacionResponse(
                    resultado.getId(),
                    propuesta.getId(),
                    propuesta.getTitulo(),
                    resultado.getPuntuacionPonderada(),
                    resultado.getClasificacion().name(),
                    resultado.getFechaCalculo(),
                    true
            );
        }

        // Resultado incompleto o aún no calculado
        return new ResultadoEvaluacionResponse(
                null,
                propuesta.getId(),
                propuesta.getTitulo(),
                null,
                null,
                null,
                false
        );
    }
}
