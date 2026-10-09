package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.parametro.TipoActividadRequest;
import com.sigea.demosigea_backend.dto.parametro.TipoActividadResponse;
import com.sigea.demosigea_backend.dto.parametro.UsoParametroResponse;
import com.sigea.demosigea_backend.exception.ParametroEventoDuplicadoException;
import com.sigea.demosigea_backend.exception.ParametroEventoEnUsoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.TipoActividad;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.TipoActividadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Servicio de negocio para el catálogo de tipos de actividad de un evento (HU-05, RF06).
 * <ul>
 *   <li><b>Criterio 1:</b> crear (y editar) un tipo indicando nombre y características; queda
 *       disponible para la programación de actividades.</li>
 *   <li><b>Criterio 2:</b> si el tipo está en uso en la agenda, se impide su eliminación y se
 *       informa el impacto. {@link #consultarUso} permite advertirlo antes de intentar borrar.</li>
 *   <li><b>Criterio 3:</b> se rechaza un nombre ya existente en el evento, sin distinguir
 *       mayúsculas ni espacios sobrantes.</li>
 * </ul>
 * Las modificaciones se permiten solo con el evento en {@code en_configuracion} o
 * {@code habilitado} (ver {@link ParametroEventoReglas#ESTADOS_CATALOGO_MODIFICABLE}) y se
 * registran en la auditoría (RF56).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TipoActividadService {

    /** Nombre de la entidad usado en el log de auditoría. */
    static final String ENTIDAD = "tipos_actividad";

    /** Índices únicos sobre (evento_id, nombre): el original del esquema y el insensible a mayúsculas (changeset hu05-001). */
    static final Set<String> INDICES_NOMBRE = Set.of(
            "tipos_actividad_evento_id_nombre_idx",
            "ux_tipos_actividad_evento_nombre_ci");

    private static final String CATALOGO = "Los tipos de actividad";

    private final TipoActividadRepository tipoRepository;
    private final EventoRepository eventoRepository;
    private final ActividadRepository actividadRepository;
    private final AuditoriaService auditoriaService;

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    /**
     * @param eventoId ID del evento.
     * @return Tipos de actividad del evento ordenados por nombre.
     */
    @Transactional(readOnly = true)
    public List<TipoActividadResponse> listar(Long eventoId) {
        buscarEvento(eventoId);
        return tipoRepository.findByEvento_IdOrderByNombreAscIdAsc(eventoId).stream()
                .map(TipoActividadResponse::fromEntity)
                .toList();
    }

    /**
     * @return El tipo de actividad indicado.
     * @throws RecursoNoEncontradoException si el evento o el tipo no existen.
     */
    @Transactional(readOnly = true)
    public TipoActividadResponse obtener(Long eventoId, Long tipoId) {
        buscarEvento(eventoId);
        return TipoActividadResponse.fromEntity(buscarTipo(eventoId, tipoId));
    }

    /**
     * Criterio 2: informa en cuántas actividades se usa el tipo, para advertir del impacto
     * antes de eliminarlo.
     */
    @Transactional(readOnly = true)
    public UsoParametroResponse consultarUso(Long eventoId, Long tipoId) {
        buscarEvento(eventoId);
        TipoActividad tipo = buscarTipo(eventoId, tipoId);
        return calcularUso(tipo);
    }

    // ------------------------------------------------------------------
    // Criterios 1 y 3: crear y actualizar
    // ------------------------------------------------------------------

    /**
     * Crea un tipo de actividad en el evento.
     *
     * @throws RecursoNoEncontradoException       si el evento no existe.
     * @throws com.sigea.demosigea_backend.exception.OperacionNoPermitidaException si el evento está en ejecución o cerrado.
     * @throws ParametroEventoDuplicadoException  si ya existe un tipo con ese nombre (Criterio 3).
     */
    @Transactional
    public TipoActividadResponse crear(Long eventoId, TipoActividadRequest request) {
        Evento evento = buscarEvento(eventoId);
        ParametroEventoReglas.validarEventoModificable(evento, CATALOGO);

        String nombre = ParametroEventoReglas.normalizarNombre(request.nombre());
        if (tipoRepository.existsByEvento_IdAndNombreIgnoreCase(eventoId, nombre)) {
            throw duplicado(nombre, evento);
        }

        TipoActividad tipo = TipoActividad.builder()
                .evento(evento)
                .nombre(nombre)
                .descripcion(ParametroEventoReglas.normalizarDescripcion(request.descripcion()))
                .build();
        TipoActividad guardado = guardarControlandoDuplicado(tipo, evento);

        auditoriaService.registrar(TipoOperacionAuditoria.TIPO_ACTIVIDAD_CREADO, ENTIDAD, guardado.getId(),
                DetalleAuditoria.de("eventoId", evento.getId(), "nombre", guardado.getNombre(),
                        "descripcion", guardado.getDescripcion()));

        log.info("Tipo de actividad creado: {} '{}' en evento {}", guardado.getId(), guardado.getNombre(), evento.getId());
        return TipoActividadResponse.fromEntity(guardado);
    }

    /**
     * Actualiza el nombre y las características de un tipo de actividad. Renombrar un tipo en uso
     * está permitido: las actividades lo referencian por ID.
     *
     * @throws ParametroEventoDuplicadoException si el nuevo nombre ya existe en otro tipo del evento.
     */
    @Transactional
    public TipoActividadResponse actualizar(Long eventoId, Long tipoId, TipoActividadRequest request) {
        Evento evento = buscarEvento(eventoId);
        ParametroEventoReglas.validarEventoModificable(evento, CATALOGO);
        TipoActividad tipo = buscarTipo(eventoId, tipoId);

        String nombre = ParametroEventoReglas.normalizarNombre(request.nombre());
        if (tipoRepository.existsByEvento_IdAndNombreIgnoreCaseAndIdNot(eventoId, nombre, tipoId)) {
            throw duplicado(nombre, evento);
        }

        String nombreAnterior = tipo.getNombre();
        String descripcionAnterior = tipo.getDescripcion();
        tipo.setNombre(nombre);
        tipo.setDescripcion(ParametroEventoReglas.normalizarDescripcion(request.descripcion()));
        TipoActividad guardado = guardarControlandoDuplicado(tipo, evento);

        auditoriaService.registrar(TipoOperacionAuditoria.TIPO_ACTIVIDAD_ACTUALIZADO, ENTIDAD, guardado.getId(),
                DetalleAuditoria.de("eventoId", evento.getId(),
                        "antes", DetalleAuditoria.de("nombre", nombreAnterior, "descripcion", descripcionAnterior),
                        "despues", DetalleAuditoria.de("nombre", guardado.getNombre(), "descripcion", guardado.getDescripcion())));

        log.info("Tipo de actividad actualizado: {} en evento {}", guardado.getId(), evento.getId());
        return TipoActividadResponse.fromEntity(guardado);
    }

    // ------------------------------------------------------------------
    // Criterio 2: eliminar
    // ------------------------------------------------------------------

    /**
     * Elimina un tipo de actividad que no esté en uso.
     *
     * @throws ParametroEventoEnUsoException si alguna actividad de la agenda usa el tipo (Criterio 2).
     */
    @Transactional
    public void eliminar(Long eventoId, Long tipoId) {
        Evento evento = buscarEvento(eventoId);
        ParametroEventoReglas.validarEventoModificable(evento, CATALOGO);
        TipoActividad tipo = buscarTipo(eventoId, tipoId);

        UsoParametroResponse uso = calcularUso(tipo);
        if (!uso.eliminable()) {
            throw enUso(tipo, uso);
        }

        try {
            tipoRepository.delete(tipo);
            tipoRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // Una actividad se programó con este tipo entre la verificación y el borrado.
            if (ParametroEventoReglas.esViolacionLlaveForanea(ex)) {
                log.warn("La llave foránea impidió eliminar el tipo de actividad {}: está en uso. Se responde 409.", tipoId);
                throw enUso(tipo, null);
            }
            throw ex;
        }

        auditoriaService.registrar(TipoOperacionAuditoria.TIPO_ACTIVIDAD_ELIMINADO, ENTIDAD, tipoId,
                DetalleAuditoria.de("eventoId", evento.getId(), "nombre", tipo.getNombre()));

        log.info("Tipo de actividad eliminado: {} '{}' del evento {}", tipoId, tipo.getNombre(), evento.getId());
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + id));
    }

    private TipoActividad buscarTipo(Long eventoId, Long tipoId) {
        return tipoRepository.findByIdAndEvento_Id(tipoId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "No se encontró el tipo de actividad con ID %d en el evento %d.", tipoId, eventoId)));
    }

    private UsoParametroResponse calcularUso(TipoActividad tipo) {
        long actividades = actividadRepository.countByTipoActividad_Id(tipo.getId());
        return UsoParametroResponse.de(tipo.getId(), tipo.getNombre(), actividades, 0);
    }

    /**
     * Guarda con flush inmediato: si dos peticiones simultáneas pasaron la validación del
     * Criterio 3, el índice único lo detecta aquí y se responde 409 en vez de 500.
     */
    private TipoActividad guardarControlandoDuplicado(TipoActividad tipo, Evento evento) {
        try {
            return tipoRepository.saveAndFlush(tipo);
        } catch (DataIntegrityViolationException ex) {
            if (ParametroEventoReglas.esViolacionUnicidad(ex, INDICES_NOMBRE)) {
                log.warn("El índice único rechazó el tipo de actividad duplicado '{}' en evento {}. Se responde 409.",
                        tipo.getNombre(), evento.getId());
                throw duplicado(tipo.getNombre(), evento);
            }
            throw ex;
        }
    }

    private static ParametroEventoDuplicadoException duplicado(String nombre, Evento evento) {
        return new ParametroEventoDuplicadoException(String.format(
                "Ya existe un tipo de actividad llamado '%s' en el evento '%s'.", nombre, evento.getNombre()));
    }

    private static ParametroEventoEnUsoException enUso(TipoActividad tipo, UsoParametroResponse uso) {
        String detalle = uso != null ? uso.describirUso() : "actividades de la agenda";
        return new ParametroEventoEnUsoException(String.format(
                "No se puede eliminar el tipo de actividad '%s' porque está en uso en %s. "
                        + "Reasigne esas actividades a otro tipo antes de eliminarlo.",
                tipo.getNombre(), detalle));
    }
}
