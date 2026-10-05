package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import org.hibernate.exception.ConstraintViolationException;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reglas compartidas por los catálogos de parámetros del evento (HU-05): tipos de actividad
 * (RF06) y líneas temáticas (RF07).
 * <ul>
 *   <li>Normalización de nombres para la validación de duplicidad (Criterio 3).</li>
 *   <li>Estados del evento en los que los catálogos pueden modificarse.</li>
 *   <li>Interpretación de violaciones de restricciones de PostgreSQL (unicidad y llave foránea)
 *       para responder 409 en vez de 500 ante peticiones concurrentes.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
final class ParametroEventoReglas {

    /** SQLState de PostgreSQL: violación de unicidad. */
    static final String SQLSTATE_VIOLACION_UNICIDAD = "23505";

    /** SQLState de PostgreSQL: violación de llave foránea (el registro está referenciado). */
    static final String SQLSTATE_VIOLACION_LLAVE_FORANEA = "23503";

    /**
     * Estados del evento en los que los catálogos pueden modificarse (crear, editar, eliminar).
     * <p>
     * Misma regla que el comité organizador (HU-06): los tipos de actividad y las líneas temáticas
     * se usan durante la organización, que ocurre principalmente en {@code habilitado}
     * (convocatoria, evaluación y agenda). Desde {@code en_ejecucion} quedan congelados para que la
     * agenda, los reportes y la memoria histórica (RF55) se construyan sobre una clasificación estable.
     * </p>
     */
    static final Set<EstadoEvento> ESTADOS_CATALOGO_MODIFICABLE =
            EnumSet.of(EstadoEvento.en_configuracion, EstadoEvento.habilitado);

    private ParametroEventoReglas() {
    }

    /**
     * Quita espacios al inicio y al final y colapsa los espacios internos repetidos, para que
     * "Taller", " taller " y "TALLER" se consideren el mismo nombre (Criterio 3).
     *
     * @param nombre Nombre recibido.
     * @return Nombre normalizado (o {@code null} si se recibió {@code null}).
     */
    static String normalizarNombre(String nombre) {
        return nombre == null ? null : nombre.trim().replaceAll("\\s+", " ");
    }

    /**
     * @param descripcion Descripción recibida.
     * @return Descripción sin espacios sobrantes, o {@code null} si viene vacía.
     */
    static String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        return descripcion.trim();
    }

    /**
     * Valida que el evento esté en un estado de {@link #ESTADOS_CATALOGO_MODIFICABLE}.
     *
     * @param evento   Evento a validar.
     * @param catalogo Nombre legible del catálogo para el mensaje (ej. "Los tipos de actividad").
     * @throws OperacionNoPermitidaException si el evento está en ejecución o cerrado.
     */
    static void validarEventoModificable(Evento evento, String catalogo) {
        if (!ESTADOS_CATALOGO_MODIFICABLE.contains(evento.getEstado())) {
            String permitidos = ESTADOS_CATALOGO_MODIFICABLE.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(" o "));
            throw new OperacionNoPermitidaException(String.format(
                    "%s del evento '%s' ya no pueden modificarse (estado actual: %s). "
                            + "Solo se permiten cambios mientras el evento está en %s.",
                    catalogo, evento.getNombre(), evento.getEstado(), permitidos));
        }
    }

    /**
     * Indica si la excepción es una violación de unicidad sobre alguno de los índices indicados.
     * Se evalúa por SQLState y nombre de la restricción, no por el texto del mensaje.
     */
    static boolean esViolacionUnicidad(Throwable ex, Set<String> indices) {
        return buscarViolacion(ex)
                .filter(v -> SQLSTATE_VIOLACION_UNICIDAD.equals(v.getSQLState()))
                .map(ConstraintViolationException::getConstraintName)
                .map(ParametroEventoReglas::normalizarNombreRestriccion)
                .filter(indices::contains)
                .isPresent();
    }

    /**
     * Normaliza el nombre de una restricción o índice reportado por la base de datos para poder
     * compararlo con los nombres declarados en las migraciones.
     * <p>
     * Según el driver y la versión de Hibernate, el nombre puede llegar entre comillas y/o
     * calificado con el esquema: {@code ux_x}, {@code "ux_x"}, {@code public.ux_x} o
     * {@code "public"."ux_x"}. Se quitan las comillas, se conserva solo la parte posterior al
     * último punto (el esquema nunca forma parte del nombre del índice) y se pasa a minúsculas.
     * </p>
     * <p>
     * Se compara por igualdad exacta después de normalizar, no con {@code endsWith}: así un índice
     * llamado, por ejemplo, {@code otro_ux_x} no se confunde con {@code ux_x}.
     * </p>
     *
     * @param nombre Nombre recibido (puede ser {@code null}).
     * @return Nombre sin comillas, sin esquema y en minúsculas; cadena vacía si es {@code null}.
     */
    static String normalizarNombreRestriccion(String nombre) {
        if (nombre == null) {
            return "";
        }
        String sinComillas = nombre.replace("\"", "").trim();
        String sinEsquema = sinComillas.substring(sinComillas.lastIndexOf('.') + 1);
        return sinEsquema.toLowerCase(Locale.ROOT);
    }

    /**
     * Indica si la excepción es una violación de llave foránea (el registro que se intenta
     * eliminar está referenciado por otra tabla).
     */
    static boolean esViolacionLlaveForanea(Throwable ex) {
        return buscarViolacion(ex)
                .map(ConstraintViolationException::getSQLState)
                .filter(SQLSTATE_VIOLACION_LLAVE_FORANEA::equals)
                .isPresent();
    }

    private static Optional<ConstraintViolationException> buscarViolacion(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause() == causa ? null : causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                return Optional.of(violacion);
            }
        }
        return Optional.empty();
    }
}
