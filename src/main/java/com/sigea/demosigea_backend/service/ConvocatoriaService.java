package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaRequest;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la gestión de convocatorias académicas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvocatoriaService {

    static final String ENTIDAD_CONVOCATORIA = "convocatorias";

    private final ConvocatoriaRepository convocatoriaRepository;
    private final EventoRepository eventoRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Lista convocatorias con filtros opcionales de evento y estado.
     *
     * @param eventoId ID del evento (opcional).
     * @param estado   Estado de la convocatoria (opcional).
     * @return Lista de convocatorias que cumplen con los criterios.
     */
    @Transactional(readOnly = true)
    public List<ConvocatoriaResponse> listar(Long eventoId, EstadoConvocatoria estado) {
        List<Convocatoria> convocatorias;
        if (eventoId != null && estado != null) {
            convocatorias = convocatoriaRepository.findByEvento_IdAndEstadoOrderByIdAsc(eventoId, estado);
        } else if (eventoId != null) {
            convocatorias = convocatoriaRepository.findByEvento_IdOrderByIdAsc(eventoId);
        } else if (estado != null) {
            convocatorias = convocatoriaRepository.findByEstadoOrderByIdDesc(estado);
        } else {
            convocatorias = convocatoriaRepository.findAllByOrderByIdDesc();
        }

        return convocatorias.stream()
                .map(ConvocatoriaResponse::fromEntity)
                .toList();
    }

    /**
     * Obtiene una convocatoria por su ID.
     *
     * @param id ID de la convocatoria.
     * @return DTO de la convocatoria.
     */
    @Transactional(readOnly = true)
    public ConvocatoriaResponse obtenerPorId(Long id) {
        Convocatoria convocatoria = buscarConvocatoria(id);
        return ConvocatoriaResponse.fromEntity(convocatoria);
    }

    /**
     * Crea una nueva convocatoria.
     *
     * @param eventoIdPath ID del evento especificado en el path URL (opcional).
     * @param request      Datos de la nueva convocatoria.
     * @return Convocatoria creada.
     */
    @Transactional
    public ConvocatoriaResponse crear(Long eventoIdPath, ConvocatoriaRequest request) {
        Long eventoIdFinal = eventoIdPath != null ? eventoIdPath : request.eventoId();
        if (eventoIdFinal == null) {
            throw new SolicitudInvalidaException("Debe especificar el ID del evento para crear la convocatoria.");
        }

        Evento evento = buscarEvento(eventoIdFinal);
        validarFechas(request);

        EstadoConvocatoria estado = request.estado() != null ? request.estado() : EstadoConvocatoria.borrador;

        Convocatoria convocatoria = Convocatoria.builder()
                .evento(evento)
                .titulo(request.titulo().trim())
                .descripcion(request.descripcion() != null ? request.descripcion().trim() : null)
                .requisitos(request.requisitos() != null ? request.requisitos().trim() : null)
                .fechaApertura(request.fechaApertura())
                .fechaCierre(request.fechaCierre())
                .estado(estado)
                .build();

        Convocatoria guardada = convocatoriaRepository.save(convocatoria);

        auditoriaService.registrar(TipoOperacionAuditoria.CONVOCATORIA_CREADA, ENTIDAD_CONVOCATORIA, guardada.getId(),
                DetalleAuditoria.de(
                        "eventoId", evento.getId(),
                        "titulo", guardada.getTitulo(),
                        "estado", guardada.getEstado()));

        log.info("Convocatoria creada: ID {}, evento {}, título '{}'", guardada.getId(), evento.getId(), guardada.getTitulo());
        return ConvocatoriaResponse.fromEntity(guardada);
    }

    /**
     * Actualiza una convocatoria existente.
     *
     * @param id      ID de la convocatoria a actualizar.
     * @param request Nuevos datos de la convocatoria.
     * @return Convocatoria actualizada.
     */
    @Transactional
    public ConvocatoriaResponse actualizar(Long id, ConvocatoriaRequest request) {
        Convocatoria convocatoria = buscarConvocatoria(id);
        validarFechas(request);

        convocatoria.setTitulo(request.titulo().trim());
        convocatoria.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        convocatoria.setRequisitos(request.requisitos() != null ? request.requisitos().trim() : null);
        convocatoria.setFechaApertura(request.fechaApertura());
        convocatoria.setFechaCierre(request.fechaCierre());
        if (request.estado() != null) {
            convocatoria.setEstado(request.estado());
        }

        Convocatoria actualizada = convocatoriaRepository.save(convocatoria);

        auditoriaService.registrar(TipoOperacionAuditoria.CONVOCATORIA_ACTUALIZADA, ENTIDAD_CONVOCATORIA, actualizada.getId(),
                DetalleAuditoria.de(
                        "eventoId", actualizada.getEvento().getId(),
                        "titulo", actualizada.getTitulo(),
                        "estado", actualizada.getEstado()));

        log.info("Convocatoria actualizada: ID {}, título '{}'", actualizada.getId(), actualizada.getTitulo());
        return ConvocatoriaResponse.fromEntity(actualizada);
    }

    /**
     * Elimina una convocatoria por ID.
     *
     * @param id ID de la convocatoria a eliminar.
     */
    @Transactional
    public void eliminar(Long id) {
        Convocatoria convocatoria = buscarConvocatoria(id);
        Long eventoId = convocatoria.getEvento().getId();
        String titulo = convocatoria.getTitulo();

        convocatoriaRepository.delete(convocatoria);

        auditoriaService.registrar(TipoOperacionAuditoria.CONVOCATORIA_ELIMINADA, ENTIDAD_CONVOCATORIA, id,
                DetalleAuditoria.de("eventoId", eventoId, "titulo", titulo));

        log.info("Convocatoria eliminada: ID {}, evento {}", id, eventoId);
    }

    private void validarFechas(ConvocatoriaRequest request) {
        if (!request.fechaCierre().isAfter(request.fechaApertura())) {
            throw new SolicitudInvalidaException(
                    "La fecha y hora de cierre debe ser posterior a la fecha y hora de apertura de la convocatoria.");
        }
    }

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + id));
    }

    private Convocatoria buscarConvocatoria(Long id) {
        return convocatoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la convocatoria con ID: " + id));
    }
}
