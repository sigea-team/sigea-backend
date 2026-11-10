package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.actividad.ActividadResponse;
import com.sigea.demosigea_backend.dto.actividad.ConflictoActividadResponse;
import com.sigea.demosigea_backend.dto.agenda.ActividadAgendaPublicaResponse;
import com.sigea.demosigea_backend.dto.agenda.ActividadIncompletaResponse;
import com.sigea.demosigea_backend.dto.agenda.ActividadPendienteResponse;
import com.sigea.demosigea_backend.dto.agenda.AgendaPublicaResponse;
import com.sigea.demosigea_backend.dto.agenda.AsignacionPropuestaResponse;
import com.sigea.demosigea_backend.dto.agenda.ConflictoAgendaResponse;
import com.sigea.demosigea_backend.dto.agenda.DiaAgendaEstadoResponse;
import com.sigea.demosigea_backend.dto.agenda.DiaAgendaPublicaResponse;
import com.sigea.demosigea_backend.dto.agenda.GeneracionAgendaResponse;
import com.sigea.demosigea_backend.dto.agenda.PublicacionAgendaResponse;
import com.sigea.demosigea_backend.dto.agenda.ValidacionAgendaResponse;
import com.sigea.demosigea_backend.exception.AgendaNoPublicableException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.AgendaPublicada;
import com.sigea.demosigea_backend.model.DisponibilidadPonente;
import com.sigea.demosigea_backend.model.DisponibilidadSala;
import com.sigea.demosigea_backend.model.EstadoAgenda;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.ModalidadEvento;
import com.sigea.demosigea_backend.model.Sala;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoSala;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.AgendaPublicadaRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadPonenteRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadSalaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.SalaRepository;
import com.sigea.demosigea_backend.service.GeneradorAgenda.ActividadPorUbicar;
import com.sigea.demosigea_backend.service.GeneradorAgenda.Asignacion;
import com.sigea.demosigea_backend.service.GeneradorAgenda.Franja;
import com.sigea.demosigea_backend.service.GeneradorAgenda.NoUbicada;
import com.sigea.demosigea_backend.service.GeneradorAgenda.Ocupacion;
import com.sigea.demosigea_backend.service.GeneradorAgenda.RequisitoSala;
import com.sigea.demosigea_backend.service.GeneradorAgenda.Resultado;
import com.sigea.demosigea_backend.service.GeneradorAgenda.SalaDisponible;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generación y publicación de la agenda del evento (HU-22, RF38 + RF39).
 * <ul>
 *   <li><b>Criterio 1</b>: {@link #proponer} calcula una distribución sin conflictos de las actividades por
 *       programar, a partir de las salas y las disponibilidades registradas, sin guardar nada; {@link #aplicar} la
 *       guarda. Después el organizador la ajusta a mano con HU-21 (PUT de la actividad) o devolviendo actividades a
 *       por programar.</li>
 *   <li><b>Criterio 2</b>: el resultado lista las actividades que no se pudieron ubicar y el motivo.</li>
 *   <li><b>Criterio 3</b>: {@link #publicar} valida que no haya cruces y publica la agenda por día;
 *       {@link #agendaPublica} la muestra a los participantes organizada por día y hora.</li>
 *   <li><b>Criterio 4</b>: no se publica si alguna actividad no tiene sala u horario.</li>
 *   <li><b>Criterio 5</b>: los cambios posteriores se notifican a los inscritos ({@link AvisoCambiosAgenda}).</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgendaService {

    private static final String ENTIDAD = "eventos";
    private static final List<String> NO_VISIBLES = List.of(Actividad.ESTADO_POR_PROGRAMAR, Actividad.ESTADO_CANCELADA);

    private final EventoRepository eventoRepository;
    private final ActividadRepository actividadRepository;
    private final SalaRepository salaRepository;
    private final DisponibilidadSalaRepository disponibilidadSalaRepository;
    private final DisponibilidadPonenteRepository disponibilidadPonenteRepository;
    private final AgendaPublicadaRepository agendaPublicadaRepository;
    private final AvisoCambiosAgenda avisoCambiosAgenda;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterios 1 y 2: generación automática
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Criterios 1 y 2: propone la ubicación de las actividades por programar sin guardar nada.
     *
     * @param eventoId             ID del evento.
     * @param reubicarAutomaticas  si es true, también vuelve a ubicar las actividades que ubicó una generación
     *                             anterior y nadie ajustó a mano.
     * @return asignaciones propuestas y actividades pendientes de asignación manual.
     */
    @Transactional(readOnly = true)
    public GeneracionAgendaResponse proponer(Long eventoId, boolean reubicarAutomaticas) {
        Evento evento = buscarEvento(eventoId);
        exigirAgendaModificable(evento);
        Calculo calculo = calcular(evento, reubicarAutomaticas);
        return respuesta(eventoId, false, calculo);
    }

    /**
     * Criterio 1: calcula y guarda la propuesta. Las actividades ubicadas quedan {@code programada} con
     * {@code asignacionAutomatica = true}; las que no se pudieron ubicar quedan por programar (Criterio 2).
     *
     * @param eventoId            ID del evento.
     * @param reubicarAutomaticas ver {@link #proponer}.
     * @return lo que se guardó y lo que quedó pendiente.
     */
    @Transactional
    public GeneracionAgendaResponse aplicar(Long eventoId, boolean reubicarAutomaticas) {
        Evento evento = eventoRepository.findByIdParaProgramar(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId));
        exigirAgendaModificable(evento);
        Calculo calculo = calcular(evento, reubicarAutomaticas);

        Map<Long, AvisoCambiosAgenda.Programacion> antes = new LinkedHashMap<>();
        calculo.porUbicar().forEach((id, a) -> antes.put(id, AvisoCambiosAgenda.Programacion.de(a)));

        for (Asignacion asignacion : calculo.resultado().asignadas()) {
            Actividad a = calculo.porUbicar().get(asignacion.actividadId());
            a.setFecha(asignacion.fecha());
            a.setHoraInicio(asignacion.inicio());
            a.setHoraFin(asignacion.fin());
            a.setSala(calculo.salas().get(asignacion.salaId()));
            a.setEstado(Actividad.ESTADO_PROGRAMADA);
            a.setAsignacionAutomatica(true);
        }
        for (NoUbicada pendiente : calculo.resultado().pendientes()) {
            Actividad a = calculo.porUbicar().get(pendiente.actividadId());
            if (!a.estaPorProgramar()) {
                // Se pidió reubicarla y ya no cabe: vuelve a por programar.
                ActividadService.quitarHorario(a);
            }
        }
        actividadRepository.saveAll(calculo.porUbicar().values());

        // Seguridad: lo que se acaba de ubicar no puede quedar con cruces (los cruces previos, hechos a mano, se
        // informan al validar y no impiden guardar la propuesta).
        Set<Long> ubicadas = calculo.resultado().asignadas().stream().map(Asignacion::actividadId)
                .collect(Collectors.toSet());
        List<ConflictoAgendaResponse> conflictos = revisarAgenda(evento).conflictos().stream()
                .filter(c -> ubicadas.contains(c.actividadId()) || ubicadas.contains(c.conflicto().actividadId()))
                .toList();
        if (!conflictos.isEmpty()) {
            throw new IllegalStateException("La agenda generada tiene cruces: " + conflictos);
        }
        for (Actividad a : calculo.porUbicar().values()) {
            avisoCambiosAgenda.actividadModificada(a, antes.get(a.getId()));
        }

        log.info("Agenda del evento {} generada: {} actividades ubicadas, {} pendientes", eventoId,
                calculo.resultado().asignadas().size(), calculo.resultado().pendientes().size());
        auditoriaService.registrar(TipoOperacionAuditoria.AGENDA_GENERADA, ENTIDAD, eventoId,
                DetalleAuditoria.de(
                        "reubicarAutomaticas", reubicarAutomaticas,
                        "ubicadas", calculo.resultado().asignadas().stream().map(Asignacion::actividadId).toList(),
                        "pendientes", calculo.resultado().pendientes().stream().map(NoUbicada::actividadId).toList()));
        return respuesta(eventoId, true, calculo);
    }

    /** Datos de entrada y resultado de una generación. */
    private record Calculo(Map<Long, Actividad> porUbicar, Map<Long, Sala> salas, Resultado resultado) {
    }

    private Calculo calcular(Evento evento, boolean reubicarAutomaticas) {
        if (evento.getFechaInicio() == null || evento.getFechaFin() == null) {
            throw new SolicitudInvalidaException("El evento no tiene fechas de inicio y fin; no se puede generar la agenda.");
        }
        Long eventoId = evento.getId();
        List<Actividad> todas = actividadRepository.findAgendaDelEvento(eventoId);

        Map<Long, Actividad> porUbicar = new LinkedHashMap<>();
        List<Ocupacion> fijas = new ArrayList<>();
        for (Actividad a : todas) {
            if (a.estaCancelada()) {
                continue;
            }
            boolean reubicable = reubicarAutomaticas && a.isAsignacionAutomatica() && a.tieneHorarioCompleto();
            if (a.estaPorProgramar() || reubicable) {
                porUbicar.put(a.getId(), a);
            } else if (a.tieneHorarioCompleto()) {
                fijas.add(new Ocupacion(a.getId(), a.getFecha(), a.getHoraInicio(), a.getHoraFin(),
                        a.getSala().getId(), a.getPonente() != null ? a.getPonente().getId() : null,
                        a.isPermiteSimultaneidad()));
            }
        }

        List<Sala> salasEvento = salaRepository.findByEvento_IdOrderByNombreAscIdAsc(eventoId);
        List<Long> salaIds = salasEvento.stream().map(Sala::getId).toList();
        // Salas reutilizables: lo que otros eventos tienen en estas salas también las ocupa (solo la sala).
        for (Actividad otra : deOtrosEventos(evento, salaIds)) {
            fijas.add(new Ocupacion(otra.getId(), otra.getFecha(), otra.getHoraInicio(), otra.getHoraFin(),
                    otra.getSala().getId(), null, otra.isPermiteSimultaneidad()));
        }
        Map<Long, List<Franja>> franjasSala = (salaIds.isEmpty() ? List.<DisponibilidadSala>of()
                : disponibilidadSalaRepository.findBySala_IdInOrderByFechaAscHoraInicioAsc(salaIds)).stream()
                .collect(Collectors.groupingBy(d -> d.getSala().getId(), LinkedHashMap::new,
                        Collectors.mapping(AgendaService::franjaSala, Collectors.toList())));
        Map<Long, List<Franja>> franjasPonente = disponibilidadPonenteRepository
                .findByEvento_IdOrderByFechaAscHoraInicioAsc(eventoId).stream()
                .collect(Collectors.groupingBy(d -> d.getPersona().getId(), LinkedHashMap::new,
                        Collectors.mapping(AgendaService::franjaPonente, Collectors.toList())));

        List<SalaDisponible> salas = salasEvento.stream()
                .map(s -> new SalaDisponible(s.getId(), s.getTipo() == TipoSala.virtual, s.isPermiteSimultaneidad(),
                        franjasSala.getOrDefault(s.getId(), List.of())))
                .toList();
        List<ActividadPorUbicar> entrada = porUbicar.values().stream()
                .map(a -> {
                    Long ponenteId = a.getPonente() != null ? a.getPonente().getId() : null;
                    return new ActividadPorUbicar(a.getId(), duracionDe(a), ponenteId, requisito(a.getModalidad()),
                            a.isPermiteSimultaneidad(),
                            ponenteId == null ? List.of() : franjasPonente.getOrDefault(ponenteId, List.of()));
                })
                .toList();

        Resultado resultado = GeneradorAgenda.generar(evento.getFechaInicio(), evento.getFechaFin(), salas, fijas,
                entrada);
        Map<Long, Sala> salasPorId = salasEvento.stream()
                .collect(Collectors.toMap(Sala::getId, Function.identity()));
        return new Calculo(porUbicar, salasPorId, resultado);
    }

    private GeneracionAgendaResponse respuesta(Long eventoId, boolean aplicada, Calculo calculo) {
        List<AsignacionPropuestaResponse> asignadas = calculo.resultado().asignadas().stream()
                .map(x -> {
                    Actividad a = calculo.porUbicar().get(x.actividadId());
                    Sala sala = calculo.salas().get(x.salaId());
                    return new AsignacionPropuestaResponse(a.getId(), a.getNombre(), x.fecha(), x.inicio(), x.fin(),
                            sala.getId(), sala.getNombre(),
                            a.getPonente() != null ? a.getPonente().getId() : null,
                            ActividadResponse.nombreCompleto(a.getPonente()));
                })
                .sorted(Comparator.comparing(AsignacionPropuestaResponse::fecha)
                        .thenComparing(AsignacionPropuestaResponse::horaInicio)
                        .thenComparing(AsignacionPropuestaResponse::actividadId))
                .toList();
        List<ActividadPendienteResponse> pendientes = calculo.resultado().pendientes().stream()
                .map(p -> {
                    Actividad a = calculo.porUbicar().get(p.actividadId());
                    return new ActividadPendienteResponse(a.getId(), a.getNombre(), duracionDe(a),
                            a.getPonente() != null ? a.getPonente().getId() : null,
                            ActividadResponse.nombreCompleto(a.getPonente()), p.motivo(), mensaje(p, a));
                })
                .toList();
        return new GeneracionAgendaResponse(eventoId, aplicada, calculo.porUbicar().size(), asignadas, pendientes);
    }

    /** Criterio 2: explicación del motivo para el organizador. */
    static String mensaje(NoUbicada p, Actividad a) {
        String ponente = ActividadResponse.nombreCompleto(a.getPonente());
        RequisitoSala requisito = requisito(a.getModalidad());
        return switch (p.motivo()) {
            case SIN_SALA_DISPONIBLE -> switch (requisito) {
                case VIRTUAL -> "No hay salas virtuales con disponibilidad registrada en las fechas del evento.";
                case FISICA -> "No hay salas físicas con disponibilidad registrada en las fechas del evento.";
                case CUALQUIERA -> "No hay salas con disponibilidad registrada en las fechas del evento.";
            };
            case SIN_DISPONIBILIDAD_PONENTE -> "El ponente " + ponente
                    + " no tiene disponibilidad registrada en las fechas del evento.";
            case SIN_FRANJA_COMUN -> ponente != null
                    ? "Ninguna franja disponible de sala coincide con la disponibilidad de " + ponente + " durante los "
                    + duracionDe(a) + " minutos que dura la actividad."
                    : "Ninguna franja disponible de sala alcanza los " + duracionDe(a) + " minutos que dura la actividad.";
            case SIN_ESPACIO_LIBRE -> "Todos los horarios posibles chocan con otras actividades (horario, sala o "
                    + "ponente). Asígnala manualmente o márcala como simultánea si es paralela.";
        };
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterios 3 y 4: validación y publicación
    // ──────────────────────────────────────────────────────────────────────────────

    /** Actividades programadas, las incompletas y los cruces de la agenda. */
    record RevisionAgenda(List<Actividad> programadas, List<ActividadIncompletaResponse> incompletas,
                          List<ConflictoAgendaResponse> conflictos) {
    }

    /**
     * Criterios 3 y 4: revisa la agenda sin publicarla.
     *
     * @param eventoId ID del evento.
     * @return si se puede publicar, qué falta y qué se cruza.
     */
    @Transactional(readOnly = true)
    public ValidacionAgendaResponse validar(Long eventoId) {
        Evento evento = buscarEvento(eventoId);
        RevisionAgenda revision = revisarAgenda(evento);
        List<AgendaPublicada> filas = agendaPublicadaRepository.findByEvento_IdOrderByFechaDiaAsc(eventoId);

        String motivo = motivoNoPublicable(evento, revision);
        boolean publicada = filas.stream().anyMatch(f -> f.getEstado() == EstadoAgenda.publicada);
        LocalDateTime ultima = ultimaPublicacion(filas);
        return new ValidacionAgendaResponse(eventoId, motivo == null, motivo, publicada, ultima,
                revision.programadas().size(), dias(revision.programadas(), filas), revision.incompletas(),
                revision.conflictos());
    }

    /**
     * Criterios 3 y 4: publica la agenda del evento organizada por día. Si ya estaba publicada, la vuelve a publicar
     * con los días actuales (los días que se quedaron sin actividades dejan de mostrarse).
     *
     * @param eventoId ID del evento.
     * @return los días publicados.
     * @throws AgendaNoPublicableException  409 si hay actividades sin sala u horario, cruces o no hay actividades.
     * @throws OperacionNoPermitidaException 409 si el evento no está habilitado o en ejecución.
     */
    @Transactional
    public PublicacionAgendaResponse publicar(Long eventoId) {
        Evento evento = eventoRepository.findByIdParaProgramar(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId));
        exigirEventoPublicable(evento);
        RevisionAgenda revision = revisarAgenda(evento);
        if (revision.programadas().isEmpty() && revision.incompletas().isEmpty()) {
            throw new AgendaNoPublicableException(AgendaNoPublicableException.AGENDA_VACIA,
                    "La agenda no tiene actividades programadas para publicar.", List.of(), List.of());
        }
        if (!revision.incompletas().isEmpty()) {
            throw new AgendaNoPublicableException(AgendaNoPublicableException.AGENDA_INCOMPLETA,
                    "Hay " + revision.incompletas().size() + " actividad(es) sin sala u horario asignado. "
                            + "Asígnalos o elimina esas actividades antes de publicar la agenda.",
                    revision.incompletas(), revision.conflictos());
        }
        if (!revision.conflictos().isEmpty()) {
            throw new AgendaNoPublicableException(AgendaNoPublicableException.AGENDA_CON_CONFLICTOS,
                    "La agenda tiene " + revision.conflictos().size() + " cruce(s) entre actividades. "
                            + "Corrígelos antes de publicarla.",
                    List.of(), revision.conflictos());
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        Set<LocalDate> diasConActividades = revision.programadas().stream().map(Actividad::getFecha)
                .collect(Collectors.toCollection(TreeSet::new));
        Map<LocalDate, AgendaPublicada> filas = agendaPublicadaRepository.findByEvento_IdOrderByFechaDiaAsc(eventoId)
                .stream().collect(Collectors.toMap(AgendaPublicada::getFechaDia, Function.identity()));
        List<AgendaPublicada> cambios = new ArrayList<>();
        for (LocalDate dia : diasConActividades) {
            AgendaPublicada fila = filas.getOrDefault(dia,
                    AgendaPublicada.builder().evento(evento).fechaDia(dia).build());
            fila.setEstado(EstadoAgenda.publicada);
            fila.setFechaPublicacion(ahora);
            cambios.add(fila);
        }
        for (AgendaPublicada fila : filas.values()) {
            if (!diasConActividades.contains(fila.getFechaDia()) && fila.getEstado() == EstadoAgenda.publicada) {
                fila.setEstado(EstadoAgenda.no_publicada);
                cambios.add(fila);
            }
        }
        agendaPublicadaRepository.saveAll(cambios);

        List<LocalDate> dias = List.copyOf(diasConActividades);
        log.info("Agenda del evento {} publicada: {} días, {} actividades", eventoId, dias.size(),
                revision.programadas().size());
        auditoriaService.registrar(TipoOperacionAuditoria.AGENDA_PUBLICADA, ENTIDAD, eventoId,
                DetalleAuditoria.de("dias", dias.stream().map(String::valueOf).toList(),
                        "actividades", revision.programadas().size()));
        return new PublicacionAgendaResponse(eventoId, ahora, dias, revision.programadas().size());
    }

    /**
     * Criterio 3: agenda publicada del evento organizada por día y hora, para los participantes (sin autenticación).
     *
     * @param eventoId ID del evento.
     * @return los días publicados con sus actividades.
     * @throws RecursoNoEncontradoException si el evento no existe, no está publicado o su agenda no se ha publicado.
     */
    @Transactional(readOnly = true)
    public AgendaPublicaResponse agendaPublica(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .filter(e -> e.getEstado() != EstadoEvento.en_configuracion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId));
        List<AgendaPublicada> filas = agendaPublicadaRepository
                .findByEvento_IdAndEstadoOrderByFechaDiaAsc(eventoId, EstadoAgenda.publicada);
        if (filas.isEmpty()) {
            throw new RecursoNoEncontradoException("La agenda del evento «" + evento.getNombre()
                    + "» todavía no está publicada.");
        }
        List<LocalDate> fechas = filas.stream().map(AgendaPublicada::getFechaDia).toList();
        Map<LocalDate, List<ActividadAgendaPublicaResponse>> porDia = new TreeMap<>();
        for (Actividad a : actividadRepository.findEnFechasSinEstados(eventoId, fechas, NO_VISIBLES)) {
            porDia.computeIfAbsent(a.getFecha(), d -> new ArrayList<>()).add(publica(a));
        }
        List<DiaAgendaPublicaResponse> dias = porDia.entrySet().stream()
                .map(e -> new DiaAgendaPublicaResponse(e.getKey(), List.copyOf(e.getValue())))
                .toList();
        return new AgendaPublicaResponse(eventoId, evento.getNombre(), ultimaPublicacion(filas), dias);
    }

    /**
     * Revisa la agenda: actividades programadas, las incompletas (Criterio 4) y los cruces (Criterio 3), incluidos los
     * cruces de sala con actividades de otros eventos (salas reutilizables, mismas reglas que HU-21).
     */
    RevisionAgenda revisarAgenda(Evento evento) {
        Long eventoId = evento.getId();
        List<Actividad> programadas = new ArrayList<>();
        List<ActividadIncompletaResponse> incompletas = new ArrayList<>();
        for (Actividad a : actividadRepository.findAgendaDelEvento(eventoId)) {
            if (a.estaCancelada()) {
                continue;
            }
            if (a.estaPorProgramar() || !a.tieneHorarioCompleto()) {
                incompletas.add(new ActividadIncompletaResponse(a.getId(), a.getNombre(), faltantes(a)));
            } else {
                programadas.add(a);
            }
        }

        List<ConflictoAgendaResponse> conflictos = new ArrayList<>();
        Map<LocalDate, List<Actividad>> porDia = programadas.stream()
                .collect(Collectors.groupingBy(Actividad::getFecha, TreeMap::new, Collectors.toList()));
        for (List<Actividad> dia : porDia.values()) {
            for (Actividad a : dia) {
                for (ConflictoActividadResponse c : ActividadService.detectarConflictos(a.getId(), a.getHoraInicio(),
                        a.getHoraFin(), a.getSala(), a.getPonente(), a.isPermiteSimultaneidad(), dia)) {
                    // Cada pareja una sola vez (las reglas de cruce son simétricas).
                    if (a.getId() < c.actividadId()) {
                        conflictos.add(new ConflictoAgendaResponse(a.getId(), a.getNombre(), c));
                    }
                }
            }
        }

        // Salas reutilizables: cruces con actividades de otros eventos en la misma sala.
        List<Long> salaIds = programadas.stream().map(a -> a.getSala().getId()).distinct().toList();
        for (Actividad otra : deOtrosEventos(evento, salaIds)) {
            for (Actividad a : porDia.getOrDefault(otra.getFecha(), List.of())) {
                if (!a.getSala().getId().equals(otra.getSala().getId())
                        || !a.getHoraInicio().isBefore(otra.getHoraFin())
                        || !otra.getHoraInicio().isBefore(a.getHoraFin())) {
                    continue;
                }
                boolean ambasSimultaneas = a.isPermiteSimultaneidad() && otra.isPermiteSimultaneidad();
                if (ambasSimultaneas && a.getSala().isPermiteSimultaneidad()) {
                    continue;
                }
                String nombreEvento = otra.getEvento() != null ? otra.getEvento().getNombre() : "otro evento";
                conflictos.add(new ConflictoAgendaResponse(a.getId(), a.getNombre(), new ConflictoActividadResponse(
                        ConflictoActividadResponse.TipoConflicto.SALA, otra.getId(), otra.getNombre(),
                        otra.getFecha(), otra.getHoraInicio(), otra.getHoraFin(), a.getSala().getNombre(),
                        "La sala «" + a.getSala().getNombre() + "» ya está ocupada por «" + otra.getNombre()
                                + "» del evento «" + nombreEvento + "».")));
            }
        }
        return new RevisionAgenda(programadas, incompletas, conflictos);
    }

    /** Actividades de otros eventos en esas salas durante las fechas del evento. */
    private List<Actividad> deOtrosEventos(Evento evento, List<Long> salaIds) {
        if (salaIds.isEmpty() || evento.getFechaInicio() == null || evento.getFechaFin() == null) {
            return List.of();
        }
        return actividadRepository.findDeOtrosEventosEnSalas(evento.getId(), salaIds, evento.getFechaInicio(),
                evento.getFechaFin());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Apoyo
    // ──────────────────────────────────────────────────────────────────────────────

    private static String motivoNoPublicable(Evento evento, RevisionAgenda revision) {
        if (evento.getEstado() == EstadoEvento.en_configuracion) {
            return "El evento todavía no está publicado; publícalo antes de publicar su agenda.";
        }
        if (evento.getEstado() == EstadoEvento.cerrado) {
            return "El evento está cerrado.";
        }
        if (revision.programadas().isEmpty() && revision.incompletas().isEmpty()) {
            return "La agenda no tiene actividades programadas.";
        }
        if (!revision.incompletas().isEmpty()) {
            return "Hay " + revision.incompletas().size() + " actividad(es) sin sala u horario asignado.";
        }
        if (!revision.conflictos().isEmpty()) {
            return "La agenda tiene " + revision.conflictos().size() + " cruce(s) entre actividades.";
        }
        return null;
    }

    private static void exigirEventoPublicable(Evento evento) {
        if (evento.getEstado() == EstadoEvento.en_configuracion) {
            throw new OperacionNoPermitidaException("El evento «" + evento.getNombre()
                    + "» todavía no está publicado. Publícalo antes de publicar su agenda.");
        }
        exigirAgendaModificable(evento);
    }

    private static void exigirAgendaModificable(Evento evento) {
        if (evento.getEstado() == EstadoEvento.cerrado) {
            throw new OperacionNoPermitidaException("El evento «" + evento.getNombre()
                    + "» está cerrado: ya no se puede modificar su agenda.");
        }
    }

    private static List<DiaAgendaEstadoResponse> dias(List<Actividad> programadas, List<AgendaPublicada> filas) {
        Map<LocalDate, Long> conteo = programadas.stream()
                .collect(Collectors.groupingBy(Actividad::getFecha, TreeMap::new, Collectors.counting()));
        Map<LocalDate, AgendaPublicada> porDia = filas.stream()
                .collect(Collectors.toMap(AgendaPublicada::getFechaDia, Function.identity()));
        Set<LocalDate> todos = new TreeSet<>(conteo.keySet());
        filas.stream().filter(f -> f.getEstado() == EstadoAgenda.publicada).forEach(f -> todos.add(f.getFechaDia()));
        return todos.stream().map(d -> {
            AgendaPublicada fila = porDia.get(d);
            return new DiaAgendaEstadoResponse(d, conteo.getOrDefault(d, 0L).intValue(),
                    fila != null ? fila.getEstado() : EstadoAgenda.no_publicada,
                    fila != null ? fila.getFechaPublicacion() : null);
        }).toList();
    }

    private static LocalDateTime ultimaPublicacion(List<AgendaPublicada> filas) {
        return filas.stream().filter(f -> f.getEstado() == EstadoAgenda.publicada)
                .map(AgendaPublicada::getFechaPublicacion).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
    }

    private static List<String> faltantes(Actividad a) {
        List<String> faltan = new ArrayList<>();
        if (a.getFecha() == null) {
            faltan.add("fecha");
        }
        if (a.getHoraInicio() == null || a.getHoraFin() == null) {
            faltan.add("horario");
        }
        if (a.getSala() == null) {
            faltan.add("sala");
        }
        return faltan;
    }

    private static ActividadAgendaPublicaResponse publica(Actividad a) {
        Sala sala = a.getSala();
        boolean fisica = sala != null && sala.getTipo() == TipoSala.fisica;
        return new ActividadAgendaPublicaResponse(a.getId(), a.getNombre(),
                a.getTipoActividad() != null ? a.getTipoActividad().getNombre() : null,
                a.getHoraInicio(), a.getHoraFin(),
                sala != null ? sala.getNombre() : null,
                fisica ? sala.getUbicacion() : null,
                ActividadResponse.nombreCompleto(a.getPonente()),
                a.getModalidad(),
                a.getLineaTematica() != null ? a.getLineaTematica().getNombre() : null,
                a.isPermiteSimultaneidad());
    }

    static RequisitoSala requisito(ModalidadEvento modalidad) {
        if (modalidad == null) {
            return RequisitoSala.CUALQUIERA;
        }
        return modalidad == ModalidadEvento.virtual ? RequisitoSala.VIRTUAL : RequisitoSala.FISICA;
    }

    private static int duracionDe(Actividad a) {
        if (a.getDuracionMinutos() != null) {
            return a.getDuracionMinutos();
        }
        return ActividadService.duracion(a.getHoraInicio(), a.getHoraFin());
    }

    private static Franja franjaSala(DisponibilidadSala d) {
        return new Franja(d.getFecha(), d.getHoraInicio(), d.getHoraFin());
    }

    private static Franja franjaPonente(DisponibilidadPonente d) {
        return new Franja(d.getFecha(), d.getHoraInicio(), d.getHoraFin());
    }

    private Evento buscarEvento(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId));
    }
}
