package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaRequest;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.exception.ConvocatoriaCerradaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de negocio para la gestión de convocatorias asociadas a un evento.
 * <ul>
 *   <li><b>Criterio 1:</b> Creación de convocatoria en estado borrador asociando evento y fechas.</li>
 *   <li><b>Criterio 2:</b> Validación de campos obligatorios y fechas (cierre > apertura).</li>
 *   <li><b>Criterio 3:</b> Edición de contenido y fechas sobre convocatorias en borrador sin publicar.</li>
 *   <li><b>Criterio 4:</b> Validación automática de impedimento de envío de propuestas tras cumplirse la fecha de cierre.</li>
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

    private final ConvocatoriaRepository convocatoriaRepository;
    private final EventoRepository eventoRepository;

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
                .estado(EstadoConvocatoria.borrador) // Criterio 1: guarda en borrador
                .build();

        Convocatoria guardada = convocatoriaRepository.save(convocatoria);
        log.info("Convocatoria creada: ID {}, evento {}, estado borrador", guardada.getId(), evento.getId());
        return ConvocatoriaResponse.fromEntity(guardada);
    }

    /**
     * Edita el contenido o fechas de una convocatoria existente (Criterio 3).
     *
     * @param id      ID de la convocatoria.
     * @param request Nuevos datos de la convocatoria.
     * @return Convocatoria actualizada.
     * @throws RecursoNoEncontradoException si la convocatoria o evento no existen.
     * @throws SolicitudInvalidaException si las fechas son inválidas.
     */
    @Transactional
    public ConvocatoriaResponse actualizar(Long id, ConvocatoriaRequest request) {
        validarFechas(request.fechaApertura(), request.fechaCierre());

        Convocatoria convocatoria = buscarPorId(id);
        Evento evento = eventoRepository.findById(request.eventoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + request.eventoId()));

        convocatoria.setEvento(evento);
        convocatoria.setTitulo(request.titulo().trim());
        convocatoria.setDescripcion(request.descripcion());
        convocatoria.setRequisitos(request.requisitos());
        convocatoria.setFechaApertura(request.fechaApertura());
        convocatoria.setFechaCierre(request.fechaCierre());
        // Criterio 3: actualiza información sin cambiar el estado a publicada

        Convocatoria actualizada = convocatoriaRepository.save(convocatoria);
        log.info("Convocatoria actualizada: ID {}, estado {}", actualizada.getId(), actualizada.getEstado());
        return ConvocatoriaResponse.fromEntity(actualizada);
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

    private Convocatoria buscarPorId(Long id) {
        return convocatoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la convocatoria con ID: " + id));
    }
}
