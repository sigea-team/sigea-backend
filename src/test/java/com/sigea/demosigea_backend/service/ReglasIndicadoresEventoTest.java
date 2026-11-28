package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.indicador.DashboardIndicadoresResponse;
import com.sigea.demosigea_backend.dto.indicador.GrupoIndicadoresResponse;
import com.sigea.demosigea_backend.dto.indicador.IndicadorEventoResponse;
import com.sigea.demosigea_backend.dto.indicador.SerieIndicadorResponse;
import com.sigea.demosigea_backend.model.EstadoEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link ReglasIndicadoresEvento} (HU-29). Las filas imitan lo que devuelven las consultas nativas de
 * {@code IndicadorEventoRepository} (los tipos varían según el controlador JDBC: Long, BigInteger, BigDecimal,
 * java.sql.Date, LocalDate...).
 */
class ReglasIndicadoresEventoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 26, 1, 0);

    /** Indicadores con los 16 códigos del cálculo común; {@code null} = no se puede calcular. */
    private static List<Object[]> indicadores(Object... valores) {
        String[][] base = {
                {"INSCRITOS", "personas"}, {"ASISTENTES", "personas"}, {"TASA_ASISTENCIA", "%"},
                {"ASISTENCIAS_REGISTRADAS", "registros"}, {"ACTIVIDADES_REALIZADAS", "actividades"},
                {"ACTIVIDADES_CANCELADAS", "actividades"}, {"PROPUESTAS_RECIBIDAS", "propuestas"},
                {"PROPUESTAS_APROBADAS", "propuestas"}, {"PROPUESTAS_RECHAZADAS", "propuestas"},
                {"TASA_APROBACION", "%"}, {"CONFERENCISTAS", "personas"}, {"RESPUESTAS_ENCUESTAS", "respuestas"},
                {"SATISFACCION_PROMEDIO", "%"}, {"PRESUPUESTO_APROBADO", "COP"}, {"PRESUPUESTO_EJECUTADO", "COP"},
                {"EJECUCION_PRESUPUESTAL", "%"}};
        List<Object[]> filas = new ArrayList<>();
        for (int i = 0; i < base.length; i++) {
            Object valor = valores[i];
            filas.add(new Object[]{base[i][0], "Nombre " + base[i][0],
                    valor == null ? null : new BigDecimal(valor.toString()), base[i][1]});
        }
        return filas;
    }

    private static ReglasIndicadoresEvento.Datos vacio() {
        return new ReglasIndicadoresEvento.Datos(
                indicadores(0, 0, null, 0, 0, 0, 0, 0, 0, null, 0, 0, null, null, 0, null),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static DashboardIndicadoresResponse construir(ReglasIndicadoresEvento.Datos datos, boolean archivado) {
        return ReglasIndicadoresEvento.construir(7L, "Congreso", EstadoEvento.en_ejecucion, archivado, AHORA, datos);
    }

    private static IndicadorEventoResponse indicador(DashboardIndicadoresResponse r, String codigo) {
        return r.indicadores().stream().filter(i -> i.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static SerieIndicadorResponse serie(DashboardIndicadoresResponse r, String codigo) {
        return r.series().stream().filter(s -> s.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static GrupoIndicadoresResponse grupo(DashboardIndicadoresResponse r, String codigo) {
        return r.grupos().stream().filter(g -> g.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static void assertValor(String esperado, BigDecimal valor) {
        assertTrue(valor != null && valor.compareTo(new BigDecimal(esperado)) == 0,
                () -> "Se esperaba " + esperado + " y llegó " + valor);
    }

    @Test
    @DisplayName("Criterio 1: indicadores para la tabla, agrupados y disponibles")
    void indicadoresCompletos() {
        ReglasIndicadoresEvento.Datos datos = new ReglasIndicadoresEvento.Datos(
                indicadores(4, 3, "75.00", 4, 3, 1, 4, 2, 1, "50.00", 1, 3, "85.00", 1200000, 650000, "54.17"),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        DashboardIndicadoresResponse r = construir(datos, false);

        assertTrue(r.hayDatos());
        assertNull(r.mensaje());
        assertEquals(AHORA, r.fechaCalculo());
        assertEquals(16, r.indicadores().size());
        assertTrue(r.indicadores().stream().allMatch(IndicadorEventoResponse::disponible));
        assertEquals(List.of("PARTICIPACION", "ASISTENCIA", "ACTIVIDADES", "PROPUESTAS", "SATISFACCION", "PRESUPUESTO"),
                r.grupos().stream().map(GrupoIndicadoresResponse::codigo).toList());
        assertEquals("ASISTENCIA", indicador(r, "TASA_ASISTENCIA").grupo());
        assertValor("75.00", indicador(r, "TASA_ASISTENCIA").valor());
        assertEquals("INSCRITOS", r.indicadores().get(0).codigo());
        assertEquals(7, r.series().size());
    }

    @Test
    @DisplayName("Criterio 2: evento sin datos responde sin error, con avisos en grupos, indicadores y series")
    void eventoSinDatos() {
        DashboardIndicadoresResponse r = construir(vacio(), false);

        assertFalse(r.hayDatos());
        assertEquals(ReglasIndicadoresEvento.MENSAJE_SIN_DATOS, r.mensaje());
        assertTrue(r.grupos().stream().noneMatch(GrupoIndicadoresResponse::hayDatos));
        assertTrue(r.grupos().stream().allMatch(g -> g.mensaje() != null));
        for (IndicadorEventoResponse i : r.indicadores()) {
            assertFalse(i.disponible(), i.codigo());
            assertNull(i.valor(), i.codigo());
            assertTrue(i.motivo() != null && !i.motivo().isBlank(), i.codigo());
        }
        for (SerieIndicadorResponse s : r.series()) {
            assertFalse(s.hayDatos(), s.codigo());
            assertTrue(s.puntos().isEmpty(), s.codigo());
            assertTrue(s.mensaje() != null && !s.mensaje().isBlank(), s.codigo());
        }
        assertEquals("El evento no tiene actividades programadas.", serie(r, "ASISTENCIA_POR_ACTIVIDAD").mensaje());
    }

    @Test
    @DisplayName("Criterio 2: un grupo sin datos no afecta a los demás")
    void grupoSinDatos() {
        // Inscritos sin asistencia, sin propuestas ni presupuesto.
        ReglasIndicadoresEvento.Datos datos = new ReglasIndicadoresEvento.Datos(
                indicadores(5, 0, "0.00", 0, 2, 0, 0, 0, 0, null, 0, 0, null, null, 0, null),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        DashboardIndicadoresResponse r = construir(datos, false);

        assertTrue(r.hayDatos());
        assertTrue(indicador(r, "INSCRITOS").disponible());
        assertValor("5", indicador(r, "INSCRITOS").valor());
        assertTrue(grupo(r, "ACTIVIDADES").hayDatos());
        assertFalse(grupo(r, "ASISTENCIA").hayDatos());
        IndicadorEventoResponse tasa = indicador(r, "TASA_ASISTENCIA");
        assertFalse(tasa.disponible());
        assertNull(tasa.valor());
        assertEquals(grupo(r, "ASISTENCIA").mensaje(), tasa.motivo());
        assertFalse(indicador(r, "TASA_APROBACION").disponible());
    }

    @Test
    @DisplayName("Criterio 2: indicador no calculable dentro de un grupo con datos trae su motivo")
    void indicadorSinValorConGrupoConDatos() {
        // Hay respuestas a encuestas, pero ninguna de escala; hay gastos sin presupuesto aprobado.
        ReglasIndicadoresEvento.Datos datos = new ReglasIndicadoresEvento.Datos(
                indicadores(1, 0, "0.00", 0, 0, 0, 0, 0, 0, null, 0, 2, null, null, 50000, null),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        DashboardIndicadoresResponse r = construir(datos, false);

        assertTrue(indicador(r, "RESPUESTAS_ENCUESTAS").disponible());
        IndicadorEventoResponse satisfaccion = indicador(r, "SATISFACCION_PROMEDIO");
        assertFalse(satisfaccion.disponible());
        assertTrue(satisfaccion.motivo().contains("escala"));
        assertTrue(indicador(r, "PRESUPUESTO_EJECUTADO").disponible());
        assertEquals("El evento no tiene presupuesto aprobado.", indicador(r, "PRESUPUESTO_APROBADO").motivo());
        assertTrue(indicador(r, "EJECUCION_PRESUPUESTAL").motivo().startsWith("Sin presupuesto aprobado"));
    }

    @Test
    @DisplayName("Evento archivado: avisa que los indicadores son definitivos")
    void eventoArchivado() {
        ReglasIndicadoresEvento.Datos datos = new ReglasIndicadoresEvento.Datos(
                indicadores(4, 3, "75.00", 4, 3, 1, 4, 2, 1, "50.00", 1, 3, "85.00", 1200000, 650000, "54.17"),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        assertEquals(ReglasIndicadoresEvento.MENSAJE_ARCHIVADO, construir(datos, true).mensaje());
        assertEquals(ReglasIndicadoresEvento.MENSAJE_SIN_DATOS_ARCHIVADO, construir(vacio(), true).mensaje());
        assertTrue(construir(datos, true).archivado());
    }

    @Test
    @DisplayName("Serie de inscritos por categoría: omite ceros, etiqueta legible y orden de la consulta")
    void serieCategorias() {
        SerieIndicadorResponse s = ReglasIndicadoresEvento.inscripcionesPorCategoria(List.of(
                new Object[]{"estudiante", 3L}, new Object[]{"docente", java.math.BigInteger.ONE},
                new Object[]{"egresado", 0L}));

        assertTrue(s.hayDatos());
        assertEquals(ReglasIndicadoresEvento.GRAFICO_CIRCULAR, s.grafico());
        assertEquals(2, s.puntos().size());
        assertEquals("Estudiante", s.puntos().get(0).etiqueta());
        assertValor("3", s.puntos().get(0).valores().get("inscritos"));
        assertEquals("docente", s.puntos().get(1).clave());
    }

    @Test
    @DisplayName("Serie de inscripciones por fecha: total acumulado")
    void serieFechas() {
        SerieIndicadorResponse s = ReglasIndicadoresEvento.inscripcionesPorFecha(List.of(
                new Object[]{Date.valueOf("2026-11-01"), 2L}, new Object[]{LocalDate.of(2026, 11, 3), 1L},
                new Object[]{"2026-11-05", 4L}));

        assertEquals(List.of("2026-11-01", "2026-11-03", "2026-11-05"),
                s.puntos().stream().map(p -> p.clave()).toList());
        assertValor("7", s.puntos().get(2).valores().get("acumulado"));
        assertValor("4", s.puntos().get(2).valores().get("inscripciones"));
    }

    @Test
    @DisplayName("Series de asistencia por actividad y por día, con tasa sin dividir por cero")
    void seriesAsistencia() {
        List<Object[]> filas = List.of(
                new Object[]{11L, "Apertura", Date.valueOf("2026-11-20"), LocalTime.of(8, 0), 4L, 3L},
                new Object[]{12, "Taller", LocalDate.of(2026, 11, 20), LocalTime.of(10, 0), 2L, 1L},
                new Object[]{13, "Cierre", LocalDate.of(2026, 11, 21), LocalTime.of(8, 0), 0L, 0L});

        SerieIndicadorResponse actividad = ReglasIndicadoresEvento.asistenciaPorActividad(filas);
        assertEquals(3, actividad.puntos().size());
        assertEquals("11", actividad.puntos().get(0).clave());
        assertValor("75.00", actividad.puntos().get(0).valores().get("tasaAsistencia"));
        assertNull(actividad.puntos().get(2).valores().get("tasaAsistencia"));

        SerieIndicadorResponse dia = ReglasIndicadoresEvento.asistenciaPorDia(filas);
        assertEquals(2, dia.puntos().size());
        assertEquals("2026-11-20", dia.puntos().get(0).clave());
        assertValor("6", dia.puntos().get(0).valores().get("inscritos"));
        assertValor("4", dia.puntos().get(0).valores().get("asistentes"));
        assertValor("66.67", dia.puntos().get(0).valores().get("tasaAsistencia"));
    }

    @Test
    @DisplayName("Series sin datos: actividades sin inscritos y encuestas sin respuestas")
    void seriesSinDatos() {
        SerieIndicadorResponse actividad = ReglasIndicadoresEvento.asistenciaPorActividad(List.<Object[]>of(
                new Object[]{1L, "Charla", LocalDate.of(2026, 12, 1), LocalTime.of(8, 0), 0L, 0L}));
        assertFalse(actividad.hayDatos());
        assertTrue(actividad.puntos().isEmpty());
        assertTrue(actividad.mensaje().startsWith("Las actividades"));

        SerieIndicadorResponse encuestas = ReglasIndicadoresEvento.satisfaccionPorEncuesta(List.<Object[]>of(
                new Object[]{1L, "General", "publicada", null, 0L, null}));
        assertFalse(encuestas.hayDatos());
        assertEquals("Las encuestas de satisfacción del evento todavía no tienen respuestas.", encuestas.mensaje());
    }

    @Test
    @DisplayName("Serie de propuestas por estado en orden del proceso")
    void seriePropuestas() {
        SerieIndicadorResponse s = ReglasIndicadoresEvento.propuestasPorEstado(List.of(
                new Object[]{"rechazada", 1L}, new Object[]{"aprobada", 2L}, new Object[]{"recibida", 3L}));

        assertEquals(List.of("recibida", "aprobada", "rechazada"), s.puntos().stream().map(p -> p.clave()).toList());
        assertEquals("Recibidas", s.puntos().get(0).etiqueta());
    }

    @Test
    @DisplayName("Serie de satisfacción: encuesta de actividad y encuesta sin preguntas de escala")
    void serieSatisfaccion() {
        SerieIndicadorResponse s = ReglasIndicadoresEvento.satisfaccionPorEncuesta(List.of(
                new Object[]{1L, "Taller", "cerrada", "Taller IA", 1L, new BigDecimal("80.00")},
                new Object[]{2L, "General", "publicada", null, 2L, null}));

        assertTrue(s.hayDatos());
        assertEquals("Taller — Taller IA", s.puntos().get(0).etiqueta());
        assertValor("80.00", s.puntos().get(0).valores().get("satisfaccion"));
        assertNull(s.puntos().get(1).valores().get("satisfaccion"));
        assertValor("2", s.puntos().get(1).valores().get("respuestas"));
    }

    @Test
    @DisplayName("Serie de presupuesto por rubro")
    void seriePresupuesto() {
        SerieIndicadorResponse s = ReglasIndicadoresEvento.presupuestoPorRubro(List.of(
                new Object[]{1L, "Refrigerios", new BigDecimal("500000.0000"), new BigDecimal("350000.00")},
                new Object[]{2L, "Viáticos", new BigDecimal("600000"), 0L}));

        assertTrue(s.hayDatos());
        assertValor("500000", s.puntos().get(0).valores().get("proyectado"));
        assertValor("0", s.puntos().get(1).valores().get("ejecutado"));
        assertFalse(ReglasIndicadoresEvento.presupuestoPorRubro(List.of()).hayDatos());
    }

    @Test
    @DisplayName("Porcentaje con dos decimales y sin división por cero")
    void porcentaje() {
        assertValor("33.33", ReglasIndicadoresEvento.porcentaje(1, 3));
        assertNull(ReglasIndicadoresEvento.porcentaje(1, 0));
    }

    @Test
    @DisplayName("Indicador con código desconocido va al grupo Otros")
    void codigoDesconocido() {
        List<Object[]> filas = new ArrayList<>(vacio().indicadores());
        filas.add(new Object[]{"NUEVO", "Indicador nuevo", new BigDecimal("2"), "u"});
        ReglasIndicadoresEvento.Datos datos = new ReglasIndicadoresEvento.Datos(filas,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        DashboardIndicadoresResponse r = construir(datos, false);

        assertEquals("OTROS", indicador(r, "NUEVO").grupo());
        assertTrue(indicador(r, "NUEVO").disponible());
        assertEquals("OTROS", r.grupos().get(r.grupos().size() - 1).codigo());
    }
}
