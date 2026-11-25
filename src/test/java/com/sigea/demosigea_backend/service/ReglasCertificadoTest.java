package com.sigea.demosigea_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link ReglasCertificado} (HU-26, RN-01).
 */
class ReglasCertificadoTest {

    private static final BigDecimal OCHENTA = new BigDecimal("80.00");

    @Test
    @DisplayName("Cumple con un porcentaje igual o mayor al mínimo, comparando la fracción exacta")
    void cumpleAsistencia() {
        assertTrue(ReglasCertificado.cumpleAsistencia(4, 5, OCHENTA));
        assertTrue(ReglasCertificado.cumpleAsistencia(5, 5, OCHENTA));
        assertFalse(ReglasCertificado.cumpleAsistencia(3, 5, OCHENTA));
        // 79,99 % no alcanza el 80 % aunque al redondear diera 80
        assertFalse(ReglasCertificado.cumpleAsistencia(7999, 10000, OCHENTA));
        assertTrue(ReglasCertificado.cumpleAsistencia(2, 3, new BigDecimal("66.66")));
        assertFalse(ReglasCertificado.cumpleAsistencia(2, 3, new BigDecimal("66.67")));
        assertTrue(ReglasCertificado.cumpleAsistencia(1, 1, new BigDecimal("100")));
    }

    @Test
    @DisplayName("Sin actividades consideradas o sin mínimo no cumple")
    void casosLimite() {
        assertFalse(ReglasCertificado.cumpleAsistencia(0, 0, OCHENTA));
        assertFalse(ReglasCertificado.cumpleAsistencia(1, 1, null));
        assertFalse(ReglasCertificado.cumpleAsistencia(-1, 2, OCHENTA));
    }

    @Test
    @DisplayName("El porcentaje se trunca a dos decimales y no pasa de 100")
    void porcentaje() {
        assertEquals(new BigDecimal("42.85"), ReglasCertificado.porcentaje(3, 7));
        assertEquals(new BigDecimal("66.66"), ReglasCertificado.porcentaje(2, 3));
        assertEquals(new BigDecimal("100.00"), ReglasCertificado.porcentaje(5, 5));
        assertEquals(new BigDecimal("100.00"), ReglasCertificado.porcentaje(6, 5));
        assertEquals(new BigDecimal("0.00"), ReglasCertificado.porcentaje(0, 4));
        assertNull(ReglasCertificado.porcentaje(0, 0));
    }

    @Test
    @DisplayName("Formato para mostrar: coma decimal y sin ceros sobrantes")
    void formatear() {
        assertEquals("80 %", ReglasCertificado.formatear(OCHENTA));
        assertEquals("85,71 %", ReglasCertificado.formatear(new BigDecimal("85.71")));
        assertEquals("42,5 %", ReglasCertificado.formatear(new BigDecimal("42.50")));
        assertEquals("100 %", ReglasCertificado.formatear(new BigDecimal("100.00")));
        assertEquals("0 %", ReglasCertificado.formatear(new BigDecimal("0.00")));
        assertEquals("-", ReglasCertificado.formatear(null));
    }
}
