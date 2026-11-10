package com.sigea.demosigea_backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Algoritmo que propone una agenda sin conflictos (HU-22, RF38, Criterios 1 y 2).
 * <p>
 * Es Java puro, sin Spring ni JPA, para poder probarlo de forma aislada. {@link AgendaService} le entrega las
 * salas con sus franjas de disponibilidad, las actividades que ya tienen horario (quedan fijas) y las actividades
 * por ubicar; el algoritmo devuelve dónde y cuándo ubicar cada una, y cuáles no pudo ubicar y por qué.
 * </p>
 * <p><b>Estrategia (determinista):</b></p>
 * <ol>
 *   <li>Para cada actividad se calculan los huecos posibles: franjas de las salas compatibles dentro de las fechas
 *       del evento, recortadas a la disponibilidad del ponente si lo tiene, con duración suficiente.</li>
 *   <li>Se ubican primero las actividades con menos opciones (más restringidas), luego las más largas.</li>
 *   <li>Voraz: cada actividad toma el primer inicio libre (por fecha, hora y orden de sala), en pasos de
 *       {@value #PASO_MINUTOS} minutos, que no choque con lo ya ubicado.</li>
 *   <li>Si quedaron actividades sin ubicar, una búsqueda con retroceso (ramificación y poda, limitada a
 *       {@value #PRESUPUESTO_BUSQUEDA} comprobaciones) intenta otras combinaciones y se queda con la que ubique más
 *       actividades. Si no encuentra una mejor, se mantiene el resultado voraz.</li>
 * </ol>
 * <p>
 * Las reglas de choque son las mismas de la programación manual (HU-21,
 * {@link ActividadService#detectarConflictos}): el ponente nunca puede estar en dos actividades a la vez; dos
 * actividades en la misma sala solo se traslapan si ambas son simultáneas y la sala lo admite; en salas distintas
 * solo se traslapan si ambas son simultáneas.
 * </p>
 * <p>
 * Compatibilidad de sala por modalidad: una actividad virtual va en una sala virtual; una presencial o híbrida, en
 * una sala física; sin modalidad, en cualquiera.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public final class GeneradorAgenda {

    /** Granularidad de los inicios propuestos, en minutos. */
    public static final int PASO_MINUTOS = 15;

    /** Máximo de comprobaciones de la búsqueda con retroceso (acota el tiempo de respuesta). */
    public static final int PRESUPUESTO_BUSQUEDA = 200_000;

    private static final int MINUTOS_DIA = 24 * 60;

    private GeneradorAgenda() {
    }

    /** Motivo por el que una actividad quedó pendiente de asignación manual (Criterio 2). */
    public enum MotivoPendiente {
        /** No hay salas compatibles con la modalidad que tengan disponibilidad registrada en las fechas del evento. */
        SIN_SALA_DISPONIBLE,
        /** El ponente no tiene disponibilidad registrada en las fechas del evento. */
        SIN_DISPONIBILIDAD_PONENTE,
        /** Hay disponibilidad, pero ninguna franja de sala coincide con la del ponente el tiempo que dura la actividad. */
        SIN_FRANJA_COMUN,
        /** Había huecos posibles, pero todos chocan con otras actividades (horario, sala o ponente). */
        SIN_ESPACIO_LIBRE
    }

    /** Tipo de sala que admite una actividad según su modalidad. */
    public enum RequisitoSala {
        /** Presencial o híbrida. */
        FISICA,
        /** Virtual. */
        VIRTUAL,
        /** Sin modalidad definida: cualquier sala. */
        CUALQUIERA
    }

    /** Franja horaria de disponibilidad (de sala o de ponente). */
    public record Franja(LocalDate fecha, LocalTime inicio, LocalTime fin) {
    }

    /** Sala del evento con su disponibilidad registrada. */
    public record SalaDisponible(Long id, boolean virtual, boolean permiteSimultaneidad, List<Franja> franjas) {
    }

    /** Actividad que ya tiene horario y sala: ocupa su espacio y no se mueve. */
    public record Ocupacion(Long actividadId, LocalDate fecha, LocalTime inicio, LocalTime fin, Long salaId,
                            Long ponenteId, boolean simultanea) {
    }

    /** Actividad registrada sin horario ni sala, que el algoritmo debe ubicar. */
    public record ActividadPorUbicar(Long actividadId, int duracionMinutos, Long ponenteId, RequisitoSala requisitoSala,
                                     boolean simultanea, List<Franja> franjasPonente) {
    }

    /** Ubicación propuesta para una actividad. */
    public record Asignacion(Long actividadId, LocalDate fecha, LocalTime inicio, LocalTime fin, Long salaId) {
    }

    /** Actividad que no se pudo ubicar sin conflictos. */
    public record NoUbicada(Long actividadId, MotivoPendiente motivo) {
    }

    /** Resultado de la generación. */
    public record Resultado(List<Asignacion> asignadas, List<NoUbicada> pendientes) {
    }

    /** Espacio ocupado durante la generación, en minutos del día. */
    private record Ocupado(Long actividadId, Long salaId, Long ponenteId, boolean simultanea, int inicio, int fin) {
    }

    /** Inicio posible para una actividad. */
    private record Candidato(LocalDate fecha, int inicio, int ordenSala, SalaDisponible sala) {
    }

    /** Actividad por ubicar con sus inicios posibles ya calculados. */
    private record Opciones(ActividadPorUbicar actividad, List<Candidato> candidatos, MotivoPendiente motivoSinOpciones) {
    }

    /**
     * Propone la ubicación de las actividades por ubicar.
     *
     * @param desde      primera fecha del evento.
     * @param hasta      última fecha del evento.
     * @param salas      salas del evento con sus franjas, en el orden de preferencia.
     * @param fijas      actividades que ya tienen horario y sala.
     * @param porUbicar  actividades a ubicar.
     * @return asignaciones propuestas y actividades pendientes, ambas en el orden en que se procesaron.
     */
    public static Resultado generar(LocalDate desde, LocalDate hasta, List<SalaDisponible> salas,
                                    List<Ocupacion> fijas, List<ActividadPorUbicar> porUbicar) {
        Objects.requireNonNull(desde, "desde");
        Objects.requireNonNull(hasta, "hasta");

        Map<LocalDate, List<Ocupado>> ocupados = new HashMap<>();
        for (Ocupacion o : fijas) {
            if (o.fecha() == null || o.inicio() == null || o.fin() == null) {
                continue;
            }
            ocupados.computeIfAbsent(o.fecha(), f -> new ArrayList<>())
                    .add(new Ocupado(o.actividadId(), o.salaId(), o.ponenteId(), o.simultanea(), minutos(o.inicio()),
                            minutos(o.fin())));
        }

        List<Opciones> opciones = new ArrayList<>();
        for (ActividadPorUbicar a : porUbicar) {
            opciones.add(opcionesDe(a, desde, hasta, salas));
        }
        // Primero las más restringidas (menos inicios posibles), luego las más largas, luego por ID.
        opciones.sort(Comparator.<Opciones>comparingInt(o -> o.candidatos().size())
                .thenComparing(o -> -o.actividad().duracionMinutos())
                .thenComparing(o -> o.actividad().actividadId(), Comparator.nullsLast(Comparator.naturalOrder())));

        // 1) Voraz: siempre da un resultado.
        Map<Long, Asignacion> mejor = new HashMap<>();
        for (Opciones o : opciones) {
            Asignacion a = primeraLibre(o, ocupados);
            if (a != null) {
                ocupar(ocupados, a, o.actividad());
                mejor.put(a.actividadId(), a);
            }
        }
        long ubicables = opciones.stream().filter(o -> !o.candidatos().isEmpty()).count();

        // 2) Si quedaron ubicables sin ubicar, buscar una combinación mejor con retroceso acotado.
        if (mejor.size() < ubicables) {
            for (Asignacion a : mejor.values()) {
                desocupar(ocupados, a);
            }
            Busqueda b = new Busqueda(opciones, ocupados, mejor.size(), (int) ubicables);
            b.buscar(0, new ArrayList<>());
            if (b.mejor != null) {
                mejor = new HashMap<>();
                for (Asignacion a : b.mejor) {
                    mejor.put(a.actividadId(), a);
                }
            }
        }

        List<Asignacion> asignadas = new ArrayList<>();
        List<NoUbicada> pendientes = new ArrayList<>();
        for (Opciones o : opciones) {
            Asignacion a = mejor.get(o.actividad().actividadId());
            if (a != null) {
                asignadas.add(a);
            } else {
                pendientes.add(new NoUbicada(o.actividad().actividadId(),
                        o.candidatos().isEmpty() ? o.motivoSinOpciones() : MotivoPendiente.SIN_ESPACIO_LIBRE));
            }
        }
        return new Resultado(List.copyOf(asignadas), List.copyOf(pendientes));
    }

    /** Primer inicio de la actividad que no choca con lo ocupado, o null. */
    private static Asignacion primeraLibre(Opciones o, Map<LocalDate, List<Ocupado>> ocupados) {
        ActividadPorUbicar a = o.actividad();
        for (Candidato c : o.candidatos()) {
            if (libre(c, a, ocupados)) {
                return asignacion(c, a);
            }
        }
        return null;
    }

    private static boolean libre(Candidato c, ActividadPorUbicar a, Map<LocalDate, List<Ocupado>> ocupados) {
        return !chocaCon(ocupados.getOrDefault(c.fecha(), List.of()), c.sala(), a, c.inicio(),
                c.inicio() + a.duracionMinutos());
    }

    private static Asignacion asignacion(Candidato c, ActividadPorUbicar a) {
        return new Asignacion(a.actividadId(), c.fecha(), hora(c.inicio()), hora(c.inicio() + a.duracionMinutos()),
                c.sala().id());
    }

    private static void ocupar(Map<LocalDate, List<Ocupado>> ocupados, Asignacion asignacion, ActividadPorUbicar a) {
        ocupados.computeIfAbsent(asignacion.fecha(), f -> new ArrayList<>())
                .add(new Ocupado(asignacion.actividadId(), asignacion.salaId(), a.ponenteId(), a.simultanea(),
                        minutos(asignacion.inicio()), minutos(asignacion.inicio()) + a.duracionMinutos()));
    }

    private static void desocupar(Map<LocalDate, List<Ocupado>> ocupados, Asignacion asignacion) {
        List<Ocupado> dia = ocupados.get(asignacion.fecha());
        for (int i = dia.size() - 1; i >= 0; i--) {
            if (Objects.equals(dia.get(i).actividadId(), asignacion.actividadId())) {
                dia.remove(i);
                return;
            }
        }
    }

    /** Búsqueda con retroceso y poda: maximiza las actividades ubicadas, dentro del presupuesto. */
    private static final class Busqueda {
        private final List<Opciones> opciones;
        private final Map<LocalDate, List<Ocupado>> ocupados;
        private final int ubicables;
        /** Actividades con opciones desde la posición i hasta el final (cota superior). */
        private final int[] restantes;
        private int pasos;
        private int mejorCantidad;
        private List<Asignacion> mejor;

        Busqueda(List<Opciones> opciones, Map<LocalDate, List<Ocupado>> ocupados, int cantidadVoraz, int ubicables) {
            this.opciones = opciones;
            this.ocupados = ocupados;
            this.ubicables = ubicables;
            this.mejorCantidad = cantidadVoraz;
            this.restantes = new int[opciones.size() + 1];
            for (int i = opciones.size() - 1; i >= 0; i--) {
                restantes[i] = restantes[i + 1] + (opciones.get(i).candidatos().isEmpty() ? 0 : 1);
            }
        }

        private boolean terminar() {
            return pasos >= PRESUPUESTO_BUSQUEDA || mejorCantidad >= ubicables;
        }

        void buscar(int i, List<Asignacion> actual) {
            if (terminar() || actual.size() + restantes[i] <= mejorCantidad) {
                return;
            }
            if (i == opciones.size()) {
                mejorCantidad = actual.size();
                mejor = List.copyOf(actual);
                return;
            }
            Opciones o = opciones.get(i);
            for (Candidato c : o.candidatos()) {
                if (++pasos >= PRESUPUESTO_BUSQUEDA) {
                    return;
                }
                if (!libre(c, o.actividad(), ocupados)) {
                    continue;
                }
                Asignacion a = asignacion(c, o.actividad());
                ocupar(ocupados, a, o.actividad());
                actual.add(a);
                buscar(i + 1, actual);
                actual.remove(actual.size() - 1);
                desocupar(ocupados, a);
                if (terminar()) {
                    return;
                }
            }
            // Dejarla pendiente y seguir con las demás.
            buscar(i + 1, actual);
        }
    }

    /** Inicios posibles de la actividad, ignorando por ahora las demás actividades. */
    private static Opciones opcionesDe(ActividadPorUbicar a, LocalDate desde, LocalDate hasta,
                                       List<SalaDisponible> salas) {
        boolean haySalaConFranjas = false;
        List<Franja> franjasPonente = a.ponenteId() == null ? List.of()
                : a.franjasPonente().stream().filter(f -> dentro(f.fecha(), desde, hasta)).toList();
        List<Candidato> candidatos = new ArrayList<>();

        for (int i = 0; i < salas.size(); i++) {
            SalaDisponible sala = salas.get(i);
            if (!compatible(sala, a.requisitoSala())) {
                continue;
            }
            for (Franja fs : sala.franjas()) {
                if (!dentro(fs.fecha(), desde, hasta)) {
                    continue;
                }
                haySalaConFranjas = true;
                if (a.ponenteId() == null) {
                    agregarInicios(candidatos, fs.fecha(), minutos(fs.inicio()), minutos(fs.fin()), a, i, sala);
                    continue;
                }
                for (Franja fp : franjasPonente) {
                    if (!fp.fecha().equals(fs.fecha())) {
                        continue;
                    }
                    int ini = Math.max(minutos(fs.inicio()), minutos(fp.inicio()));
                    int fin = Math.min(minutos(fs.fin()), minutos(fp.fin()));
                    agregarInicios(candidatos, fs.fecha(), ini, fin, a, i, sala);
                }
            }
        }

        candidatos.sort(Comparator.comparing(Candidato::fecha)
                .thenComparingInt(Candidato::inicio)
                .thenComparingInt(Candidato::ordenSala));
        List<Candidato> unicos = new ArrayList<>();
        Candidato anterior = null;
        for (Candidato c : candidatos) {
            // Franjas solapadas de la misma sala producen el mismo inicio dos veces.
            if (anterior == null || !anterior.fecha().equals(c.fecha()) || anterior.inicio() != c.inicio()
                    || anterior.ordenSala() != c.ordenSala()) {
                unicos.add(c);
            }
            anterior = c;
        }

        MotivoPendiente motivo = null;
        if (unicos.isEmpty()) {
            if (!haySalaConFranjas) {
                motivo = MotivoPendiente.SIN_SALA_DISPONIBLE;
            } else if (a.ponenteId() != null && franjasPonente.isEmpty()) {
                motivo = MotivoPendiente.SIN_DISPONIBILIDAD_PONENTE;
            } else {
                motivo = MotivoPendiente.SIN_FRANJA_COMUN;
            }
        }
        return new Opciones(a, unicos, motivo);
    }

    private static void agregarInicios(List<Candidato> candidatos, LocalDate fecha, int ini, int fin,
                                       ActividadPorUbicar a, int ordenSala, SalaDisponible sala) {
        for (int inicio = ini; inicio + a.duracionMinutos() <= fin; inicio += PASO_MINUTOS) {
            candidatos.add(new Candidato(fecha, inicio, ordenSala, sala));
        }
    }

    /** Mismas reglas de choque que {@link ActividadService#detectarConflictos} (HU-21). */
    private static boolean chocaCon(List<Ocupado> mismoDia, SalaDisponible sala, ActividadPorUbicar a,
                                    int inicio, int fin) {
        for (Ocupado o : mismoDia) {
            boolean seTraslapan = inicio < o.fin() && o.inicio() < fin;
            if (!seTraslapan) {
                continue;
            }
            if (a.ponenteId() != null && a.ponenteId().equals(o.ponenteId())) {
                return true;
            }
            boolean ambasSimultaneas = a.simultanea() && o.simultanea();
            if (Objects.equals(o.salaId(), sala.id())) {
                if (!ambasSimultaneas || !sala.permiteSimultaneidad()) {
                    return true;
                }
            } else if (!ambasSimultaneas) {
                return true;
            }
        }
        return false;
    }

    private static boolean compatible(SalaDisponible sala, RequisitoSala requisito) {
        return switch (requisito == null ? RequisitoSala.CUALQUIERA : requisito) {
            case FISICA -> !sala.virtual();
            case VIRTUAL -> sala.virtual();
            case CUALQUIERA -> true;
        };
    }

    private static boolean dentro(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        return fecha != null && !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    private static int minutos(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private static LocalTime hora(int minutos) {
        if (minutos >= MINUTOS_DIA) {
            return LocalTime.MAX.withSecond(0).withNano(0);
        }
        return LocalTime.of(minutos / 60, minutos % 60);
    }
}
