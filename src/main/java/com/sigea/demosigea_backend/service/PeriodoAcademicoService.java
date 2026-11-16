package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.periodo.ActividadPeriodoResponse;
import com.sigea.demosigea_backend.dto.periodo.ComparacionSemestresResponse;
import com.sigea.demosigea_backend.dto.periodo.ConsultaPeriodoResponse;
import com.sigea.demosigea_backend.dto.periodo.EventoPeriodoResponse;
import com.sigea.demosigea_backend.dto.periodo.FiltroPeriodo;
import com.sigea.demosigea_backend.dto.periodo.IndicadoresPeriodoResponse;
import com.sigea.demosigea_backend.dto.periodo.VariacionIndicadorResponse;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.InscripcionActividadRepository;
import com.sigea.demosigea_backend.repository.InscripcionEventoRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consulta de eventos e indicadores del programa por periodo académico y comparación entre semestres
 * (HU-27, Criterios 3, 4 y 5).
 * <p>
 * Los indicadores se calculan al vuelo con consultas agregadas sobre las tablas existentes
 * ({@code eventos}, {@code actividades}, {@code inscripciones_evento}, {@code inscripciones_actividad},
 * {@code participaciones_conferencista} y {@code propuestas}); no se persisten.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Service
@RequiredArgsConstructor
public class PeriodoAcademicoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final String SIN_TIPO = "Sin tipo";

    private final EventoRepository eventoRepository;
    private final ActividadRepository actividadRepository;
    private final InscripcionEventoRepository inscripcionEventoRepository;
    private final InscripcionActividadRepository inscripcionActividadRepository;
    private final ParticipacionConferencistaRepository participacionRepository;
    private final PropuestaRepository propuestaRepository;

    /**
     * @return Semestres que tienen eventos registrados, del más reciente al más antiguo (para los filtros).
     */
    @Transactional(readOnly = true)
    public List<String> listarSemestres() {
        return eventoRepository.findSemestresRegistrados();
    }

    /**
     * Criterios 3 y 4: eventos (con sus actividades) que cumplen los filtros.
     *
     * @param filtro Filtros de periodo (opcionales).
     * @return Eventos encontrados o un mensaje si no hay información.
     */
    @Transactional(readOnly = true)
    public ConsultaPeriodoResponse consultarEventos(FiltroPeriodo filtro) {
        FiltroPeriodo f = ReglasFiltroPeriodo.validar(filtro);
        DatosPeriodo datos = cargar(f);
        if (datos.eventos().isEmpty()) {
            return new ConsultaPeriodoResponse(f, false, ReglasFiltroPeriodo.mensajeSinInformacion(f), 0, List.of());
        }

        Map<Long, Long> inscritosPorEvento = aMapaLong(inscripcionEventoRepository.contarPorEvento(datos.eventoIds()));
        List<Long> actividadIds = datos.actividades().stream().map(Actividad::getId).toList();
        Map<Long, Long> inscritosPorActividad = actividadIds.isEmpty()
                ? Map.of() : aMapaLong(inscripcionActividadRepository.contarPorActividad(actividadIds));
        Map<Long, List<Actividad>> actividadesPorEvento = datos.actividades().stream()
                .collect(Collectors.groupingBy(a -> a.getEvento().getId()));

        List<EventoPeriodoResponse> eventos = datos.eventos().stream().map(evento -> {
            List<ActividadPeriodoResponse> actividades = actividadesPorEvento.getOrDefault(evento.getId(), List.of())
                    .stream()
                    .map(a -> aResponse(a, inscritosPorActividad.getOrDefault(a.getId(), 0L)))
                    .toList();
            return new EventoPeriodoResponse(
                    evento.getId(), evento.getNombre(), nombreTipo(evento), evento.getSemestre(),
                    evento.getFechaInicio(), evento.getFechaFin(), evento.getEstado(), evento.getModalidad(),
                    inscritosPorEvento.getOrDefault(evento.getId(), 0L), actividades.size(), actividades);
        }).toList();

        return new ConsultaPeriodoResponse(f, true, null, eventos.size(), eventos);
    }

    /**
     * Criterios 3 y 4: indicadores del programa sobre los eventos que cumplen los filtros.
     *
     * @param filtro Filtros de periodo (opcionales).
     * @return Indicadores; en cero y con mensaje si no hay información.
     */
    @Transactional(readOnly = true)
    public IndicadoresPeriodoResponse calcularIndicadores(FiltroPeriodo filtro) {
        FiltroPeriodo f = ReglasFiltroPeriodo.validar(filtro);
        return indicadores(f);
    }

    /**
     * Criterio 5: indicadores de dos semestres presentados de forma comparativa.
     * Los filtros de tipo de evento y de actividad se aplican a ambos semestres.
     *
     * @param semestreA     Semestre base de la comparación.
     * @param semestreB     Semestre comparado.
     * @param tipoEventoId  Tipo de evento (opcional).
     * @param tipoActividad Nombre del tipo de actividad (opcional).
     * @return Indicadores de ambos semestres y su variación.
     * @throws SolicitudInvalidaException si falta un semestre, no tiene formato válido o ambos son iguales.
     */
    @Transactional(readOnly = true)
    public ComparacionSemestresResponse comparar(String semestreA, String semestreB, Long tipoEventoId,
                                                 String tipoActividad) {
        String a = semestreA == null ? null : semestreA.trim();
        String b = semestreB == null ? null : semestreB.trim();
        if (a == null || a.isEmpty() || b == null || b.isEmpty()) {
            throw new SolicitudInvalidaException("Seleccione los dos semestres que desea comparar.");
        }
        ReglasFiltroPeriodo.validarSemestre(a);
        ReglasFiltroPeriodo.validarSemestre(b);
        if (a.equals(b)) {
            throw new SolicitudInvalidaException("Seleccione dos semestres distintos para comparar.");
        }

        FiltroPeriodo base = ReglasFiltroPeriodo.validar(new FiltroPeriodo(null, null, null, tipoEventoId, tipoActividad));
        IndicadoresPeriodoResponse periodoA = indicadores(base.conSemestre(a));
        IndicadoresPeriodoResponse periodoB = indicadores(base.conSemestre(b));

        String mensaje = null;
        if (!periodoA.hayInformacion() && !periodoB.hayInformacion()) {
            mensaje = "No hay información disponible para los semestres " + a + " y " + b + ".";
        } else if (!periodoA.hayInformacion()) {
            mensaje = "No hay información disponible para el semestre " + a + ".";
        } else if (!periodoB.hayInformacion()) {
            mensaje = "No hay información disponible para el semestre " + b + ".";
        }
        return new ComparacionSemestresResponse(a, b, periodoA, periodoB, comparativo(periodoA, periodoB), mensaje);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Cálculo
    // ------------------------------------------------------------------------------------------------------------

    private IndicadoresPeriodoResponse indicadores(FiltroPeriodo f) {
        DatosPeriodo datos = cargar(f);
        if (datos.eventos().isEmpty()) {
            return new IndicadoresPeriodoResponse(f, false, ReglasFiltroPeriodo.mensajeSinInformacion(f),
                    0, Map.of(), Map.of(), 0, Map.of(), 0, 0, Map.of(), 0,
                    BigDecimal.ZERO.setScale(2), 0, 0, 0, 0, null);
        }
        List<Long> eventoIds = datos.eventoIds();

        Map<String, Long> eventosPorTipo = contarOrdenado(datos.eventos(), this::nombreTipoOSinTipo);
        Map<String, Long> eventosPorEstado = contarOrdenado(datos.eventos(), e -> e.getEstado().name());

        List<Actividad> vigentes = datos.actividades().stream().filter(a -> !a.estaCancelada()).toList();
        Map<String, Long> actividadesPorTipo = contarOrdenado(vigentes,
                a -> a.getTipoActividad() == null ? SIN_TIPO : a.getTipoActividad().getNombre().trim());
        List<Long> actividadIds = vigentes.stream().map(Actividad::getId).toList();
        long inscripcionesActividades = actividadIds.isEmpty() ? 0
                : sumar(aMapaLong(inscripcionActividadRepository.contarPorActividad(actividadIds)).values());

        long totalInscripciones = sumar(aMapaLong(inscripcionEventoRepository.contarPorEvento(eventoIds)).values());
        long participantesUnicos = inscripcionEventoRepository.contarPersonasDistintas(eventoIds);
        Map<String, Long> inscripcionesPorCategoria = new TreeMap<>(aMapaTexto(
                inscripcionEventoRepository.contarPorCategoria(eventoIds)));

        Map<String, Long> participaciones = aMapaTexto(participacionRepository.contarPorTipo(eventoIds));
        Map<String, Long> propuestasPorEstado = aMapaTexto(propuestaRepository.contarPorEstado(eventoIds));
        long propuestasRecibidas = sumar(propuestasPorEstado.values());
        long propuestasAprobadas = propuestasPorEstado.getOrDefault(EstadoPropuesta.aprobada.name(), 0L);

        long totalEventos = datos.eventos().size();
        return new IndicadoresPeriodoResponse(
                f, true, null,
                totalEventos, eventosPorTipo, eventosPorEstado,
                vigentes.size(), actividadesPorTipo,
                totalInscripciones, participantesUnicos, inscripcionesPorCategoria, inscripcionesActividades,
                dividir(totalInscripciones, totalEventos, BigDecimal.ONE),
                participaciones.getOrDefault(TipoParticipacionConferencista.conferencista.name(), 0L),
                participaciones.getOrDefault(TipoParticipacionConferencista.ponente.name(), 0L),
                propuestasRecibidas, propuestasAprobadas,
                propuestasRecibidas == 0 ? null : dividir(propuestasAprobadas, propuestasRecibidas, CIEN));
    }

    /** Eventos que cumplen los filtros y sus actividades (filtradas por tipo y rango de fechas). */
    private DatosPeriodo cargar(FiltroPeriodo f) {
        List<Evento> eventos = eventoRepository.findAll(especificacion(f),
                Sort.by(Sort.Order.desc("fechaInicio"), Sort.Order.desc("id")));
        if (eventos.isEmpty()) {
            return new DatosPeriodo(List.of(), List.of(), List.of());
        }
        List<Long> ids = eventos.stream().map(Evento::getId).toList();
        List<Actividad> actividades = actividadRepository.findDeEventos(ids).stream()
                .filter(a -> ReglasFiltroPeriodo.actividadCumple(a, f))
                .toList();
        return new DatosPeriodo(eventos, ids, actividades);
    }

    /** Filtros de semestre, tipo de evento, rango de fechas (cruce) y existencia de actividades del tipo. */
    static Specification<Evento> especificacion(FiltroPeriodo f) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (f.semestre() != null) {
                condiciones.add(cb.equal(root.get("semestre"), f.semestre()));
            }
            if (f.tipoEventoId() != null) {
                condiciones.add(cb.equal(root.get("tipoEvento").get("id"), f.tipoEventoId()));
            }
            if (f.fechaDesde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fechaFin"), f.fechaDesde()));
            }
            if (f.fechaHasta() != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.get("fechaInicio"), f.fechaHasta()));
            }
            if (f.tipoActividad() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Actividad> actividad = sub.from(Actividad.class);
                sub.select(actividad.get("id")).where(
                        cb.equal(actividad.get("evento"), root),
                        cb.equal(cb.lower(cb.trim(actividad.get("tipoActividad").get("nombre"))),
                                f.tipoActividad().toLowerCase()));
                condiciones.add(cb.exists(sub));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    private static List<VariacionIndicadorResponse> comparativo(IndicadoresPeriodoResponse a,
                                                                IndicadoresPeriodoResponse b) {
        List<VariacionIndicadorResponse> lista = new ArrayList<>();
        lista.add(variacion("totalEventos", "Eventos realizados", a.totalEventos(), b.totalEventos()));
        lista.add(variacion("totalActividades", "Actividades programadas", a.totalActividades(), b.totalActividades()));
        lista.add(variacion("totalInscripciones", "Inscripciones a eventos", a.totalInscripciones(), b.totalInscripciones()));
        lista.add(variacion("participantesUnicos", "Participantes únicos", a.participantesUnicos(), b.participantesUnicos()));
        lista.add(variacion("inscripcionesActividades", "Inscripciones a actividades",
                a.inscripcionesActividades(), b.inscripcionesActividades()));
        lista.add(variacion("promedioInscritosPorEvento", "Promedio de inscritos por evento",
                a.promedioInscritosPorEvento(), b.promedioInscritosPorEvento()));
        lista.add(variacion("totalConferencistas", "Conferencistas", a.totalConferencistas(), b.totalConferencistas()));
        lista.add(variacion("totalPonentes", "Ponentes", a.totalPonentes(), b.totalPonentes()));
        lista.add(variacion("propuestasRecibidas", "Propuestas recibidas", a.propuestasRecibidas(), b.propuestasRecibidas()));
        lista.add(variacion("propuestasAprobadas", "Propuestas aprobadas", a.propuestasAprobadas(), b.propuestasAprobadas()));
        lista.add(variacion("tasaAprobacionPropuestas", "Tasa de aprobación de propuestas (%)",
                a.tasaAprobacionPropuestas(), b.tasaAprobacionPropuestas()));
        return lista;
    }

    private static VariacionIndicadorResponse variacion(String codigo, String nombre, long a, long b) {
        return variacion(codigo, nombre, BigDecimal.valueOf(a), BigDecimal.valueOf(b));
    }

    static VariacionIndicadorResponse variacion(String codigo, String nombre, BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return new VariacionIndicadorResponse(codigo, nombre, a, b, null, null, "NO_COMPARABLE");
        }
        BigDecimal diferencia = b.subtract(a);
        BigDecimal porcentaje = a.signum() == 0 ? null
                : diferencia.multiply(CIEN).divide(a.abs(), 2, RoundingMode.HALF_UP);
        String tendencia = switch (diferencia.signum()) {
            case 1 -> "AUMENTO";
            case -1 -> "DISMINUCION";
            default -> "SIN_CAMBIO";
        };
        return new VariacionIndicadorResponse(codigo, nombre, a, b, diferencia, porcentaje, tendencia);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------------------------------------------------

    private static ActividadPeriodoResponse aResponse(Actividad a, long inscritos) {
        String ponente = a.getPonente() == null ? null
                : (a.getPonente().getNombres() + " " + a.getPonente().getApellidos()).trim();
        return new ActividadPeriodoResponse(
                a.getId(), a.getNombre(),
                a.getTipoActividad() == null ? null : a.getTipoActividad().getNombre(),
                a.getFecha(), a.getHoraInicio(), a.getHoraFin(),
                a.getSala() == null ? null : a.getSala().getNombre(),
                ponente, a.getEstado(), inscritos);
    }

    private static String nombreTipo(Evento e) {
        return e.getTipoEvento() == null ? null : e.getTipoEvento().getNombre();
    }

    private String nombreTipoOSinTipo(Evento e) {
        String nombre = nombreTipo(e);
        return nombre == null ? SIN_TIPO : nombre;
    }

    private static <T> Map<String, Long> contarOrdenado(Collection<T> elementos, Function<T, String> clave) {
        return elementos.stream().collect(Collectors.groupingBy(clave, TreeMap::new, Collectors.counting()));
    }

    /** Convierte filas {@code [id, cantidad]} en mapa. */
    private static Map<Long, Long> aMapaLong(List<Object[]> filas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] fila : filas) {
            mapa.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
        }
        return mapa;
    }

    /** Convierte filas {@code [enum o texto, cantidad]} en mapa con la clave como texto. */
    private static Map<String, Long> aMapaTexto(List<Object[]> filas) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            String clave = fila[0] instanceof Enum<?> e ? e.name() : String.valueOf(fila[0]);
            mapa.merge(clave, ((Number) fila[1]).longValue(), Long::sum);
        }
        return mapa;
    }

    private static long sumar(Collection<Long> valores) {
        return valores.stream().mapToLong(Long::longValue).sum();
    }

    /** (numerador / denominador) * factor con dos decimales. */
    private static BigDecimal dividir(long numerador, long denominador, BigDecimal factor) {
        if (denominador == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(numerador).multiply(factor)
                .divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP);
    }

    /** Eventos que cumplen los filtros, sus ids y sus actividades filtradas. */
    private record DatosPeriodo(List<Evento> eventos, List<Long> eventoIds, List<Actividad> actividades) {
    }
}
