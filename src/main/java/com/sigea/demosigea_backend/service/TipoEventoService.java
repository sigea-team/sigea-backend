package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.tipoevento.TipoEventoRequest;
import com.sigea.demosigea_backend.dto.tipoevento.TipoEventoResponse;
import com.sigea.demosigea_backend.dto.tipoevento.UsoTipoEventoResponse;
import com.sigea.demosigea_backend.exception.ParametroEventoDuplicadoException;
import com.sigea.demosigea_backend.exception.ParametroEventoEnUsoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.TipoEvento;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.TipoEventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Servicio de negocio del catálogo global de tipos de evento.
 * <ul>
 *   <li>Crear y editar un tipo con nombre y descripción; queda disponible al crear o editar eventos.</li>
 *   <li>El nombre es único sin distinguir mayúsculas ni espacios sobrantes (mismas reglas que los
 *       catálogos de la HU-05, en {@link ParametroEventoReglas}).</li>
 *   <li>Un tipo asignado a algún evento no se puede eliminar; {@link #consultarUso} permite advertirlo
 *       antes. Renombrarlo sí está permitido, porque los eventos lo referencian por ID.</li>
 * </ul>
 * Las modificaciones se registran en la auditoría (RF56).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TipoEventoService {

    /** Nombre de la entidad usado en el log de auditoría. */
    static final String ENTIDAD = "tipos_evento";

    /** Índice único del nombre sin distinguir mayúsculas (changeset tipos-evento-001). */
    static final Set<String> INDICES_NOMBRE = Set.of("ux_tipos_evento_nombre_ci");

    private final TipoEventoRepository tipoRepository;
    private final EventoRepository eventoRepository;
    private final AuditoriaService auditoriaService;

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    /**
     * @return Catálogo completo de tipos de evento, ordenado por nombre.
     */
    @Transactional(readOnly = true)
    public List<TipoEventoResponse> listar() {
        return tipoRepository.findAllByOrderByNombreAscIdAsc().stream()
                .map(TipoEventoResponse::fromEntity)
                .toList();
    }

    /**
     * @param id ID del tipo.
     * @return El tipo de evento indicado.
     * @throws RecursoNoEncontradoException si no existe.
     */
    @Transactional(readOnly = true)
    public TipoEventoResponse obtener(Long id) {
        return TipoEventoResponse.fromEntity(buscar(id));
    }

    /**
     * Informa cuántos eventos usan el tipo, para advertir del impacto antes de eliminarlo.
     *
     * @param id ID del tipo.
     * @return Uso del tipo.
     */
    @Transactional(readOnly = true)
    public UsoTipoEventoResponse consultarUso(Long id) {
        return calcularUso(buscar(id));
    }

    // ------------------------------------------------------------------
    // Crear y actualizar
    // ------------------------------------------------------------------

    /**
     * Crea un tipo de evento.
     *
     * @param request Datos del tipo.
     * @return Tipo creado.
     * @throws ParametroEventoDuplicadoException si ya existe un tipo con ese nombre.
     */
    @Transactional
    public TipoEventoResponse crear(TipoEventoRequest request) {
        String nombre = ParametroEventoReglas.normalizarNombre(request.nombre());
        if (tipoRepository.existsByNombreIgnoreCase(nombre)) {
            throw duplicado(nombre);
        }

        TipoEvento tipo = TipoEvento.builder()
                .nombre(nombre)
                .descripcion(ParametroEventoReglas.normalizarDescripcion(request.descripcion()))
                .build();
        TipoEvento guardado = guardarControlandoDuplicado(tipo);

        auditoriaService.registrar(TipoOperacionAuditoria.TIPO_EVENTO_CREADO, ENTIDAD, guardado.getId(),
                DetalleAuditoria.de("nombre", guardado.getNombre(), "descripcion", guardado.getDescripcion()));

        log.info("Tipo de evento creado: {} '{}'", guardado.getId(), guardado.getNombre());
        return TipoEventoResponse.fromEntity(guardado);
    }

    /**
     * Actualiza el nombre y la descripción de un tipo de evento. Renombrar un tipo en uso está
     * permitido: los eventos lo referencian por ID y muestran el nombre nuevo.
     *
     * @param id      ID del tipo.
     * @param request Nuevos datos.
     * @return Tipo actualizado.
     * @throws ParametroEventoDuplicadoException si el nuevo nombre ya existe en otro tipo.
     */
    @Transactional
    public TipoEventoResponse actualizar(Long id, TipoEventoRequest request) {
        TipoEvento tipo = buscar(id);

        String nombre = ParametroEventoReglas.normalizarNombre(request.nombre());
        if (tipoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw duplicado(nombre);
        }

        String nombreAnterior = tipo.getNombre();
        String descripcionAnterior = tipo.getDescripcion();
        tipo.setNombre(nombre);
        tipo.setDescripcion(ParametroEventoReglas.normalizarDescripcion(request.descripcion()));
        TipoEvento guardado = guardarControlandoDuplicado(tipo);

        auditoriaService.registrar(TipoOperacionAuditoria.TIPO_EVENTO_ACTUALIZADO, ENTIDAD, guardado.getId(),
                DetalleAuditoria.de(
                        "antes", DetalleAuditoria.de("nombre", nombreAnterior, "descripcion", descripcionAnterior),
                        "despues", DetalleAuditoria.de("nombre", guardado.getNombre(), "descripcion", guardado.getDescripcion())));

        log.info("Tipo de evento actualizado: {}", guardado.getId());
        return TipoEventoResponse.fromEntity(guardado);
    }

    // ------------------------------------------------------------------
    // Eliminar
    // ------------------------------------------------------------------

    /**
     * Elimina un tipo de evento que ningún evento usa.
     *
     * @param id ID del tipo.
     * @throws ParametroEventoEnUsoException si algún evento tiene asignado el tipo.
     */
    @Transactional
    public void eliminar(Long id) {
        TipoEvento tipo = buscar(id);

        UsoTipoEventoResponse uso = calcularUso(tipo);
        if (!uso.eliminable()) {
            throw enUso(tipo, uso.eventos());
        }

        try {
            tipoRepository.delete(tipo);
            tipoRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // Un evento tomó este tipo entre la verificación y el borrado.
            if (ParametroEventoReglas.esViolacionLlaveForanea(ex)) {
                log.warn("La llave foránea impidió eliminar el tipo de evento {}: está en uso. Se responde 409.", id);
                throw enUso(tipo, -1);
            }
            throw ex;
        }

        auditoriaService.registrar(TipoOperacionAuditoria.TIPO_EVENTO_ELIMINADO, ENTIDAD, id,
                DetalleAuditoria.de("nombre", tipo.getNombre()));

        log.info("Tipo de evento eliminado: {} '{}'", id, tipo.getNombre());
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private TipoEvento buscar(Long id) {
        return tipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el tipo de evento con ID: " + id));
    }

    private UsoTipoEventoResponse calcularUso(TipoEvento tipo) {
        return UsoTipoEventoResponse.de(tipo.getId(), tipo.getNombre(), eventoRepository.countByTipoEvento_Id(tipo.getId()));
    }

    /**
     * Guarda con flush inmediato: si dos peticiones simultáneas pasaron la validación de nombre,
     * el índice único lo detecta aquí y se responde 409 en vez de 500.
     */
    private TipoEvento guardarControlandoDuplicado(TipoEvento tipo) {
        try {
            return tipoRepository.saveAndFlush(tipo);
        } catch (DataIntegrityViolationException ex) {
            if (ParametroEventoReglas.esViolacionUnicidad(ex, INDICES_NOMBRE)) {
                log.warn("El índice único rechazó el tipo de evento duplicado '{}'. Se responde 409.", tipo.getNombre());
                throw duplicado(tipo.getNombre());
            }
            throw ex;
        }
    }

    private static ParametroEventoDuplicadoException duplicado(String nombre) {
        return new ParametroEventoDuplicadoException(
                String.format("Ya existe un tipo de evento llamado '%s'.", nombre));
    }

    /**
     * @param eventos Eventos que usan el tipo; negativo si no se conoce (detectado por la llave foránea).
     */
    private static ParametroEventoEnUsoException enUso(TipoEvento tipo, long eventos) {
        String detalle = eventos >= 0 ? eventos + " evento(s)" : "uno o más eventos";
        return new ParametroEventoEnUsoException(String.format(
                "No se puede eliminar el tipo de evento '%s' porque lo usan %s. "
                        + "Asigna otro tipo a esos eventos antes de eliminarlo.",
                tipo.getNombre(), detalle));
    }
}
