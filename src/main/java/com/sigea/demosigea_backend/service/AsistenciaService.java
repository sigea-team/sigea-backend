package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.asistencia.AsistenciaResponse;
import com.sigea.demosigea_backend.dto.asistencia.AsistenciasActividadResponse;
import com.sigea.demosigea_backend.exception.AsistenciaRechazadaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Asistencia;
import com.sigea.demosigea_backend.model.CodigoQr;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.MetodoRegistroAsistencia;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.AsistenciaRepository;
import com.sigea.demosigea_backend.repository.CodigoQrRepository;
import com.sigea.demosigea_backend.repository.InscripcionActividadRepository;
import com.sigea.demosigea_backend.repository.InscripcionEventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Registro de asistencia a las actividades con el código QR de la inscripción (HU-24, RF45).
 * <ul>
 *   <li>Criterio 2: el organizador escanea el QR; el sistema lo valida y registra la asistencia con fecha y hora.</li>
 *   <li>Criterio 3: si el QR no es de ese evento, la persona no está inscrita en la actividad o la asistencia
 *       ya existe, se rechaza sin duplicar. La inscripción se bloquea (SELECT ... FOR UPDATE) mientras se
 *       registra, y el índice único (inscripcion_id, actividad_id) de la BD es la última garantía.</li>
 * </ul>
 * También permite el registro manual por inscripción cuando el participante no tiene su QR a mano.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private static final String ENTIDAD = "asistencias";
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AsistenciaRepository asistenciaRepository;
    private final CodigoQrRepository codigoQrRepository;
    private final InscripcionEventoRepository inscripcionEventoRepository;
    private final InscripcionActividadRepository inscripcionActividadRepository;
    private final ActividadRepository actividadRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Registra la asistencia con el valor leído del QR (Criterio 2).
     *
     * @param eventoId    evento de la actividad.
     * @param actividadId actividad en la que se escanea.
     * @param codigoLeido texto leído del QR.
     * @return la asistencia registrada.
     * @throws AsistenciaRechazadaException si el QR no es válido para la actividad o la asistencia ya existe (Criterio 3).
     */
    @Transactional
    public AsistenciaResponse registrarPorQr(Long eventoId, Long actividadId, String codigoLeido) {
        Actividad actividad = actividadDisponible(eventoId, actividadId);
        String codigo = codigoLeido == null ? "" : codigoLeido.trim();

        CodigoQr qr = codigoQrRepository.findByCodigoConInscripcion(codigo)
                .orElseThrow(() -> new AsistenciaRechazadaException(AsistenciaRechazadaException.QR_INVALIDO,
                        HttpStatus.NOT_FOUND, "El código QR no corresponde a ninguna inscripción registrada en SIGEA."));

        if (!qr.estaVigente()) {
            throw new AsistenciaRechazadaException(AsistenciaRechazadaException.QR_NO_VIGENTE, HttpStatus.CONFLICT,
                    "El código QR ya no está vigente y no se puede usar para registrar asistencia.");
        }

        InscripcionEvento inscripcion = qr.getInscripcion();
        Evento eventoDelQr = inscripcion.getEvento();
        if (eventoDelQr == null || !Objects.equals(eventoDelQr.getId(), eventoId)) {
            throw new AsistenciaRechazadaException(AsistenciaRechazadaException.QR_OTRO_EVENTO, HttpStatus.CONFLICT,
                    "El código QR corresponde a una inscripción al evento «"
                            + (eventoDelQr != null ? eventoDelQr.getNombre() : "desconocido")
                            + "», no al evento de esta actividad.");
        }

        return registrar(eventoId, actividad, inscripcion.getId(), MetodoRegistroAsistencia.qr);
    }

    /**
     * Registra la asistencia a mano a partir de la inscripción al evento.
     *
     * @param eventoId      evento de la actividad.
     * @param actividadId   actividad.
     * @param inscripcionId inscripción al evento del participante.
     * @return la asistencia registrada.
     */
    @Transactional
    public AsistenciaResponse registrarManual(Long eventoId, Long actividadId, Long inscripcionId) {
        Actividad actividad = actividadDisponible(eventoId, actividadId);
        return registrar(eventoId, actividad, inscripcionId, MetodoRegistroAsistencia.manual);
    }

    /**
     * Asistencias registradas en una actividad con el total de inscritos.
     */
    @Transactional(readOnly = true)
    public AsistenciasActividadResponse listarPorActividad(Long eventoId, Long actividadId) {
        Actividad actividad = buscarActividad(eventoId, actividadId);
        List<AsistenciaResponse> asistencias = asistenciaRepository.findByActividadConParticipante(actividadId).stream()
                .map(AsistenciaResponse::fromEntity)
                .toList();
        long inscritos = inscripcionActividadRepository.countByActividad_Id(actividadId);
        return new AsistenciasActividadResponse(
                actividad.getId(),
                actividad.getNombre(),
                actividad.getFecha(),
                actividad.getHoraInicio(),
                actividad.getHoraFin(),
                inscritos,
                asistencias.size(),
                asistencias
        );
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Auxiliares
    // ──────────────────────────────────────────────────────────────────────────────

    private AsistenciaResponse registrar(Long eventoId, Actividad actividad, Long inscripcionId,
                                         MetodoRegistroAsistencia metodo) {
        // Bloquea la inscripción: dos lecturas simultáneas del mismo QR quedan en fila y la segunda ve la primera.
        InscripcionEvento inscripcion = inscripcionEventoRepository.findByIdAndEvento_IdParaAsistencia(inscripcionId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la inscripción con ID " + inscripcionId + " en este evento."));
        Persona persona = inscripcion.getPersona();
        String participante = nombreDe(persona);

        if (!inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(inscripcion.getId(), actividad.getId())) {
            throw new AsistenciaRechazadaException(AsistenciaRechazadaException.NO_INSCRITO_ACTIVIDAD, HttpStatus.CONFLICT,
                    participante + " está inscrito en el evento, pero no en la actividad «" + actividad.getNombre() + "».");
        }

        Optional<Asistencia> existente = asistenciaRepository.findByInscripcion_IdAndActividad_Id(
                inscripcion.getId(), actividad.getId());
        if (existente.isPresent()) {
            Asistencia previa = existente.get();
            throw new AsistenciaRechazadaException(AsistenciaRechazadaException.ASISTENCIA_DUPLICADA, HttpStatus.CONFLICT,
                    "La asistencia de " + participante + " a «" + actividad.getNombre() + "» ya estaba registrada ("
                            + formatear(previa.getFechaHoraRegistro()) + "). No se registró de nuevo.",
                    AsistenciaResponse.fromEntity(previa));
        }

        Asistencia asistencia = Asistencia.builder()
                .inscripcion(inscripcion)
                .actividad(actividad)
                .fechaHoraRegistro(LocalDateTime.now())
                .metodoRegistro(metodo)
                .build();

        Asistencia guardada;
        try {
            guardada = asistenciaRepository.save(asistencia);
            asistenciaRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new AsistenciaRechazadaException(AsistenciaRechazadaException.ASISTENCIA_DUPLICADA, HttpStatus.CONFLICT,
                    "La asistencia de " + participante + " a «" + actividad.getNombre() + "» ya estaba registrada.");
        }

        log.info("Asistencia {} registrada ({}) para la inscripción {} en la actividad {}",
                guardada.getId(), metodo, inscripcion.getId(), actividad.getId());

        auditoriaService.registrar(TipoOperacionAuditoria.ASISTENCIA_REGISTRADA, ENTIDAD, guardada.getId(),
                DetalleAuditoria.de(
                        "eventoId", eventoId,
                        "actividadId", actividad.getId(),
                        "actividadNombre", actividad.getNombre(),
                        "inscripcionId", inscripcion.getId(),
                        "personaId", persona != null ? persona.getId() : null,
                        "metodo", metodo.name()
                ));

        return AsistenciaResponse.fromEntity(guardada);
    }

    /** Actividad del evento en la que se puede registrar asistencia. */
    private Actividad actividadDisponible(Long eventoId, Long actividadId) {
        Actividad actividad = buscarActividad(eventoId, actividadId);

        Evento evento = actividad.getEvento();
        MemoriaHistoricaReglas.verificarEditable(evento);
        if (evento != null && (evento.getEstado() == EstadoEvento.en_configuracion
                || evento.getEstado() == EstadoEvento.cerrado)) {
            throw new AsistenciaRechazadaException(AsistenciaRechazadaException.EVENTO_NO_DISPONIBLE, HttpStatus.CONFLICT,
                    evento.getEstado() == EstadoEvento.cerrado
                            ? "El evento «" + evento.getNombre() + "» está cerrado; ya no se registran asistencias."
                            : "El evento «" + evento.getNombre() + "» está en configuración; aún no se registran asistencias.");
        }
        if (actividad.estaPorProgramar() || actividad.estaCancelada()) {
            throw new AsistenciaRechazadaException(AsistenciaRechazadaException.ACTIVIDAD_NO_DISPONIBLE, HttpStatus.CONFLICT,
                    "La actividad «" + actividad.getNombre() + "» "
                            + (actividad.estaCancelada() ? "está cancelada" : "aún no está programada")
                            + "; no se puede registrar asistencia.");
        }
        return actividad;
    }

    private Actividad buscarActividad(Long eventoId, Long actividadId) {
        return actividadRepository.findByIdAndEvento_Id(actividadId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la actividad con ID " + actividadId + " en este evento."));
    }

    private static String nombreDe(Persona p) {
        if (p == null) {
            return "El participante";
        }
        String nombre = ((p.getNombres() != null ? p.getNombres() : "") + " "
                + (p.getApellidos() != null ? p.getApellidos() : "")).trim();
        return nombre.isEmpty() ? "El participante" : nombre;
    }

    private static String formatear(LocalDateTime fecha) {
        return fecha == null ? "sin fecha" : fecha.format(FORMATO_FECHA_HORA);
    }
}
