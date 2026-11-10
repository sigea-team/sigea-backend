package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.rubrica.CriterioRubricaRequest;
import com.sigea.demosigea_backend.dto.rubrica.ImpactoRubricaResponse;
import com.sigea.demosigea_backend.dto.rubrica.RubricaRequest;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.RubricaEnUsoException;
import com.sigea.demosigea_backend.exception.RubricaInvalidaException;
import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.CriterioRubrica;
import com.sigea.demosigea_backend.model.EstadoAsignacion;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.ResultadoEvaluacion;
import com.sigea.demosigea_backend.model.RubricaEvaluacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.CriterioRubricaRepository;
import com.sigea.demosigea_backend.repository.EvaluacionRepository;
import com.sigea.demosigea_backend.repository.ResultadoEvaluacionRepository;
import com.sigea.demosigea_backend.repository.RubricaEvaluacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link RubricaEvaluacionService} (HU-15, Criterios 1, 2 y 4).
 */
@ExtendWith(MockitoExtension.class)
class RubricaEvaluacionServiceTest {

    @Mock private RubricaEvaluacionRepository rubricaRepository;
    @Mock private CriterioRubricaRepository criterioRepository;
    @Mock private ConvocatoriaRepository convocatoriaRepository;
    @Mock private EvaluacionRepository evaluacionRepository;
    @Mock private AsignacionEvaluacionRepository asignacionRepository;
    @Mock private ResultadoEvaluacionRepository resultadoRepository;
    @Mock private AuditoriaService auditoriaService;

    private RubricaEvaluacionService service;
    private Convocatoria convocatoria;

    @BeforeEach
    void setUp() {
        service = new RubricaEvaluacionService(rubricaRepository, criterioRepository, convocatoriaRepository,
                evaluacionRepository, asignacionRepository, resultadoRepository, auditoriaService);
        convocatoria = Convocatoria.builder().id(10L).titulo("Convocatoria de Ponencias 2026").build();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static CriterioRubricaRequest criterio(String nombre, String peso) {
        return new CriterioRubricaRequest(nombre, new BigDecimal(peso), null, null);
    }

    private static RubricaRequest rubrica(CriterioRubricaRequest... criterios) {
        return new RubricaRequest("Rúbrica de ponencias", List.of(criterios));
    }

    private void convocatoriaExiste() {
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(convocatoria));
    }

    /** {@code configurar} lee la convocatoria con bloqueo exclusivo. */
    private void convocatoriaBloqueada() {
        when(convocatoriaRepository.findByIdParaActualizar(10L)).thenReturn(Optional.of(convocatoria));
    }

    /** El repositorio devuelve lo que recibe y asigna ID a las rúbricas nuevas. */
    private void persistenciaSimulada(Long idNuevaRubrica) {
        when(rubricaRepository.save(any(RubricaEvaluacion.class))).thenAnswer(inv -> {
            RubricaEvaluacion r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(idNuevaRubrica);
            }
            return r;
        });
        when(criterioRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private RubricaEvaluacion rubricaActiva() {
        return RubricaEvaluacion.builder().id(4L).convocatoria(convocatoria).nombre("Rúbrica anterior")
                .activo(true).build();
    }

    private static AsignacionEvaluacion asignacion(Long id, Long propuestaId) {
        return AsignacionEvaluacion.builder().id(id)
                .propuesta(Propuesta.builder().id(propuestaId).build())
                .estado(EstadoAsignacion.completada).build();
    }

    // ------------------------------------------------------------------
    // Criterio 1
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 1: crea la rúbrica cuando los pesos suman 100% y usa la escala 0-10 por defecto")
    void configurar_creaRubrica() {
        convocatoriaBloqueada();
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.empty());
        persistenciaSimulada(7L);

        RubricaEvaluacionService.ResultadoConfiguracion resultado = service.configurar(10L,
                rubrica(criterio("Originalidad", "40"), criterio("Pertinencia", "35.50"), criterio("Claridad", "24.50")),
                false);

        assertTrue(resultado.creada());
        assertEquals(7L, resultado.rubrica().rubricaId());
        assertEquals(10L, resultado.rubrica().convocatoriaId());
        assertEquals(0, new BigDecimal("100").compareTo(resultado.rubrica().sumaPesos()));
        assertEquals(3, resultado.rubrica().criterios().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.rubrica().criterios().get(0).escalaMinima()));
        assertEquals(0, BigDecimal.TEN.compareTo(resultado.rubrica().criterios().get(0).escalaMaxima()));
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.RUBRICA_CREADA), eq("rubricas_evaluacion"),
                eq(7L), anyMap());
    }

    @Test
    @DisplayName("Criterio 1: respeta la escala definida por criterio")
    void configurar_conEscalaPropia() {
        convocatoriaBloqueada();
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.empty());
        persistenciaSimulada(7L);

        RubricaEvaluacionService.ResultadoConfiguracion resultado = service.configurar(10L,
                new RubricaRequest("Rúbrica 1 a 5", List.of(
                        new CriterioRubricaRequest("Impacto", new BigDecimal("100"), BigDecimal.ONE, new BigDecimal("5")))),
                false);

        assertEquals(0, BigDecimal.ONE.compareTo(resultado.rubrica().criterios().get(0).escalaMinima()));
        assertEquals(0, new BigDecimal("5").compareTo(resultado.rubrica().criterios().get(0).escalaMaxima()));
    }

    // ------------------------------------------------------------------
    // Criterio 2
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 2: rechaza la rúbrica si los pesos no suman 100% y señala el error")
    void configurar_pesosNoSuman100_rechaza() {
        convocatoriaBloqueada();

        RubricaInvalidaException ex = assertThrows(RubricaInvalidaException.class, () -> service.configurar(10L,
                rubrica(criterio("Originalidad", "60"), criterio("Pertinencia", "30")), false));

        assertTrue(ex.getCampos().get("pesoPorcentual").contains("90"));
        assertTrue(ex.getMessage().contains("100%"));
        verify(rubricaRepository, never()).save(any());
        verify(criterioRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Criterio 2: también rechaza pesos que superan 100%")
    void configurar_pesosSuperan100_rechaza() {
        convocatoriaBloqueada();

        RubricaInvalidaException ex = assertThrows(RubricaInvalidaException.class, () -> service.configurar(10L,
                rubrica(criterio("Originalidad", "60"), criterio("Pertinencia", "40.01")), false));

        assertTrue(ex.getCampos().containsKey("pesoPorcentual"));
    }

    @Test
    @DisplayName("Rechaza criterios con el mismo nombre (sin distinguir mayúsculas)")
    void configurar_criterioRepetido_rechaza() {
        convocatoriaBloqueada();

        RubricaInvalidaException ex = assertThrows(RubricaInvalidaException.class, () -> service.configurar(10L,
                rubrica(criterio("Originalidad", "50"), criterio(" originalidad ", "50")), false));

        assertTrue(ex.getCampos().containsKey("criterios[1].nombre"));
        assertFalse(ex.getCampos().containsKey("pesoPorcentual"));
    }

    @Test
    @DisplayName("Rechaza una escala cuyo máximo no supera al mínimo")
    void configurar_escalaInvalida_rechaza() {
        convocatoriaBloqueada();

        RubricaInvalidaException ex = assertThrows(RubricaInvalidaException.class, () -> service.configurar(10L,
                new RubricaRequest("R", List.of(new CriterioRubricaRequest("Impacto", new BigDecimal("100"),
                        new BigDecimal("5"), new BigDecimal("5")))), false));

        assertTrue(ex.getCampos().containsKey("criterios[0].escalaMaxima"));
    }

    @Test
    @DisplayName("Convocatoria inexistente responde 404")
    void configurar_convocatoriaInexistente() {
        when(convocatoriaRepository.findByIdParaActualizar(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.configurar(99L, rubrica(criterio("A", "100")), false));
    }

    // ------------------------------------------------------------------
    // Modificación sin calificaciones
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Sin calificaciones registradas, modifica la misma rúbrica reemplazando sus criterios")
    void configurar_sinCalificaciones_actualizaEnSitio() {
        convocatoriaBloqueada();
        RubricaEvaluacion actual = rubricaActiva();
        List<CriterioRubrica> anteriores = List.of(CriterioRubrica.builder().id(1L).rubrica(actual).nombre("Viejo")
                .pesoPorcentual(new BigDecimal("100")).build());
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.of(actual));
        when(evaluacionRepository.countByCriterio_Rubrica_Id(4L)).thenReturn(0L);
        when(criterioRepository.findByRubrica_Id(4L)).thenReturn(anteriores);
        persistenciaSimulada(99L);

        RubricaEvaluacionService.ResultadoConfiguracion resultado = service.configurar(10L,
                rubrica(criterio("Originalidad", "50"), criterio("Pertinencia", "50")), false);

        assertFalse(resultado.creada());
        assertEquals(4L, resultado.rubrica().rubricaId());
        assertEquals("Rúbrica de ponencias", actual.getNombre());
        assertTrue(actual.getActivo());
        verify(criterioRepository).deleteAll(anteriores);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.RUBRICA_ACTUALIZADA), eq("rubricas_evaluacion"),
                eq(4L), anyMap());
    }

    // ------------------------------------------------------------------
    // Criterio 4
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 4: rúbrica en uso sin confirmar advierte el impacto y no cambia nada")
    void configurar_rubricaEnUso_sinConfirmar_advierte() {
        convocatoriaBloqueada();
        RubricaEvaluacion actual = rubricaActiva();
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.of(actual));
        when(evaluacionRepository.countByCriterio_Rubrica_Id(4L)).thenReturn(6L);
        when(evaluacionRepository.findAsignacionesConCalificacionesDeRubrica(4L))
                .thenReturn(List.of(asignacion(1L, 40L), asignacion(2L, 40L), asignacion(3L, 41L)));

        RubricaEnUsoException ex = assertThrows(RubricaEnUsoException.class, () -> service.configurar(10L,
                rubrica(criterio("Originalidad", "100")), false));

        assertEquals(6L, ex.getCalificacionesRegistradas());
        assertEquals(3L, ex.getAsignacionesAfectadas());
        assertEquals(2L, ex.getPropuestasAfectadas());
        assertTrue(actual.getActivo());
        verify(rubricaRepository, never()).save(any());
        verify(rubricaRepository, never()).saveAndFlush(any());
        verify(asignacionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Criterio 4: con confirmación crea una nueva versión, desactiva la anterior y reabre las evaluaciones")
    void configurar_rubricaEnUso_confirmado_reemplaza() {
        convocatoriaBloqueada();
        RubricaEvaluacion actual = rubricaActiva();
        AsignacionEvaluacion a1 = asignacion(1L, 40L);
        AsignacionEvaluacion a2 = asignacion(2L, 41L);
        ResultadoEvaluacion resultadoViejo = ResultadoEvaluacion.builder().id(8L).build();
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.of(actual));
        when(evaluacionRepository.countByCriterio_Rubrica_Id(4L)).thenReturn(4L);
        when(evaluacionRepository.findAsignacionesConCalificacionesDeRubrica(4L)).thenReturn(List.of(a1, a2));
        when(resultadoRepository.findByPropuesta_Id(40L)).thenReturn(Optional.of(resultadoViejo));
        when(resultadoRepository.findByPropuesta_Id(41L)).thenReturn(Optional.empty());
        persistenciaSimulada(5L);

        RubricaEvaluacionService.ResultadoConfiguracion resultado = service.configurar(10L,
                rubrica(criterio("Originalidad", "70"), criterio("Pertinencia", "30")), true);

        assertTrue(resultado.creada());
        assertEquals(5L, resultado.rubrica().rubricaId());
        assertEquals(2L, resultado.rubrica().asignacionesReabiertas());
        assertFalse(actual.getActivo());
        verify(rubricaRepository).saveAndFlush(actual);
        assertEquals(EstadoAsignacion.pendiente, a1.getEstado());
        assertEquals(EstadoAsignacion.pendiente, a2.getEstado());
        verify(asignacionRepository).saveAll(List.of(a1, a2));
        verify(resultadoRepository).delete(resultadoViejo);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.RUBRICA_REEMPLAZADA), eq("rubricas_evaluacion"),
                eq(5L), anyMap());
    }

    @Test
    @DisplayName("Criterio 4: los pesos se validan antes de advertir el impacto")
    void configurar_rubricaEnUso_pesosInvalidos_rechazaPrimero() {
        convocatoriaBloqueada();

        assertThrows(RubricaInvalidaException.class, () -> service.configurar(10L,
                rubrica(criterio("Originalidad", "50")), true));

        verify(evaluacionRepository, never()).countByCriterio_Rubrica_Id(any());
    }

    @Test
    @DisplayName("Impacto: sin rúbrica activa no requiere confirmación")
    void impacto_sinRubrica() {
        convocatoriaExiste();
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.empty());

        ImpactoRubricaResponse impacto = service.impacto(10L);

        assertNull(impacto.rubricaId());
        assertFalse(impacto.requiereConfirmacion());
    }

    @Test
    @DisplayName("Impacto: con calificaciones informa lo afectado y exige confirmación")
    void impacto_conCalificaciones() {
        convocatoriaExiste();
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.of(rubricaActiva()));
        when(evaluacionRepository.countByCriterio_Rubrica_Id(4L)).thenReturn(3L);
        when(evaluacionRepository.findAsignacionesConCalificacionesDeRubrica(4L))
                .thenReturn(List.of(asignacion(1L, 40L)));

        ImpactoRubricaResponse impacto = service.impacto(10L);

        assertEquals(4L, impacto.rubricaId());
        assertEquals(3L, impacto.calificacionesRegistradas());
        assertEquals(1L, impacto.asignacionesAfectadas());
        assertEquals(1L, impacto.propuestasAfectadas());
        assertTrue(impacto.requiereConfirmacion());
        assertFalse(impacto.bloqueada());
    }

    @Test
    @DisplayName("Obtener la rúbrica activa de una convocatoria sin rúbrica responde 404")
    void obtenerActiva_sinRubrica() {
        convocatoriaExiste();
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerActiva(10L));
    }

    // ------------------------------------------------------------------
    // HU-18: la rúbrica queda fija desde la primera clasificación
    // ------------------------------------------------------------------

    @Test
    @DisplayName("HU-18: con propuestas ya clasificadas no se puede modificar la rúbrica, aunque se confirme")
    void configurar_conPropuestasClasificadas_rechaza() {
        convocatoriaBloqueada();
        RubricaEvaluacion actual = rubricaActiva();
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.of(actual));
        when(resultadoRepository.countByPropuesta_Convocatoria_IdAndFechaClasificacionIsNotNull(10L)).thenReturn(2L);

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> service.configurar(10L, rubrica(criterio("Originalidad", "100")), true));

        assertTrue(ex.getMessage().contains("2 propuesta(s)"));
        assertTrue(actual.getActivo());
        verify(rubricaRepository, never()).save(any());
        verify(rubricaRepository, never()).saveAndFlush(any());
        verify(criterioRepository, never()).deleteAll(anyList());
        verify(asignacionRepository, never()).saveAll(anyList());
        verify(resultadoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("HU-18: configurar lee la convocatoria con bloqueo exclusivo")
    void configurar_bloqueaConvocatoria() {
        convocatoriaBloqueada();
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.empty());
        persistenciaSimulada(5L);

        service.configurar(10L, rubrica(criterio("Originalidad", "100")), false);

        verify(convocatoriaRepository).findByIdParaActualizar(10L);
        verify(convocatoriaRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Impacto: con propuestas clasificadas informa que la rúbrica está bloqueada")
    void impacto_conPropuestasClasificadas_bloqueada() {
        convocatoriaExiste();
        when(rubricaRepository.findByConvocatoria_IdAndActivoTrue(10L)).thenReturn(Optional.of(rubricaActiva()));
        when(resultadoRepository.countByPropuesta_Convocatoria_IdAndFechaClasificacionIsNotNull(10L)).thenReturn(3L);
        when(evaluacionRepository.countByCriterio_Rubrica_Id(4L)).thenReturn(5L);
        when(evaluacionRepository.findAsignacionesConCalificacionesDeRubrica(4L))
                .thenReturn(List.of(asignacion(1L, 40L)));

        ImpactoRubricaResponse impacto = service.impacto(10L);

        assertEquals(3L, impacto.propuestasClasificadas());
        assertTrue(impacto.bloqueada());
    }
}
