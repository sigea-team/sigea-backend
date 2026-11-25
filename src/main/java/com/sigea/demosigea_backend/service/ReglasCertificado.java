package com.sigea.demosigea_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas de emisión del certificado de asistente (HU-26, RF47, RN-01).
 * <p>
 * Cumple quien asistió a un porcentaje de las actividades en las que estaba inscrito (no canceladas ni por programar)
 * igual o mayor al mínimo del evento. La comparación se hace con la fracción exacta, sin redondear; el porcentaje
 * que se muestra se trunca a dos decimales para que nunca aparezca, por ejemplo, 80 % a quien tiene 79,999 %.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public final class ReglasCertificado {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private ReglasCertificado() {
    }

    /**
     * @param asistidas    actividades con asistencia registrada.
     * @param consideradas actividades inscritas que cuentan.
     * @param minimo       porcentaje mínimo del evento (0 &lt; mínimo ≤ 100).
     * @return {@code true} si cumple el porcentaje mínimo. Sin actividades consideradas no cumple.
     */
    public static boolean cumpleAsistencia(long asistidas, long consideradas, BigDecimal minimo) {
        if (consideradas <= 0 || asistidas < 0 || minimo == null) {
            return false;
        }
        // asistidas / consideradas ≥ mínimo / 100  ⇔  asistidas · 100 ≥ mínimo · consideradas
        return BigDecimal.valueOf(Math.min(asistidas, consideradas)).multiply(CIEN)
                .compareTo(minimo.multiply(BigDecimal.valueOf(consideradas))) >= 0;
    }

    /**
     * Porcentaje de asistencia truncado a dos decimales, o {@code null} si no hay actividades consideradas.
     */
    public static BigDecimal porcentaje(long asistidas, long consideradas) {
        if (consideradas <= 0) {
            return null;
        }
        return BigDecimal.valueOf(Math.min(Math.max(asistidas, 0), consideradas)).multiply(CIEN)
                .divide(BigDecimal.valueOf(consideradas), 2, RoundingMode.DOWN);
    }

    /** «85,71 %», «80 %». */
    public static String formatear(BigDecimal porcentaje) {
        if (porcentaje == null) {
            return "-";
        }
        return porcentaje.stripTrailingZeros().toPlainString().replace('.', ',') + " %";
    }
}
