package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.evaluacion.AsignacionEvaluacionResponse;
import com.sigea.demosigea_backend.dto.evaluacion.CalificacionCriterioRequest;
import com.sigea.demosigea_backend.dto.evaluacion.RegistrarEvaluacionRequest;
import com.sigea.demosigea_backend.dto.evaluacion.ResultadoEvaluacionResponse;
import com.sigea.demosigea_backend.exception.EvaluacionCerradaException;
import com.sigea.demosigea_backend.exception.EvaluacionIncompletaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.ClasificacionResultado;
import com.sigea.demosigea_backend.model.ComiteEvaluador;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.CriterioRubrica;
import com.sigea.demosigea_backend.model.EstadoAsignacion;
import com.sigea.demosigea_backend.model.Evaluacion;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.ResultadoEvaluacion;
import com.sigea.demosigea_backend.model.RubricaEvaluacion;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.CriterioRubricaRepository;
import com.sigea.demosigea_backend.repository.EvaluacionRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.ResultadoEvaluacionRepository;
import com.sigea.demosigea_backend.repository.RubricaEvaluacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del servicio {@link EvaluacionService} (HU-17).
 * <p>
 * Verifica los cuatro criterios de aceptación de la historia de usuario:
 * registro de evaluación, validación de criterios completos, cálculo de puntuación
 * ponderada y restricción de modificación en periodo cerrado.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@ExtendWith(MockitoExtension.class)
class EvaluacionServiceTest {

    @Mock
    private EvaluacionRepository evaluacionRepository;

    @Mock
    private AsignacionEvaluacionRepository asignacionRepository;

    @Mock
    private CriterioRubricaRepository criterioRubricaRepository;

    @Mock
    private RubricaEvaluacionRepository rubricaRepository;

    @Mock
    private ResultadoEvaluacionRepository resultadoRepository;

    @Mock
    private PropuestaRepository propuestaRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private EvaluacionService service;

    // ──────────────────────────────────────────────────────────────────────────────
    // Helpers de construcción de entidades de prueba
    // ──────────────────────────────────────────────────────────────────────────────

    private Convocatoria crearConvocatoria(Long id, LocalDateTime fechaCierre) {
        return Convocatoria.builder()
                .id(id)
                .titulo("Convocatoria Test")
                .fechaCierre(fechaCierre)
                .build();
    }

    private Propuesta crearPropuesta(Long id, Convocatoria conv) {
        return Propuesta.builder()
                .id(id)
                .convocatoria(conv)
                .titulo("Propuesta de prueba")
                .build();
    }

    private Persona crearPersona(Long id) {
        return Persona.builder()
                .id(id)
                .nombres("Juan")
                .apellidos("Evaluador")
                .build();
    }

    private ComiteEvaluador crearComite(Long id, Convocatoria conv) {
        return ComiteEvaluador.builder()
                .id(id)
                .convocatoria(conv)
                .persona(crearPersona(10L))
                .build();
    }

    private AsignacionEvaluacion crearAsignacion(Long id, Propuesta propuesta,
                                                  ComiteEvaluador comite,
                                                  EstadoAsignacion estado) {
        return AsignacionEvaluacion.builder()
                .id(id)
                .propuesta(propuesta)
                .comiteEvaluador(comite)
                .estado(estado)
                .fechaAsignacion(LocalDateTime.now().minusDays(1))
                .build();
    }

    private RubricaEvaluacion crearRubrica(Long id, Convocatoria conv) {
        return RubricaEvaluacion.builder()
                .id(id)
                .convocatoria(conv)
                .nombre("Rúbrica Test")
                .activo(true)
                .build();
    }

    private CriterioRubrica crearCriterio(Long id, RubricaEvaluacion rubrica,
                                           String nombre, BigDecimal peso) {
        return CriterioRubrica.builder()
                .id(id)
                .rubrica(rubrica)
                .nombre(nombre)
                .pesoPorcentual(peso)
                .build();
    }

    private Evaluacion crearEvaluacion(Long id, AsignacionEvaluacion asig,
                                        CriterioRubrica criterio, BigDecimal calificacion) {
        return Evaluacion.builder()
                .id(id)
                .asignacion(asig)
                .criterio(criterio)
                .calificacion(calificacion)
                .fechaEvaluacion(LocalDateTime.now())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Test Criterio 1
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 1: Registra evaluación y calcula puntuación cuando todas las asignaciones están completas")
    void criterio1_registraEvaluacionYCalculaPuntuacion() {
        // Arrange
        Convocatoria conv = crearConvocatoria(1L, LocalDateTime.now().plusDays(30));
        Propuesta propuesta = crearPropuesta(1L, conv);
        ComiteEvaluador comite = crearComite(1L, conv);
        AsignacionEvaluacion asignacion = crearAsignacion(1L, propuesta, comite, EstadoAsignacion.pendiente);

        RubricaEvaluacion rubrica = crearRubrica(1L, conv);
        CriterioRubrica criterio1 = crearCriterio(1L, rubrica, "Originalidad", new BigDecimal("60.00"));
        CriterioRubrica criterio2 = crearCriterio(2L, rubrica, "Pertinencia", new BigDecimal("40.00"));
        List<CriterioRubrica> criterios = List.of(criterio1, criterio2);

        RegistrarEvaluacionRequest request = new RegistrarEvaluacionRequest(
                1L,
                List.of(
                        new CalificacionCriterioRequest(1L, new BigDecimal("8.0"), "Muy original"),
                        new CalificacionCriterioRequest(2L, new BigDecimal("7.0"), "Pertinente")
                )
        );

        when(asignacionRepository.findById(1L)).thenReturn(Optional.of(asignacion));
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(1L)).thenReturn(Optional.of(rubrica));
        when(criterioRubricaRepository.findByRubrica_Id(1L)).thenReturn(criterios);
        when(evaluacionRepository.findByAsignacion_IdAndCriterio_Id(eq(1L), eq(1L))).thenReturn(Optional.empty());
        when(evaluacionRepository.findByAsignacion_IdAndCriterio_Id(eq(1L), eq(2L))).thenReturn(Optional.empty());
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(asignacionRepository.save(any())).thenReturn(asignacion);

        // Para intentarCalcularPuntuacionPonderada
        when(asignacionRepository.findByPropuesta_Id(1L)).thenReturn(List.of(asignacion));
        Evaluacion ev1 = crearEvaluacion(1L, asignacion, criterio1, new BigDecimal("8.0"));
        Evaluacion ev2 = crearEvaluacion(2L, asignacion, criterio2, new BigDecimal("7.0"));
        when(evaluacionRepository.findByAsignacion_Propuesta_Id(1L)).thenReturn(List.of(ev1, ev2));
        when(resultadoRepository.findByPropuesta_Id(1L)).thenReturn(Optional.empty());
        ResultadoEvaluacion resultadoGuardado = ResultadoEvaluacion.builder()
                .id(1L).propuesta(propuesta)
                .puntuacionPonderada(new BigDecimal("7.60"))
                .clasificacion(ClasificacionResultado.aprobada)
                .fechaCalculo(LocalDateTime.now())
                .build();
        when(resultadoRepository.save(any(ResultadoEvaluacion.class))).thenReturn(resultadoGuardado);

        // Para construir respuesta
        when(evaluacionRepository.findByAsignacion_Id(1L)).thenReturn(List.of(ev1, ev2));

        // Act
        AsignacionEvaluacionResponse response = service.registrarEvaluacion(request);

        // Assert
        assertNotNull(response);
        assertEquals("completada", response.estado());
        assertEquals(2, response.evaluaciones().size());

        // Verificar que se guardó el resultado ponderado
        ArgumentCaptor<ResultadoEvaluacion> captor = ArgumentCaptor.forClass(ResultadoEvaluacion.class);
        verify(resultadoRepository).save(captor.capture());
        ResultadoEvaluacion capturado = captor.getValue();
        // Puntuación esperada: 8.0 * 0.60 + 7.0 * 0.40 = 4.80 + 2.80 = 7.60
        assertEquals(new BigDecimal("7.60"), capturado.getPuntuacionPonderada());
        assertEquals(ClasificacionResultado.aprobada, capturado.getClasificacion());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Test Criterio 2
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 2: Rechaza evaluación cuando faltan criterios por calificar")
    void criterio2_rechazaEvaluacionConCriteriosFaltantes() {
        // Arrange
        Convocatoria conv = crearConvocatoria(1L, LocalDateTime.now().plusDays(30));
        Propuesta propuesta = crearPropuesta(1L, conv);
        ComiteEvaluador comite = crearComite(1L, conv);
        AsignacionEvaluacion asignacion = crearAsignacion(1L, propuesta, comite, EstadoAsignacion.pendiente);

        RubricaEvaluacion rubrica = crearRubrica(1L, conv);
        CriterioRubrica criterio1 = crearCriterio(1L, rubrica, "Originalidad", new BigDecimal("60.00"));
        CriterioRubrica criterio2 = crearCriterio(2L, rubrica, "Pertinencia", new BigDecimal("40.00"));

        // Solo se envía calificación para un criterio (falta "Pertinencia")
        RegistrarEvaluacionRequest request = new RegistrarEvaluacionRequest(
                1L,
                List.of(new CalificacionCriterioRequest(1L, new BigDecimal("8.0"), "Muy original"))
        );

        when(asignacionRepository.findById(1L)).thenReturn(Optional.of(asignacion));
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(1L)).thenReturn(Optional.of(rubrica));
        when(criterioRubricaRepository.findByRubrica_Id(1L)).thenReturn(List.of(criterio1, criterio2));

        // Act & Assert
        EvaluacionIncompletaException ex = assertThrows(
                EvaluacionIncompletaException.class,
                () -> service.registrarEvaluacion(request));

        assertTrue(ex.getCriteriosFaltantes().contains("Pertinencia"),
                "Debe indicar el criterio 'Pertinencia' como faltante.");
        verify(evaluacionRepository, never()).save(any());
        verify(resultadoRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Test Criterio 3
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 3: No calcula puntuación final cuando hay asignaciones pendientes")
    void criterio3_noCalculaPuntuacionSiHayAsignacionesPendientes() {
        // Arrange
        Convocatoria conv = crearConvocatoria(1L, LocalDateTime.now().plusDays(30));
        Propuesta propuesta = crearPropuesta(1L, conv);
        ComiteEvaluador comite1 = crearComite(1L, conv);
        ComiteEvaluador comite2 = crearComite(2L, conv);

        // Una asignación completada y otra pendiente
        AsignacionEvaluacion asig1 = crearAsignacion(1L, propuesta, comite1, EstadoAsignacion.pendiente);
        AsignacionEvaluacion asig2 = crearAsignacion(2L, propuesta, comite2, EstadoAsignacion.pendiente);

        RubricaEvaluacion rubrica = crearRubrica(1L, conv);
        CriterioRubrica criterio = crearCriterio(1L, rubrica, "Calidad", new BigDecimal("100.00"));

        RegistrarEvaluacionRequest request = new RegistrarEvaluacionRequest(
                1L,
                List.of(new CalificacionCriterioRequest(1L, new BigDecimal("9.0"), "Excelente"))
        );

        when(asignacionRepository.findById(1L)).thenReturn(Optional.of(asig1));
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(1L)).thenReturn(Optional.of(rubrica));
        when(criterioRubricaRepository.findByRubrica_Id(1L)).thenReturn(List.of(criterio));
        when(evaluacionRepository.findByAsignacion_IdAndCriterio_Id(1L, 1L)).thenReturn(Optional.empty());
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(asignacionRepository.save(any())).thenReturn(asig1);

        // Hay dos asignaciones: la que acabamos de completar (asig1 → completada después del save)
        // y asig2 que sigue pendiente
        AsignacionEvaluacion asig1Completada = crearAsignacion(1L, propuesta, comite1, EstadoAsignacion.completada);
        when(asignacionRepository.findByPropuesta_Id(1L)).thenReturn(List.of(asig1Completada, asig2));

        Evaluacion ev = crearEvaluacion(1L, asig1, criterio, new BigDecimal("9.0"));
        when(evaluacionRepository.findByAsignacion_Id(1L)).thenReturn(List.of(ev));

        // Act
        AsignacionEvaluacionResponse response = service.registrarEvaluacion(request);

        // Assert: La asignación se marca como completada
        assertNotNull(response);

        // El resultado NO debe haberse calculado porque asig2 está pendiente
        verify(resultadoRepository, never()).save(any(ResultadoEvaluacion.class));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Test Criterio 4
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 4: Impide modificar evaluación cerrada cuando ya fue enviada y el periodo está cerrado")
    void criterio4_impideModificarEvaluacionCuandoPeriodoCerrado() {
        // Arrange: asignación ya completada y convocatoria con fecha de cierre en el pasado
        Convocatoria conv = crearConvocatoria(1L, LocalDateTime.now().minusDays(1));
        Propuesta propuesta = crearPropuesta(1L, conv);
        ComiteEvaluador comite = crearComite(1L, conv);
        AsignacionEvaluacion asignacion = crearAsignacion(1L, propuesta, comite, EstadoAsignacion.completada);

        RegistrarEvaluacionRequest request = new RegistrarEvaluacionRequest(
                1L,
                List.of(new CalificacionCriterioRequest(1L, new BigDecimal("7.0"), "Buena"))
        );

        when(asignacionRepository.findById(1L)).thenReturn(Optional.of(asignacion));

        // Act & Assert
        EvaluacionCerradaException ex = assertThrows(
                EvaluacionCerradaException.class,
                () -> service.registrarEvaluacion(request));

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("cerrado"),
                "El mensaje debe indicar que el periodo está cerrado.");
        verify(evaluacionRepository, never()).save(any());
        verify(resultadoRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Test obtenerResultado – incompleto
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerResultado: retorna completo=false cuando hay asignaciones pendientes")
    void obtenerResultado_retornaIncompletoSiHayPendientes() {
        // Arrange
        Convocatoria conv = crearConvocatoria(1L, LocalDateTime.now().plusDays(10));
        Propuesta propuesta = crearPropuesta(1L, conv);

        when(propuestaRepository.findById(1L)).thenReturn(Optional.of(propuesta));
        when(asignacionRepository.existsByPropuesta_IdAndEstadoNot(1L, EstadoAsignacion.completada))
                .thenReturn(true); // hay pendientes

        // Act
        ResultadoEvaluacionResponse response = service.obtenerResultado(1L);

        // Assert
        assertFalse(response.completo());
        assertFalse(response.completo());
        assertEquals(1L, response.propuestaId());
        assertFalse(response.completo());
    }
}
