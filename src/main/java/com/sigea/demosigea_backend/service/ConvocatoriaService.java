package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.convocatoria.CambioFechaCierreResponse;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaRequest;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.exception.ConvocatoriaCerradaException;
import com.sigea.demosigea_backend.exception.ConvocatoriaIncompletaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicio de negocio para la gestión de convocatorias asociadas a un evento.
 * <ul>
 *   <li><b>Criterio 1:</b> Creación de convocatoria en estado borrador asociando evento y fechas.</li>
 *   <li><b>Criterio 2:</b> Validación de campos obligatorios y fechas (cierre > apertura).</li>
 *   <li><b>Criterio 3:</b> Edición de contenido y fechas sobre convocatorias en borrador sin publicar.</li>
 *   <li><b>Criterio 4:</b> Validación automática de impedimento de envío de propuestas tras cumplirse la fecha de cierre.</li>
 * </ul>
 * <p><b>HU-11 (RF14) — Publicar convocatorias:</b></p>
 * <ul>
 *   <li><b>Criterio 1:</b> una convocatoria completa en borrador pasa a 'publicada' y queda visible en la
 *       consulta pública ({@link #listarPublicadas(Long)}, {@link #obtenerPublicada(Long)}).</li>
 *   <li><b>Criterio 2:</b> la fecha límite de una convocatoria publicada puede modificarse antes de que expire,
 *       notificando en el sistema (campana) a todos los usuarios activos que aún pueden enviar propuestas ({@link #actualizarFechaCierre(Long, LocalDateTime)}).</li>
 *   <li><b>Criterio 3:</b> no se publica una convocatoria sin fecha límite de recepción; se indica el dato a completar.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvocatoriaService {

    private static final String ENTIDAD = "convocatorias";

    /** Formato legible de fecha y hora para los mensajes de notificación (ej. 30/11/2026 23:59). */
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Estados del evento en los que su convocatoria puede divulgarse externamente: el evento debe estar
     * aprobado/publicado en el sistema antes de ofertarse (regla de negocio RN-04).
     */
    private static final Set<EstadoEvento> ESTADOS_EVENTO_DIVULGABLE =
            EnumSet.of(EstadoEvento.habilitado, EstadoEvento.en_ejecucion);

    private final ConvocatoriaRepository convocatoriaRepository;
    private final EventoRepository eventoRepository;
    private final AuditoriaService auditoriaService;
    private final NotificacionService notificacionService;
    private final Clock clock;

    /**
     * Consulta el listado de convocatorias filtrando opcionalmente por evento o estado.
     *
     * @param eventoId ID opcional del evento.
     * @param estado   Estado opcional de la convocatoria.
     * @return Listado de DTOs de respuesta.
     */
    @Transactional(readOnly = true)
    public List<ConvocatoriaResponse> listar(Long eventoId, EstadoConvocatoria estado) {
        List<Convocatoria> convocatorias;
        if (eventoId != null && estado != null) {
            convocatorias = convocatoriaRepository.findByEvento_IdAndEstadoOrderByIdDesc(eventoId, estado);
        } else if (eventoId != null) {
            convocatorias = convocatoriaRepository.findByEvento_IdOrderByIdDesc(eventoId);
        } else if (estado != null) {
            convocatorias = convocatoriaRepository.findByEstadoOrderByIdDesc(estado);
        } else {
            convocatorias = convocatoriaRepository.findAllByOrderByIdDesc();
        }
        return convocatorias.stream().map(ConvocatoriaResponse::fromEntity).toList();
    }

    /**
     * Consulta una convocatoria específica por su ID.
     *
     * @param id Identificador de la convocatoria.
     * @return DTO de la convocatoria encontrada.
     * @throws RecursoNoEncontradoException si no existe.
     */
    @Transactional(readOnly = true)
    public ConvocatoriaResponse obtenerPorId(Long id) {
        Convocatoria convocatoria = buscarPorId(id);
        return ConvocatoriaResponse.fromEntity(convocatoria);
    }

    /**
     * Crea una nueva convocatoria asociada a un evento (Criterio 1).
     * Mantiene las fechas validadas (Criterio 2) y guarda en estado 'borrador'.
     *
     * @param request Datos de la convocatoria.
     * @return Convocatoria creada.
     * @throws RecursoNoEncontradoException si el evento no existe.
     * @throws SolicitudInvalidaException si la fecha de cierre es anterior o igual a la de apertura.
     */
    @Transactional
    public ConvocatoriaResponse crear(ConvocatoriaRequest request) {
        if (request.eventoId() == null) {
            throw new SolicitudInvalidaException("El ID del evento es obligatorio.");
        }
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new SolicitudInvalidaException("El título de la convocatoria es obligatorio.");
        }
        validarFechas(request.fechaApertura(), request.fechaCierre());

        Evento evento = eventoRepository.findById(request.eventoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + request.eventoId()));

        Convocatoria convocatoria = Convocatoria.builder()
                .evento(evento)
                .titulo(request.titulo().trim())
                .descripcion(request.descripcion())
                .requisitos(request.requisitos())
                .fechaApertura(request.fechaApertura())
                .fechaCierre(request.fechaCierre())
                .estado(EstadoConvocatoria.borrador) // Criterio 1: guarda SIEMPRE en borrador en creación
                .build();

        Convocatoria guardada = convocatoriaRepository.save(convocatoria);
        log.info("Convocatoria creada: ID {}, evento {}, estado borrador", guardada.getId(), evento.getId());
        return ConvocatoriaResponse.fromEntity(guardada);
    }

    /**
     * Edita el contenido o fechas de una convocatoria existente (Criterio 3).
     * Solo permite editar convocatorias que se encuentren en estado borrador;
     * una vez publicadas o cerradas no pueden modificarse para preservar la integridad
     * de los periodos de recepción ya comunicados.
     *
     * @param id      ID de la convocatoria.
     * @param request Nuevos datos de la convocatoria.
     * @return Convocatoria actualizada.
     * @throws RecursoNoEncontradoException  si la convocatoria o evento no existen.
     * @throws OperacionNoPermitidaException si la convocatoria no está en estado borrador.
     * @throws SolicitudInvalidaException    si las fechas o campos son inválidos.
     */
    @Transactional
    public ConvocatoriaResponse actualizar(Long id, ConvocatoriaRequest request) {
        if (request.eventoId() == null) {
            throw new SolicitudInvalidaException("El ID del evento es obligatorio.");
        }
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new SolicitudInvalidaException("El título de la convocatoria es obligatorio.");
        }
        validarFechas(request.fechaApertura(), request.fechaCierre());

        Convocatoria convocatoria = buscarPorId(id);

        // Criterio 3: solo se editan convocatorias en borrador
        if (convocatoria.getEstado() != EstadoConvocatoria.borrador) {
            throw new OperacionNoPermitidaException(String.format(
                    "Solo se pueden editar convocatorias en estado borrador. El estado actual es '%s'.",
                    convocatoria.getEstado()));
        }

        Evento evento = eventoRepository.findById(request.eventoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + request.eventoId()));

        convocatoria.setEvento(evento);
        convocatoria.setTitulo(request.titulo().trim());
        convocatoria.setDescripcion(request.descripcion());
        convocatoria.setRequisitos(request.requisitos());
        convocatoria.setFechaApertura(request.fechaApertura());
        convocatoria.setFechaCierre(request.fechaCierre());

        Convocatoria actualizada = convocatoriaRepository.save(convocatoria);
        log.info("Convocatoria actualizada: ID {}, estado {}", actualizada.getId(), actualizada.getEstado());
        return ConvocatoriaResponse.fromEntity(actualizada);
    }

    /**
     * Publica una convocatoria que se encuentra en estado borrador (HU-11, Criterio 1).
     * <p>
     * Antes de publicar verifica que la convocatoria esté completa (Criterio 3): título, fecha de apertura y
     * fecha límite de recepción de propuestas; que la fecha límite sea posterior a la apertura y aún no haya
     * pasado, y que el evento ya esté publicado (habilitado o en ejecución), porque un evento no puede
     * divulgarse externamente antes de estar aprobado (RN-04). Al publicarse queda visible en la consulta
     * pública y habilita la recepción de propuestas durante su vigencia.
     * </p>
     *
     * @param id ID de la convocatoria.
     * @return Convocatoria actualizada con estado 'publicada'.
     * @throws RecursoNoEncontradoException   si no existe.
     * @throws OperacionNoPermitidaException  si no está en borrador, su fecha límite ya pasó o el evento no está publicado.
     * @throws ConvocatoriaIncompletaException si falta algún dato obligatorio, como la fecha límite (Criterio 3).
     * @throws SolicitudInvalidaException     si la fecha límite no es posterior a la de apertura.
     */
    @Transactional
    public ConvocatoriaResponse publicar(Long id) {
        Convocatoria convocatoria = buscarPorId(id);
        if (convocatoria.getEstado() != EstadoConvocatoria.borrador) {
            throw new OperacionNoPermitidaException(String.format(
                    "Solo se pueden publicar convocatorias en estado borrador. El estado actual es '%s'.",
                    convocatoria.getEstado()));
        }

        // Criterio 3: la convocatoria debe estar completa, en especial la fecha límite de recepción.
        validarCompletaParaPublicar(convocatoria);
        validarFechas(convocatoria.getFechaApertura(), convocatoria.getFechaCierre());

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!convocatoria.getFechaCierre().isAfter(ahora)) {
            throw new OperacionNoPermitidaException(String.format(
                    "No es posible publicar la convocatoria '%s' porque su fecha límite de recepción (%s) ya pasó. "
                            + "Actualice la fecha límite antes de publicarla.",
                    convocatoria.getTitulo(), convocatoria.getFechaCierre()));
        }

        Evento evento = convocatoria.getEvento();
        if (evento == null || !ESTADOS_EVENTO_DIVULGABLE.contains(evento.getEstado())) {
            throw new OperacionNoPermitidaException(String.format(
                    "No es posible publicar la convocatoria '%s' porque el evento '%s' no está publicado (estado actual: %s). "
                            + "El evento debe estar aprobado y habilitado antes de divulgar su convocatoria.",
                    convocatoria.getTitulo(),
                    evento != null ? evento.getNombre() : "-",
                    evento != null ? evento.getEstado() : "-"));
        }

        convocatoria.setEstado(EstadoConvocatoria.publicada);
        Convocatoria publicada = convocatoriaRepository.save(convocatoria);

        auditoriaService.registrar(TipoOperacionAuditoria.CONVOCATORIA_PUBLICADA, ENTIDAD, publicada.getId(),
                DetalleAuditoria.de(
                        "titulo", publicada.getTitulo(),
                        "eventoId", evento.getId(),
                        "fechaApertura", publicada.getFechaApertura(),
                        "fechaCierre", publicada.getFechaCierre()));

        log.info("Convocatoria publicada: ID {}, evento {}, cierre {}", publicada.getId(), evento.getId(),
                publicada.getFechaCierre());
        return ConvocatoriaResponse.fromEntity(publicada);
    }

    /**
     * Modifica la fecha límite de recepción de propuestas de una convocatoria publicada (HU-11, Criterio 2).
     * <p>
     * Solo se permite mientras la convocatoria no haya expirado. La nueva fecha debe ser futura y posterior
     * a la de apertura. En la misma transacción se crea una notificación (campana del frontend) para todos los
     * usuarios con cuenta activa y correo verificado, avisando que aún pueden enviar propuestas. Si algo falla,
     * no queda ni el cambio de fecha ni las notificaciones.
     * </p>
     *
     * @param id          ID de la convocatoria.
     * @param nuevaFecha  Nueva fecha límite de recepción de propuestas.
     * @return Convocatoria actualizada, fecha anterior y número de usuarios notificados.
     * @throws RecursoNoEncontradoException  si la convocatoria no existe.
     * @throws OperacionNoPermitidaException si la convocatoria no está publicada.
     * @throws ConvocatoriaCerradaException  si la fecha límite actual ya expiró.
     * @throws SolicitudInvalidaException    si la nueva fecha es nula, pasada, igual a la actual o no posterior a la apertura.
     */
    @Transactional
    public CambioFechaCierreResponse actualizarFechaCierre(Long id, LocalDateTime nuevaFecha) {
        if (nuevaFecha == null) {
            throw new SolicitudInvalidaException("La nueva fecha límite de recepción de propuestas es obligatoria.");
        }

        Convocatoria convocatoria = buscarPorId(id);
        if (convocatoria.getEstado() != EstadoConvocatoria.publicada) {
            throw new OperacionNoPermitidaException(String.format(
                    "Solo se puede modificar la fecha límite de convocatorias publicadas. El estado actual es '%s'. "
                            + "Si está en borrador, edítela con la actualización general.",
                    convocatoria.getEstado()));
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime anterior = convocatoria.getFechaCierre();
        if (anterior != null && ahora.isAfter(anterior)) {
            throw new ConvocatoriaCerradaException(String.format(
                    "La convocatoria '%s' ya expiró (%s). No es posible modificar su fecha límite.",
                    convocatoria.getTitulo(), anterior));
        }

        if (nuevaFecha.equals(anterior)) {
            throw new SolicitudInvalidaException("La nueva fecha límite es igual a la actual.");
        }
        if (!nuevaFecha.isAfter(ahora)) {
            throw new SolicitudInvalidaException("La nueva fecha límite debe ser posterior a la fecha y hora actual.");
        }
        validarFechas(convocatoria.getFechaApertura(), nuevaFecha);

        convocatoria.setFechaCierre(nuevaFecha);
        Convocatoria actualizada = convocatoriaRepository.save(convocatoria);

        String nombreEvento = actualizada.getEvento() != null ? actualizada.getEvento().getNombre() : null;
        int notificados = notificacionService.notificarATodos(
                TipoNotificacion.CONVOCATORIA_FECHA_MODIFICADA,
                "Nueva fecha límite: " + actualizada.getTitulo(),
                mensajeCambioFechaCierre(actualizada.getTitulo(), nombreEvento, anterior, nuevaFecha),
                ENTIDAD,
                actualizada.getId());

        auditoriaService.registrar(TipoOperacionAuditoria.CONVOCATORIA_FECHA_CIERRE_MODIFICADA, ENTIDAD,
                actualizada.getId(),
                DetalleAuditoria.de(
                        "titulo", actualizada.getTitulo(),
                        "fechaCierreAnterior", anterior,
                        "fechaCierreNueva", nuevaFecha,
                        "usuariosNotificados", notificados));

        log.info("Fecha límite de convocatoria {} modificada: {} -> {}. Usuarios notificados: {}",
                actualizada.getId(), anterior, nuevaFecha, notificados);
        return new CambioFechaCierreResponse(ConvocatoriaResponse.fromEntity(actualizada), anterior, notificados);
    }

    /**
     * Consulta pública de convocatorias publicadas, opcionalmente filtradas por evento (HU-11, Criterio 1).
     * No requiere autenticación: es lo que ven los autores y usuarios externos.
     *
     * @param eventoId ID opcional del evento.
     * @return Convocatorias en estado 'publicada'.
     */
    @Transactional(readOnly = true)
    public List<ConvocatoriaResponse> listarPublicadas(Long eventoId) {
        List<Convocatoria> convocatorias = eventoId != null
                ? convocatoriaRepository.findByEvento_IdAndEstadoOrderByIdDesc(eventoId, EstadoConvocatoria.publicada)
                : convocatoriaRepository.findByEstadoOrderByIdDesc(EstadoConvocatoria.publicada);
        return convocatorias.stream().map(ConvocatoriaResponse::fromEntity).toList();
    }

    /**
     * Consulta pública de una convocatoria (HU-11, Criterio 1). Las convocatorias en borrador o cerradas
     * no son visibles para usuarios externos y se responden como no encontradas.
     *
     * @param id ID de la convocatoria.
     * @return Convocatoria publicada.
     * @throws RecursoNoEncontradoException si no existe o no está publicada.
     */
    @Transactional(readOnly = true)
    public ConvocatoriaResponse obtenerPublicada(Long id) {
        return convocatoriaRepository.findByIdAndEstado(id, EstadoConvocatoria.publicada)
                .map(ConvocatoriaResponse::fromEntity)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró una convocatoria publicada con ID: " + id));
    }

    /**
     * Elimina una convocatoria del sistema.
     *
     * @param id ID de la convocatoria a eliminar.
     * @throws RecursoNoEncontradoException si no existe.
     */
    @Transactional
    public void eliminar(Long id) {
        Convocatoria convocatoria = buscarPorId(id);
        convocatoriaRepository.delete(convocatoria);
        log.info("Convocatoria eliminada: ID {}", id);
    }

    /**
     * Valida si la convocatoria está habilitada y en periodo vigente para la recepción de propuestas (Criterio 4).
     * Si la fecha de cierre ya se cumplió o la convocatoria no está abierta, impide la operación automáticamente.
     *
     * @param id ID de la convocatoria.
     * @throws RecursoNoEncontradoException si no existe.
     * @throws ConvocatoriaCerradaException si la fecha de cierre ya pasó (Criterio 4).
     * @throws OperacionNoPermitidaException si la convocatoria no está publicada o aún no abre.
     */
    @Transactional(readOnly = true)
    public void validarRecepcionPropuestas(Long id) {
        Convocatoria convocatoria = buscarPorId(id);
        LocalDateTime ahora = LocalDateTime.now();

        if (convocatoria.getFechaCierre() != null && ahora.isAfter(convocatoria.getFechaCierre())) {
            throw new ConvocatoriaCerradaException(String.format(
                    "La fecha de cierre de la convocatoria '%s' ya se cumplió (%s). El sistema impide automáticamente el envío de propuestas.",
                    convocatoria.getTitulo(), convocatoria.getFechaCierre()));
        }

        if (convocatoria.getEstado() != EstadoConvocatoria.publicada) {
            throw new OperacionNoPermitidaException(String.format(
                    "La convocatoria '%s' se encuentra en estado '%s' y no está publicada para recepción de propuestas.",
                    convocatoria.getTitulo(), convocatoria.getEstado()));
        }

        if (convocatoria.getFechaApertura() != null && ahora.isBefore(convocatoria.getFechaApertura())) {
            throw new OperacionNoPermitidaException(String.format(
                    "La convocatoria '%s' aún no abre el periodo de recepción de propuestas (%s).",
                    convocatoria.getTitulo(), convocatoria.getFechaApertura()));
        }
    }

    /**
     * Valida que la fecha de cierre sea estrictamente posterior a la de apertura (Criterio 2).
     */
    private void validarFechas(LocalDateTime apertura, LocalDateTime cierre) {
        if (apertura == null || cierre == null || !cierre.isAfter(apertura)) {
            throw new SolicitudInvalidaException(
                    "La fecha de cierre debe ser estrictamente posterior a la fecha de apertura.");
        }
    }

    /**
     * HU-11, Criterio 3: verifica que la convocatoria tenga los datos obligatorios para publicarse.
     * Reúne todos los faltantes en una sola respuesta para que el organizador los complete de una vez.
     */
    private void validarCompletaParaPublicar(Convocatoria convocatoria) {
        Map<String, String> faltantes = new LinkedHashMap<>();
        if (convocatoria.getTitulo() == null || convocatoria.getTitulo().isBlank()) {
            faltantes.put("titulo", "El título de la convocatoria es obligatorio para publicarla.");
        }
        if (convocatoria.getFechaApertura() == null) {
            faltantes.put("fechaApertura", "La fecha de apertura de recepción de propuestas es obligatoria para publicarla.");
        }
        if (convocatoria.getFechaCierre() == null) {
            faltantes.put("fechaCierre", "La fecha límite de recepción de propuestas es obligatoria para publicarla.");
        }
        if (!faltantes.isEmpty()) {
            String mensaje = faltantes.containsKey("fechaCierre")
                    ? "No es posible publicar la convocatoria sin fecha límite de recepción de propuestas. Complete el dato e intente de nuevo."
                    : "No es posible publicar la convocatoria: complete los datos obligatorios e intente de nuevo.";
            throw new ConvocatoriaIncompletaException(mensaje, faltantes);
        }
    }

    /** Texto de la notificación de cambio de fecha límite (HU-11, Criterio 2). */
    private static String mensajeCambioFechaCierre(String titulo, String nombreEvento,
                                                   LocalDateTime anterior, LocalDateTime nueva) {
        String evento = nombreEvento != null && !nombreEvento.isBlank() ? " del evento '" + nombreEvento + "'" : "";
        String desde = anterior != null ? " del " + anterior.format(FORMATO_FECHA_HORA) : "";
        return String.format("La fecha límite de recepción de propuestas de la convocatoria '%s'%s cambió%s al %s. "
                        + "Aún estás a tiempo de enviar tu propuesta.",
                titulo, evento, desde, nueva.format(FORMATO_FECHA_HORA));
    }

    private Convocatoria buscarPorId(Long id) {
        return convocatoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la convocatoria con ID: " + id));
    }
}
