package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.parametro.LineaTematicaRequest;
import com.sigea.demosigea_backend.dto.parametro.LineaTematicaResponse;
import com.sigea.demosigea_backend.dto.parametro.UsoParametroResponse;
import com.sigea.demosigea_backend.exception.ParametroEventoDuplicadoException;
import com.sigea.demosigea_backend.exception.ParametroEventoEnUsoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.LineaTematica;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.LineaTematicaRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Servicio de negocio para las líneas temáticas de un evento (HU-05, RF07).
 * <ul>
 *   <li><b>Criterio 1:</b> crear (y editar) una línea indicando nombre y características; queda
 *       disponible para las convocatorias/propuestas y la agenda.</li>
 *   <li><b>Criterio 2:</b> si la línea está en uso (propuestas o actividades), se impide su
 *       eliminación y se informa el impacto. {@link #consultarUso} permite advertirlo antes.</li>
 *   <li><b>Criterio 3:</b> se rechaza un nombre ya existente en el evento, sin distinguir
 *       mayúsculas ni espacios sobrantes.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see TipoActividadService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineaTematicaService {

    /** Nombre de la entidad usado en el log de auditoría. */
    static final String ENTIDAD = "lineas_tematicas";

    /** Índices únicos sobre (evento_id, nombre): el original del esquema y el insensible a mayúsculas (changeset hu05-002). */
    static final Set<String> INDICES_NOMBRE = Set.of(
            "lineas_tematicas_evento_id_nombre_idx",
            "ux_lineas_tematicas_evento_nombre_ci");

    private static final String CATALOGO = "Las líneas temáticas";

    private final LineaTematicaRepository lineaRepository;
    private final EventoRepository eventoRepository;
    private final ActividadRepository actividadRepository;
    private final PropuestaRepository propuestaRepository;
    private final AuditoriaService auditoriaService;

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    /**
     * @param eventoId ID del evento.
     * @return Líneas temáticas del evento ordenadas por nombre.
     */
    @Transactional(readOnly = true)
    public List<LineaTematicaResponse> listar(Long eventoId) {
        buscarEvento(eventoId);
        return lineaRepository.findByEvento_IdOrderByNombreAscIdAsc(eventoId).stream()
                .map(LineaTematicaResponse::fromEntity)
                .toList();
    }

    /**
     * @return La línea temática indicada.
     * @throws RecursoNoEncontradoException si el evento o la línea no existen.
     */
    @Transactional(readOnly = true)
    public LineaTematicaResponse obtener(Long eventoId, Long lineaId) {
        buscarEvento(eventoId);
        return LineaTematicaResponse.fromEntity(buscarLinea(eventoId, lineaId));
    }

    /**
     * Criterio 2: informa en cuántas propuestas y actividades se usa la línea, para advertir del
     * impacto antes de eliminarla.
     */
    @Transactional(readOnly = true)
    public UsoParametroResponse consultarUso(Long eventoId, Long lineaId) {
        buscarEvento(eventoId);
        return calcularUso(buscarLinea(eventoId, lineaId));
    }

    // ------------------------------------------------------------------
    // Criterios 1 y 3: crear y actualizar
    // ------------------------------------------------------------------

    /**
     * Crea una línea temática en el evento.
     *
     * @throws ParametroEventoDuplicadoException si ya existe una línea con ese nombre (Criterio 3).
     */
    @Transactional
    public LineaTematicaResponse crear(Long eventoId, LineaTematicaRequest request) {
        Evento evento = buscarEvento(eventoId);
        ParametroEventoReglas.validarEventoModificable(evento, CATALOGO);

        String nombre = ParametroEventoReglas.normalizarNombre(request.nombre());
        if (lineaRepository.existsByEvento_IdAndNombreIgnoreCase(eventoId, nombre)) {
            throw duplicado(nombre, evento);
        }

        LineaTematica linea = LineaTematica.builder()
                .evento(evento)
                .nombre(nombre)
                .descripcion(ParametroEventoReglas.normalizarDescripcion(request.descripcion()))
                .build();
        LineaTematica guardada = guardarControlandoDuplicado(linea, evento);

        auditoriaService.registrar(TipoOperacionAuditoria.LINEA_TEMATICA_CREADA, ENTIDAD, guardada.getId(),
                DetalleAuditoria.de("eventoId", evento.getId(), "nombre", guardada.getNombre(),
                        "descripcion", guardada.getDescripcion()));

        log.info("Línea temática creada: {} '{}' en evento {}", guardada.getId(), guardada.getNombre(), evento.getId());
        return LineaTematicaResponse.fromEntity(guardada);
    }

    /**
     * Actualiza el nombre y la descripción de una línea temática. Renombrar una línea en uso está
     * permitido: propuestas y actividades la referencian por ID.
     *
     * @throws ParametroEventoDuplicadoException si el nuevo nombre ya existe en otra línea del evento.
     */
    @Transactional
    public LineaTematicaResponse actualizar(Long eventoId, Long lineaId, LineaTematicaRequest request) {
        Evento evento = buscarEvento(eventoId);
        ParametroEventoReglas.validarEventoModificable(evento, CATALOGO);
        LineaTematica linea = buscarLinea(eventoId, lineaId);

        String nombre = ParametroEventoReglas.normalizarNombre(request.nombre());
        if (lineaRepository.existsByEvento_IdAndNombreIgnoreCaseAndIdNot(eventoId, nombre, lineaId)) {
            throw duplicado(nombre, evento);
        }

        String nombreAnterior = linea.getNombre();
        String descripcionAnterior = linea.getDescripcion();
        linea.setNombre(nombre);
        linea.setDescripcion(ParametroEventoReglas.normalizarDescripcion(request.descripcion()));
        LineaTematica guardada = guardarControlandoDuplicado(linea, evento);

        auditoriaService.registrar(TipoOperacionAuditoria.LINEA_TEMATICA_ACTUALIZADA, ENTIDAD, guardada.getId(),
                DetalleAuditoria.de("eventoId", evento.getId(),
                        "antes", DetalleAuditoria.de("nombre", nombreAnterior, "descripcion", descripcionAnterior),
                        "despues", DetalleAuditoria.de("nombre", guardada.getNombre(), "descripcion", guardada.getDescripcion())));

        log.info("Línea temática actualizada: {} en evento {}", guardada.getId(), evento.getId());
        return LineaTematicaResponse.fromEntity(guardada);
    }

    // ------------------------------------------------------------------
    // Criterio 2: eliminar
    // ------------------------------------------------------------------

    /**
     * Elimina una línea temática que no esté en uso.
     *
     * @throws ParametroEventoEnUsoException si alguna propuesta o actividad usa la línea (Criterio 2).
     */
    @Transactional
    public void eliminar(Long eventoId, Long lineaId) {
        Evento evento = buscarEvento(eventoId);
        ParametroEventoReglas.validarEventoModificable(evento, CATALOGO);
        LineaTematica linea = buscarLinea(eventoId, lineaId);

        UsoParametroResponse uso = calcularUso(linea);
        if (!uso.eliminable()) {
            throw enUso(linea, uso);
        }

        try {
            lineaRepository.delete(linea);
            lineaRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // Una propuesta o actividad se registró con esta línea entre la verificación y el borrado.
            if (ParametroEventoReglas.esViolacionLlaveForanea(ex)) {
                log.warn("La llave foránea impidió eliminar la línea temática {}: está en uso. Se responde 409.", lineaId);
                throw enUso(linea, null);
            }
            throw ex;
        }

        auditoriaService.registrar(TipoOperacionAuditoria.LINEA_TEMATICA_ELIMINADA, ENTIDAD, lineaId,
                DetalleAuditoria.de("eventoId", evento.getId(), "nombre", linea.getNombre()));

        log.info("Línea temática eliminada: {} '{}' del evento {}", lineaId, linea.getNombre(), evento.getId());
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + id));
    }

    private LineaTematica buscarLinea(Long eventoId, Long lineaId) {
        return lineaRepository.findByIdAndEvento_Id(lineaId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "No se encontró la línea temática con ID %d en el evento %d.", lineaId, eventoId)));
    }

    private UsoParametroResponse calcularUso(LineaTematica linea) {
        long actividades = actividadRepository.countByLineaTematica_Id(linea.getId());
        long propuestas = propuestaRepository.countByLineaTematica_Id(linea.getId());
        return UsoParametroResponse.de(linea.getId(), linea.getNombre(), actividades, propuestas);
    }

    private LineaTematica guardarControlandoDuplicado(LineaTematica linea, Evento evento) {
        try {
            return lineaRepository.saveAndFlush(linea);
        } catch (DataIntegrityViolationException ex) {
            if (ParametroEventoReglas.esViolacionUnicidad(ex, INDICES_NOMBRE)) {
                log.warn("El índice único rechazó la línea temática duplicada '{}' en evento {}. Se responde 409.",
                        linea.getNombre(), evento.getId());
                throw duplicado(linea.getNombre(), evento);
            }
            throw ex;
        }
    }

    private static ParametroEventoDuplicadoException duplicado(String nombre, Evento evento) {
        return new ParametroEventoDuplicadoException(String.format(
                "Ya existe una línea temática llamada '%s' en el evento '%s'.", nombre, evento.getNombre()));
    }

    private static ParametroEventoEnUsoException enUso(LineaTematica linea, UsoParametroResponse uso) {
        String detalle = uso != null ? uso.describirUso() : "propuestas o actividades de la agenda";
        return new ParametroEventoEnUsoException(String.format(
                "No se puede eliminar la línea temática '%s' porque está en uso en %s. "
                        + "Reasigne esos registros a otra línea antes de eliminarla.",
                linea.getNombre(), detalle));
    }
}
