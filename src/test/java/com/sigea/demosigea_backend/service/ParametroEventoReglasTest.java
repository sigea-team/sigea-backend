package com.sigea.demosigea_backend.service;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParametroEventoReglasTest {

    private static final Set<String> INDICES = TipoActividadService.INDICES_NOMBRE;

    private static DataIntegrityViolationException violacion(String sqlState, String restriccion) {
        return new DataIntegrityViolationException("violación", new ConstraintViolationException(
                "violación", new SQLException("violación", sqlState), restriccion));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ux_tipos_actividad_evento_nombre_ci",
            "\"ux_tipos_actividad_evento_nombre_ci\"",
            "public.ux_tipos_actividad_evento_nombre_ci",
            "\"public\".\"ux_tipos_actividad_evento_nombre_ci\"",
            "PUBLIC.UX_TIPOS_ACTIVIDAD_EVENTO_NOMBRE_CI",
            "otro_esquema.ux_tipos_actividad_evento_nombre_ci"
    })
    @DisplayName("Reconoce el índice aunque llegue con comillas, esquema o mayúsculas")
    void esViolacionUnicidad_nombreConVariantes_seReconoce(String nombreReportado) {
        assertTrue(ParametroEventoReglas.esViolacionUnicidad(violacion("23505", nombreReportado), INDICES));
    }

    @Test
    @DisplayName("No confunde un índice cuyo nombre solo termina igual")
    void esViolacionUnicidad_nombreParecido_noSeReconoce() {
        assertFalse(ParametroEventoReglas.esViolacionUnicidad(
                violacion("23505", "otro_ux_tipos_actividad_evento_nombre_ci"), INDICES));
    }

    @Test
    @DisplayName("Otro SQLState o nombre nulo no se interpretan como duplicado")
    void esViolacionUnicidad_otroSqlStateONulo_noSeReconoce() {
        assertFalse(ParametroEventoReglas.esViolacionUnicidad(
                violacion("23503", "ux_tipos_actividad_evento_nombre_ci"), INDICES));
        assertFalse(ParametroEventoReglas.esViolacionUnicidad(violacion("23505", null), INDICES));
    }

    @Test
    @DisplayName("normalizarNombreRestriccion quita comillas, esquema y mayúsculas")
    void normalizarNombreRestriccion() {
        assertEquals("ux_a", ParametroEventoReglas.normalizarNombreRestriccion("\"Public\".\"UX_A\""));
        assertEquals("", ParametroEventoReglas.normalizarNombreRestriccion(null));
    }
}
