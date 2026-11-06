package com.sigea.demosigea_backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas de las reglas de clasificación por umbrales (HU-18, Criterio 1).
 */
class ReglasClasificacionTest {

    @ParameterizedTest(name = "{0} con umbrales por defecto -> {1}")
    @CsvSource({
            "10.00, aprobada",
            "7.00, aprobada",
            "6.99, aprobada_con_ajustes",
            "5.00, aprobada_con_ajustes",
            "4.99, rechazada",
            "0.00, rechazada"
    })
    @DisplayName("Criterio 1: los límites de los umbrales son inclusivos")
    void clasificar_umbralesPorDefecto(String puntuacion, ClasificacionResultado esperada) {
        assertEquals(esperada, ReglasClasificacion.clasificar(new BigDecimal(puntuacion), (Convocatoria) null));
    }

    @Test
    @DisplayName("Criterio 1: usa los umbrales definidos en la convocatoria")
    void clasificar_umbralesDeLaConvocatoria() {
        Convocatoria convocatoria = Convocatoria.builder()
                .umbralAceptacion(new BigDecimal("8.50"))
                .umbralAjustes(new BigDecimal("6.00"))
                .build();

        assertEquals(ClasificacionResultado.aprobada,
                ReglasClasificacion.clasificar(new BigDecimal("8.50"), convocatoria));
        assertEquals(ClasificacionResultado.aprobada_con_ajustes,
                ReglasClasificacion.clasificar(new BigDecimal("8.00"), convocatoria));
        assertEquals(ClasificacionResultado.rechazada,
                ReglasClasificacion.clasificar(new BigDecimal("5.99"), convocatoria));
    }

    @Test
    @DisplayName("Una convocatoria nueva toma los umbrales 7 y 5")
    void convocatoria_umbralesPorDefecto() {
        Convocatoria convocatoria = Convocatoria.builder().build();
        assertEquals(0, new BigDecimal("7").compareTo(convocatoria.getUmbralAceptacion()));
        assertEquals(0, new BigDecimal("5").compareTo(convocatoria.getUmbralAjustes()));
    }

    @Test
    @DisplayName("Cada clasificación corresponde a un estado de la propuesta")
    void estadoPropuesta() {
        assertEquals(EstadoPropuesta.aprobada, ReglasClasificacion.estadoPropuesta(ClasificacionResultado.aprobada));
        assertEquals(EstadoPropuesta.ajustes,
                ReglasClasificacion.estadoPropuesta(ClasificacionResultado.aprobada_con_ajustes));
        assertEquals(EstadoPropuesta.rechazada, ReglasClasificacion.estadoPropuesta(ClasificacionResultado.rechazada));
        assertEquals("Aceptada con modificaciones",
                ReglasClasificacion.etiqueta(ClasificacionResultado.aprobada_con_ajustes));
    }

    @Test
    @DisplayName("Valida que los umbrales estén en 0 - 10 y en orden")
    void validarUmbrales() {
        assertNull(ReglasClasificacion.validarUmbrales(new BigDecimal("7"), new BigDecimal("5")));
        assertNull(ReglasClasificacion.validarUmbrales(new BigDecimal("6"), new BigDecimal("6")));
        assertNotNull(ReglasClasificacion.validarUmbrales(new BigDecimal("5"), new BigDecimal("7")));
        assertNotNull(ReglasClasificacion.validarUmbrales(new BigDecimal("10.5"), new BigDecimal("5")));
        assertNotNull(ReglasClasificacion.validarUmbrales(new BigDecimal("7"), new BigDecimal("-1")));
        assertNotNull(ReglasClasificacion.validarUmbrales(null, new BigDecimal("5")));
    }
}
