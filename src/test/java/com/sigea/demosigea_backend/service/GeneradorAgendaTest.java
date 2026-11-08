package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.service.GeneradorAgenda.ActividadPorUbicar;
import com.sigea.demosigea_backend.service.GeneradorAgenda.Asignacion;
import com.sigea.demosigea_backend.service.GeneradorAgenda.Franja;
import com.sigea.demosigea_backend.service.GeneradorAgenda.MotivoPendiente;
import com.sigea.demosigea_backend.service.GeneradorAgenda.Ocupacion;
import com.sigea.demosigea_backend.service.GeneradorAgenda.RequisitoSala;
import com.sigea.demosigea_backend.service.GeneradorAgenda.Resultado;
import com.sigea.demosigea_backend.service.GeneradorAgenda.SalaDisponible;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del algoritmo de generación de agenda (HU-22, Criterios 1 y 2).
 */
class GeneradorAgendaTest {

    private static final LocalDate DIA1 = LocalDate.of(2027, 3, 10);
    private static final LocalDate DIA2 = DIA1.plusDays(1);
    private static final Long ANA = 20L;
    private static final Long LUIS = 21L;

    private static Franja franja(LocalDate dia, int desde, int hasta) {
        return new Franja(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    private static SalaDisponible sala(long id, boolean virtual, boolean simultaneidad, Franja... franjas) {
        return new SalaDisponible(id, virtual, simultaneidad, List.of(franjas));
    }

    private static ActividadPorUbicar actividad(long id, int minutos, Long ponente, List<Franja> franjasPonente) {
        return new ActividadPorUbicar(id, minutos, ponente, RequisitoSala.CUALQUIERA, false, franjasPonente);
    }

    private static Resultado generar(List<SalaDisponible> salas, List<Ocupacion> fijas, List<ActividadPorUbicar> a) {
        return GeneradorAgenda.generar(DIA1, DIA2, salas, fijas, a);
    }

    private static Asignacion de(Resultado r, long id) {
        return r.asignadas().stream().filter(x -> x.actividadId() == id).findFirst().orElseThrow();
    }

    /** Ninguna pareja de asignaciones (ni con las fijas) viola las reglas de HU-21. */
    private static void sinChoques(Resultado r, List<SalaDisponible> salas, List<Ocupacion> fijas,
                                   List<ActividadPorUbicar> actividades) {
        List<Ocupacion> todas = new ArrayList<>(fijas);
        for (Asignacion a : r.asignadas()) {
            ActividadPorUbicar origen = actividades.stream()
                    .filter(x -> x.actividadId().equals(a.actividadId())).findFirst().orElseThrow();
            todas.add(new Ocupacion(a.actividadId(), a.fecha(), a.inicio(), a.fin(), a.salaId(),
                    origen.ponenteId(), origen.simultanea()));
        }
        for (int i = 0; i < todas.size(); i++) {
            for (int j = i + 1; j < todas.size(); j++) {
                Ocupacion x = todas.get(i);
                Ocupacion y = todas.get(j);
                if (!x.fecha().equals(y.fecha()) || !x.inicio().isBefore(y.fin()) || !y.inicio().isBefore(x.fin())) {
                    continue;
                }
                boolean mismoPonente = x.ponenteId() != null && x.ponenteId().equals(y.ponenteId());
                boolean ambas = x.simultanea() && y.simultanea();
                boolean salaPermite = salas.stream().filter(s -> s.id().equals(x.salaId()))
                        .anyMatch(SalaDisponible::permiteSimultaneidad);
                boolean mismaSala = x.salaId().equals(y.salaId());
                boolean choque = mismoPonente || (mismaSala ? !(ambas && salaPermite) : !ambas);
                assertFalse(choque, "Choque entre " + x.actividadId() + " y " + y.actividadId());
            }
        }
    }

    @Test
    @DisplayName("Criterio 1: ubica las actividades sin choques de horario, sala ni ponente")
    void ubicaSinChoques() {
        List<SalaDisponible> salas = List.of(sala(10, false, false, franja(DIA1, 8, 12), franja(DIA2, 8, 12)));
        List<ActividadPorUbicar> acts = List.of(
                actividad(1, 120, ANA, List.of(franja(DIA1, 8, 12))),
                actividad(2, 60, LUIS, List.of(franja(DIA1, 8, 12), franja(DIA2, 8, 12))),
                actividad(3, 90, null, List.of()));

        Resultado r = generar(salas, List.of(), acts);

        assertEquals(3, r.asignadas().size());
        assertTrue(r.pendientes().isEmpty());
        sinChoques(r, salas, List.of(), acts);
        // La de Ana (la más restringida) queda dentro de su disponibilidad
        assertEquals(DIA1, de(r, 1).fecha());
        assertEquals(LocalTime.of(8, 0), de(r, 1).inicio());
        assertEquals(LocalTime.of(10, 0), de(r, 1).fin());
    }

    @Test
    @DisplayName("Criterio 1: respeta las actividades que ya tienen horario")
    void respetaFijas() {
        List<SalaDisponible> salas = List.of(sala(10, false, false, franja(DIA1, 8, 12)));
        List<Ocupacion> fijas = List.of(new Ocupacion(100L, DIA1, LocalTime.of(8, 0), LocalTime.of(10, 0),
                10L, ANA, false));
        List<ActividadPorUbicar> acts = List.of(actividad(1, 60, LUIS, List.of(franja(DIA1, 8, 12))));

        Resultado r = generar(salas, fijas, acts);

        assertEquals(LocalTime.of(10, 0), de(r, 1).inicio());
        sinChoques(r, salas, fijas, acts);
    }

    @Test
    @DisplayName("Criterio 1: el ponente nunca queda en dos actividades a la vez, aunque sean simultáneas")
    void ponenteNoSeDuplica() {
        List<SalaDisponible> salas = List.of(
                sala(10, false, true, franja(DIA1, 8, 10)),
                sala(11, false, true, franja(DIA1, 8, 10)));
        List<ActividadPorUbicar> acts = List.of(
                new ActividadPorUbicar(1L, 120, ANA, RequisitoSala.CUALQUIERA, true, List.of(franja(DIA1, 8, 10))),
                new ActividadPorUbicar(2L, 120, ANA, RequisitoSala.CUALQUIERA, true, List.of(franja(DIA1, 8, 10))));

        Resultado r = generar(salas, List.of(), acts);

        assertEquals(1, r.asignadas().size());
        assertEquals(MotivoPendiente.SIN_ESPACIO_LIBRE, r.pendientes().get(0).motivo());
    }

    @Test
    @DisplayName("Actividades simultáneas pueden ir en paralelo en salas distintas")
    void simultaneasEnParalelo() {
        List<SalaDisponible> salas = List.of(
                sala(10, false, false, franja(DIA1, 8, 10)),
                sala(11, false, false, franja(DIA1, 8, 10)));
        List<ActividadPorUbicar> acts = List.of(
                new ActividadPorUbicar(1L, 120, ANA, RequisitoSala.CUALQUIERA, true, List.of(franja(DIA1, 8, 10))),
                new ActividadPorUbicar(2L, 120, LUIS, RequisitoSala.CUALQUIERA, true, List.of(franja(DIA1, 8, 10))));

        Resultado r = generar(salas, List.of(), acts);

        assertEquals(2, r.asignadas().size());
        assertFalse(de(r, 1).salaId().equals(de(r, 2).salaId()));
        sinChoques(r, salas, List.of(), acts);
    }

    @Test
    @DisplayName("Sin marcar simultáneas, dos actividades no se traslapan aunque estén en salas distintas")
    void noSimultaneasNoSeTraslapan() {
        List<SalaDisponible> salas = List.of(
                sala(10, false, false, franja(DIA1, 8, 12)),
                sala(11, false, false, franja(DIA1, 8, 12)));
        List<ActividadPorUbicar> acts = List.of(actividad(1, 120, null, List.of()), actividad(2, 120, null, List.of()));

        Resultado r = generar(salas, List.of(), acts);

        assertEquals(2, r.asignadas().size());
        sinChoques(r, salas, List.of(), acts);
    }

    @Test
    @DisplayName("Una actividad virtual va a una sala virtual y una presencial a una física")
    void respetaModalidad() {
        List<SalaDisponible> salas = List.of(
                sala(10, false, false, franja(DIA1, 8, 9)),
                sala(12, true, false, franja(DIA1, 9, 10)));
        List<ActividadPorUbicar> acts = List.of(
                new ActividadPorUbicar(1L, 60, null, RequisitoSala.VIRTUAL, false, List.of()),
                new ActividadPorUbicar(2L, 60, null, RequisitoSala.FISICA, false, List.of()));

        Resultado r = generar(salas, List.of(), acts);

        assertEquals(12L, de(r, 1).salaId());
        assertEquals(10L, de(r, 2).salaId());
    }

    @Test
    @DisplayName("Criterio 2: informa las actividades que no se pudieron ubicar, con el motivo")
    void informaPendientes() {
        List<SalaDisponible> salas = List.of(
                sala(10, false, false, franja(DIA1, 8, 10)),
                sala(11, false, false));
        List<ActividadPorUbicar> acts = List.of(
                actividad(1, 60, ANA, List.of()),                                // ponente sin disponibilidad
                actividad(2, 60, LUIS, List.of(franja(DIA1, 14, 16))),           // no coincide con la sala
                actividad(3, 180, null, List.of()),                              // no cabe en ninguna franja
                new ActividadPorUbicar(4L, 60, null, RequisitoSala.VIRTUAL, false, List.of()), // no hay sala virtual
                actividad(5, 120, null, List.of()),                              // cabe sola
                actividad(6, 120, null, List.of()));                             // ya no queda espacio

        Resultado r = generar(salas, List.of(), acts);

        assertEquals(1, r.asignadas().size());
        assertEquals(5L, r.asignadas().get(0).actividadId());
        assertEquals(MotivoPendiente.SIN_DISPONIBILIDAD_PONENTE, motivo(r, 1));
        assertEquals(MotivoPendiente.SIN_FRANJA_COMUN, motivo(r, 2));
        assertEquals(MotivoPendiente.SIN_FRANJA_COMUN, motivo(r, 3));
        assertEquals(MotivoPendiente.SIN_SALA_DISPONIBLE, motivo(r, 4));
        assertEquals(MotivoPendiente.SIN_ESPACIO_LIBRE, motivo(r, 6));
    }

    @Test
    @DisplayName("Ignora franjas fuera de las fechas del evento")
    void ignoraFranjasFueraDelEvento() {
        List<SalaDisponible> salas = List.of(sala(10, false, false, franja(DIA1.minusDays(1), 8, 12)));
        Resultado r = generar(salas, List.of(), List.of(actividad(1, 60, null, List.of())));
        assertEquals(MotivoPendiente.SIN_SALA_DISPONIBLE, motivo(r, 1));
    }

    @Test
    @DisplayName("Usa inicios en pasos de 15 minutos y no se sale de la franja")
    void pasosDeQuinceMinutos() {
        List<SalaDisponible> salas = List.of(sala(10, false, false,
                new Franja(DIA1, LocalTime.of(8, 0), LocalTime.of(9, 45))));
        List<Ocupacion> fijas = List.of(new Ocupacion(100L, DIA1, LocalTime.of(8, 0), LocalTime.of(8, 20),
                10L, null, false));
        Resultado r = generar(salas, fijas, List.of(actividad(1, 75, null, List.of())));
        assertEquals(LocalTime.of(8, 30), de(r, 1).inicio());
        assertEquals(LocalTime.of(9, 45), de(r, 1).fin());
    }

    @Test
    @DisplayName("Es determinista: los mismos datos producen la misma propuesta")
    void determinista() {
        List<SalaDisponible> salas = List.of(sala(10, false, false, franja(DIA1, 8, 18)),
                sala(11, false, true, franja(DIA1, 8, 18), franja(DIA2, 8, 18)));
        List<ActividadPorUbicar> acts = new ArrayList<>();
        for (long i = 1; i <= 12; i++) {
            acts.add(new ActividadPorUbicar(i, 45 + (int) (i % 3) * 30, i % 2 == 0 ? ANA : LUIS,
                    RequisitoSala.CUALQUIERA, i % 4 == 0, List.of(franja(DIA1, 8, 18), franja(DIA2, 8, 13))));
        }
        Resultado a = generar(salas, List.of(), acts);
        Resultado b = generar(salas, List.of(), acts);
        assertEquals(a, b);
        sinChoques(a, salas, List.of(), acts);
    }

    @Test
    @DisplayName("Si el voraz deja actividades afuera, el retroceso encuentra una combinación que las ubica")
    void retrocesoMejoraAlVoraz() {
        // Luis (más restringido) iría primero a las 10:00 y Ana (2 h) ya no cabría; sí caben si Luis va a las 11:00.
        List<SalaDisponible> salas = List.of(sala(10, false, false, franja(DIA1, 8, 12)));
        List<Ocupacion> fijas = List.of(new Ocupacion(100L, DIA1, LocalTime.of(8, 0), LocalTime.of(9, 0),
                10L, null, false));
        List<ActividadPorUbicar> acts = List.of(
                actividad(2, 120, ANA, List.of(franja(DIA1, 8, 12))),
                actividad(3, 60, LUIS, List.of(franja(DIA1, 10, 12))));

        Resultado r = generar(salas, fijas, acts);

        assertTrue(r.pendientes().isEmpty(), "pendientes: " + r.pendientes());
        assertEquals(LocalTime.of(9, 0), de(r, 2).inicio());
        assertEquals(LocalTime.of(11, 0), de(r, 3).inicio());
        sinChoques(r, salas, fijas, acts);
    }

    @Test
    @DisplayName("Con muchas actividades responde rápido y sin choques aunque no quepan todas")
    void muchasActividades() {
        List<SalaDisponible> salas = List.of(sala(10, false, false, franja(DIA1, 8, 18), franja(DIA2, 8, 18)),
                sala(11, false, true, franja(DIA1, 8, 18), franja(DIA2, 8, 18)));
        List<ActividadPorUbicar> acts = new ArrayList<>();
        for (long i = 1; i <= 60; i++) {
            acts.add(new ActividadPorUbicar(i, 30 + (int) (i % 4) * 15, i % 5 == 0 ? null : 100L + i % 7,
                    RequisitoSala.CUALQUIERA, i % 3 == 0, List.of(franja(DIA1, 8, 18), franja(DIA2, 9, 16))));
        }
        long t0 = System.nanoTime();
        Resultado r = generar(salas, List.of(), acts);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertEquals(60, r.asignadas().size() + r.pendientes().size());
        assertTrue(ms < 5000, "tardó " + ms + " ms");
        sinChoques(r, salas, List.of(), acts);
    }

    private static MotivoPendiente motivo(Resultado r, long id) {
        return r.pendientes().stream().filter(p -> p.actividadId() == id).findFirst().orElseThrow().motivo();
    }
}
