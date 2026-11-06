package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.model.CriterioRubrica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * HU-15: la puntuación ponderada de HU-17 se calcula sobre 0 - 10 aunque un criterio use otra escala.
 */
class EvaluacionEscalaTest {

    @Test
    @DisplayName("Con la escala por defecto 0-10 el valor no cambia")
    void escalaPorDefecto_noCambia() {
        CriterioRubrica criterio = CriterioRubrica.builder().nombre("Originalidad").pesoPorcentual(BigDecimal.TEN).build();
        BigDecimal valor = new BigDecimal("7.5000");

        assertSame(valor, EvaluacionService.normalizarADiez(valor, criterio));
    }

    @Test
    @DisplayName("Una escala 1-5 se lleva a 0-10")
    void escala1a5_seNormaliza() {
        CriterioRubrica criterio = CriterioRubrica.builder().nombre("Impacto").pesoPorcentual(BigDecimal.TEN)
                .escalaMinima(BigDecimal.ONE).escalaMaxima(new BigDecimal("5")).build();

        assertEquals(0, new BigDecimal("7.5").compareTo(EvaluacionService.normalizarADiez(new BigDecimal("4"), criterio)));
        assertEquals(0, BigDecimal.TEN.compareTo(EvaluacionService.normalizarADiez(new BigDecimal("5"), criterio)));
        assertEquals(0, BigDecimal.ZERO.compareTo(EvaluacionService.normalizarADiez(BigDecimal.ONE, criterio)));
    }
}
