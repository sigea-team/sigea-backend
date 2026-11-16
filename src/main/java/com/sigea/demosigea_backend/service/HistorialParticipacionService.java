package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.historial.ActividadHistorialResponse;
import com.sigea.demosigea_backend.dto.historial.EventoHistorialResponse;
import com.sigea.demosigea_backend.dto.historial.HistorialParticipacionResponse;
import com.sigea.demosigea_backend.dto.historial.PropuestaHistorialResponse;
import com.sigea.demosigea_backend.dto.historial.RolParticipacion;
import com.sigea.demosigea_backend.dto.periodo.FiltroPeriodo;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.InscripcionActividad;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.PropuestaAutor;
import com.sigea.demosigea_backend.model.RolAutor;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.InscripcionActividadRepository;
import com.sigea.demosigea_backend.repository.InscripcionEventoRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.PropuestaAutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Historial de participación de una persona en los eventos del programa (HU-27, Criterios 1 a 3).
 * <p>
 * El historial se arma a partir de las tablas existentes, sin tablas nuevas:
 * </p>
 * <ul>
 *   <li>{@code inscripciones_evento} e {@code inscripciones_actividad}: participante.</li>
 *   <li>{@code participaciones_conferencista}: conferencista o ponente invitado.</li>
 *   <li>{@code actividades.ponente_id}: ponente de una actividad de la agenda.</li>
 *   <li>{@code propuestas_autores}: autor, coautor o ponente de una propuesta (ponente solo si fue aprobada).</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Service
@RequiredArgsConstructor
public class HistorialParticipacionService {

    static final String MENSAJE_PRIMERA_PARTICIPACION = "La persona no tiene participación registrada en los eventos "
            + "del programa; esta sería su primera participación.";
    static final String MENSAJE_SIN_COINCIDENCIAS = "La persona no tiene participaciones que coincidan con los "
            + "filtros aplicados.";

    private final PersonaRepository personaRepository;
    private final InscripcionEventoRepository inscripcionEventoRepository;
    private final InscripcionActividadRepository inscripcionActividadRepository;
    private final ParticipacionConferencistaRepository participacionRepository;
    private final ActividadRepository actividadRepository;
    private final PropuestaAutorRepository propuestaAutorRepository;

    /**
     * Consulta el historial de una persona por su identificador.
     *
     * @param personaId Persona a consultar.
     * @param filtro    Filtros de periodo (opcionales).
     * @return Historial de la persona.
     * @throws RecursoNoEncontradoException si la persona no existe.
     */
    @Transactional(readOnly = true)
    public HistorialParticipacionResponse consultar(Long personaId, FiltroPeriodo filtro) {
        FiltroPeriodo f = ReglasFiltroPeriodo.validar(filtro);
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la persona con id " + personaId + "."));
        return construir(persona, f);
    }

    /**
     * Consulta el historial de una persona buscándola por número de documento o correo.
     *
     * @param numeroDocumento Número de documento (tiene prioridad si se envían ambos).
     * @param correo          Correo electrónico.
     * @param filtro          Filtros de periodo (opcionales).
     * @return Historial de la persona.
     * @throws SolicitudInvalidaException   si no se envía documento ni correo.
     * @throws RecursoNoEncontradoException si no hay persona con ese documento o correo.
     */
    @Transactional(readOnly = true)
    public HistorialParticipacionResponse consultarPorIdentificacion(String numeroDocumento, String correo,
                                                                     FiltroPeriodo filtro) {
        FiltroPeriodo f = ReglasFiltroPeriodo.validar(filtro);
        Persona persona;
        if (numeroDocumento != null && !numeroDocumento.isBlank()) {
            persona = personaRepository.findByNumeroDocumento(numeroDocumento.trim())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe una persona con el documento " + numeroDocumento.trim() + "."));
        } else if (correo != null && !correo.isBlank()) {
            persona = personaRepository.findByCorreoIgnoreCase(correo.trim())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe una persona con el correo " + correo.trim() + "."));
        } else {
            throw new SolicitudInvalidaException("Indique el número de documento o el correo de la persona.");
        }
        return construir(persona, f);
    }

    private HistorialParticipacionResponse construir(Persona persona, FiltroPeriodo f) {
        Map<Long, Acumulado> porEvento = recolectar(persona.getId());
        boolean primeraParticipacion = porEvento.isEmpty();

        List<EventoHistorialResponse> eventos = porEvento.values().stream()
                .filter(a -> ReglasFiltroPeriodo.eventoCumple(a.evento, f))
                .map(a -> a.aResponse(f))
                .filter(e -> f.tipoActividad() == null || !e.actividades().isEmpty())
                .sorted(Comparator.comparing(EventoHistorialResponse::fechaInicio,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(EventoHistorialResponse::eventoId, Comparator.reverseOrder()))
                .toList();

        Map<RolParticipacion, Long> eventosPorRol = new EnumMap<>(RolParticipacion.class);
        for (RolParticipacion rol : RolParticipacion.values()) {
            eventosPorRol.put(rol, eventos.stream().filter(e -> e.roles().contains(rol)).count());
        }

        String mensaje = null;
        if (primeraParticipacion) {
            mensaje = MENSAJE_PRIMERA_PARTICIPACION;
        } else if (eventos.isEmpty()) {
            mensaje = MENSAJE_SIN_COINCIDENCIAS;
        }

        return new HistorialParticipacionResponse(
                persona.getId(),
                (persona.getNombres() + " " + persona.getApellidos()).trim(),
                persona.getNumeroDocumento(),
                persona.getCorreo(),
                !eventos.isEmpty(),
                primeraParticipacion,
                mensaje,
                eventos.size(),
                eventosPorRol,
                eventos);
    }

    /** Reúne por evento todas las participaciones registradas de la persona. */
    private Map<Long, Acumulado> recolectar(Long personaId) {
        Map<Long, Acumulado> porEvento = new LinkedHashMap<>();

        for (InscripcionEvento inscripcion : inscripcionEventoRepository.findByPersona_IdOrderByFechaInscripcionDesc(personaId)) {
            acumulado(porEvento, inscripcion.getEvento()).roles.add(RolParticipacion.PARTICIPANTE);
        }

        for (InscripcionActividad ia : inscripcionActividadRepository.findHistorialDePersona(personaId)) {
            Actividad actividad = ia.getActividad();
            Acumulado a = acumulado(porEvento, actividad.getEvento());
            a.roles.add(RolParticipacion.PARTICIPANTE);
            a.agregarActividad(actividad, RolParticipacion.PARTICIPANTE);
        }

        for (ParticipacionConferencista p : participacionRepository.historialDePersona(personaId)) {
            Acumulado a = acumulado(porEvento, p.getEvento());
            a.roles.add(p.getTipoParticipacion() == TipoParticipacionConferencista.conferencista
                    ? RolParticipacion.CONFERENCISTA : RolParticipacion.PONENTE);
            if (p.getTema() != null && !p.getTema().isBlank()) {
                a.tema = p.getTema();
            }
        }

        for (Actividad actividad : actividadRepository.findComoPonente(personaId)) {
            Acumulado a = acumulado(porEvento, actividad.getEvento());
            // Si fue invitado como conferencista, ese es su rol en el evento; si no, es ponente.
            if (!a.roles.contains(RolParticipacion.CONFERENCISTA)) {
                a.roles.add(RolParticipacion.PONENTE);
            }
            a.agregarActividad(actividad, RolParticipacion.PONENTE);
        }

        for (PropuestaAutor autor : propuestaAutorRepository.findHistorialDePersona(personaId)) {
            Propuesta propuesta = autor.getPropuesta();
            Acumulado a = acumulado(porEvento, propuesta.getConvocatoria().getEvento());
            boolean ponenteAprobado = autor.getRol() == RolAutor.ponente
                    && propuesta.getEstado() == EstadoPropuesta.aprobada;
            a.roles.add(ponenteAprobado ? RolParticipacion.PONENTE : RolParticipacion.AUTOR);
            a.propuestas.add(new PropuestaHistorialResponse(
                    propuesta.getId(), propuesta.getTitulo(), propuesta.getEstado(), autor.getRol()));
        }
        return porEvento;
    }

    private static Acumulado acumulado(Map<Long, Acumulado> porEvento, Evento evento) {
        return porEvento.computeIfAbsent(evento.getId(), id -> new Acumulado(evento));
    }

    /** Participaciones de la persona en un mismo evento. */
    private static final class Acumulado {

        private static final Comparator<ActividadHistorialResponse> ORDEN_ACTIVIDADES = Comparator
                .comparing(ActividadHistorialResponse::fecha, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
                .thenComparing(ActividadHistorialResponse::horaInicio, Comparator.nullsLast(Comparator.<LocalTime>naturalOrder()))
                .thenComparing(ActividadHistorialResponse::actividadId);

        private final Evento evento;
        private final EnumSet<RolParticipacion> roles = EnumSet.noneOf(RolParticipacion.class);
        /** Actividad + rol, para no repetir y conservar el orden de llegada. */
        private final Map<String, Actividad> actividades = new LinkedHashMap<>();
        private final Map<String, RolParticipacion> rolPorActividad = new LinkedHashMap<>();
        private final List<PropuestaHistorialResponse> propuestas = new ArrayList<>();
        private String tema;

        private Acumulado(Evento evento) {
            this.evento = evento;
        }

        private void agregarActividad(Actividad actividad, RolParticipacion rol) {
            String clave = actividad.getId() + ":" + rol;
            actividades.putIfAbsent(clave, actividad);
            rolPorActividad.putIfAbsent(clave, rol);
        }

        private EventoHistorialResponse aResponse(FiltroPeriodo f) {
            List<ActividadHistorialResponse> lista = actividades.entrySet().stream()
                    .filter(e -> ReglasFiltroPeriodo.actividadCumple(e.getValue(), f))
                    .map(e -> {
                        Actividad act = e.getValue();
                        return new ActividadHistorialResponse(
                                act.getId(), act.getNombre(),
                                act.getTipoActividad() == null ? null : act.getTipoActividad().getNombre(),
                                act.getFecha(), act.getHoraInicio(), act.getHoraFin(), act.getEstado(),
                                rolPorActividad.get(e.getKey()));
                    })
                    .sorted(ORDEN_ACTIVIDADES)
                    .toList();

            return new EventoHistorialResponse(
                    evento.getId(),
                    evento.getNombre(),
                    evento.getTipoEvento() == null ? null : evento.getTipoEvento().getNombre(),
                    evento.getSemestre(),
                    evento.getFechaInicio(),
                    evento.getFechaFin(),
                    evento.getEstado(),
                    EnumSet.copyOf(roles),
                    tema,
                    lista,
                    List.copyOf(propuestas));
        }
    }
}
