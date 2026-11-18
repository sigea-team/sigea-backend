package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.encuesta.EncuestaParticipanteResponse;
import com.sigea.demosigea_backend.dto.encuesta.EnvioEncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.PreguntaEncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResponderEncuestaRequest;
import com.sigea.demosigea_backend.dto.encuesta.RespuestaPreguntaRequest;
import com.sigea.demosigea_backend.exception.EncuestaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Encuesta;
import com.sigea.demosigea_backend.model.EstadoEncuesta;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.OpcionPreguntaEncuesta;
import com.sigea.demosigea_backend.model.ParticipacionEncuesta;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.PreguntaEncuesta;
import com.sigea.demosigea_backend.model.RespuestaEncuesta;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AsistenciaRepository;
import com.sigea.demosigea_backend.repository.EncuestaRepository;
import com.sigea.demosigea_backend.repository.ParticipacionEncuestaRepository;
import com.sigea.demosigea_backend.repository.RespuestaEncuestaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Respuesta de las encuestas de satisfacción por parte de los participantes (HU-25, RF46, CU-22).
 * <ul>
 *   <li>Criterio 1: el participante ve las encuestas publicadas del evento o de las actividades a las que asistió.</li>
 *   <li>Criterio 2: al responder, las respuestas se guardan asociadas a la encuesta; con la persona si la encuesta
 *       es identificada, o sin ella si es anónima. En ambos casos queda la constancia de que la persona respondió
 *       ({@code participaciones_encuesta}), que evita duplicados (CU-22, flujo alterno 1.1) sin decir qué respondió.
 *       En las anónimas la fecha de cada respuesta se guarda sin hora para no poder cruzarla con esa constancia.</li>
 *   <li>Criterio 3: si no registró asistencia (HU-24), la encuesta no se le muestra ni se le permite responder.</li>
 * </ul>
 * Mientras se guardan las respuestas, la encuesta se bloquea en modo compartido: varias personas responden a la vez,
 * pero el cierre espera y las respuestas posteriores la ven cerrada.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RespuestaEncuestaService {

    private final EncuestaRepository encuestaRepository;
    private final ParticipacionEncuestaRepository participacionRepository;
    private final RespuestaEncuestaRepository respuestaRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    /**
     * Criterio 1: encuestas habilitadas para el participante autenticado. Incluye las publicadas de eventos y
     * actividades a las que asistió, y las cerradas que ya respondió.
     */
    @Transactional(readOnly = true)
    public List<EncuestaParticipanteResponse> misEncuestas() {
        Persona persona = personaAutenticada();
        Map<Long, LocalDateTime> respondidas = new HashMap<>();
        for (ParticipacionEncuesta p : participacionRepository.findByPersona_Id(persona.getId())) {
            respondidas.put(p.getEncuesta().getId(), p.getFechaRespuesta());
        }
        LocalDateTime ahora = ahora();
        return encuestaRepository.findHabilitadasParaPersona(persona.getId(),
                        EnumSet.of(EstadoEncuesta.publicada, EstadoEncuesta.cerrada)).stream()
                .filter(e -> e.getEstado() == EstadoEncuesta.publicada || respondidas.containsKey(e.getId()))
                .map(e -> aResponse(e, respondidas.get(e.getId()), ahora, false))
                .toList();
    }

    /**
     * Formulario de la encuesta con sus preguntas. Criterio 3: si el participante no asistió, se rechaza.
     */
    @Transactional(readOnly = true)
    public EncuestaParticipanteResponse formulario(Long encuestaId) {
        Persona persona = personaAutenticada();
        Encuesta encuesta = encuestaRepository.findByIdConEvento(encuestaId)
                .filter(e -> e.getEstado() != EstadoEncuesta.borrador)
                .orElseThrow(() -> noEncontrada(encuestaId));
        exigirAsistencia(encuesta, persona);
        LocalDateTime respondida = participacionRepository.findByEncuesta_IdAndPersona_Id(encuestaId, persona.getId())
                .map(ParticipacionEncuesta::getFechaRespuesta)
                .orElse(null);
        return aResponse(encuesta, respondida, ahora(), true);
    }

    /**
     * Criterio 2: guarda las respuestas del participante, anónimas o identificadas según la configuración.
     *
     * @throws EncuestaException 403 si no asistió (Criterio 3); 409 si está cerrada o ya la respondió;
     *                           400 si faltan respuestas obligatorias o alguna no es válida.
     */
    @Transactional
    public EnvioEncuestaResponse responder(Long encuestaId, ResponderEncuestaRequest request) {
        Persona persona = personaAutenticada();
        Encuesta encuesta = encuestaRepository.findByIdParaResponder(encuestaId)
                .filter(e -> e.getEstado() != EstadoEncuesta.borrador)
                .orElseThrow(() -> noEncontrada(encuestaId));

        exigirAsistencia(encuesta, persona);

        LocalDateTime ahora = ahora();
        if (!encuesta.recibeRespuestas(ahora)) {
            throw new EncuestaException(EncuestaException.CERRADA, HttpStatus.CONFLICT,
                    "La encuesta «" + encuesta.getTitulo() + "» está cerrada desde el "
                            + EncuestaService.formatear(encuesta.getFechaCierre()) + "; ya no recibe respuestas.");
        }

        Optional<ParticipacionEncuesta> previa =
                participacionRepository.findByEncuesta_IdAndPersona_Id(encuestaId, persona.getId());
        if (previa.isPresent()) {
            throw yaRespondida(encuesta, previa.get().getFechaRespuesta());
        }

        List<RespuestaEncuesta> respuestas = validarRespuestas(encuesta, request);

        try {
            participacionRepository.save(ParticipacionEncuesta.builder()
                    .encuesta(encuesta)
                    .persona(persona)
                    .fechaRespuesta(ahora)
                    .build());
            participacionRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // Dos envíos simultáneos de la misma persona: el índice único deja pasar solo el primero.
            throw yaRespondida(encuesta, null);
        }

        // Criterio 2: anónima → sin persona y sin hora; identificada → con persona y fecha y hora.
        LocalDateTime fechaRespuesta = encuesta.isAnonima() ? ahora.toLocalDate().atStartOfDay() : ahora;
        for (RespuestaEncuesta r : respuestas) {
            r.setPersona(encuesta.isAnonima() ? null : persona);
            r.setFechaRespuesta(fechaRespuesta);
        }
        respuestaRepository.saveAll(respuestas);

        log.info("Encuesta {} respondida ({} respuesta(s), {})", encuestaId, respuestas.size(),
                encuesta.isAnonima() ? "anónima" : "identificada");
        auditoriaService.registrar(TipoOperacionAuditoria.ENCUESTA_RESPONDIDA, EncuestaService.ENTIDAD, encuestaId,
                DetalleAuditoria.de(
                        "eventoId", encuesta.getEvento().getId(),
                        "actividadId", encuesta.getActividad() != null ? encuesta.getActividad().getId() : null,
                        "anonima", encuesta.isAnonima(),
                        "preguntasRespondidas", respuestas.size()));

        return new EnvioEncuestaResponse(
                encuestaId,
                encuesta.getTitulo(),
                encuesta.isAnonima(),
                respuestas.size(),
                ahora,
                encuesta.isAnonima()
                        ? "Gracias por responder. Tus respuestas se guardaron de forma anónima."
                        : "Gracias por responder. Tus respuestas quedaron registradas.");
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Validación de las respuestas
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Valida las respuestas contra las preguntas de la encuesta y las convierte en entidades (sin persona ni fecha).
     * Los errores se informan por pregunta con la clave {@code pregunta_<id>}.
     */
    List<RespuestaEncuesta> validarRespuestas(Encuesta encuesta, ResponderEncuestaRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        Map<Long, PreguntaEncuesta> preguntas = new LinkedHashMap<>();
        for (PreguntaEncuesta p : encuesta.getPreguntas()) {
            preguntas.put(p.getId(), p);
        }

        Map<Long, RespuestaPreguntaRequest> recibidas = new HashMap<>();
        List<RespuestaPreguntaRequest> lista = request != null && request.respuestas() != null
                ? request.respuestas() : List.of();
        for (RespuestaPreguntaRequest r : lista) {
            if (r == null || r.preguntaId() == null) {
                errores.put("respuestas", "Cada respuesta debe indicar su pregunta.");
            } else if (!preguntas.containsKey(r.preguntaId())) {
                errores.put(clave(r.preguntaId()), "La pregunta no pertenece a esta encuesta.");
            } else if (recibidas.putIfAbsent(r.preguntaId(), r) != null) {
                errores.put(clave(r.preguntaId()), "La pregunta se respondió más de una vez.");
            }
        }

        List<RespuestaEncuesta> respuestas = new ArrayList<>();
        for (PreguntaEncuesta p : preguntas.values()) {
            String clave = clave(p.getId());
            if (errores.containsKey(clave)) {
                continue;
            }
            RespuestaPreguntaRequest r = recibidas.get(p.getId());
            String texto = r != null && r.texto() != null && !r.texto().isBlank() ? r.texto().trim() : null;
            boolean vacia = r == null || (r.valorEscala() == null && r.opcionId() == null && texto == null);
            if (vacia) {
                if (p.isObligatoria()) {
                    errores.put(clave, "Esta pregunta es obligatoria.");
                }
                continue;
            }

            RespuestaEncuesta respuesta = RespuestaEncuesta.builder().pregunta(p).build();
            switch (p.getTipoRespuesta()) {
                case escala -> {
                    Integer valor = r.valorEscala();
                    if (valor == null || r.opcionId() != null || texto != null) {
                        errores.put(clave, "Responda con un valor de la escala de " + p.getEscalaMinima()
                                + " a " + p.getEscalaMaxima() + ".");
                    } else if (valor < p.getEscalaMinima() || valor > p.getEscalaMaxima()) {
                        errores.put(clave, "El valor " + valor + " está fuera de la escala de " + p.getEscalaMinima()
                                + " a " + p.getEscalaMaxima() + ".");
                    } else {
                        respuesta.setValorEscala(valor);
                        respuesta.setRespuesta(String.valueOf(valor));
                        respuestas.add(respuesta);
                    }
                }
                case opcion -> {
                    OpcionPreguntaEncuesta opcion = r.opcionId() == null || r.valorEscala() != null || texto != null
                            ? null
                            : p.getOpciones().stream().filter(o -> r.opcionId().equals(o.getId())).findFirst()
                            .orElse(null);
                    if (opcion == null) {
                        errores.put(clave, "Elija una de las opciones de la pregunta.");
                    } else {
                        respuesta.setOpcion(opcion);
                        respuesta.setRespuesta(opcion.getTexto());
                        respuestas.add(respuesta);
                    }
                }
                case texto -> {
                    if (texto == null || r.valorEscala() != null || r.opcionId() != null) {
                        errores.put(clave, "Responda con texto.");
                    } else {
                        respuesta.setRespuesta(texto);
                        respuestas.add(respuesta);
                    }
                }
            }
        }

        if (errores.isEmpty() && respuestas.isEmpty()) {
            errores.put("respuestas", "Responda al menos una pregunta.");
        }
        if (!errores.isEmpty()) {
            throw new EncuestaException(EncuestaException.RESPUESTAS_INVALIDAS, HttpStatus.BAD_REQUEST,
                    "Revise sus respuestas antes de enviar la encuesta.", errores);
        }
        return respuestas;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Auxiliares
    // ──────────────────────────────────────────────────────────────────────────────

    /** Criterio 3: la encuesta solo se habilita a quien registró asistencia (HU-24). */
    private void exigirAsistencia(Encuesta encuesta, Persona persona) {
        Actividad actividad = encuesta.getActividad();
        boolean asistio = actividad != null
                ? asistenciaRepository.existsByInscripcion_Persona_IdAndActividad_Id(persona.getId(), actividad.getId())
                : asistenciaRepository.existsByInscripcion_Persona_IdAndInscripcion_Evento_Id(persona.getId(),
                encuesta.getEvento().getId());
        if (!asistio) {
            throw new EncuestaException(EncuestaException.NO_HABILITADA, HttpStatus.FORBIDDEN,
                    actividad != null
                            ? "La encuesta solo está habilitada para quienes asistieron a la actividad «"
                            + actividad.getNombre() + "», y no tienes asistencia registrada en ella."
                            : "La encuesta solo está habilitada para quienes asistieron al evento «"
                            + encuesta.getEvento().getNombre() + "», y no tienes asistencia registrada en él.");
        }
    }

    private EncuestaParticipanteResponse aResponse(Encuesta e, LocalDateTime fechaRespuesta, LocalDateTime ahora,
                                                   boolean conPreguntas) {
        Evento evento = e.getEvento();
        Actividad actividad = e.getActividad();
        return new EncuestaParticipanteResponse(
                e.getId(),
                evento.getId(),
                evento.getNombre(),
                actividad != null ? actividad.getId() : null,
                actividad != null ? actividad.getNombre() : null,
                EncuestaService.ambito(e),
                e.getTitulo(),
                e.getDescripcion(),
                e.isAnonima(),
                e.getEstado(),
                e.getFechaCierre(),
                e.recibeRespuestas(ahora) && fechaRespuesta == null,
                fechaRespuesta != null,
                fechaRespuesta,
                e.getPreguntas().size(),
                conPreguntas ? e.getPreguntas().stream().map(PreguntaEncuestaResponse::fromEntity).toList() : null);
    }

    private static EncuestaException yaRespondida(Encuesta encuesta, LocalDateTime fecha) {
        return new EncuestaException(EncuestaException.YA_RESPONDIDA, HttpStatus.CONFLICT,
                "Ya respondiste la encuesta «" + encuesta.getTitulo() + "»"
                        + (fecha != null ? " el " + EncuestaService.formatear(fecha) : "")
                        + ". No se registró de nuevo.");
    }

    private static RecursoNoEncontradoException noEncontrada(Long encuestaId) {
        return new RecursoNoEncontradoException("No se encontró la encuesta con ID " + encuestaId + ".");
    }

    static String clave(Long preguntaId) {
        return "pregunta_" + preguntaId;
    }

    private Persona personaAutenticada() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debe iniciar sesión para responder encuestas.");
        }
        Usuario usuario = usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("No se encontró el usuario autenticado."));
        if (usuario.getPersona() == null) {
            throw new OperacionNoPermitidaException("El usuario autenticado no tiene una persona asociada.");
        }
        return usuario.getPersona();
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
