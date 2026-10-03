package com.sigea.demosigea_backend.service;

import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilidad para construir y serializar a JSON el campo {@code detalle} de la auditoría
 * ("datos afectados", HU-03 Criterio 1).
 * <p>
 * Se implementa sin depender de Jackson para que el formato guardado en la base de datos
 * no cambie si el proyecto actualiza la versión de la librería de serialización.
 * Soporta: {@code null}, texto, números, booleanos, fechas, enums, colecciones y mapas anidados.
 * </p>
 */
public final class DetalleAuditoria {

    private DetalleAuditoria() {
    }

    /**
     * Construye un mapa ordenado a partir de pares clave/valor.
     * Ejemplo: {@code DetalleAuditoria.de("nombre", "ADMIN", "permisos", List.of("ROLES_VER"))}.
     */
    public static Map<String, Object> de(Object... clavesYValores) {
        if (clavesYValores.length % 2 != 0) {
            throw new IllegalArgumentException("Se esperaban pares clave/valor");
        }
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < clavesYValores.length; i += 2) {
            mapa.put(String.valueOf(clavesYValores[i]), clavesYValores[i + 1]);
        }
        return mapa;
    }

    /** Serializa el valor a JSON. */
    public static String aJson(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(sb, valor);
        return sb.toString();
    }

    private static void escribir(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                escribirTexto(sb, String.valueOf(e.getKey()));
                sb.append(':');
                escribir(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Collection<?> c) {
            sb.append('[');
            boolean primero = true;
            for (Object o : c) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                escribir(sb, o);
            }
            sb.append(']');
        } else if (v instanceof Enum<?> e) {
            escribirTexto(sb, e.name());
        } else if (v instanceof TemporalAccessor) {
            escribirTexto(sb, v.toString());
        } else {
            escribirTexto(sb, v.toString());
        }
    }

    private static void escribirTexto(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        sb.append('"');
    }
}
