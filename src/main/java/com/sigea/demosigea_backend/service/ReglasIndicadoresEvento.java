package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.indicador.DashboardIndicadoresResponse;
import com.sigea.demosigea_backend.dto.indicador.GrupoIndicadoresResponse;
import com.sigea.demosigea_backend.dto.indicador.IndicadorEventoResponse;
import com.sigea.demosigea_backend.dto.indicador.MedidaSerieResponse;
import com.sigea.demosigea_backend.dto.indicador.PuntoSerieResponse;
import com.sigea.demosigea_backend.dto.indicador.SerieIndicadorResponse;
import com.sigea.demosigea_backend.model.EstadoEvento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reglas del dashboard de indicadores de un evento (HU-29): agrupa los indicadores, decide cuáles tienen datos
 * suficientes (Criterio 2) y arma las series de las gráficas (Criterio 1) a partir de las filas que devuelve
 * {@link com.sigea.demosigea_backend.repository.IndicadorEventoRepository}.
 * <p>
 * Reglas de disponibilidad:
 * </p>
 * <ul>
 *   <li>Un grupo tiene datos si alguno de sus indicadores vale más que cero (por ejemplo, Asistencia tiene datos si
 *       hay al menos un registro de asistencia).</li>
 *   <li>Un indicador está disponible si su grupo tiene datos y su valor se puede calcular (las tasas sin
 *       denominador, la satisfacción sin preguntas de escala o la ejecución sin presupuesto aprobado no se
 *       calculan). Si no está disponible, su valor es {@code null} y trae el motivo.</li>
 *   <li>Una serie sin datos llega sin puntos y con su mensaje.</li>
 * </ul>
 * Clase sin estado ni dependencias de Spring, para probarla directamente.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public final class ReglasIndicadoresEvento {

    static final String GRAFICO_BARRAS = "barras";
    static final String GRAFICO_BARRAS_AGRUPADAS = "barras_agrupadas";
    static final String GRAFICO_CIRCULAR = "circular";
    static final String GRAFICO_LINEA = "linea";

    static final String MENSAJE_SIN_DATOS = "El evento todavía no tiene inscripciones, asistencia, actividades, "
            + "propuestas, encuestas ni presupuesto registrados. Los indicadores se mostrarán a medida que se "
            + "registre información.";
    static final String MENSAJE_ARCHIVADO = "El evento fue cerrado formalmente y está en la memoria histórica: "
            + "su información ya no cambia y estos indicadores son definitivos.";
    static final String MENSAJE_SIN_DATOS_ARCHIVADO = "El evento no tuvo información registrada para los "
            + "indicadores. " + MENSAJE_ARCHIVADO;
    static final String MOTIVO_GENERICO = "No hay datos suficientes para calcular este indicador.";

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /** Grupo del dashboard: nombre y aviso cuando el evento no tiene datos para él. */
    private record Grupo(String nombre, String mensajeSinDatos) {
    }

    /** Grupos en el orden en que se muestran. */
    private static final Map<String, Grupo> GRUPOS = new LinkedHashMap<>();

    static {
        GRUPOS.put("PARTICIPACION", new Grupo("Participación",
                "El evento no tiene inscripciones ni conferencistas registrados."));
        GRUPOS.put("ASISTENCIA", new Grupo("Asistencia",
                "No hay asistencia registrada en las actividades del evento."));
        GRUPOS.put("ACTIVIDADES", new Grupo("Actividades",
                "El evento no tiene actividades programadas."));
        GRUPOS.put("PROPUESTAS", new Grupo("Propuestas",
                "El evento no tiene propuestas recibidas en sus convocatorias."));
        GRUPOS.put("SATISFACCION", new Grupo("Satisfacción",
                "No hay respuestas a encuestas de satisfacción del evento."));
        GRUPOS.put("PRESUPUESTO", new Grupo("Presupuesto",
                "El evento no tiene presupuesto aprobado ni gastos registrados."));
        GRUPOS.put("OTROS", new Grupo("Otros indicadores",
                "No hay datos suficientes para estos indicadores."));
    }

    /** Grupo de cada indicador (los códigos son los de {@code ConsultasIndicadoresEvento}). */
    private static final Map<String, String> GRUPO_DE_INDICADOR = Map.ofEntries(
            Map.entry("INSCRITOS", "PARTICIPACION"),
            Map.entry("CONFERENCISTAS", "PARTICIPACION"),
            Map.entry("ASISTENTES", "ASISTENCIA"),
            Map.entry("TASA_ASISTENCIA", "ASISTENCIA"),
            Map.entry("ASISTENCIAS_REGISTRADAS", "ASISTENCIA"),
            Map.entry("ACTIVIDADES_REALIZADAS", "ACTIVIDADES"),
            Map.entry("ACTIVIDADES_CANCELADAS", "ACTIVIDADES"),
            Map.entry("PROPUESTAS_RECIBIDAS", "PROPUESTAS"),
            Map.entry("PROPUESTAS_APROBADAS", "PROPUESTAS"),
            Map.entry("PROPUESTAS_RECHAZADAS", "PROPUESTAS"),
            Map.entry("TASA_APROBACION", "PROPUESTAS"),
            Map.entry("RESPUESTAS_ENCUESTAS", "SATISFACCION"),
            Map.entry("SATISFACCION_PROMEDIO", "SATISFACCION"),
            Map.entry("PRESUPUESTO_APROBADO", "PRESUPUESTO"),
            Map.entry("PRESUPUESTO_EJECUTADO", "PRESUPUESTO"),
            Map.entry("EJECUCION_PRESUPUESTAL", "PRESUPUESTO"));

    /** Motivo de los indicadores que no se calculan aunque su grupo tenga datos. */
    private static final Map<String, String> MOTIVO_SIN_VALOR = Map.of(
            "TASA_ASISTENCIA", "No hay inscripciones registradas para calcular la tasa de asistencia.",
            "TASA_APROBACION", "No hay propuestas recibidas para calcular la tasa de aprobación.",
            "SATISFACCION_PROMEDIO", "Las respuestas recibidas no incluyen preguntas de escala; no se puede "
                    + "calcular la satisfacción promedio.",
            "PRESUPUESTO_APROBADO", "El evento no tiene presupuesto aprobado.",
            "EJECUCION_PRESUPUESTAL", "Sin presupuesto aprobado (o aprobado en cero) no se puede calcular el "
                    + "porcentaje de ejecución.");

    private static final Map<String, String> ETIQUETA_ESTADO_PROPUESTA = new LinkedHashMap<>();

    static {
        ETIQUETA_ESTADO_PROPUESTA.put("recibida", "Recibidas");
        ETIQUETA_ESTADO_PROPUESTA.put("en_evaluacion", "En evaluación");
        ETIQUETA_ESTADO_PROPUESTA.put("ajustes", "Aprobadas con ajustes");
        ETIQUETA_ESTADO_PROPUESTA.put("aprobada", "Aprobadas");
        ETIQUETA_ESTADO_PROPUESTA.put("rechazada", "Rechazadas");
    }

    private static final MedidaSerieResponse MEDIDA_INSCRITOS =
            new MedidaSerieResponse("inscritos", "Inscritos", "personas");
    private static final MedidaSerieResponse MEDIDA_ASISTENTES =
            new MedidaSerieResponse("asistentes", "Asistentes", "personas");
    private static final MedidaSerieResponse MEDIDA_TASA_ASISTENCIA =
            new MedidaSerieResponse("tasaAsistencia", "Tasa de asistencia", "%");

    private ReglasIndicadoresEvento() {
    }

    /**
     * Filas leídas de la base de datos para un evento (ver {@code IndicadorEventoRepository}).
     *
     * @param indicadores             {@code [codigo, nombre, valor, unidad]}.
     * @param inscritosPorCategoria   {@code [categoria, cantidad]}.
     * @param inscripcionesPorFecha   {@code [fecha, cantidad]}.
     * @param asistenciaPorActividad  {@code [actividadId, nombre, fecha, horaInicio, inscritos, asistentes]}.
     * @param propuestasPorEstado     {@code [estado, cantidad]}.
     * @param satisfaccionPorEncuesta {@code [encuestaId, titulo, estado, nombreActividad, respuestas, satisfaccion]}.
     * @param presupuestoPorRubro     {@code [rubroId, nombre, proyectado, ejecutado]}.
     */
    public record Datos(
            List<Object[]> indicadores,
            List<Object[]> inscritosPorCategoria,
            List<Object[]> inscripcionesPorFecha,
            List<Object[]> asistenciaPorActividad,
            List<Object[]> propuestasPorEstado,
            List<Object[]> satisfaccionPorEncuesta,
            List<Object[]> presupuestoPorRubro
    ) {
    }

    /**
     * Arma la respuesta del dashboard.
     *
     * @param eventoId     evento.
     * @param nombreEvento nombre del evento.
     * @param estado       estado actual del evento.
     * @param archivado    si está en la memoria histórica (HU-28).
     * @param fechaCalculo momento del cálculo.
     * @param datos        filas leídas de la base de datos.
     */
    public static DashboardIndicadoresResponse construir(Long eventoId, String nombreEvento, EstadoEvento estado,
                                                         boolean archivado, LocalDateTime fechaCalculo, Datos datos) {
        List<IndicadorCalculado> calculados = new ArrayList<>();
        for (Object[] fila : vacioSiNulo(datos.indicadores())) {
            String codigo = texto(fila[0]);
            calculados.add(new IndicadorCalculado(codigo, texto(fila[1]),
                    GRUPO_DE_INDICADOR.getOrDefault(codigo, "OTROS"), decimal(fila[2]), texto(fila[3])));
        }

        Map<String, Boolean> conDatos = new LinkedHashMap<>();
        for (IndicadorCalculado indicador : calculados) {
            boolean positivo = indicador.valor() != null && indicador.valor().signum() > 0;
            conDatos.merge(indicador.grupo(), positivo, Boolean::logicalOr);
        }

        List<GrupoIndicadoresResponse> grupos = new ArrayList<>();
        GRUPOS.forEach((codigo, grupo) -> {
            Boolean hay = conDatos.get(codigo);
            if (hay != null) {
                grupos.add(new GrupoIndicadoresResponse(codigo, grupo.nombre(), hay,
                        hay ? null : grupo.mensajeSinDatos()));
            }
        });

        List<IndicadorEventoResponse> indicadores = calculados.stream().map(indicador -> {
            boolean grupoConDatos = conDatos.getOrDefault(indicador.grupo(), false);
            if (!grupoConDatos) {
                return new IndicadorEventoResponse(indicador.codigo(), indicador.nombre(), indicador.grupo(), null,
                        indicador.unidad(), false, GRUPOS.get(indicador.grupo()).mensajeSinDatos());
            }
            if (indicador.valor() == null) {
                return new IndicadorEventoResponse(indicador.codigo(), indicador.nombre(), indicador.grupo(), null,
                        indicador.unidad(), false, MOTIVO_SIN_VALOR.getOrDefault(indicador.codigo(), MOTIVO_GENERICO));
            }
            return new IndicadorEventoResponse(indicador.codigo(), indicador.nombre(), indicador.grupo(),
                    indicador.valor(), indicador.unidad(), true, null);
        }).toList();

        boolean hayDatos = conDatos.containsValue(true);
        String mensaje;
        if (!hayDatos) {
            mensaje = archivado ? MENSAJE_SIN_DATOS_ARCHIVADO : MENSAJE_SIN_DATOS;
        } else {
            mensaje = archivado ? MENSAJE_ARCHIVADO : null;
        }

        List<SerieIndicadorResponse> series = List.of(
                inscripcionesPorCategoria(datos.inscritosPorCategoria()),
                inscripcionesPorFecha(datos.inscripcionesPorFecha()),
                asistenciaPorActividad(datos.asistenciaPorActividad()),
                asistenciaPorDia(datos.asistenciaPorActividad()),
                propuestasPorEstado(datos.propuestasPorEstado()),
                satisfaccionPorEncuesta(datos.satisfaccionPorEncuesta()),
                presupuestoPorRubro(datos.presupuestoPorRubro()));

        return new DashboardIndicadoresResponse(eventoId, nombreEvento, estado, archivado, fechaCalculo, hayDatos,
                mensaje, grupos, indicadores, series);
    }

    private record IndicadorCalculado(String codigo, String nombre, String grupo, BigDecimal valor, String unidad) {
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Series
    // ──────────────────────────────────────────────────────────────────────────────

    static SerieIndicadorResponse inscripcionesPorCategoria(List<Object[]> filas) {
        List<PuntoSerieResponse> puntos = new ArrayList<>();
        for (Object[] fila : vacioSiNulo(filas)) {
            long cantidad = entero(fila[1]);
            if (cantidad > 0) {
                String categoria = texto(fila[0]);
                puntos.add(punto(categoria, etiquetaCategoria(categoria), "inscritos", BigDecimal.valueOf(cantidad)));
            }
        }
        return serie("INSCRIPCIONES_POR_CATEGORIA", "Inscritos por categoría de participante", GRAFICO_CIRCULAR,
                List.of(MEDIDA_INSCRITOS), puntos, "El evento no tiene inscripciones registradas.");
    }

    static SerieIndicadorResponse inscripcionesPorFecha(List<Object[]> filas) {
        List<PuntoSerieResponse> puntos = new ArrayList<>();
        long acumulado = 0;
        for (Object[] fila : vacioSiNulo(filas)) {
            long cantidad = entero(fila[1]);
            acumulado += cantidad;
            String dia = fecha(fila[0]).toString();
            Map<String, BigDecimal> valores = new LinkedHashMap<>();
            valores.put("inscripciones", BigDecimal.valueOf(cantidad));
            valores.put("acumulado", BigDecimal.valueOf(acumulado));
            puntos.add(new PuntoSerieResponse(dia, dia, valores));
        }
        return serie("INSCRIPCIONES_POR_FECHA", "Evolución de las inscripciones", GRAFICO_LINEA,
                List.of(new MedidaSerieResponse("inscripciones", "Inscripciones del día", "personas"),
                        new MedidaSerieResponse("acumulado", "Total acumulado", "personas")),
                acumulado > 0 ? puntos : List.of(), "El evento no tiene inscripciones registradas.");
    }

    static SerieIndicadorResponse asistenciaPorActividad(List<Object[]> filas) {
        List<Object[]> actividades = vacioSiNulo(filas);
        List<PuntoSerieResponse> puntos = new ArrayList<>();
        boolean hayDatos = false;
        for (Object[] fila : actividades) {
            long inscritos = entero(fila[4]);
            long asistentes = entero(fila[5]);
            hayDatos |= inscritos > 0 || asistentes > 0;
            puntos.add(new PuntoSerieResponse(texto(fila[0]), texto(fila[1]),
                    valoresAsistencia(inscritos, asistentes)));
        }
        return serie("ASISTENCIA_POR_ACTIVIDAD", "Inscritos y asistentes por actividad", GRAFICO_BARRAS_AGRUPADAS,
                List.of(MEDIDA_INSCRITOS, MEDIDA_ASISTENTES, MEDIDA_TASA_ASISTENCIA), hayDatos ? puntos : List.of(),
                actividades.isEmpty() ? "El evento no tiene actividades programadas."
                        : "Las actividades del evento no tienen inscritos ni asistencia registrada.");
    }

    static SerieIndicadorResponse asistenciaPorDia(List<Object[]> filas) {
        List<Object[]> actividades = vacioSiNulo(filas);
        Map<LocalDate, long[]> porDia = new TreeMap<>();
        boolean hayDatos = false;
        for (Object[] fila : actividades) {
            if (fila[2] == null) {
                continue;
            }
            long[] acumulado = porDia.computeIfAbsent(fecha(fila[2]), d -> new long[2]);
            acumulado[0] += entero(fila[4]);
            acumulado[1] += entero(fila[5]);
            hayDatos |= acumulado[0] > 0 || acumulado[1] > 0;
        }
        List<PuntoSerieResponse> puntos = new ArrayList<>();
        porDia.forEach((dia, valores) -> puntos.add(new PuntoSerieResponse(dia.toString(), dia.toString(),
                valoresAsistencia(valores[0], valores[1]))));
        return serie("ASISTENCIA_POR_DIA", "Inscripciones a actividades y asistencias por día",
                GRAFICO_BARRAS_AGRUPADAS,
                List.of(new MedidaSerieResponse("inscritos", "Inscripciones a actividades", "inscripciones"),
                        new MedidaSerieResponse("asistentes", "Asistencias registradas", "registros"),
                        MEDIDA_TASA_ASISTENCIA),
                hayDatos ? puntos : List.of(),
                actividades.isEmpty() ? "El evento no tiene actividades programadas."
                        : "Las actividades del evento no tienen inscritos ni asistencia registrada.");
    }

    static SerieIndicadorResponse propuestasPorEstado(List<Object[]> filas) {
        Map<String, Long> cantidades = new LinkedHashMap<>();
        for (Object[] fila : vacioSiNulo(filas)) {
            cantidades.merge(texto(fila[0]), entero(fila[1]), Long::sum);
        }
        List<PuntoSerieResponse> puntos = new ArrayList<>();
        ETIQUETA_ESTADO_PROPUESTA.forEach((estado, etiqueta) -> {
            long cantidad = cantidades.getOrDefault(estado, 0L);
            if (cantidad > 0) {
                puntos.add(punto(estado, etiqueta, "propuestas", BigDecimal.valueOf(cantidad)));
            }
        });
        // Estados que no estén en la lista (no debería haber por el CHECK de la BD) se muestran al final.
        cantidades.forEach((estado, cantidad) -> {
            if (!ETIQUETA_ESTADO_PROPUESTA.containsKey(estado) && cantidad > 0) {
                puntos.add(punto(estado, etiquetaCategoria(estado), "propuestas", BigDecimal.valueOf(cantidad)));
            }
        });
        return serie("PROPUESTAS_POR_ESTADO", "Propuestas por estado", GRAFICO_CIRCULAR,
                List.of(new MedidaSerieResponse("propuestas", "Propuestas", "propuestas")), puntos,
                "El evento no tiene propuestas recibidas en sus convocatorias.");
    }

    static SerieIndicadorResponse satisfaccionPorEncuesta(List<Object[]> filas) {
        List<Object[]> encuestas = vacioSiNulo(filas);
        List<PuntoSerieResponse> puntos = new ArrayList<>();
        boolean hayDatos = false;
        for (Object[] fila : encuestas) {
            long respuestas = entero(fila[4]);
            hayDatos |= respuestas > 0;
            String titulo = texto(fila[1]);
            String actividad = texto(fila[3]);
            Map<String, BigDecimal> valores = new LinkedHashMap<>();
            valores.put("satisfaccion", respuestas > 0 ? decimal(fila[5]) : null);
            valores.put("respuestas", BigDecimal.valueOf(respuestas));
            puntos.add(new PuntoSerieResponse(texto(fila[0]),
                    actividad == null ? titulo : titulo + " — " + actividad, valores));
        }
        return serie("SATISFACCION_POR_ENCUESTA", "Satisfacción por encuesta", GRAFICO_BARRAS,
                List.of(new MedidaSerieResponse("satisfaccion", "Satisfacción promedio", "%"),
                        new MedidaSerieResponse("respuestas", "Respuestas", "respuestas")),
                hayDatos ? puntos : List.of(),
                encuestas.isEmpty() ? "El evento no tiene encuestas de satisfacción publicadas."
                        : "Las encuestas de satisfacción del evento todavía no tienen respuestas.");
    }

    static SerieIndicadorResponse presupuestoPorRubro(List<Object[]> filas) {
        List<Object[]> rubros = vacioSiNulo(filas);
        List<PuntoSerieResponse> puntos = new ArrayList<>();
        boolean hayDatos = false;
        for (Object[] fila : rubros) {
            BigDecimal proyectado = cero(decimal(fila[2]));
            BigDecimal ejecutado = cero(decimal(fila[3]));
            hayDatos |= proyectado.signum() > 0 || ejecutado.signum() > 0;
            Map<String, BigDecimal> valores = new LinkedHashMap<>();
            valores.put("proyectado", proyectado);
            valores.put("ejecutado", ejecutado);
            puntos.add(new PuntoSerieResponse(texto(fila[0]), texto(fila[1]), valores));
        }
        return serie("PRESUPUESTO_POR_RUBRO", "Presupuesto proyectado y ejecutado por rubro",
                GRAFICO_BARRAS_AGRUPADAS,
                List.of(new MedidaSerieResponse("proyectado", "Proyectado", "COP"),
                        new MedidaSerieResponse("ejecutado", "Ejecutado", "COP")),
                hayDatos ? puntos : List.of(),
                rubros.isEmpty() ? "El evento no tiene rubros de presupuesto registrados."
                        : "Los rubros del presupuesto no tienen valores proyectados ni gastos registrados.");
    }

    private static SerieIndicadorResponse serie(String codigo, String titulo, String grafico,
                                                List<MedidaSerieResponse> medidas, List<PuntoSerieResponse> puntos,
                                                String mensajeSinDatos) {
        boolean hayDatos = !puntos.isEmpty();
        return new SerieIndicadorResponse(codigo, titulo, grafico, hayDatos, hayDatos ? null : mensajeSinDatos,
                medidas, List.copyOf(puntos));
    }

    private static PuntoSerieResponse punto(String clave, String etiqueta, String medida, BigDecimal valor) {
        Map<String, BigDecimal> valores = new LinkedHashMap<>();
        valores.put(medida, valor);
        return new PuntoSerieResponse(clave, etiqueta, valores);
    }

    private static Map<String, BigDecimal> valoresAsistencia(long inscritos, long asistentes) {
        Map<String, BigDecimal> valores = new LinkedHashMap<>();
        valores.put("inscritos", BigDecimal.valueOf(inscritos));
        valores.put("asistentes", BigDecimal.valueOf(asistentes));
        valores.put("tasaAsistencia", porcentaje(asistentes, inscritos));
        return valores;
    }

    /** {@code parte / total} en porcentaje con dos decimales; {@code null} si {@code total} es cero. */
    static BigDecimal porcentaje(long parte, long total) {
        if (total <= 0) {
            return null;
        }
        return BigDecimal.valueOf(parte).multiply(CIEN).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    static String etiquetaCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return "Sin categoría";
        }
        String texto = categoria.trim().replace('_', ' ');
        return texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Conversión de las columnas de las consultas nativas
    // ──────────────────────────────────────────────────────────────────────────────

    private static <T> List<T> vacioSiNulo(List<T> lista) {
        return lista == null ? List.of() : lista;
    }

    private static BigDecimal cero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof Enum<?> e ? e.name() : valor.toString();
    }

    static long entero(Object valor) {
        if (valor == null) {
            return 0L;
        }
        if (valor instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(valor.toString().trim());
    }

    static BigDecimal decimal(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal d) {
            return d;
        }
        if (valor instanceof Long || valor instanceof Integer || valor instanceof Short
                || valor instanceof java.math.BigInteger) {
            return new BigDecimal(valor.toString());
        }
        if (valor instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(valor.toString().trim());
    }

    static LocalDate fecha(Object valor) {
        if (valor instanceof LocalDate d) {
            return d;
        }
        if (valor instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (valor instanceof java.sql.Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        if (valor instanceof LocalDateTime t) {
            return t.toLocalDate();
        }
        return LocalDate.parse(valor.toString().trim().substring(0, 10));
    }
}
