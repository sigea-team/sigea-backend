package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.encuesta.ConteoResultadoResponse;
import com.sigea.demosigea_backend.dto.encuesta.EncuestaRequest;
import com.sigea.demosigea_backend.dto.encuesta.EncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.PreguntaEncuestaRequest;
import com.sigea.demosigea_backend.dto.encuesta.PreguntaEncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.RespuestaTextoResponse;
import com.sigea.demosigea_backend.dto.encuesta.RespuestasParticipanteResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResultadoPreguntaResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResultadosEncuestaResponse;
import com.sigea.demosigea_backend.exception.EncuestaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Encuesta;
import com.sigea.demosigea_backend.model.EstadoEncuesta;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.OpcionPreguntaEncuesta;
import com.sigea.demosigea_backend.model.ParticipacionEncuesta;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.PreguntaEncuesta;
import com.sigea.demosigea_backend.model.RespuestaEncuesta;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoRespuestaEncuesta;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.AsistenciaRepository;
import com.sigea.demosigea_backend.repository.EncuestaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.ParticipacionEncuestaRepository;
import com.sigea.demosigea_backend.repository.RespuestaEncuestaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Configuración, publicación y resultados de las encuestas de satisfacción (HU-25, RF46), del lado del
 * administrador u organizador.
 * <ul>
 *   <li>Criterio 1: la encuesta se configura en borrador (del evento en general o de una actividad) y al publicarla
 *       queda habilitada para quienes asistieron (HU-24). Se avisa en la campana a los asistentes de ese momento;
 *       quien asista después la ve igualmente en «Mis encuestas».</li>
 *   <li>Criterio 2: la configuración define si las respuestas son anónimas o identificadas; no cambia después de
 *       publicar.</li>
 *   <li>Criterio 4: los resultados se presentan consolidados por pregunta (promedio y distribución de la escala,
 *       conteo por opción y textos libres), con la tasa de respuesta.</li>
 * </ul>
 * Una encuesta publicada ya no se edita (cambiar preguntas invalidaría las respuestas recibidas); se puede cerrar.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncuestaService {

    static final String ENTIDAD = "encuestas";
    static final int ESCALA_MINIMA_POR_DEFECTO = 1;
    static final int ESCALA_MAXIMA_POR_DEFECTO = 5;
    static final int MINIMO_OPCIONES = 2;
    static final int MAXIMO_OPCIONES = 20;
    static final int LARGO_OPCION = 200;

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EncuestaRepository encuestaRepository;
    private final ParticipacionEncuestaRepository participacionRepository;
    private final RespuestaEncuestaRepository respuestaRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EventoRepository eventoRepository;
    private final ActividadRepository actividadRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final NotificacionService notificacionService;
    private final Clock clock;

    // ──────────────────────────────────────────────────────────────────────────────
    // Configuración (Criterio 1)
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Crea una encuesta en borrador para el evento o para una de sus actividades.
     */
    @Transactional
    public EncuestaResponse crear(Long eventoId, EncuestaRequest request) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID " + eventoId + "."));

        Encuesta encuesta = Encuesta.builder()
                .evento(evento)
                .estado(EstadoEncuesta.borrador)
                .fechaCreacion(ahora())
                .creadaPor(usuarioAutenticado())
                .build();
        aplicarConfiguracion(encuesta, eventoId, request);

        Encuesta guardada = encuestaRepository.save(encuesta);
        log.info("Encuesta {} creada en el evento {} ({} pregunta(s))", guardada.getId(), eventoId,
                guardada.getPreguntas().size());
        auditoriaService.registrar(TipoOperacionAuditoria.ENCUESTA_CREADA, ENTIDAD, guardada.getId(),
                resumenAuditoria(guardada));
        return aResponse(guardada);
    }

    /**
     * Reemplaza la configuración y las preguntas de una encuesta en borrador.
     */
    @Transactional
    public EncuestaResponse actualizar(Long eventoId, Long encuestaId, EncuestaRequest request) {
        Encuesta encuesta = bloquear(eventoId, encuestaId);
        if (encuesta.getEstado() != EstadoEncuesta.borrador) {
            throw new EncuestaException(EncuestaException.NO_EDITABLE, HttpStatus.CONFLICT,
                    "La encuesta «" + encuesta.getTitulo() + "» ya fue " + encuesta.getEstado().name()
                            + " y no se puede modificar: cambiar sus preguntas alteraría las respuestas recibidas.");
        }
        Map<String, Object> antes = resumenAuditoria(encuesta);

        encuesta.getPreguntas().clear();
        // Las preguntas anteriores se borran antes de insertar las nuevas (mismo orden).
        encuestaRepository.flush();
        aplicarConfiguracion(encuesta, eventoId, request);

        Encuesta guardada = encuestaRepository.save(encuesta);
        auditoriaService.registrar(TipoOperacionAuditoria.ENCUESTA_ACTUALIZADA, ENTIDAD, guardada.getId(),
                DetalleAuditoria.de("antes", antes, "despues", resumenAuditoria(guardada)));
        return aResponse(guardada);
    }

    /**
     * Elimina una encuesta que todavía no tiene respuestas.
     */
    @Transactional
    public void eliminar(Long eventoId, Long encuestaId) {
        Encuesta encuesta = bloquear(eventoId, encuestaId);
        long respuestas = participacionRepository.countByEncuesta_Id(encuestaId);
        if (respuestas > 0) {
            throw new EncuestaException(EncuestaException.NO_EDITABLE, HttpStatus.CONFLICT,
                    "La encuesta «" + encuesta.getTitulo() + "» ya tiene " + respuestas
                            + " respuesta(s) y no se puede eliminar. Puede cerrarla para que no reciba más.");
        }
        Map<String, Object> antes = resumenAuditoria(encuesta);
        encuestaRepository.delete(encuesta);
        log.info("Encuesta {} eliminada del evento {}", encuestaId, eventoId);
        auditoriaService.registrar(TipoOperacionAuditoria.ENCUESTA_ELIMINADA, ENTIDAD, encuestaId,
                DetalleAuditoria.de("antes", antes));
    }

    /**
     * Criterio 1: publica la encuesta y la habilita para los participantes que asistieron.
     */
    @Transactional
    public EncuestaResponse publicar(Long eventoId, Long encuestaId) {
        Encuesta encuesta = bloquear(eventoId, encuestaId);
        if (encuesta.getEstado() != EstadoEncuesta.borrador) {
            throw new EncuestaException(EncuestaException.ESTADO_INVALIDO, HttpStatus.CONFLICT,
                    "La encuesta «" + encuesta.getTitulo() + "» ya está " + encuesta.getEstado().name() + ".");
        }

        Evento evento = encuesta.getEvento();
        if (evento.getEstado() == EstadoEvento.en_configuracion) {
            throw new EncuestaException(EncuestaException.EVENTO_NO_DISPONIBLE, HttpStatus.CONFLICT,
                    "El evento «" + evento.getNombre() + "» está en configuración: todavía nadie ha podido asistir. "
                            + "Publique la encuesta cuando el evento esté habilitado.");
        }
        Actividad actividad = encuesta.getActividad();
        if (actividad != null) {
            validarActividadDisponible(actividad);
        }

        LocalDateTime ahora = ahora();
        Map<String, String> faltantes = new LinkedHashMap<>();
        if (encuesta.getPreguntas().isEmpty()) {
            faltantes.put("preguntas", "Agregue al menos una pregunta antes de publicar.");
        }
        if (encuesta.getFechaCierre() != null && !encuesta.getFechaCierre().isAfter(ahora)) {
            faltantes.put("fechaCierre", "La fecha de cierre (" + formatear(encuesta.getFechaCierre())
                    + ") ya pasó. Cámbiela o déjela vacía.");
        }
        if (!faltantes.isEmpty()) {
            throw new EncuestaException(EncuestaException.INCOMPLETA, HttpStatus.CONFLICT,
                    "La encuesta no se puede publicar todavía.", faltantes);
        }

        encuesta.setEstado(EstadoEncuesta.publicada);
        encuesta.setFechaPublicacion(ahora);
        Encuesta guardada = encuestaRepository.save(encuesta);

        String sobre = actividad != null ? "la actividad «" + actividad.getNombre() + "»" : "el evento «" + evento.getNombre() + "»";
        int notificados = notificacionService.notificarAAsistentes(evento.getId(),
                actividad != null ? actividad.getId() : null,
                TipoNotificacion.ENCUESTA_PUBLICADA,
                "Encuesta de satisfacción: " + guardada.getTitulo(),
                "Ya puedes responder la encuesta de satisfacción sobre " + sobre + "."
                        + (guardada.getFechaCierre() != null
                        ? " Está abierta hasta el " + formatear(guardada.getFechaCierre()) + "." : ""),
                ENTIDAD, guardada.getId());

        log.info("Encuesta {} publicada ({} asistente(s) notificado(s))", guardada.getId(), notificados);
        Map<String, Object> detalle = resumenAuditoria(guardada);
        detalle.put("notificados", notificados);
        auditoriaService.registrar(TipoOperacionAuditoria.ENCUESTA_PUBLICADA, ENTIDAD, guardada.getId(), detalle);
        return aResponse(guardada);
    }

    /**
     * Cierra una encuesta publicada: no recibe más respuestas y sus resultados siguen disponibles.
     */
    @Transactional
    public EncuestaResponse cerrar(Long eventoId, Long encuestaId) {
        Encuesta encuesta = bloquear(eventoId, encuestaId);
        if (encuesta.getEstado() != EstadoEncuesta.publicada) {
            throw new EncuestaException(EncuestaException.ESTADO_INVALIDO, HttpStatus.CONFLICT,
                    encuesta.getEstado() == EstadoEncuesta.borrador
                            ? "La encuesta «" + encuesta.getTitulo() + "» está en borrador; puede eliminarla."
                            : "La encuesta «" + encuesta.getTitulo() + "» ya está cerrada.");
        }
        LocalDateTime ahora = ahora();
        LocalDateTime cierreAnterior = encuesta.getFechaCierre();
        encuesta.setEstado(EstadoEncuesta.cerrada);
        if (cierreAnterior == null || cierreAnterior.isAfter(ahora)) {
            encuesta.setFechaCierre(ahora);
        }
        Encuesta guardada = encuestaRepository.save(encuesta);
        auditoriaService.registrar(TipoOperacionAuditoria.ENCUESTA_CERRADA, ENTIDAD, guardada.getId(),
                DetalleAuditoria.de(
                        "titulo", guardada.getTitulo(),
                        "fechaCierreAnterior", cierreAnterior,
                        "respuestas", participacionRepository.countByEncuesta_Id(guardada.getId())));
        return aResponse(guardada);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Consulta
    // ──────────────────────────────────────────────────────────────────────────────

    /** Encuestas del evento con sus preguntas y totales. */
    @Transactional(readOnly = true)
    public List<EncuestaResponse> listar(Long eventoId) {
        if (!eventoRepository.existsById(eventoId)) {
            throw new RecursoNoEncontradoException("No se encontró el evento con ID " + eventoId + ".");
        }
        return encuestaRepository.findByEventoConActividad(eventoId).stream().map(this::aResponse).toList();
    }

    /** Detalle de una encuesta del evento. */
    @Transactional(readOnly = true)
    public EncuestaResponse obtener(Long eventoId, Long encuestaId) {
        return aResponse(buscar(eventoId, encuestaId));
    }

    /**
     * Criterio 4: resultados consolidados de la encuesta. Se pueden consultar mientras está publicada (parciales)
     * o después de cerrada.
     */
    @Transactional(readOnly = true)
    public ResultadosEncuestaResponse resultados(Long eventoId, Long encuestaId) {
        Encuesta encuesta = buscar(eventoId, encuestaId);
        long habilitados = totalHabilitados(encuesta);
        long respondieron = participacionRepository.countByEncuesta_Id(encuestaId);

        Map<Long, Map<Integer, Long>> porValor = new HashMap<>();
        for (Object[] fila : respuestaRepository.contarPorValorEscala(encuestaId)) {
            porValor.computeIfAbsent(aLong(fila[0]), k -> new HashMap<>())
                    .merge(aInteger(fila[1]), aLong(fila[2]), Long::sum);
        }
        Map<Long, Long> porOpcion = new HashMap<>();
        for (Object[] fila : respuestaRepository.contarPorOpcion(encuestaId)) {
            porOpcion.merge(aLong(fila[0]), aLong(fila[1]), Long::sum);
        }
        Map<Long, List<RespuestaEncuesta>> textos = new HashMap<>();
        for (RespuestaEncuesta r : respuestaRepository.findTextosDeEncuesta(encuestaId)) {
            textos.computeIfAbsent(r.getPregunta().getId(), k -> new ArrayList<>()).add(r);
        }

        List<ResultadoPreguntaResponse> preguntas = new ArrayList<>();
        for (PreguntaEncuesta p : encuesta.getPreguntas()) {
            preguntas.add(switch (p.getTipoRespuesta()) {
                case escala -> resultadoEscala(p, porValor.getOrDefault(p.getId(), Map.of()), respondieron);
                case opcion -> resultadoOpcion(p, porOpcion, respondieron);
                case texto -> resultadoTexto(p, textos.getOrDefault(p.getId(), List.of()), respondieron,
                        encuesta.isAnonima());
            });
        }

        Actividad actividad = encuesta.getActividad();
        return new ResultadosEncuestaResponse(
                encuesta.getId(),
                encuesta.getTitulo(),
                encuesta.getEvento().getId(),
                encuesta.getEvento().getNombre(),
                actividad != null ? actividad.getId() : null,
                actividad != null ? actividad.getNombre() : null,
                ambito(encuesta),
                encuesta.isAnonima(),
                encuesta.getEstado(),
                habilitados,
                respondieron,
                habilitados > 0 ? porcentaje(respondieron, habilitados) : null,
                ahora(),
                preguntas);
    }

    /**
     * Criterio 2: respuestas por participante de una encuesta identificada. En una anónima se rechaza.
     */
    @Transactional(readOnly = true)
    public List<RespuestasParticipanteResponse> respuestasPorParticipante(Long eventoId, Long encuestaId) {
        Encuesta encuesta = buscar(eventoId, encuestaId);
        if (encuesta.isAnonima()) {
            throw new EncuestaException(EncuestaException.ANONIMA, HttpStatus.CONFLICT,
                    "La encuesta «" + encuesta.getTitulo() + "» es anónima: sus respuestas no están asociadas a "
                            + "los participantes. Consulte los resultados consolidados.");
        }

        Map<Long, List<RespuestasParticipanteResponse.Detalle>> porPersona = new HashMap<>();
        for (RespuestaEncuesta r : respuestaRepository.findIdentificadasDeEncuesta(encuestaId)) {
            PreguntaEncuesta p = r.getPregunta();
            porPersona.computeIfAbsent(r.getPersona().getId(), k -> new ArrayList<>())
                    .add(new RespuestasParticipanteResponse.Detalle(p.getId(), p.getOrden(), p.getTexto(),
                            r.getRespuesta()));
        }

        List<RespuestasParticipanteResponse> resultado = new ArrayList<>();
        for (ParticipacionEncuesta participacion : participacionRepository.findByEncuestaConPersona(encuestaId)) {
            Persona persona = participacion.getPersona();
            resultado.add(new RespuestasParticipanteResponse(
                    persona.getId(),
                    nombreDe(persona),
                    persona.getNumeroDocumento(),
                    persona.getCorreo(),
                    participacion.getFechaRespuesta(),
                    porPersona.getOrDefault(persona.getId(), List.of())));
        }
        return resultado;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Consolidación (Criterio 4)
    // ──────────────────────────────────────────────────────────────────────────────

    private static ResultadoPreguntaResponse resultadoEscala(PreguntaEncuesta p, Map<Integer, Long> conteos,
                                                             long respondieronEncuesta) {
        int minimo = p.getEscalaMinima() != null ? p.getEscalaMinima() : ESCALA_MINIMA_POR_DEFECTO;
        int maximo = p.getEscalaMaxima() != null ? p.getEscalaMaxima() : ESCALA_MAXIMA_POR_DEFECTO;
        long total = conteos.values().stream().mapToLong(Long::longValue).sum();
        long suma = conteos.entrySet().stream().mapToLong(e -> (long) e.getKey() * e.getValue()).sum();

        List<ConteoResultadoResponse> distribucion = new ArrayList<>();
        for (int valor = minimo; valor <= maximo; valor++) {
            long cantidad = conteos.getOrDefault(valor, 0L);
            distribucion.add(new ConteoResultadoResponse(valor, null, String.valueOf(valor), cantidad,
                    total > 0 ? porcentaje(cantidad, total) : BigDecimal.ZERO.setScale(1)));
        }
        BigDecimal promedio = total > 0
                ? BigDecimal.valueOf(suma).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : null;
        return new ResultadoPreguntaResponse(p.getId(), p.getOrden(), p.getTexto(), p.getTipoRespuesta(),
                p.isObligatoria(), total, Math.max(0, respondieronEncuesta - total), minimo, maximo, promedio,
                distribucion, null);
    }

    private static ResultadoPreguntaResponse resultadoOpcion(PreguntaEncuesta p, Map<Long, Long> porOpcion,
                                                             long respondieronEncuesta) {
        long total = p.getOpciones().stream().mapToLong(o -> porOpcion.getOrDefault(o.getId(), 0L)).sum();
        List<ConteoResultadoResponse> distribucion = new ArrayList<>();
        for (OpcionPreguntaEncuesta o : p.getOpciones()) {
            long cantidad = porOpcion.getOrDefault(o.getId(), 0L);
            distribucion.add(new ConteoResultadoResponse(null, o.getId(), o.getTexto(), cantidad,
                    total > 0 ? porcentaje(cantidad, total) : BigDecimal.ZERO.setScale(1)));
        }
        return new ResultadoPreguntaResponse(p.getId(), p.getOrden(), p.getTexto(), p.getTipoRespuesta(),
                p.isObligatoria(), total, Math.max(0, respondieronEncuesta - total), null, null, null,
                distribucion, null);
    }

    private static ResultadoPreguntaResponse resultadoTexto(PreguntaEncuesta p, List<RespuestaEncuesta> respuestas,
                                                            long respondieronEncuesta, boolean anonima) {
        List<RespuestaTextoResponse> textos = respuestas.stream()
                .map(r -> new RespuestaTextoResponse(
                        r.getRespuesta(),
                        r.getFechaRespuesta() != null ? r.getFechaRespuesta().toLocalDate() : null,
                        anonima || r.getPersona() == null ? null : nombreDe(r.getPersona())))
                .toList();
        return new ResultadoPreguntaResponse(p.getId(), p.getOrden(), p.getTexto(), p.getTipoRespuesta(),
                p.isObligatoria(), textos.size(), Math.max(0, respondieronEncuesta - textos.size()), null, null, null,
                null, textos);
    }

    static BigDecimal porcentaje(long parte, long total) {
        return BigDecimal.valueOf(parte * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Validación de la configuración
    // ──────────────────────────────────────────────────────────────────────────────

    private void aplicarConfiguracion(Encuesta encuesta, Long eventoId, EncuestaRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();

        Actividad actividad = null;
        if (request.actividadId() != null) {
            actividad = actividadRepository.findByIdAndEvento_Id(request.actividadId(), eventoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró la actividad con ID " + request.actividadId() + " en este evento."));
            if (actividad.estaCancelada() || actividad.estaPorProgramar()) {
                errores.put("actividadId", "La actividad «" + actividad.getNombre() + "» "
                        + (actividad.estaCancelada() ? "está cancelada" : "aún no está programada")
                        + "; no se le puede aplicar una encuesta.");
            }
        }

        if (request.fechaCierre() != null && !request.fechaCierre().isAfter(ahora())) {
            errores.put("fechaCierre", "La fecha de cierre debe ser posterior a la fecha y hora actual.");
        }

        List<PreguntaEncuesta> preguntas = new ArrayList<>();
        List<PreguntaEncuestaRequest> solicitadas = request.preguntas() != null ? request.preguntas() : List.of();
        for (int i = 0; i < solicitadas.size(); i++) {
            PreguntaEncuestaRequest pr = solicitadas.get(i);
            String campo = "preguntas[" + i + "]";
            if (pr == null) {
                errores.put(campo, "La pregunta no puede estar vacía.");
                continue;
            }
            preguntas.add(construirPregunta(pr, i + 1, campo, errores));
        }

        if (!errores.isEmpty()) {
            throw new EncuestaException(EncuestaException.CONFIGURACION_INVALIDA, HttpStatus.BAD_REQUEST,
                    "Revise la configuración de la encuesta.", errores);
        }

        encuesta.setTitulo(request.titulo().trim());
        encuesta.setDescripcion(textoOpcional(request.descripcion()));
        encuesta.setActividad(actividad);
        encuesta.setAnonima(Boolean.TRUE.equals(request.anonima()));
        encuesta.setFechaCierre(request.fechaCierre());
        preguntas.forEach(encuesta::agregarPregunta);
    }

    private static PreguntaEncuesta construirPregunta(PreguntaEncuestaRequest pr, int orden, String campo,
                                                      Map<String, String> errores) {
        TipoRespuestaEncuesta tipo = pr.tipoRespuesta();
        PreguntaEncuesta pregunta = PreguntaEncuesta.builder()
                .texto(pr.texto() != null ? pr.texto().trim() : null)
                .tipoRespuesta(tipo)
                .orden(orden)
                .obligatoria(pr.obligatoria() == null || pr.obligatoria())
                .build();
        List<String> opciones = pr.opciones() != null ? pr.opciones() : List.of();

        if (tipo == TipoRespuestaEncuesta.escala) {
            int minimo = pr.escalaMinima() != null ? pr.escalaMinima() : ESCALA_MINIMA_POR_DEFECTO;
            int maximo = pr.escalaMaxima() != null ? pr.escalaMaxima() : ESCALA_MAXIMA_POR_DEFECTO;
            if (minimo < 0 || maximo > 10 || minimo >= maximo) {
                errores.put(campo + ".escalaMaxima",
                        "La escala debe ir de un mínimo a un máximo mayor, entre 0 y 10 (por ejemplo, 1 a 5).");
            }
            pregunta.setEscalaMinima(minimo);
            pregunta.setEscalaMaxima(maximo);
        } else if (pr.escalaMinima() != null || pr.escalaMaxima() != null) {
            errores.put(campo + ".escalaMinima", "Solo las preguntas de escala llevan rango.");
        }

        if (tipo == TipoRespuestaEncuesta.opcion) {
            Set<String> vistas = new HashSet<>();
            int ordenOpcion = 1;
            for (int j = 0; j < opciones.size(); j++) {
                String texto = opciones.get(j) != null ? opciones.get(j).trim() : "";
                if (texto.isEmpty()) {
                    errores.put(campo + ".opciones[" + j + "]", "La opción no puede estar vacía.");
                } else if (texto.length() > LARGO_OPCION) {
                    errores.put(campo + ".opciones[" + j + "]",
                            "La opción no puede superar " + LARGO_OPCION + " caracteres.");
                } else if (!vistas.add(texto.toLowerCase(Locale.ROOT))) {
                    errores.put(campo + ".opciones[" + j + "]", "La opción «" + texto + "» está repetida.");
                } else {
                    pregunta.agregarOpcion(OpcionPreguntaEncuesta.builder().texto(texto).orden(ordenOpcion++).build());
                }
            }
            if (opciones.size() < MINIMO_OPCIONES || opciones.size() > MAXIMO_OPCIONES) {
                errores.put(campo + ".opciones",
                        "Una pregunta de opción necesita entre " + MINIMO_OPCIONES + " y " + MAXIMO_OPCIONES
                                + " opciones.");
            }
        } else if (!opciones.isEmpty()) {
            errores.put(campo + ".opciones", "Solo las preguntas de opción llevan opciones.");
        }
        return pregunta;
    }

    private static void validarActividadDisponible(Actividad actividad) {
        if (actividad.estaCancelada() || actividad.estaPorProgramar()) {
            throw new EncuestaException(EncuestaException.ACTIVIDAD_NO_DISPONIBLE, HttpStatus.CONFLICT,
                    "La actividad «" + actividad.getNombre() + "» "
                            + (actividad.estaCancelada() ? "está cancelada" : "aún no está programada")
                            + "; no se puede publicar su encuesta.");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Auxiliares
    // ──────────────────────────────────────────────────────────────────────────────

    /** Personas que pueden responder: asistentes del evento (encuesta general) o de la actividad. */
    long totalHabilitados(Encuesta encuesta) {
        return encuesta.getActividad() != null
                ? asistenciaRepository.contarPersonasConAsistenciaEnActividad(encuesta.getActividad().getId())
                : asistenciaRepository.contarPersonasConAsistenciaEnEvento(encuesta.getEvento().getId());
    }

    private EncuestaResponse aResponse(Encuesta e) {
        Actividad actividad = e.getActividad();
        Evento evento = e.getEvento();
        List<PreguntaEncuestaResponse> preguntas = e.getPreguntas().stream()
                .map(PreguntaEncuestaResponse::fromEntity)
                .toList();
        return new EncuestaResponse(
                e.getId(),
                evento.getId(),
                evento.getNombre(),
                actividad != null ? actividad.getId() : null,
                actividad != null ? actividad.getNombre() : null,
                ambito(e),
                e.getTitulo(),
                e.getDescripcion(),
                e.isAnonima(),
                e.getEstado(),
                e.recibeRespuestas(ahora()),
                e.getFechaCreacion(),
                e.getFechaPublicacion(),
                e.getFechaCierre(),
                e.getId() != null ? totalHabilitados(e) : 0,
                e.getId() != null ? participacionRepository.countByEncuesta_Id(e.getId()) : 0,
                preguntas);
    }

    static String ambito(Encuesta e) {
        return e.getActividad() != null ? "actividad" : "evento";
    }

    private Encuesta buscar(Long eventoId, Long encuestaId) {
        return encuestaRepository.findByIdAndEvento_Id(encuestaId, eventoId)
                .orElseThrow(() -> noEncontrada(encuestaId));
    }

    /** Bloquea la fila de la encuesta y la devuelve con su evento y actividad cargados. */
    private Encuesta bloquear(Long eventoId, Long encuestaId) {
        return encuestaRepository.findByIdAndEvento_IdParaActualizar(encuestaId, eventoId)
                .orElseThrow(() -> noEncontrada(encuestaId));
    }

    private static RecursoNoEncontradoException noEncontrada(Long encuestaId) {
        return new RecursoNoEncontradoException("No se encontró la encuesta con ID " + encuestaId + " en este evento.");
    }

    private Map<String, Object> resumenAuditoria(Encuesta e) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("eventoId", e.getEvento() != null ? e.getEvento().getId() : null);
        datos.put("actividadId", e.getActividad() != null ? e.getActividad().getId() : null);
        datos.put("titulo", e.getTitulo());
        datos.put("anonima", e.isAnonima());
        datos.put("estado", e.getEstado() != null ? e.getEstado().name() : null);
        datos.put("fechaCierre", e.getFechaCierre());
        datos.put("preguntas", e.getPreguntas().size());
        return datos;
    }

    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName()).orElse(null);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    private static String textoOpcional(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    static String nombreDe(Persona p) {
        if (p == null) {
            return null;
        }
        String nombre = ((p.getNombres() != null ? p.getNombres() : "") + " "
                + (p.getApellidos() != null ? p.getApellidos() : "")).trim();
        return nombre.isEmpty() ? p.getCorreo() : nombre;
    }

    static String formatear(LocalDateTime fecha) {
        return fecha == null ? "sin fecha" : fecha.format(FORMATO_FECHA_HORA);
    }

    private static Long aLong(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }

    private static Integer aInteger(Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }
}
