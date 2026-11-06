package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.rubrica.CriterioRubricaRequest;
import com.sigea.demosigea_backend.dto.rubrica.ImpactoRubricaResponse;
import com.sigea.demosigea_backend.dto.rubrica.RubricaRequest;
import com.sigea.demosigea_backend.dto.rubrica.RubricaResponse;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.RubricaEnUsoException;
import com.sigea.demosigea_backend.exception.RubricaInvalidaException;
import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.CriterioRubrica;
import com.sigea.demosigea_backend.model.EstadoAsignacion;
import com.sigea.demosigea_backend.model.RubricaEvaluacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.CriterioRubricaRepository;
import com.sigea.demosigea_backend.repository.EvaluacionRepository;
import com.sigea.demosigea_backend.repository.ResultadoEvaluacionRepository;
import com.sigea.demosigea_backend.repository.RubricaEvaluacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Configuración de la rúbrica de evaluación de una convocatoria (HU-15, RF22).
 * <ul>
 *   <li><b>Criterio 1</b>: crea la rúbrica con criterios, escalas y pesos, validando que los pesos sumen 100 %.</li>
 *   <li><b>Criterio 2</b>: si los pesos no suman 100 % (o hay criterios repetidos o escalas inválidas) rechaza la
 *       operación e indica el error por campo.</li>
 *   <li><b>Criterio 4</b>: si la rúbrica ya tiene calificaciones registradas, advierte el impacto y solo la
 *       reemplaza con confirmación explícita. Al reemplazarla, la rúbrica anterior queda inactiva (sus
 *       calificaciones se conservan como histórico) y las asignaciones afectadas vuelven a {@code pendiente}
 *       para calificarse con la nueva rúbrica.</li>
 * </ul>
 * Una convocatoria tiene como máximo una rúbrica activa: es la que usa HU-17 para calificar.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RubricaEvaluacionService {

    static final BigDecimal CIEN = new BigDecimal("100");

    private final RubricaEvaluacionRepository rubricaRepository;
    private final CriterioRubricaRepository criterioRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final AsignacionEvaluacionRepository asignacionRepository;
    private final ResultadoEvaluacionRepository resultadoRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Rúbrica activa de la convocatoria con sus criterios.
     *
     * @param convocatoriaId ID de la convocatoria.
     * @return la rúbrica activa.
     * @throws RecursoNoEncontradoException si la convocatoria no existe o no tiene rúbrica activa.
     */
    @Transactional(readOnly = true)
    public RubricaResponse obtenerActiva(Long convocatoriaId) {
        buscarConvocatoria(convocatoriaId);
        RubricaEvaluacion rubrica = rubricaRepository.findByConvocatoria_IdAndActivoTrue(convocatoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La convocatoria " + convocatoriaId + " aún no tiene una rúbrica de evaluación configurada."));
        return RubricaResponse.of(rubrica, criterioRepository.findByRubrica_IdOrderByIdAsc(rubrica.getId()), 0);
    }

    /**
     * HU-15, Criterio 4: impacto que tendría modificar la rúbrica activa (para advertir antes de enviar).
     *
     * @param convocatoriaId ID de la convocatoria.
     * @return calificaciones, asignaciones y propuestas que dependen de la rúbrica activa.
     */
    @Transactional(readOnly = true)
    public ImpactoRubricaResponse impacto(Long convocatoriaId) {
        buscarConvocatoria(convocatoriaId);
        Optional<RubricaEvaluacion> activa = rubricaRepository.findByConvocatoria_IdAndActivoTrue(convocatoriaId);
        if (activa.isEmpty()) {
            return new ImpactoRubricaResponse(null, 0, 0, 0, false);
        }
        Long rubricaId = activa.get().getId();
        long calificaciones = evaluacionRepository.countByCriterio_Rubrica_Id(rubricaId);
        if (calificaciones == 0) {
            return new ImpactoRubricaResponse(rubricaId, 0, 0, 0, false);
        }
        List<AsignacionEvaluacion> afectadas = evaluacionRepository.findAsignacionesConCalificacionesDeRubrica(rubricaId);
        return new ImpactoRubricaResponse(rubricaId, calificaciones, afectadas.size(), contarPropuestas(afectadas), true);
    }

    /**
     * Crea la rúbrica de la convocatoria o reemplaza la activa (HU-15, Criterios 1, 2 y 4).
     *
     * @param convocatoriaId ID de la convocatoria.
     * @param request        nombre y criterios de la rúbrica.
     * @param confirmar      confirma reemplazar una rúbrica que ya tiene calificaciones registradas.
     * @return la rúbrica guardada y, si hubo reemplazo, cuántas asignaciones se reabrieron.
     * @throws RecursoNoEncontradoException si la convocatoria no existe.
     * @throws RubricaInvalidaException     si los pesos no suman 100, hay criterios repetidos o escalas inválidas.
     * @throws RubricaEnUsoException        si la rúbrica tiene calificaciones y no se confirmó el reemplazo.
     */
    @Transactional
    public ResultadoConfiguracion configurar(Long convocatoriaId, RubricaRequest request, boolean confirmar) {
        Convocatoria convocatoria = buscarConvocatoria(convocatoriaId);
        validar(request);

        Optional<RubricaEvaluacion> activa = rubricaRepository.findByConvocatoria_IdAndActivoTrue(convocatoriaId);
        if (activa.isEmpty()) {
            RubricaEvaluacion nueva = crearRubrica(convocatoria, request);
            List<CriterioRubrica> criterios = guardarCriterios(nueva, request.criterios());
            auditoriaService.registrar(TipoOperacionAuditoria.RUBRICA_CREADA, "rubricas_evaluacion", nueva.getId(),
                    datosAuditoria(convocatoriaId, nueva, criterios.size()));
            return new ResultadoConfiguracion(RubricaResponse.of(nueva, criterios, 0), true);
        }

        RubricaEvaluacion actual = activa.get();
        long calificaciones = evaluacionRepository.countByCriterio_Rubrica_Id(actual.getId());

        if (calificaciones == 0) {
            // Sin calificaciones: se modifica la misma rúbrica, reemplazando sus criterios.
            actual.setNombre(request.nombre().trim());
            rubricaRepository.save(actual);
            criterioRepository.deleteAll(criterioRepository.findByRubrica_Id(actual.getId()));
            criterioRepository.flush();
            List<CriterioRubrica> criterios = guardarCriterios(actual, request.criterios());
            auditoriaService.registrar(TipoOperacionAuditoria.RUBRICA_ACTUALIZADA, "rubricas_evaluacion",
                    actual.getId(), datosAuditoria(convocatoriaId, actual, criterios.size()));
            return new ResultadoConfiguracion(RubricaResponse.of(actual, criterios, 0), false);
        }

        // Criterio 4: la rúbrica ya se usó para calificar.
        List<AsignacionEvaluacion> afectadas = evaluacionRepository
                .findAsignacionesConCalificacionesDeRubrica(actual.getId());
        long propuestas = contarPropuestas(afectadas);
        if (!confirmar) {
            throw new RubricaEnUsoException(String.format(
                    "La rúbrica '%s' ya se usó en %d calificación(es) de %d evaluación(es) sobre %d propuesta(s). "
                            + "Si la modifica, esas evaluaciones deberán hacerse de nuevo con la nueva rúbrica. "
                            + "Confirme la operación para continuar.",
                    actual.getNombre(), calificaciones, afectadas.size(), propuestas),
                    calificaciones, afectadas.size(), propuestas);
        }

        // Reemplazo confirmado: la anterior queda inactiva (histórico) y se crea la nueva.
        actual.setActivo(false);
        rubricaRepository.saveAndFlush(actual); // libera el índice único de rúbrica activa por convocatoria
        RubricaEvaluacion nueva = crearRubrica(convocatoria, request);
        List<CriterioRubrica> criterios = guardarCriterios(nueva, request.criterios());

        Set<Long> propuestaIds = new HashSet<>();
        for (AsignacionEvaluacion asignacion : afectadas) {
            asignacion.setEstado(EstadoAsignacion.pendiente);
            propuestaIds.add(asignacion.getPropuesta().getId());
        }
        asignacionRepository.saveAll(afectadas);
        // Los resultados calculados con la rúbrica anterior ya no son válidos; HU-17 los recalcula.
        propuestaIds.forEach(id -> resultadoRepository.findByPropuesta_Id(id).ifPresent(resultadoRepository::delete));

        Map<String, Object> datos = datosAuditoria(convocatoriaId, nueva, criterios.size());
        datos.put("rubricaAnteriorId", actual.getId());
        datos.put("calificacionesAfectadas", calificaciones);
        datos.put("asignacionesReabiertas", afectadas.size());
        datos.put("propuestasAfectadas", propuestas);
        auditoriaService.registrar(TipoOperacionAuditoria.RUBRICA_REEMPLAZADA, "rubricas_evaluacion",
                nueva.getId(), datos);
        log.info("Rúbrica {} de la convocatoria {} reemplazada por {}; {} asignaciones reabiertas.",
                actual.getId(), convocatoriaId, nueva.getId(), afectadas.size());

        return new ResultadoConfiguracion(RubricaResponse.of(nueva, criterios, afectadas.size()), true);
    }

    /**
     * Resultado de {@link #configurar}: la rúbrica y si se creó una nueva (para responder 201 o 200).
     *
     * @param rubrica rúbrica guardada.
     * @param creada  {@code true} si se creó una rúbrica nueva.
     */
    public record ResultadoConfiguracion(RubricaResponse rubrica, boolean creada) {
    }

    // ------------------------------------------------------------------
    // Validaciones (Criterios 1 y 2)
    // ------------------------------------------------------------------

    /**
     * Reglas que Bean Validation no cubre: suma de pesos igual a 100, nombres de criterio únicos y escala
     * mínima menor que la máxima.
     *
     * @param request rúbrica enviada.
     * @throws RubricaInvalidaException con el detalle por campo.
     */
    void validar(RubricaRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        Set<String> nombres = new HashSet<>();
        BigDecimal suma = BigDecimal.ZERO;

        List<CriterioRubricaRequest> criterios = request.criterios();
        for (int i = 0; i < criterios.size(); i++) {
            CriterioRubricaRequest c = criterios.get(i);
            String nombre = c.nombre().trim();
            if (!nombres.add(nombre.toLowerCase(Locale.ROOT))) {
                errores.put("criterios[" + i + "].nombre", "El criterio '" + nombre + "' está repetido en la rúbrica.");
            }
            BigDecimal minima = escalaMinima(c);
            BigDecimal maxima = escalaMaxima(c);
            if (maxima.compareTo(minima) <= 0) {
                errores.put("criterios[" + i + "].escalaMaxima",
                        "La escala máxima del criterio '" + nombre + "' debe ser mayor que la mínima.");
            }
            suma = suma.add(c.pesoPorcentual());
        }

        if (suma.compareTo(CIEN) != 0) {
            errores.put("pesoPorcentual", "La suma de los pesos es " + suma.stripTrailingZeros().toPlainString()
                    + "% y debe ser exactamente 100%.");
        }

        if (!errores.isEmpty()) {
            String mensaje = suma.compareTo(CIEN) != 0
                    ? "No se guardó la rúbrica: la suma de los pesos de los criterios debe ser 100%."
                    : "No se guardó la rúbrica: revise los criterios señalados.";
            throw new RubricaInvalidaException(mensaje, errores);
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Convocatoria buscarConvocatoria(Long convocatoriaId) {
        return convocatoriaRepository.findById(convocatoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la convocatoria con ID: " + convocatoriaId));
    }

    private RubricaEvaluacion crearRubrica(Convocatoria convocatoria, RubricaRequest request) {
        return rubricaRepository.save(RubricaEvaluacion.builder()
                .convocatoria(convocatoria)
                .nombre(request.nombre().trim())
                .activo(true)
                .build());
    }

    private List<CriterioRubrica> guardarCriterios(RubricaEvaluacion rubrica, List<CriterioRubricaRequest> criterios) {
        List<CriterioRubrica> entidades = criterios.stream()
                .map(c -> CriterioRubrica.builder()
                        .rubrica(rubrica)
                        .nombre(c.nombre().trim())
                        .pesoPorcentual(c.pesoPorcentual())
                        .escalaMinima(escalaMinima(c))
                        .escalaMaxima(escalaMaxima(c))
                        .build())
                .toList();
        return criterioRepository.saveAll(entidades);
    }

    private static BigDecimal escalaMinima(CriterioRubricaRequest c) {
        return c.escalaMinima() != null ? c.escalaMinima() : CriterioRubrica.ESCALA_MINIMA_POR_DEFECTO;
    }

    private static BigDecimal escalaMaxima(CriterioRubricaRequest c) {
        return c.escalaMaxima() != null ? c.escalaMaxima() : CriterioRubrica.ESCALA_MAXIMA_POR_DEFECTO;
    }

    private static long contarPropuestas(List<AsignacionEvaluacion> asignaciones) {
        return asignaciones.stream().map(a -> a.getPropuesta().getId()).distinct().count();
    }

    private static Map<String, Object> datosAuditoria(Long convocatoriaId, RubricaEvaluacion rubrica, int criterios) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("convocatoriaId", convocatoriaId);
        datos.put("nombre", rubrica.getNombre());
        datos.put("criterios", criterios);
        return datos;
    }
}
