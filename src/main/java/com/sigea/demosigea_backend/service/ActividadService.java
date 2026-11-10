package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.actividad.ActividadRequest;
import com.sigea.demosigea_backend.dto.actividad.ActividadResponse;
import com.sigea.demosigea_backend.dto.actividad.AdvertenciaActividadResponse;
import com.sigea.demosigea_backend.dto.actividad.AdvertenciaActividadResponse.TipoAdvertencia;
import com.sigea.demosigea_backend.dto.actividad.ConflictoActividadResponse;
import com.sigea.demosigea_backend.dto.actividad.ConflictoActividadResponse.TipoConflicto;
import com.sigea.demosigea_backend.dto.actividad.ValidacionActividadResponse;
import com.sigea.demosigea_backend.dto.agenda.ActividadPorProgramarRequest;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.ProgramacionActividadException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.LineaTematica;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.Sala;
import com.sigea.demosigea_backend.model.TipoActividad;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadPonenteRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadSalaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.LineaTematicaRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.SalaRepository;
import com.sigea.demosigea_backend.repository.TipoActividadRepository;
import com.sigea.demosigea_backend.repository.VersionPropuestaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Programación de actividades de la agenda del evento (HU-21).
 * <ul>
 *   <li><b>Criterio 1</b>: agrega la actividad y la agenda se devuelve en orden cronológico.</li>
 *   <li><b>Criterio 2</b>: detecta cruces de horario, sala o ponente y los notifica antes de guardar
 *       ({@link #validar} sin guardar; {@link #crear} rechaza con 409 si los hay).</li>
 *   <li><b>Criterio 3</b>: dos actividades marcadas como simultáneas pueden traslaparse sin conflicto de horario,
 *       pero no compartir sala salvo que la sala admita simultaneidad. El ponente nunca puede estar en dos a la vez.</li>
 *   <li><b>Criterio 4</b>: fecha, horas y sala son obligatorias (Bean Validation en {@link ActividadRequest}).</li>
 *   <li><b>Criterio 5</b>: al modificar una actividad se revalidan los cruces con el resto de la agenda.</li>
 * </ul>
 * Además (HU-20), si el horario queda fuera de la disponibilidad registrada de la sala o del ponente, se advierte y
 * se exige confirmación ({@code confirmarAdvertencias}).
 * <p>
 * HU-22: registra actividades sin horario ({@code por_programar}) para la generación automática
 * ({@link #registrarPorProgramar}), las devuelve a ese estado ({@link #desprogramar}) y, si la agenda ya está
 * publicada, avisa a los inscritos de los cambios ({@link AvisoCambiosAgenda}, Criterio 5). Programar a mano una
 * actividad por programar (PUT de HU-21) la deja {@code programada}; es el ajuste manual del Criterio 1.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActividadService {

    private static final String ENTIDAD = "actividades";
    static final String ESTADO_CANCELADA = Actividad.ESTADO_CANCELADA;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ActividadRepository actividadRepository;
    private final EventoRepository eventoRepository;
    private final SalaRepository salaRepository;
    private final TipoActividadRepository tipoActividadRepository;
    private final LineaTematicaRepository lineaTematicaRepository;
    private final PersonaRepository personaRepository;
    private final PropuestaRepository propuestaRepository;
    private final VersionPropuestaRepository versionPropuestaRepository;
    private final ParticipacionConferencistaRepository participacionRepository;
    private final DisponibilidadSalaRepository disponibilidadSalaRepository;
    private final DisponibilidadPonenteRepository disponibilidadPonenteRepository;
    private final AuditoriaService auditoriaService;
    private final AvisoCambiosAgenda avisoCambiosAgenda;

    // ──────────────────────────────────────────────────────────────────────────────
    // Consultas
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Criterio 1: agenda del evento en orden cronológico, opcionalmente de una sola fecha.
     *
     * @param eventoId ID del evento.
     * @param fecha    fecha a consultar (opcional).
     * @return actividades ordenadas por fecha y hora de inicio.
     */
    @Transactional(readOnly = true)
    public List<ActividadResponse> listar(Long eventoId, LocalDate fecha) {
        buscarEvento(eventoId);
        List<Actividad> agenda = fecha == null
                ? actividadRepository.findAgendaDelEvento(eventoId)
                : actividadRepository.findAgendaDelEventoEnFecha(eventoId, fecha);
        return agenda.stream().map(ActividadResponse::fromEntity).toList();
    }

    /**
     * Detalle de una actividad del evento.
     *
     * @param eventoId    ID del evento.
     * @param actividadId ID de la actividad.
     * @return la actividad.
     */
    @Transactional(readOnly = true)
    public ActividadResponse obtener(Long eventoId, Long actividadId) {
        return ActividadResponse.fromEntity(buscarActividad(eventoId, actividadId));
    }

    /**
     * Criterio 2: revisa una actividad contra la agenda sin guardarla, para notificar antes de confirmar.
     *
     * @param eventoId    ID del evento.
     * @param actividadId ID de la actividad si es una edición (se excluye de la revisión); null si es nueva.
     * @param request     datos de la actividad.
     * @return conflictos y advertencias encontrados.
     */
    @Transactional(readOnly = true)
    public ValidacionActividadResponse validar(Long eventoId, Long actividadId, ActividadRequest request) {
        Evento evento = buscarEvento(eventoId);
        if (actividadId != null) {
            buscarActividad(eventoId, actividadId);
        }
        Revision revision = revisar(evento, actividadId, request);
        return ValidacionActividadResponse.de(revision.conflictos(), revision.advertencias());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Programación
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Criterios 1 a 4: programa una actividad si no tiene cruces y, si hay advertencias, solo si se confirmaron.
     *
     * @param eventoId ID del evento.
     * @param request  datos de la actividad.
     * @return la actividad programada.
     * @throws ProgramacionActividadException 409 con los conflictos o las advertencias sin confirmar.
     */
    @Transactional
    public ActividadResponse crear(Long eventoId, ActividadRequest request) {
        Evento evento = eventoParaProgramar(eventoId);
        Revision revision = revisar(evento, null, request);
        exigirSinConflictos(revision, request);

        Actividad actividad = Actividad.builder().evento(evento).build();
        aplicar(actividad, request, revision);
        Actividad guardada = actividadRepository.save(actividad);
        avisoCambiosAgenda.actividadModificada(guardada, AvisoCambiosAgenda.Programacion.NINGUNA);

        log.info("Actividad {} programada en el evento {} ({} {}-{}, sala {})", guardada.getId(), eventoId,
                guardada.getFecha(), guardada.getHoraInicio(), guardada.getHoraFin(), revision.sala().getId());
        auditoriaService.registrar(TipoOperacionAuditoria.ACTIVIDAD_PROGRAMADA, ENTIDAD, guardada.getId(),
                DetalleAuditoria.de("despues", instantanea(guardada),
                        "advertenciasConfirmadas", revision.advertencias().size()));
        return ActividadResponse.fromEntity(guardada, revision.advertencias());
    }

    /**
     * Criterio 5: modifica una actividad revalidando los cruces con el resto de la agenda.
     *
     * @param eventoId    ID del evento.
     * @param actividadId ID de la actividad.
     * @param request     nuevos datos.
     * @return la actividad actualizada.
     * @throws ProgramacionActividadException 409 si el cambio genera cruces o advertencias sin confirmar.
     */
    @Transactional
    public ActividadResponse actualizar(Long eventoId, Long actividadId, ActividadRequest request) {
        Evento evento = eventoParaProgramar(eventoId);
        Actividad actividad = buscarActividad(eventoId, actividadId);
        Map<String, Object> antes = instantanea(actividad);
        AvisoCambiosAgenda.Programacion programacionAntes = AvisoCambiosAgenda.Programacion.de(actividad);

        Revision revision = revisar(evento, actividadId, request);
        exigirSinConflictos(revision, request);

        aplicar(actividad, request, revision);
        Actividad guardada = actividadRepository.save(actividad);
        avisoCambiosAgenda.actividadModificada(guardada, programacionAntes);

        log.info("Actividad {} reprogramada en el evento {}", actividadId, eventoId);
        auditoriaService.registrar(TipoOperacionAuditoria.ACTIVIDAD_REPROGRAMADA, ENTIDAD, actividadId,
                DetalleAuditoria.de("antes", antes, "despues", instantanea(guardada),
                        "advertenciasConfirmadas", revision.advertencias().size()));
        return ActividadResponse.fromEntity(guardada, revision.advertencias());
    }

    /**
     * Elimina una actividad de la agenda.
     *
     * @param eventoId    ID del evento.
     * @param actividadId ID de la actividad.
     * @throws OperacionNoPermitidaException si la actividad ya tiene registros asociados (p. ej. asistencias).
     */
    @Transactional
    public void eliminar(Long eventoId, Long actividadId) {
        eventoParaProgramar(eventoId);
        Actividad actividad = buscarActividad(eventoId, actividadId);
        Map<String, Object> antes = instantanea(actividad);
        // Criterio 5 (HU-22): avisar antes de borrar; las inscripciones se borran en cascada con la actividad.
        avisoCambiosAgenda.actividadRetirada(actividad, AvisoCambiosAgenda.Programacion.de(actividad));
        try {
            actividadRepository.delete(actividad);
            actividadRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar la actividad «" + actividad.getNombre()
                            + "» porque ya tiene registros asociados (asistencias o encuestas).");
        }
        log.info("Actividad {} eliminada del evento {}", actividadId, eventoId);
        auditoriaService.registrar(TipoOperacionAuditoria.ACTIVIDAD_ELIMINADA, ENTIDAD, actividadId,
                DetalleAuditoria.de("antes", antes));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // HU-22: actividades por programar
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * HU-22, Criterio 1: registra una actividad sin fecha, horario ni sala, para que la generación automática de la
     * agenda la ubique.
     *
     * @param eventoId ID del evento.
     * @param request  datos de la actividad y su duración.
     * @return la actividad en estado {@code por_programar}.
     */
    @Transactional
    public ActividadResponse registrarPorProgramar(Long eventoId, ActividadPorProgramarRequest request) {
        Evento evento = eventoParaProgramar(eventoId);
        Actividad actividad = Actividad.builder().evento(evento).estado(Actividad.ESTADO_POR_PROGRAMAR).build();
        aplicarPorProgramar(actividad, eventoId, request);
        Actividad guardada = actividadRepository.save(actividad);

        log.info("Actividad {} registrada por programar en el evento {} ({} min)", guardada.getId(), eventoId,
                guardada.getDuracionMinutos());
        auditoriaService.registrar(TipoOperacionAuditoria.ACTIVIDAD_REGISTRADA_POR_PROGRAMAR, ENTIDAD,
                guardada.getId(), DetalleAuditoria.de("despues", instantanea(guardada)));
        return ActividadResponse.fromEntity(guardada);
    }

    /**
     * HU-22: modifica los datos de una actividad que sigue por programar (nombre, duración, ponente, etc.).
     *
     * @param eventoId    ID del evento.
     * @param actividadId ID de la actividad.
     * @param request     nuevos datos.
     * @return la actividad actualizada.
     * @throws OperacionNoPermitidaException si la actividad ya tiene horario (se modifica con el PUT de HU-21).
     */
    @Transactional
    public ActividadResponse actualizarPorProgramar(Long eventoId, Long actividadId,
                                                    ActividadPorProgramarRequest request) {
        eventoParaProgramar(eventoId);
        Actividad actividad = buscarActividad(eventoId, actividadId);
        if (!actividad.estaPorProgramar()) {
            throw new OperacionNoPermitidaException("La actividad «" + actividad.getNombre()
                    + "» ya tiene horario y sala. Modifícala desde la agenda o devuélvela a por programar.");
        }
        Map<String, Object> antes = instantanea(actividad);
        aplicarPorProgramar(actividad, eventoId, request);
        Actividad guardada = actividadRepository.save(actividad);

        auditoriaService.registrar(TipoOperacionAuditoria.ACTIVIDAD_REGISTRADA_POR_PROGRAMAR, ENTIDAD, actividadId,
                DetalleAuditoria.de("antes", antes, "despues", instantanea(guardada)));
        return ActividadResponse.fromEntity(guardada);
    }

    /**
     * HU-22, Criterio 1 (ajuste manual): quita la fecha, el horario y la sala de una actividad, que vuelve a por
     * programar conservando su duración. Si la agenda ya estaba publicada se avisa a sus inscritos (Criterio 5).
     *
     * @param eventoId    ID del evento.
     * @param actividadId ID de la actividad.
     * @return la actividad por programar.
     */
    @Transactional
    public ActividadResponse desprogramar(Long eventoId, Long actividadId) {
        eventoParaProgramar(eventoId);
        Actividad actividad = buscarActividad(eventoId, actividadId);
        if (actividad.estaPorProgramar()) {
            return ActividadResponse.fromEntity(actividad);
        }
        if (actividad.estaCancelada()) {
            throw new OperacionNoPermitidaException("La actividad «" + actividad.getNombre() + "» está cancelada.");
        }
        Map<String, Object> antes = instantanea(actividad);
        AvisoCambiosAgenda.Programacion programacionAntes = AvisoCambiosAgenda.Programacion.de(actividad);
        if (actividad.getDuracionMinutos() == null) {
            actividad.setDuracionMinutos(duracion(actividad.getHoraInicio(), actividad.getHoraFin()));
        }
        quitarHorario(actividad);
        Actividad guardada = actividadRepository.save(actividad);
        avisoCambiosAgenda.actividadRetirada(guardada, programacionAntes);

        log.info("Actividad {} del evento {} devuelta a por programar", actividadId, eventoId);
        auditoriaService.registrar(TipoOperacionAuditoria.ACTIVIDAD_DESPROGRAMADA, ENTIDAD, actividadId,
                DetalleAuditoria.de("antes", antes, "despues", instantanea(guardada)));
        return ActividadResponse.fromEntity(guardada);
    }

    /** Deja la actividad sin fecha, horario ni sala, en estado por programar. */
    static void quitarHorario(Actividad a) {
        a.setFecha(null);
        a.setHoraInicio(null);
        a.setHoraFin(null);
        a.setSala(null);
        a.setEstado(Actividad.ESTADO_POR_PROGRAMAR);
        a.setAsignacionAutomatica(false);
    }

    private void aplicarPorProgramar(Actividad a, Long eventoId, ActividadPorProgramarRequest r) {
        TipoActividad tipo = tipoActividadRepository.findByIdAndEvento_Id(r.tipoActividadId(), eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El tipo de actividad con ID " + r.tipoActividadId() + " no existe en este evento."));
        LineaTematica linea = r.lineaTematicaId() == null ? null
                : lineaTematicaRepository.findByIdAndEvento_Id(r.lineaTematicaId(), eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La línea temática con ID " + r.lineaTematicaId() + " no existe en este evento."));
        a.setNombre(r.nombre().strip());
        a.setTipoActividad(tipo);
        a.setLineaTematica(linea);
        a.setPonente(resolverPonente(eventoId, r.ponenteId()));
        a.setPropuesta(resolverPropuesta(eventoId, r.propuestaId(),
                a.getPropuesta() != null ? a.getPropuesta().getId() : null));
        a.setModalidad(r.modalidad());
        a.setPermiteSimultaneidad(r.esSimultanea());
        a.setDuracionMinutos(r.duracionMinutos());
    }

    /** Minutos entre dos horas del mismo día. */
    static int duracion(LocalTime inicio, LocalTime fin) {
        return (int) Duration.between(inicio, fin).toMinutes();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Revisión de la actividad contra la agenda
    // ──────────────────────────────────────────────────────────────────────────────

    /** Referencias resueltas de la actividad y lo que se encontró al revisarla. */
    private record Revision(Sala sala, TipoActividad tipo, Persona ponente, Propuesta propuesta,
                            LineaTematica linea, List<ConflictoActividadResponse> conflictos,
                            List<AdvertenciaActividadResponse> advertencias) {
    }

    private Revision revisar(Evento evento, Long actividadId, ActividadRequest r) {
        Long eventoId = evento.getId();

        if (!r.horaFin().isAfter(r.horaInicio())) {
            throw new SolicitudInvalidaException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (evento.getFechaInicio() != null && evento.getFechaFin() != null
                && (r.fecha().isBefore(evento.getFechaInicio()) || r.fecha().isAfter(evento.getFechaFin()))) {
            throw new SolicitudInvalidaException(String.format(
                    "La fecha %s está fuera del evento, que va del %s al %s.",
                    r.fecha(), evento.getFechaInicio(), evento.getFechaFin()));
        }

        Sala sala = salaRepository.findByIdAndEvento_Id(r.salaId(), eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La sala con ID " + r.salaId() + " no existe en este evento."));
        TipoActividad tipo = tipoActividadRepository.findByIdAndEvento_Id(r.tipoActividadId(), eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El tipo de actividad con ID " + r.tipoActividadId() + " no existe en este evento."));
        LineaTematica linea = r.lineaTematicaId() == null ? null
                : lineaTematicaRepository.findByIdAndEvento_Id(r.lineaTematicaId(), eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La línea temática con ID " + r.lineaTematicaId() + " no existe en este evento."));
        Persona ponente = resolverPonente(eventoId, r.ponenteId());
        Propuesta propuesta = resolverPropuesta(eventoId, r.propuestaId(),
                r.propuestaId() != null ? propuestaActual(eventoId, actividadId) : null);

        List<Actividad> mismoDia = actividadRepository.findAgendaDelEventoEnFecha(eventoId, r.fecha());
        List<ConflictoActividadResponse> conflictos = detectarConflictos(
                actividadId, r.horaInicio(), r.horaFin(), sala, ponente, r.esSimultanea(), mismoDia);

        // Control de cruces de la sala en TODOS los eventos (salas reutilizables)
        List<Actividad> actividadesSalaMismoDia = actividadRepository.findActividadesPorSalaYFecha(sala.getId(), r.fecha());
        if (actividadesSalaMismoDia != null) {
            for (Actividad otraSala : actividadesSalaMismoDia) {
                if (Objects.equals(otraSala.getId(), actividadId) || ESTADO_CANCELADA.equalsIgnoreCase(otraSala.getEstado())) {
                    continue;
                }
                if (mismoDia.stream().anyMatch(a -> Objects.equals(a.getId(), otraSala.getId()))) {
                    continue;
                }
                boolean seTraslapan = r.horaInicio().isBefore(otraSala.getHoraFin()) && otraSala.getHoraInicio().isBefore(r.horaFin());
                if (!seTraslapan) continue;

                boolean ambasSimultaneas = r.esSimultanea() && otraSala.isPermiteSimultaneidad();
                String horario = otraSala.getHoraInicio().format(HORA) + "–" + otraSala.getHoraFin().format(HORA);
                String evNombre = otraSala.getEvento() != null ? otraSala.getEvento().getNombre() : "otro evento";
                String nombreOtra = "«" + otraSala.getNombre() + "» (" + horario + ") del evento «" + evNombre + "»";

                if (!ambasSimultaneas) {
                    conflictos.add(conflicto(
                            TipoConflicto.SALA,
                            otraSala,
                            "La sala «" + sala.getNombre() + "» ya está ocupada por " + nombreOtra + "."
                    ));
                } else if (!sala.isPermiteSimultaneidad()) {
                    conflictos.add(conflicto(
                            TipoConflicto.SALA,
                            otraSala,
                            "La sala «" + sala.getNombre() + "» no está configurada para actividades simultáneas y ya tiene " + nombreOtra + "."
                    ));
                }
            }
        }

        List<AdvertenciaActividadResponse> advertencias = new ArrayList<>();
        List<Franja> franjasSala = disponibilidadSalaRepository.findBySala_IdOrderByFechaAscHoraInicioAsc(sala.getId())
                .stream().map(d -> new Franja(d.getFecha(), d.getHoraInicio(), d.getHoraFin())).toList();
        advertirDisponibilidad(advertencias, TipoAdvertencia.FUERA_DISPONIBILIDAD_SALA,
                "la sala «" + sala.getNombre() + "»", franjasSala, r);
        if (ponente != null) {
            List<Franja> franjasPonente = disponibilidadPonenteRepository
                    .findByPersona_IdAndEvento_IdOrderByFechaAscHoraInicioAsc(ponente.getId(), eventoId)
                    .stream().map(d -> new Franja(d.getFecha(), d.getHoraInicio(), d.getHoraFin())).toList();
            advertirDisponibilidad(advertencias, TipoAdvertencia.FUERA_DISPONIBILIDAD_PONENTE,
                    "el ponente " + ActividadResponse.nombreCompleto(ponente), franjasPonente, r);
        }

        return new Revision(sala, tipo, ponente, propuesta, linea, conflictos, advertencias);
    }

    /**
     * Criterios 2, 3 y 5: cruces de la actividad con las demás del mismo día.
     * <ul>
     *   <li>PONENTE: el mismo ponente en dos actividades que se traslapan (nunca se permite).</li>
     *   <li>SALA: la misma sala en dos actividades que se traslapan, salvo que ambas sean simultáneas y la sala
     *       admita simultaneidad.</li>
     *   <li>HORARIO: actividades en salas distintas que se traslapan sin estar ambas marcadas como simultáneas.</li>
     * </ul>
     *
     * @param actividadId ID de la actividad revisada (se excluye a sí misma); null si es nueva.
     * @param inicio      hora de inicio de la actividad revisada.
     * @param fin         hora de fin de la actividad revisada.
     * @param sala        sala de la actividad revisada.
     * @param ponente     ponente de la actividad revisada (puede ser null).
     * @param simultanea  si la actividad revisada está marcada como simultánea.
     * @param mismoDia    actividades del evento en la misma fecha.
     * @return los cruces encontrados.
     */
    static List<ConflictoActividadResponse> detectarConflictos(Long actividadId, LocalTime inicio, LocalTime fin,
                                                              Sala sala, Persona ponente, boolean simultanea,
                                                              List<Actividad> mismoDia) {
        List<ConflictoActividadResponse> conflictos = new ArrayList<>();
        for (Actividad otra : mismoDia) {
            if (Objects.equals(otra.getId(), actividadId) || ESTADO_CANCELADA.equalsIgnoreCase(otra.getEstado())) {
                continue;
            }
            boolean seTraslapan = inicio.isBefore(otra.getHoraFin()) && otra.getHoraInicio().isBefore(fin);
            if (!seTraslapan) {
                continue;
            }
            boolean ambasSimultaneas = simultanea && otra.isPermiteSimultaneidad();
            String horario = otra.getHoraInicio().format(HORA) + "–" + otra.getHoraFin().format(HORA);
            String nombreOtra = "«" + otra.getNombre() + "» (" + horario + ")";

            if (ponente != null && otra.getPonente() != null && Objects.equals(ponente.getId(), otra.getPonente().getId())) {
                conflictos.add(conflicto(TipoConflicto.PONENTE, otra, "El ponente "
                        + ActividadResponse.nombreCompleto(ponente) + " ya tiene la actividad " + nombreOtra
                        + " a esa hora."));
            }

            boolean mismaSala = otra.getSala() != null && Objects.equals(otra.getSala().getId(), sala.getId());
            if (mismaSala) {
                if (!ambasSimultaneas) {
                    conflictos.add(conflicto(TipoConflicto.SALA, otra, "La sala «" + sala.getNombre()
                            + "» ya está ocupada por " + nombreOtra + "."));
                } else if (!sala.isPermiteSimultaneidad()) {
                    conflictos.add(conflicto(TipoConflicto.SALA, otra, "La sala «" + sala.getNombre()
                            + "» no está configurada para actividades simultáneas y ya tiene " + nombreOtra
                            + ". Usa otra sala para la actividad paralela."));
                }
            } else if (!ambasSimultaneas) {
                String sugerencia = simultanea
                        ? " La otra actividad no está marcada como simultánea."
                        : " Si son actividades paralelas, márcalas ambas como simultáneas.";
                conflictos.add(conflicto(TipoConflicto.HORARIO, otra,
                        "El horario se cruza con " + nombreOtra + "." + sugerencia));
            }
        }
        return conflictos;
    }

    private static ConflictoActividadResponse conflicto(TipoConflicto tipo, Actividad otra, String mensaje) {
        return new ConflictoActividadResponse(tipo, otra.getId(), otra.getNombre(), otra.getFecha(),
                otra.getHoraInicio(), otra.getHoraFin(),
                otra.getSala() != null ? otra.getSala().getNombre() : null, mensaje);
    }

    /** Franja de disponibilidad (de sala o de ponente). */
    record Franja(LocalDate fecha, LocalTime inicio, LocalTime fin) {
    }

    /** HU-20: advierte si el horario no queda dentro de alguna franja registrada (o si no hay franjas). */
    static void advertirDisponibilidad(List<AdvertenciaActividadResponse> advertencias, TipoAdvertencia tipo,
                                       String quien, List<Franja> franjas, ActividadRequest r) {
        if (franjas.isEmpty()) {
            advertencias.add(new AdvertenciaActividadResponse(tipo,
                    "No hay disponibilidad horaria registrada para " + quien + "."));
            return;
        }
        boolean cubierta = franjas.stream().anyMatch(f -> f.fecha().equals(r.fecha())
                && !r.horaInicio().isBefore(f.inicio()) && !r.horaFin().isAfter(f.fin()));
        if (!cubierta) {
            advertencias.add(new AdvertenciaActividadResponse(tipo, String.format(
                    "El horario %s %s–%s está fuera de la disponibilidad registrada para %s.",
                    r.fecha(), r.horaInicio().format(HORA), r.horaFin().format(HORA), quien)));
        }
    }

    /** Lanza 409 si hay cruces, o si hay advertencias sin confirmar. */
    private static void exigirSinConflictos(Revision revision, ActividadRequest request) {
        if (!revision.conflictos().isEmpty()) {
            throw new ProgramacionActividadException(ProgramacionActividadException.CONFLICTO_PROGRAMACION,
                    "La actividad se cruza con otras de la agenda. Cambia el horario, la sala o el ponente, "
                            + "o márcalas como simultáneas si son paralelas.",
                    revision.conflictos(), revision.advertencias());
        }
        if (!revision.advertencias().isEmpty() && !request.advertenciasConfirmadas()) {
            throw new ProgramacionActividadException(ProgramacionActividadException.CONFIRMACION_REQUERIDA,
                    "La actividad queda fuera de la disponibilidad registrada. Confirma para programarla de todos modos.",
                    List.of(), revision.advertencias());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Apoyo
    // ──────────────────────────────────────────────────────────────────────────────

    private static void aplicar(Actividad a, ActividadRequest r, Revision rev) {
        a.setNombre(r.nombre().strip());
        a.setTipoActividad(rev.tipo());
        a.setFecha(r.fecha());
        a.setHoraInicio(r.horaInicio());
        a.setHoraFin(r.horaFin());
        a.setSala(rev.sala());
        a.setPonente(rev.ponente());
        a.setPropuesta(rev.propuesta());
        a.setLineaTematica(rev.linea());
        a.setModalidad(r.modalidad());
        a.setPermiteSimultaneidad(r.esSimultanea());
        // HU-22: programarla a mano la deja programada (si estaba por programar) y deja de ser automática.
        a.setDuracionMinutos(duracion(r.horaInicio(), r.horaFin()));
        a.setAsignacionAutomatica(false);
        if (a.estaPorProgramar()) {
            a.setEstado(Actividad.ESTADO_PROGRAMADA);
        }
    }

    private Persona resolverPonente(Long eventoId, Long ponenteId) {
        if (ponenteId == null) {
            return null;
        }
        Persona ponente = personaRepository.findById(ponenteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la persona con ID " + ponenteId + "."));
        if (!participacionRepository.existsByEvento_IdAndPersona_Id(eventoId, ponenteId)) {
            throw new SolicitudInvalidaException(ActividadResponse.nombreCompleto(ponente)
                    + " no está asociado a este evento como conferencista o ponente.");
        }
        return ponente;
    }

    /** ID de la propuesta que ya tiene vinculada la actividad (al editarla), o {@code null}. */
    private Long propuestaActual(Long eventoId, Long actividadId) {
        if (actividadId == null) {
            return null;
        }
        return actividadRepository.findByIdAndEvento_Id(actividadId, eventoId)
                .map(Actividad::getPropuesta)
                .map(Propuesta::getId)
                .orElse(null);
    }

    /**
     * Resuelve la propuesta que presenta la actividad. HU-19, Criterio 3: solo se pueden vincular propuestas con
     * versión definitiva aprobada; una actividad que ya estaba vinculada a una propuesta la conserva.
     */
    private Propuesta resolverPropuesta(Long eventoId, Long propuestaId, Long propuestaActualId) {
        if (propuestaId == null) {
            return null;
        }
        Propuesta propuesta = propuestaRepository.findById(propuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la propuesta con ID " + propuestaId + "."));
        Long eventoDePropuesta = propuesta.getConvocatoria() != null && propuesta.getConvocatoria().getEvento() != null
                ? propuesta.getConvocatoria().getEvento().getId() : null;
        if (!Objects.equals(eventoDePropuesta, eventoId)) {
            throw new SolicitudInvalidaException("La propuesta con ID " + propuestaId
                    + " no pertenece a una convocatoria de este evento.");
        }
        if (!Objects.equals(propuestaId, propuestaActualId)
                && !versionPropuestaRepository.existsByPropuesta_IdAndEsDefinitivaTrue(propuestaId)) {
            throw new SolicitudInvalidaException("La propuesta «" + propuesta.getTitulo()
                    + "» aún no tiene versión definitiva aprobada; solo las propuestas aprobadas en versión definitiva "
                    + "se pueden programar en la agenda.");
        }
        return propuesta;
    }

    private Evento buscarEvento(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId));
    }

    /** Bloquea el evento durante la transacción (dos programaciones a la vez no se saltan la revisión de cruces). */
    private Evento eventoParaProgramar(Long eventoId) {
        Evento evento = eventoRepository.findByIdParaProgramar(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId));
        if (evento.getEstado() == EstadoEvento.cerrado) {
            throw new OperacionNoPermitidaException("El evento «" + evento.getNombre()
                    + "» está cerrado: ya no se puede modificar su agenda.");
        }
        return evento;
    }

    private Actividad buscarActividad(Long eventoId, Long actividadId) {
        return actividadRepository.findByIdAndEvento_Id(actividadId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la actividad con ID " + actividadId + " en este evento."));
    }

    private static Map<String, Object> instantanea(Actividad a) {
        return DetalleAuditoria.de(
                "nombre", a.getNombre(),
                "fecha", String.valueOf(a.getFecha()),
                "horaInicio", String.valueOf(a.getHoraInicio()),
                "horaFin", String.valueOf(a.getHoraFin()),
                "salaId", a.getSala() != null ? a.getSala().getId() : null,
                "ponenteId", a.getPonente() != null ? a.getPonente().getId() : null,
                "simultanea", a.isPermiteSimultaneidad(),
                "estado", a.getEstado(),
                "duracionMinutos", a.getDuracionMinutos(),
                "asignacionAutomatica", a.isAsignacionAutomatica());
    }
}
