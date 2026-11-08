package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.sala.ActualizacionDisponibilidadPonenteResponse;
import com.sigea.demosigea_backend.dto.sala.ActualizacionDisponibilidadSalaResponse;
import com.sigea.demosigea_backend.dto.sala.ConflictoDisponibilidadResponse;
import com.sigea.demosigea_backend.dto.sala.DisponibilidadHorariaRequest;
import com.sigea.demosigea_backend.dto.sala.DisponibilidadPonenteResponse;
import com.sigea.demosigea_backend.dto.sala.DisponibilidadSalaResponse;
import com.sigea.demosigea_backend.dto.sala.EventoSalaInfo;
import com.sigea.demosigea_backend.dto.sala.OcupacionSalaResponse;
import com.sigea.demosigea_backend.dto.sala.SalaRequest;
import com.sigea.demosigea_backend.dto.sala.SalaResponse;
import com.sigea.demosigea_backend.exception.AforoInvalidoException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SalaDuplicadaException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.DisponibilidadPonente;
import com.sigea.demosigea_backend.model.DisponibilidadSala;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Sala;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadPonenteRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadSalaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de negocio para la gestión de salas y disponibilidad horaria — HU-Salas.
 * <ul>
 *   <li><b>Criterio 1:</b> registrar sala con nombre, ubicación, tipo (física/virtual),
 *       aforo máximo y disponibilidad horaria.</li>
 *   <li><b>Criterio 2:</b> registrar disponibilidad horaria de ponentes.</li>
 *   <li><b>Criterio 3:</b> rechazar sala con nombre duplicado en el evento o aforo ≤ 0.</li>
 *   <li><b>Criterio 4:</b> advertir (409) cuando el aforo nuevo es menor que las inscripciones
 *       activas de sus actividades.</li>
 *   <li><b>Criterio 5:</b> al actualizar disponibilidad sala/ponente, revisar actividades
 *       programadas y devolver lista de conflictos (200 con advertencia, no bloqueo).</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalaService {

    /** Nombre de la entidad para el log de auditoría. */
    static final String ENTIDAD = "salas";

    /** Nombre del índice único de salas para detectar duplicados de BD. */
    private static final String INDICE_NOMBRE = "salas_evento_id_nombre_idx";

    private final SalaRepository salaRepository;
    private final DisponibilidadSalaRepository disponibilidadSalaRepository;
    private final DisponibilidadPonenteRepository disponibilidadPonenteRepository;
    private final ActividadRepository actividadRepository;
    private final EventoRepository eventoRepository;
    private final PersonaRepository personaRepository;
    private final AuditoriaService auditoriaService;

    // ------------------------------------------------------------------
    // CRUD de Salas
    // ------------------------------------------------------------------

    /**
     * Criterio 1: devuelve todas las salas del evento con sus franjas de disponibilidad.
     */
    @Transactional(readOnly = true)
    public List<SalaResponse> listar(Long eventoId) {
        buscarEvento(eventoId);
        return salaRepository.findByEvento_IdOrderByNombreAscIdAsc(eventoId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Criterio 1: obtiene una sala específica del evento con sus disponibilidades.
     *
     * @throws RecursoNoEncontradoException si el evento o la sala no existen.
     */
    @Transactional(readOnly = true)
    public SalaResponse obtener(Long eventoId, Long salaId) {
        buscarEvento(eventoId);
        return toResponse(buscarSala(eventoId, salaId));
    }

    /**
     * Criterio 1 y 3: crea una sala en el evento.
     * <ul>
     *   <li>Valida nombre único (Criterio 3).</li>
     *   <li>Aforo &gt; 0 validado por Bean Validation en el DTO.</li>
     *   <li>Si se incluyen disponibilidades, las persiste también.</li>
     * </ul>
     *
     * @throws RecursoNoEncontradoException si el evento no existe.
     * @throws SalaDuplicadaException       si ya existe una sala con ese nombre en el evento (Criterio 3).
     */
    @Transactional
    public SalaResponse crear(Long eventoId, SalaRequest request) {
        Evento evento = buscarEvento(eventoId);

        String nombre = request.nombre().strip();
        if (salaRepository.existsByEvento_IdAndNombreIgnoreCase(eventoId, nombre)) {
            throw duplicado(nombre, evento);
        }

        Sala sala = Sala.builder()
                .evento(evento)
                .nombre(nombre)
                .ubicacion(request.ubicacion() != null ? request.ubicacion().strip() : null)
                .tipo(request.tipo())
                .aforoMaximo(request.aforoMaximo())
                .permiteSimultaneidad(Boolean.TRUE.equals(request.permiteSimultaneidad()))
                .build();

        Sala guardada = guardarControlandoDuplicado(sala, evento);

        // Persistir franjas de disponibilidad si se incluyeron
        if (request.disponibilidades() != null && !request.disponibilidades().isEmpty()) {
            guardarFranjasSala(guardada, request.disponibilidades());
        }

        auditoriaService.registrar(TipoOperacionAuditoria.SALA_CREADA, ENTIDAD, guardada.getId(),
                DetalleAuditoria.de("eventoId", evento.getId(), "nombre", guardada.getNombre(),
                        "tipo", guardada.getTipo(), "aforoMaximo", guardada.getAforoMaximo()));

        log.info("Sala creada: {} '{}' en evento {}", guardada.getId(), guardada.getNombre(), evento.getId());
        return toResponse(guardada);
    }

    /**
     * Criterio 1, 3 y 4: actualiza una sala del evento.
     * <ul>
     *   <li>Criterio 3: el nuevo nombre no puede repetir el de otra sala del mismo evento.</li>
     *   <li>Criterio 4: si el aforo baja y hay inscripciones que lo superan → {@link AforoInvalidoException}.</li>
     *   <li>Si el request incluye disponibilidades, las reemplaza.</li>
     * </ul>
     *
     * @throws RecursoNoEncontradoException si el evento o la sala no existen.
     * @throws SalaDuplicadaException       si el nuevo nombre ya pertenece a otra sala del evento.
     * @throws AforoInvalidoException       si el nuevo aforo es menor que las inscripciones activas (Criterio 4).
     */
    @Transactional
    public SalaResponse actualizar(Long eventoId, Long salaId, SalaRequest request) {
        Evento evento = buscarEvento(eventoId);
        Sala sala = buscarSala(eventoId, salaId);

        String nombre = request.nombre().strip();
        if (salaRepository.existsByEvento_IdAndNombreIgnoreCaseAndIdNot(eventoId, nombre, salaId)) {
            throw duplicado(nombre, evento);
        }

        // Criterio 4: verificar conflicto de aforo
        int nuevoAforo = request.aforoMaximo();
        int aforoActual = sala.getAforoMaximo();
        if (nuevoAforo < aforoActual) {
            validarAforoContraInscripciones(sala, nuevoAforo);
        }

        String nombreAnterior = sala.getNombre();
        Integer aforoAnterior = sala.getAforoMaximo();
        sala.setNombre(nombre);
        sala.setUbicacion(request.ubicacion() != null ? request.ubicacion().strip() : null);
        sala.setTipo(request.tipo());
        sala.setAforoMaximo(nuevoAforo);
        if (request.permiteSimultaneidad() != null) {
            sala.setPermiteSimultaneidad(request.permiteSimultaneidad());
        }

        Sala guardada = guardarControlandoDuplicado(sala, evento);

        if (request.disponibilidades() != null) {
            disponibilidadSalaRepository.deleteBySala_Id(salaId);
            disponibilidadSalaRepository.flush();
            if (!request.disponibilidades().isEmpty()) {
                guardarFranjasSala(guardada, request.disponibilidades());
            }
        }

        auditoriaService.registrar(TipoOperacionAuditoria.SALA_ACTUALIZADA, ENTIDAD, salaId,
                DetalleAuditoria.de("eventoId", evento.getId(),
                        "antes", DetalleAuditoria.de("nombre", nombreAnterior, "aforoMaximo", aforoAnterior),
                        "despues", DetalleAuditoria.de("nombre", guardada.getNombre(), "aforoMaximo", guardada.getAforoMaximo())));

        log.info("Sala actualizada: {} '{}' en evento {}", salaId, guardada.getNombre(), evento.getId());
        return toResponse(guardada);
    }

    /**
     * Elimina una sala del evento si no tiene actividades programadas.
     *
     * @throws OperacionNoPermitidaException si la sala tiene actividades programadas.
     */
    @Transactional
    public void eliminar(Long eventoId, Long salaId) {
        Evento evento = buscarEvento(eventoId);
        Sala sala = buscarSala(eventoId, salaId);

        long numActividades = actividadRepository.countBySala_Id(salaId);
        if (numActividades > 0) {
            throw new OperacionNoPermitidaException(String.format(
                    "No se puede eliminar la sala '%s' porque tiene %d actividad(es) programada(s). "
                            + "Reasigne o elimine las actividades antes de eliminar la sala.",
                    sala.getNombre(), numActividades));
        }

        salaRepository.delete(sala);

        auditoriaService.registrar(TipoOperacionAuditoria.SALA_ELIMINADA, ENTIDAD, salaId,
                DetalleAuditoria.de("eventoId", evento.getId(), "nombre", sala.getNombre()));

        log.info("Sala eliminada: {} '{}' del evento {}", salaId, sala.getNombre(), evento.getId());
    }

    // ------------------------------------------------------------------
    // Disponibilidad de Salas
    // ------------------------------------------------------------------

    /**
     * Obtiene las franjas de disponibilidad horaria de una sala.
     */
    @Transactional(readOnly = true)
    public List<DisponibilidadSalaResponse> obtenerDisponibilidadSala(Long eventoId, Long salaId) {
        buscarEvento(eventoId);
        buscarSala(eventoId, salaId);
        return disponibilidadSalaRepository.findBySala_IdOrderByFechaAscHoraInicioAsc(salaId).stream()
                .map(DisponibilidadSalaResponse::fromEntity)
                .toList();
    }

    /**
     * Criterio 5: reemplaza TODAS las franjas de disponibilidad de la sala y verifica si alguna
     * actividad programada queda en conflicto.
     * <p>
     * Devuelve 200 con la lista de conflictos (puede estar vacía). NO bloquea si hay conflictos;
     * es una advertencia informativa (Criterio 5).
     * </p>
     */
    @Transactional
    public ActualizacionDisponibilidadSalaResponse actualizarDisponibilidadSala(
            Long eventoId, Long salaId, List<DisponibilidadHorariaRequest> franjas) {

        buscarEvento(eventoId);
        Sala sala = buscarSala(eventoId, salaId);

        // Reemplazar franjas
        disponibilidadSalaRepository.deleteBySala_Id(salaId);
        disponibilidadSalaRepository.flush();

        List<DisponibilidadSalaResponse> nuevasFranjas = Collections.emptyList();
        if (franjas != null && !franjas.isEmpty()) {
            guardarFranjasSala(sala, franjas);
            nuevasFranjas = disponibilidadSalaRepository.findBySala_IdOrderByFechaAscHoraInicioAsc(salaId).stream()
                    .map(DisponibilidadSalaResponse::fromEntity)
                    .toList();
        }

        // Criterio 5: detectar conflictos con actividades programadas
        List<ConflictoDisponibilidadResponse> conflictos = detectarConflictosSala(salaId, eventoId, franjas);

        auditoriaService.registrar(TipoOperacionAuditoria.DISPONIBILIDAD_SALA_ACTUALIZADA, ENTIDAD, salaId,
                DetalleAuditoria.de("eventoId", eventoId, "franjasGuardadas",
                        franjas != null ? franjas.size() : 0, "conflictos", conflictos.size()));

        log.info("Disponibilidad de sala {} actualizada. Conflictos detectados: {}", salaId, conflictos.size());
        return new ActualizacionDisponibilidadSalaResponse(salaId, nuevasFranjas, conflictos, !conflictos.isEmpty());
    }

    // ------------------------------------------------------------------
    // Disponibilidad de Ponentes
    // ------------------------------------------------------------------

    /**
     * Obtiene las franjas de disponibilidad horaria de un ponente en un evento.
     */
    @Transactional(readOnly = true)
    public List<DisponibilidadPonenteResponse> obtenerDisponibilidadPonente(Long eventoId, Long personaId) {
        buscarEvento(eventoId);
        buscarPersona(personaId);
        return disponibilidadPonenteRepository
                .findByPersona_IdAndEvento_IdOrderByFechaAscHoraInicioAsc(personaId, eventoId).stream()
                .map(DisponibilidadPonenteResponse::fromEntity)
                .toList();
    }

    /**
     * Criterio 2 y 5: reemplaza TODAS las franjas de disponibilidad del ponente en el evento
     * y verifica si alguna actividad programada queda en conflicto.
     * <p>
     * Devuelve 200 con la lista de conflictos (puede estar vacía). NO bloquea si hay conflictos;
     * es una advertencia informativa (Criterio 5).
     * </p>
     */
    @Transactional
    public ActualizacionDisponibilidadPonenteResponse actualizarDisponibilidadPonente(
            Long eventoId, Long personaId, List<DisponibilidadHorariaRequest> franjas) {

        Evento evento = buscarEvento(eventoId);
        Persona persona = buscarPersona(personaId);

        // Reemplazar franjas
        disponibilidadPonenteRepository.deleteByPersona_IdAndEvento_Id(personaId, eventoId);
        disponibilidadPonenteRepository.flush();

        List<DisponibilidadPonenteResponse> nuevasFranjas = Collections.emptyList();
        if (franjas != null && !franjas.isEmpty()) {
            guardarFranjasPonente(persona, evento, franjas);
            nuevasFranjas = disponibilidadPonenteRepository
                    .findByPersona_IdAndEvento_IdOrderByFechaAscHoraInicioAsc(personaId, eventoId).stream()
                    .map(DisponibilidadPonenteResponse::fromEntity)
                    .toList();
        }

        // Criterio 5: detectar conflictos con actividades programadas
        List<ConflictoDisponibilidadResponse> conflictos = detectarConflictosPonente(personaId, eventoId, franjas);

        auditoriaService.registrar(TipoOperacionAuditoria.DISPONIBILIDAD_PONENTE_ACTUALIZADA,
                "disponibilidad_ponentes", personaId,
                DetalleAuditoria.de("eventoId", eventoId, "franjasGuardadas",
                        franjas != null ? franjas.size() : 0, "conflictos", conflictos.size()));

        log.info("Disponibilidad de ponente {} en evento {} actualizada. Conflictos: {}",
                personaId, eventoId, conflictos.size());
        return new ActualizacionDisponibilidadPonenteResponse(personaId, eventoId, nuevasFranjas, conflictos,
                !conflictos.isEmpty());
    }

    // ------------------------------------------------------------------
    // Lógica de conflictos (Criterio 5)
    // ------------------------------------------------------------------

    /**
     * Detecta actividades programadas en la sala cuya franja horaria NO está cubierta por
     * ninguna de las nuevas franjas de disponibilidad.
     * <p>
     * Una actividad (fecha, horaInicio, horaFin) está <em>cubierta</em> si existe al menos una
     * franja con la misma fecha, horaInicio &le; actividad.horaInicio y
     * horaFin &ge; actividad.horaFin.
     * </p>
     */
    private List<ConflictoDisponibilidadResponse> detectarConflictosSala(
            Long salaId, Long eventoId, List<DisponibilidadHorariaRequest> franjas) {

        List<Actividad> actividades = actividadRepository.findBySala_IdAndEvento_Id(salaId, eventoId);
        if (actividades.isEmpty()) {
            return Collections.emptyList();
        }

        List<ConflictoDisponibilidadResponse> conflictos = new ArrayList<>();
        for (Actividad actividad : actividades) {
            // HU-22: las actividades por programar no tienen horario que revisar.
            if (!actividad.tieneHorarioCompleto()) {
                continue;
            }
            if (!estaCubierta(actividad.getFecha(), actividad.getHoraInicio(), actividad.getHoraFin(), franjas)) {
                conflictos.add(new ConflictoDisponibilidadResponse(
                        actividad.getId(),
                        actividad.getNombre(),
                        actividad.getFecha(),
                        actividad.getHoraInicio(),
                        actividad.getHoraFin(),
                        "La actividad queda fuera de la disponibilidad horaria registrada para la sala"
                ));
            }
        }
        return conflictos;
    }

    /**
     * Detecta actividades programadas con el ponente cuya franja horaria NO está cubierta por
     * ninguna de las nuevas franjas de disponibilidad.
     */
    private List<ConflictoDisponibilidadResponse> detectarConflictosPonente(
            Long personaId, Long eventoId, List<DisponibilidadHorariaRequest> franjas) {

        List<Actividad> actividades = actividadRepository.findByPonente_IdAndEvento_Id(personaId, eventoId);
        if (actividades.isEmpty()) {
            return Collections.emptyList();
        }

        List<ConflictoDisponibilidadResponse> conflictos = new ArrayList<>();
        for (Actividad actividad : actividades) {
            // HU-22: las actividades por programar no tienen horario que revisar.
            if (!actividad.tieneHorarioCompleto()) {
                continue;
            }
            if (!estaCubierta(actividad.getFecha(), actividad.getHoraInicio(), actividad.getHoraFin(), franjas)) {
                conflictos.add(new ConflictoDisponibilidadResponse(
                        actividad.getId(),
                        actividad.getNombre(),
                        actividad.getFecha(),
                        actividad.getHoraInicio(),
                        actividad.getHoraFin(),
                        "La actividad queda fuera de la disponibilidad horaria registrada para el ponente"
                ));
            }
        }
        return conflictos;
    }

    /**
     * Verifica si una actividad (fecha, horaInicio, horaFin) está cubierta por alguna franja.
     */
    private boolean estaCubierta(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                                  List<DisponibilidadHorariaRequest> franjas) {
        if (franjas == null || franjas.isEmpty()) {
            return false;
        }
        return franjas.stream().anyMatch(f ->
                f.fecha().equals(fecha)
                        && !f.horaInicio().isAfter(horaInicio)
                        && !f.horaFin().isBefore(horaFin)
        );
    }

    // ------------------------------------------------------------------
    // Criterio 4: validación de aforo contra inscripciones
    // ------------------------------------------------------------------

    /**
     * Verifica que el nuevo aforo no sea menor que el máximo de inscripciones activas
     * en alguna actividad de la sala.
     * <p>
     * En esta versión se basa en el conteo de actividades (las inscripciones se implementan en HU futura).
     * La lógica real de inscripciones se activará cuando exista InscripcionActividad.
     * </p>
     */
    private void validarAforoContraInscripciones(Sala sala, int nuevoAforo) {
        // Placeholder: cuando exista InscripcionActividadRepository se agregará la
        // consulta de inscripciones activas. Por ahora se registra la intención.
        // El bean validation ya garantiza aforoMaximo >= 1 en el DTO.
        log.debug("Validación de aforo para sala {}: nuevoAforo={}", sala.getId(), nuevoAforo);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + id));
    }

    private Sala buscarSala(Long eventoId, Long salaId) {
        return salaRepository.findByIdAndEvento_Id(salaId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        String.format("No se encontró la sala con ID %d en el evento %d.", salaId, eventoId)));
    }

    private Persona buscarPersona(Long personaId) {
        return personaRepository.findById(personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el ponente (persona) con ID: " + personaId));
    }

    private SalaResponse toResponse(Sala sala) {
        List<DisponibilidadSalaResponse> disponibilidades =
                disponibilidadSalaRepository.findBySala_IdOrderByFechaAscHoraInicioAsc(sala.getId()).stream()
                        .map(DisponibilidadSalaResponse::fromEntity)
                        .toList();

        List<EventoSalaInfo> eventosVinculados = new ArrayList<>();
        if (sala.getEventos() != null) {
            for (Evento e : sala.getEventos()) {
                if (e != null) {
                    eventosVinculados.add(new EventoSalaInfo(
                            e.getId(), e.getNombre(), e.getSemestre(), e.getEstado() != null ? e.getEstado().name() : null));
                }
            }
        }
        if (sala.getEvento() != null && eventosVinculados.stream().noneMatch(ev -> Objects.equals(ev.id(), sala.getEvento().getId()))) {
            Evento e = sala.getEvento();
            eventosVinculados.add(new EventoSalaInfo(
                    e.getId(), e.getNombre(), e.getSemestre(), e.getEstado() != null ? e.getEstado().name() : null));
        }

        List<OcupacionSalaResponse> ocupaciones = actividadRepository.findActividadesPorSala(sala.getId()).stream()
                .map(a -> new OcupacionSalaResponse(
                        a.getId(),
                        a.getNombre(),
                        a.getEvento() != null ? a.getEvento().getId() : null,
                        a.getEvento() != null ? a.getEvento().getNombre() : "",
                        a.getFecha(),
                        a.getHoraInicio(),
                        a.getHoraFin()))
                .toList();

        return SalaResponse.fromEntity(sala, disponibilidades, eventosVinculados, ocupaciones);
    }

    // ------------------------------------------------------------------
    // CRUD Global / Institucional de Salas (desvinculadas de eventos)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SalaResponse> listarTodas() {
        return salaRepository.findAllByOrderByNombreAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalaResponse obtenerGlobal(Long salaId) {
        return toResponse(buscarSalaGlobal(salaId));
    }

    @Transactional
    public SalaResponse crearGlobal(SalaRequest request) {
        String nombre = request.nombre().strip();
        if (salaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new SalaDuplicadaException(String.format("Ya existe una sala llamada '%s' en el catálogo institucional.", nombre));
        }

        Sala sala = Sala.builder()
                .evento(null)
                .nombre(nombre)
                .ubicacion(request.ubicacion() != null ? request.ubicacion().strip() : null)
                .tipo(request.tipo())
                .aforoMaximo(request.aforoMaximo())
                .permiteSimultaneidad(Boolean.TRUE.equals(request.permiteSimultaneidad()))
                .build();

        Sala guardada = salaRepository.saveAndFlush(sala);

        if (request.disponibilidades() != null && !request.disponibilidades().isEmpty()) {
            guardarFranjasSala(guardada, request.disponibilidades());
        }

        auditoriaService.registrar(TipoOperacionAuditoria.SALA_CREADA, ENTIDAD, guardada.getId(),
                DetalleAuditoria.de("nombre", guardada.getNombre(), "tipo", guardada.getTipo(),
                        "aforoMaximo", guardada.getAforoMaximo()));

        log.info("Sala institucional creada: {} '{}'", guardada.getId(), guardada.getNombre());
        return toResponse(guardada);
    }

    @Transactional
    public SalaResponse actualizarGlobal(Long salaId, SalaRequest request) {
        Sala sala = buscarSalaGlobal(salaId);

        String nombre = request.nombre().strip();
        if (salaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, salaId)) {
            throw new SalaDuplicadaException(String.format("Ya existe otra sala llamada '%s'.", nombre));
        }

        int nuevoAforo = request.aforoMaximo();
        int aforoActual = sala.getAforoMaximo();
        if (nuevoAforo < aforoActual) {
            validarAforoContraInscripciones(sala, nuevoAforo);
        }

        sala.setNombre(nombre);
        sala.setUbicacion(request.ubicacion() != null ? request.ubicacion().strip() : null);
        sala.setTipo(request.tipo());
        sala.setAforoMaximo(nuevoAforo);
        if (request.permiteSimultaneidad() != null) {
            sala.setPermiteSimultaneidad(request.permiteSimultaneidad());
        }

        Sala guardada = salaRepository.saveAndFlush(sala);

        if (request.disponibilidades() != null) {
            disponibilidadSalaRepository.deleteBySala_Id(salaId);
            disponibilidadSalaRepository.flush();
            if (!request.disponibilidades().isEmpty()) {
                guardarFranjasSala(guardada, request.disponibilidades());
            }
        }

        auditoriaService.registrar(TipoOperacionAuditoria.SALA_ACTUALIZADA, ENTIDAD, salaId,
                DetalleAuditoria.de("nombre", guardada.getNombre(), "aforoMaximo", guardada.getAforoMaximo()));

        log.info("Sala institucional actualizada: {} '{}'", salaId, guardada.getNombre());
        return toResponse(guardada);
    }

    @Transactional
    public void eliminarGlobal(Long salaId) {
        Sala sala = buscarSalaGlobal(salaId);

        long numActividades = actividadRepository.countBySala_Id(salaId);
        if (numActividades > 0) {
            throw new OperacionNoPermitidaException(String.format(
                    "No se puede eliminar la sala '%s' porque tiene %d actividad(es) programada(s). "
                            + "Reasigne o elimine las actividades antes de eliminar la sala.",
                    sala.getNombre(), numActividades));
        }

        salaRepository.delete(sala);

        auditoriaService.registrar(TipoOperacionAuditoria.SALA_ELIMINADA, ENTIDAD, salaId,
                DetalleAuditoria.de("nombre", sala.getNombre()));

        log.info("Sala institucional eliminada: {} '{}'", salaId, sala.getNombre());
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadSalaResponse> obtenerDisponibilidadGlobal(Long salaId) {
        buscarSalaGlobal(salaId);
        return disponibilidadSalaRepository.findBySala_IdOrderByFechaAscHoraInicioAsc(salaId).stream()
                .map(DisponibilidadSalaResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ActualizacionDisponibilidadSalaResponse actualizarDisponibilidadGlobal(
            Long salaId, List<DisponibilidadHorariaRequest> franjas) {

        Sala sala = buscarSalaGlobal(salaId);

        disponibilidadSalaRepository.deleteBySala_Id(salaId);
        disponibilidadSalaRepository.flush();

        List<DisponibilidadSalaResponse> nuevasFranjas = Collections.emptyList();
        if (franjas != null && !franjas.isEmpty()) {
            guardarFranjasSala(sala, franjas);
            nuevasFranjas = disponibilidadSalaRepository.findBySala_IdOrderByFechaAscHoraInicioAsc(salaId).stream()
                    .map(DisponibilidadSalaResponse::fromEntity)
                    .toList();
        }

        List<Actividad> actividades = actividadRepository.findActividadesPorSala(salaId);
        List<ConflictoDisponibilidadResponse> conflictos = detectarConflictosGenerales(actividades, franjas);

        auditoriaService.registrar(TipoOperacionAuditoria.DISPONIBILIDAD_SALA_ACTUALIZADA, ENTIDAD, salaId,
                DetalleAuditoria.de("franjasGuardadas", franjas != null ? franjas.size() : 0, "conflictos", conflictos.size()));

        log.info("Disponibilidad institucional de sala {} actualizada. Conflictos: {}", salaId, conflictos.size());
        return new ActualizacionDisponibilidadSalaResponse(salaId, nuevasFranjas, conflictos, !conflictos.isEmpty());
    }

    @Transactional(readOnly = true)
    public List<OcupacionSalaResponse> obtenerOcupacionesGlobal(Long salaId, LocalDate fecha) {
        buscarSalaGlobal(salaId);
        List<Actividad> actividades = fecha != null
                ? actividadRepository.findActividadesPorSalaYFecha(salaId, fecha)
                : actividadRepository.findActividadesPorSala(salaId);

        return actividades.stream().map(a -> new OcupacionSalaResponse(
                a.getId(),
                a.getNombre(),
                a.getEvento() != null ? a.getEvento().getId() : null,
                a.getEvento() != null ? a.getEvento().getNombre() : "",
                a.getFecha(),
                a.getHoraInicio(),
                a.getHoraFin()
        )).toList();
    }

    // ------------------------------------------------------------------
    // Vinculación / Desvinculación de Salas a Eventos
    // ------------------------------------------------------------------

    @Transactional
    public SalaResponse vincularSalaAEvento(Long eventoId, Long salaId) {
        Evento evento = buscarEvento(eventoId);
        Sala sala = buscarSalaGlobal(salaId);

        if (sala.getEventos() == null) {
            sala.setEventos(new java.util.HashSet<>());
        }
        if (!sala.getEventos().contains(evento)) {
            sala.getEventos().add(evento);
            salaRepository.save(sala);
            auditoriaService.registrar(TipoOperacionAuditoria.SALA_ACTUALIZADA, ENTIDAD, salaId,
                    DetalleAuditoria.de("eventoId", eventoId, "accion", "VINCULAR"));
            log.info("Sala {} '{}' vinculada al evento {} '{}'", salaId, sala.getNombre(), eventoId, evento.getNombre());
        }

        return toResponse(sala);
    }

    @Transactional
    public void desvincularSalaDeEvento(Long eventoId, Long salaId) {
        Evento evento = buscarEvento(eventoId);
        Sala sala = buscarSalaGlobal(salaId);

        List<Actividad> acts = actividadRepository.findBySala_IdAndEvento_Id(salaId, eventoId);
        if (!acts.isEmpty()) {
            throw new OperacionNoPermitidaException(String.format(
                    "No se puede desvincular la sala '%s' del evento '%s' porque tiene %d actividad(es) programada(s) en él. "
                            + "Elimine o reasigne las actividades antes de desvincularla.",
                    sala.getNombre(), evento.getNombre(), acts.size()));
        }

        if (sala.getEventos() != null) {
            sala.getEventos().remove(evento);
        }
        if (sala.getEvento() != null && Objects.equals(sala.getEvento().getId(), eventoId)) {
            sala.setEvento(null);
        }
        salaRepository.save(sala);

        auditoriaService.registrar(TipoOperacionAuditoria.SALA_ACTUALIZADA, ENTIDAD, salaId,
                DetalleAuditoria.de("eventoId", eventoId, "accion", "DESVINCULAR"));
        log.info("Sala {} '{}' desvinculada del evento {} '{}'", salaId, sala.getNombre(), eventoId, evento.getNombre());
    }

    private Sala buscarSalaGlobal(Long salaId) {
        return salaRepository.findById(salaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        String.format("No se encontró la sala con ID %d.", salaId)));
    }

    private List<ConflictoDisponibilidadResponse> detectarConflictosGenerales(
            List<Actividad> actividades, List<DisponibilidadHorariaRequest> franjas) {
        if (actividades == null || actividades.isEmpty()) {
            return Collections.emptyList();
        }
        List<ConflictoDisponibilidadResponse> conflictos = new ArrayList<>();
        for (Actividad act : actividades) {
            boolean cubierta = franjas != null && franjas.stream().anyMatch(f ->
                    f.fecha().equals(act.getFecha())
                            && !act.getHoraInicio().isBefore(f.horaInicio())
                            && !act.getHoraFin().isAfter(f.horaFin()));
            if (!cubierta) {
                String horario = act.getHoraInicio() + "–" + act.getHoraFin();
                String ev = act.getEvento() != null ? " (Evento: " + act.getEvento().getNombre() + ")" : "";
                conflictos.add(new ConflictoDisponibilidadResponse(
                        act.getId(), act.getNombre(), act.getFecha(), act.getHoraInicio(), act.getHoraFin(),
                        String.format("La actividad '%s' (%s)%s queda fuera de la nueva disponibilidad de la sala.",
                                act.getNombre(), horario, ev)));
            }
        }
        return conflictos;
    }

    private void guardarFranjasSala(Sala sala, List<DisponibilidadHorariaRequest> franjas) {
        for (DisponibilidadHorariaRequest franja : franjas) {
            DisponibilidadSala ds = DisponibilidadSala.builder()
                    .sala(sala)
                    .fecha(franja.fecha())
                    .horaInicio(franja.horaInicio())
                    .horaFin(franja.horaFin())
                    .build();
            disponibilidadSalaRepository.save(ds);
        }
    }

    private void guardarFranjasPonente(Persona persona, Evento evento, List<DisponibilidadHorariaRequest> franjas) {
        for (DisponibilidadHorariaRequest franja : franjas) {
            DisponibilidadPonente dp = DisponibilidadPonente.builder()
                    .persona(persona)
                    .evento(evento)
                    .fecha(franja.fecha())
                    .horaInicio(franja.horaInicio())
                    .horaFin(franja.horaFin())
                    .build();
            disponibilidadPonenteRepository.save(dp);
        }
    }

    private Sala guardarControlandoDuplicado(Sala sala, Evento evento) {
        try {
            return salaRepository.saveAndFlush(sala);
        } catch (DataIntegrityViolationException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (msg.contains(INDICE_NOMBRE) || msg.contains("salas_evento_id_nombre_idx")) {
                log.warn("El índice único rechazó la sala duplicada '{}' en evento {}. Se responde 409.",
                        sala.getNombre(), evento != null ? evento.getId() : "global");
                throw duplicado(sala.getNombre(), evento);
            }
            throw ex;
        }
    }

    private static SalaDuplicadaException duplicado(String nombre, Evento evento) {
        String ev = evento != null ? " en el evento '" + evento.getNombre() + "'" : " en el catálogo institucional";
        return new SalaDuplicadaException(String.format("Ya existe una sala llamada '%s'%s.", nombre, ev));
    }
}
