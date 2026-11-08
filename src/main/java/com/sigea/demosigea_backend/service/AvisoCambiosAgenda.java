package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.actividad.ActividadResponse;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.AgendaPublicada;
import com.sigea.demosigea_backend.model.EstadoAgenda;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.repository.AgendaPublicadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Cambios en actividades de una agenda ya publicada (HU-22, Criterio 5).
 * <ul>
 *   <li>Si una actividad que los participantes ya veían cambia de fecha, horario, sala, ponente o nombre, se notifica
 *       a sus inscritos con lo de antes y lo de ahora.</li>
 *   <li>Si se elimina o vuelve a por programar, se notifica a sus inscritos que se retiró.</li>
 *   <li>Si la agenda del evento está publicada y una actividad queda en un día que todavía no estaba publicado, ese
 *       día se publica, para que la actividad no desaparezca de la agenda de los participantes.</li>
 * </ul>
 * Se llama desde la transacción de la operación que origina el cambio ({@link ActividadService},
 * {@link AgendaService}); si esa operación falla, tampoco quedan notificaciones.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvisoCambiosAgenda {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final AgendaPublicadaRepository agendaPublicadaRepository;
    private final NotificacionService notificacionService;
    private final Clock clock;

    /**
     * Lo que el participante ve de una actividad: si algo de esto cambia, se le avisa.
     *
     * @param nombre        Nombre de la actividad.
     * @param fecha         Fecha (null si está por programar).
     * @param inicio        Hora de inicio.
     * @param fin           Hora de fin.
     * @param salaId        ID de la sala.
     * @param salaNombre    Nombre de la sala.
     * @param ponenteId     ID del ponente.
     * @param ponenteNombre Nombre del ponente.
     */
    public record Programacion(String nombre, LocalDate fecha, LocalTime inicio, LocalTime fin, Long salaId,
                               String salaNombre, Long ponenteId, String ponenteNombre) {

        /** Programación vacía (actividad nueva). */
        public static final Programacion NINGUNA = new Programacion(null, null, null, null, null, null, null, null);

        /** Toma la programación actual de la actividad. */
        public static Programacion de(Actividad a) {
            if (a.estaPorProgramar() || a.estaCancelada() || !a.tieneHorarioCompleto()) {
                return new Programacion(a.getNombre(), null, null, null, null, null,
                        a.getPonente() != null ? a.getPonente().getId() : null,
                        ActividadResponse.nombreCompleto(a.getPonente()));
            }
            return new Programacion(a.getNombre(), a.getFecha(), a.getHoraInicio(), a.getHoraFin(),
                    a.getSala().getId(), a.getSala().getNombre(),
                    a.getPonente() != null ? a.getPonente().getId() : null,
                    ActividadResponse.nombreCompleto(a.getPonente()));
        }

        /** @return true si tiene fecha y horario (aparece en la agenda). */
        public boolean programada() {
            return fecha != null;
        }

        /** Comparación sin los nombres de sala y ponente (cuentan los IDs). */
        boolean mismaQue(Programacion otra) {
            return Objects.equals(nombre, otra.nombre) && Objects.equals(fecha, otra.fecha)
                    && Objects.equals(inicio, otra.inicio) && Objects.equals(fin, otra.fin)
                    && Objects.equals(salaId, otra.salaId) && Objects.equals(ponenteId, otra.ponenteId);
        }

        String describir() {
            StringBuilder sb = new StringBuilder();
            sb.append(fecha.format(FECHA)).append(", ").append(inicio.format(HORA)).append("–")
                    .append(fin.format(HORA)).append(", sala ").append(salaNombre);
            if (ponenteNombre != null && !ponenteNombre.isBlank()) {
                sb.append(", ponente ").append(ponenteNombre);
            }
            return sb.toString();
        }
    }

    /**
     * La actividad se creó, se programó o se modificó. Notifica a los inscritos si la veían en la agenda publicada
     * y cambió algo, y publica el día nuevo si la agenda del evento ya está publicada.
     *
     * @param actividad la actividad ya guardada.
     * @param antes     su programación antes del cambio ({@link Programacion#NINGUNA} si es nueva).
     * @return número de inscritos notificados.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int actividadModificada(Actividad actividad, Programacion antes) {
        Programacion despues = Programacion.de(actividad);
        Evento evento = actividad.getEvento();
        if (!agendaPublicadaRepository.existsByEvento_IdAndEstado(evento.getId(), EstadoAgenda.publicada)) {
            return 0;
        }
        if (despues.programada()) {
            publicarDiaSiFalta(evento, despues.fecha());
        }
        if (despues.mismaQue(antes) || !visible(evento.getId(), antes)) {
            return 0;
        }
        if (!despues.programada()) {
            return actividadRetirada(actividad, antes);
        }
        String mensaje = "La actividad «" + actividad.getNombre() + "» cambió. Antes: " + antes.describir()
                + ". Ahora: " + despues.describir() + ".";
        return notificacionService.notificarAInscritosDeActividad(actividad.getId(),
                TipoNotificacion.ACTIVIDAD_AGENDA_MODIFICADA,
                "Cambio en la actividad «" + actividad.getNombre() + "»", mensaje);
    }

    /**
     * La actividad sale de la agenda (se elimina o vuelve a por programar). Notifica a los inscritos si la veían en
     * la agenda publicada. Si se va a eliminar, llamarlo antes de borrarla: sus inscripciones se borran en cascada.
     *
     * @param actividad la actividad.
     * @param antes     su programación antes de retirarla.
     * @return número de inscritos notificados.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int actividadRetirada(Actividad actividad, Programacion antes) {
        if (!visible(actividad.getEvento().getId(), antes)) {
            return 0;
        }
        String mensaje = "La actividad «" + antes.nombre() + "» (" + antes.describir()
                + ") se retiró de la agenda del evento. Te avisaremos si se vuelve a programar.";
        return notificacionService.notificarAInscritosDeActividad(actividad.getId(),
                TipoNotificacion.ACTIVIDAD_AGENDA_RETIRADA,
                "La actividad «" + antes.nombre() + "» se retiró de la agenda", mensaje);
    }

    /** ¿Los participantes veían esta programación? (estaba en un día con la agenda publicada). */
    private boolean visible(Long eventoId, Programacion p) {
        return p.programada()
                && agendaPublicadaRepository.existsByEvento_IdAndFechaDiaAndEstado(eventoId, p.fecha(),
                EstadoAgenda.publicada);
    }

    private void publicarDiaSiFalta(Evento evento, LocalDate dia) {
        AgendaPublicada fila = agendaPublicadaRepository.findByEvento_IdAndFechaDia(evento.getId(), dia)
                .orElseGet(() -> AgendaPublicada.builder().evento(evento).fechaDia(dia).build());
        if (fila.getEstado() == EstadoAgenda.publicada) {
            return;
        }
        fila.setEstado(EstadoAgenda.publicada);
        fila.setFechaPublicacion(LocalDateTime.now(clock));
        agendaPublicadaRepository.save(fila);
        log.info("Día {} de la agenda del evento {} publicado al programar una actividad en él", dia, evento.getId());
    }
}
