package com.sigea.demosigea_backend.model;

import java.math.BigDecimal;

/**
 * Reglas de clasificación de una propuesta según su puntuación ponderada (HU-18, RF27).
 * <p>
 * La puntuación ponderada (HU-17) está en escala 0 - 10. Con los umbrales de la convocatoria:
 * <ul>
 *   <li>puntuación &ge; {@code umbralAceptacion} &rarr; {@code aprobada} (aceptada);</li>
 *   <li>puntuación &ge; {@code umbralAjustes} &rarr; {@code aprobada_con_ajustes} (aceptada con modificaciones);</li>
 *   <li>en otro caso &rarr; {@code rechazada}.</li>
 * </ul>
 * Los límites son inclusivos: una puntuación igual al umbral queda en la categoría superior.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public final class ReglasClasificacion {

    /** Umbral por defecto para aprobar (el que usaba HU-17). */
    public static final BigDecimal UMBRAL_ACEPTACION_POR_DEFECTO = new BigDecimal("7.00");

    /** Umbral por defecto para aprobar con ajustes (el que usaba HU-17). */
    public static final BigDecimal UMBRAL_AJUSTES_POR_DEFECTO = new BigDecimal("5.00");

    /** Límite inferior de la escala de la puntuación ponderada. */
    public static final BigDecimal ESCALA_MINIMA = BigDecimal.ZERO;

    /** Límite superior de la escala de la puntuación ponderada. */
    public static final BigDecimal ESCALA_MAXIMA = BigDecimal.TEN;

    private ReglasClasificacion() {
    }

    /**
     * Clasifica una puntuación con los umbrales indicados.
     *
     * @param puntuacion       puntuación ponderada (0 - 10).
     * @param umbralAceptacion umbral para {@code aprobada} ({@code null} = valor por defecto).
     * @param umbralAjustes    umbral para {@code aprobada_con_ajustes} ({@code null} = valor por defecto).
     * @return clasificación correspondiente.
     * @throws IllegalArgumentException si la puntuación es {@code null}.
     */
    public static ClasificacionResultado clasificar(BigDecimal puntuacion, BigDecimal umbralAceptacion,
                                                    BigDecimal umbralAjustes) {
        if (puntuacion == null) {
            throw new IllegalArgumentException("La puntuación ponderada es obligatoria para clasificar.");
        }
        BigDecimal aceptacion = umbralAceptacion != null ? umbralAceptacion : UMBRAL_ACEPTACION_POR_DEFECTO;
        BigDecimal ajustes = umbralAjustes != null ? umbralAjustes : UMBRAL_AJUSTES_POR_DEFECTO;
        if (puntuacion.compareTo(aceptacion) >= 0) {
            return ClasificacionResultado.aprobada;
        }
        if (puntuacion.compareTo(ajustes) >= 0) {
            return ClasificacionResultado.aprobada_con_ajustes;
        }
        return ClasificacionResultado.rechazada;
    }

    /**
     * Clasifica una puntuación con los umbrales de la convocatoria.
     *
     * @param puntuacion   puntuación ponderada (0 - 10).
     * @param convocatoria convocatoria de la propuesta ({@code null} = umbrales por defecto).
     * @return clasificación correspondiente.
     */
    public static ClasificacionResultado clasificar(BigDecimal puntuacion, Convocatoria convocatoria) {
        return clasificar(puntuacion, umbralAceptacion(convocatoria), umbralAjustes(convocatoria));
    }

    /** @return umbral de aceptación de la convocatoria o el valor por defecto. */
    public static BigDecimal umbralAceptacion(Convocatoria convocatoria) {
        return convocatoria != null && convocatoria.getUmbralAceptacion() != null
                ? convocatoria.getUmbralAceptacion() : UMBRAL_ACEPTACION_POR_DEFECTO;
    }

    /** @return umbral de aprobación con ajustes de la convocatoria o el valor por defecto. */
    public static BigDecimal umbralAjustes(Convocatoria convocatoria) {
        return convocatoria != null && convocatoria.getUmbralAjustes() != null
                ? convocatoria.getUmbralAjustes() : UMBRAL_AJUSTES_POR_DEFECTO;
    }

    /**
     * Valida un par de umbrales: ambos en 0 - 10 y el de ajustes no mayor que el de aceptación.
     *
     * @param umbralAceptacion umbral para aprobar.
     * @param umbralAjustes    umbral para aprobar con ajustes.
     * @return mensaje del error, o {@code null} si los umbrales son válidos.
     */
    public static String validarUmbrales(BigDecimal umbralAceptacion, BigDecimal umbralAjustes) {
        if (umbralAceptacion == null || umbralAjustes == null) {
            return "Debe indicar los dos umbrales de clasificación.";
        }
        if (fueraDeEscala(umbralAceptacion) || fueraDeEscala(umbralAjustes)) {
            return "Los umbrales de clasificación deben estar entre 0 y 10.";
        }
        if (umbralAjustes.compareTo(umbralAceptacion) > 0) {
            return "El umbral de aprobación con ajustes no puede ser mayor que el umbral de aprobación.";
        }
        return null;
    }

    private static boolean fueraDeEscala(BigDecimal valor) {
        return valor.compareTo(ESCALA_MINIMA) < 0 || valor.compareTo(ESCALA_MAXIMA) > 0;
    }

    /**
     * Estado que toma la propuesta al aplicar la clasificación.
     *
     * @param clasificacion clasificación aplicada.
     * @return {@code aprobada}, {@code ajustes} o {@code rechazada}.
     */
    public static EstadoPropuesta estadoPropuesta(ClasificacionResultado clasificacion) {
        return switch (clasificacion) {
            case aprobada -> EstadoPropuesta.aprobada;
            case aprobada_con_ajustes -> EstadoPropuesta.ajustes;
            case rechazada -> EstadoPropuesta.rechazada;
        };
    }

    /**
     * Nombre legible de la clasificación, como lo usa la historia de usuario.
     *
     * @param clasificacion clasificación ({@code null} permitido).
     * @return etiqueta, o {@code null} si la clasificación es {@code null}.
     */
    public static String etiqueta(ClasificacionResultado clasificacion) {
        if (clasificacion == null) {
            return null;
        }
        return switch (clasificacion) {
            case aprobada -> "Aceptada";
            case aprobada_con_ajustes -> "Aceptada con modificaciones";
            case rechazada -> "Rechazada";
        };
    }
}
