package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.lineatematica.LineaTematicaRequest;
import com.sigea.demosigea_backend.dto.lineatematica.LineaTematicaResponse;
import com.sigea.demosigea_backend.exception.LineaTematicaDuplicadaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.LineaTematica;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.LineaTematicaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la gestión de líneas temáticas de eventos (HU-05).
 * <ul>
 *   <li><b>Criterio 1:</b> registrar datos requeridos de una línea temática en un evento y dejarla disponible.</li>
 *   <li><b>Criterio 2:</b> rechazar el registro con un mensaje explicativo si el nombre está vacío o ya existe en el evento.</li>
 *   <li><b>Criterio 3:</b> modificar datos de la línea temática actualizando la información sin afectar registros o propuestas históricas.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineaTematicaService {

    static final String ENTIDAD_LINEA_TEMATICA = "lineas_tematicas";

    private final LineaTematicaRepository lineaTematicaRepository;
    private final EventoRepository eventoRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Lista todas las líneas temáticas asociadas a un evento.
     *
     * @param eventoId ID del evento.
     * @return Lista de líneas temáticas del evento.
     */
    @Transactional(readOnly = true)
    public List<LineaTematicaResponse> listarPorEvento(Long eventoId) {
        buscarEvento(eventoId);
        return lineaTematicaRepository.findByEvento_IdOrderByIdAsc(eventoId)
                .stream()
                .map(LineaTematicaResponse::fromEntity)
                .toList();
    }

    /**
     * Obtiene los detalles de una línea temática por su ID.
     *
     * @param id ID de la línea temática.
     * @return DTO con la información de la línea temática.
     */
    @Transactional(readOnly = true)
    public LineaTematicaResponse obtenerPorId(Long id) {
        LineaTematica linea = buscarLineaTematica(id);
        return LineaTematicaResponse.fromEntity(linea);
    }

    /**
     * Crea una nueva línea temática dentro de un evento (Criterio 1 y 2).
     *
     * @param eventoIdIdPath ID del evento especificado en la ruta (opcional si viene en el request).
     * @param request        Datos de la línea temática a registrar.
     * @return Línea temática creada.
     */
    @Transactional
    public LineaTematicaResponse crear(Long eventoIdIdPath, LineaTematicaRequest request) {
        Long eventoIdFinal = eventoIdIdPath != null ? eventoIdIdPath : request.eventoId();
        if (eventoIdFinal == null) {
            throw new IllegalArgumentException("Debe especificar el ID del evento.");
        }

        Evento evento = buscarEvento(eventoIdFinal);

        String nombreLimpio = request.nombre().trim();
        if (lineaTematicaRepository.existsByEvento_IdAndNombreIgnoreCase(evento.getId(), nombreLimpio)) {
            throw new LineaTematicaDuplicadaException(String.format(
                    "Ya existe una línea temática con el nombre '%s' registrada para el evento '%s'.",
                    nombreLimpio, evento.getNombre()));
        }

        LineaTematica linea = LineaTematica.builder()
                .evento(evento)
                .nombre(nombreLimpio)
                .descripcion(request.descripcion() != null ? request.descripcion().trim() : null)
                .build();

        LineaTematica guardada = lineaTematicaRepository.save(linea);

        auditoriaService.registrar(TipoOperacionAuditoria.LINEA_TEMATICA_CREADA, ENTIDAD_LINEA_TEMATICA, guardada.getId(),
                DetalleAuditoria.de(
                        "eventoId", evento.getId(),
                        "nombre", guardada.getNombre(),
                        "descripcion", guardada.getDescripcion()));

        log.info("Línea temática creada: ID {}, evento {}, nombre '{}'", guardada.getId(), evento.getId(), guardada.getNombre());
        return LineaTematicaResponse.fromEntity(guardada);
    }

    /**
     * Modifica los datos de una línea temática (Criterio 3).
     * Preserva la clave primaria y las asociaciones con propuestas y actividades ya existentes.
     *
     * @param id      ID de la línea temática a actualizar.
     * @param request Nuevos datos de la línea temática.
     * @return Línea temática actualizada.
     */
    @Transactional
    public LineaTematicaResponse actualizar(Long id, LineaTematicaRequest request) {
        LineaTematica linea = buscarLineaTematica(id);
        Evento evento = linea.getEvento();

        String nuevoNombre = request.nombre().trim();
        if (lineaTematicaRepository.existsByEvento_IdAndNombreIgnoreCaseAndIdNot(evento.getId(), nuevoNombre, id)) {
            throw new LineaTematicaDuplicadaException(String.format(
                    "Ya existe otra línea temática con el nombre '%s' en el evento '%s'.",
                    nuevoNombre, evento.getNombre()));
        }

        linea.setNombre(nuevoNombre);
        linea.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);

        LineaTematica actualizada = lineaTematicaRepository.save(linea);

        auditoriaService.registrar(TipoOperacionAuditoria.LINEA_TEMATICA_ACTUALIZADA, ENTIDAD_LINEA_TEMATICA, actualizada.getId(),
                DetalleAuditoria.de(
                        "eventoId", evento.getId(),
                        "nombre", actualizada.getNombre(),
                        "descripcion", actualizada.getDescripcion()));

        log.info("Línea temática actualizada: ID {}, evento {}, nuevo nombre '{}'", actualizada.getId(), evento.getId(), actualizada.getNombre());
        return LineaTematicaResponse.fromEntity(actualizada);
    }

    /**
     * Elimina una línea temática si no está siendo utilizada.
     *
     * @param id ID de la línea temática a eliminar.
     */
    @Transactional
    public void eliminar(Long id) {
        LineaTematica linea = buscarLineaTematica(id);
        Long eventoId = linea.getEvento().getId();
        String nombre = linea.getNombre();

        if (lineaTematicaRepository.estaEnUso(id)) {
            throw new OperacionNoPermitidaException(String.format(
                    "No se puede eliminar la línea temática '%s' (ID %d) porque ya se encuentra vinculada a propuestas o actividades.",
                    nombre, id));
        }

        lineaTematicaRepository.delete(linea);

        auditoriaService.registrar(TipoOperacionAuditoria.LINEA_TEMATICA_ELIMINADA, ENTIDAD_LINEA_TEMATICA, id,
                DetalleAuditoria.de("eventoId", eventoId, "nombre", nombre));

        log.info("Línea temática eliminada: ID {}, evento {}", id, eventoId);
    }

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + id));
    }

    private LineaTematica buscarLineaTematica(Long id) {
        return lineaTematicaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la línea temática con ID: " + id));
    }
}
