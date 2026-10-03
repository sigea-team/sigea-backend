package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.evento.EdicionesEventoResponse;
import com.sigea.demosigea_backend.dto.evento.EventoRequest;
import com.sigea.demosigea_backend.dto.evento.EventoResponse;
import com.sigea.demosigea_backend.dto.evento.NuevaEdicionRequest;
import com.sigea.demosigea_backend.exception.ConfirmacionRequeridaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.repository.EventoRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio de negocio para la gestión de eventos y sus ediciones (HU-04: RF03 + RF04).
 * <ul>
 *   <li><b>Criterio 1:</b> crear evento en estado {@code en_configuracion}.</li>
 *   <li><b>Criterio 2:</b> modificar la configuración antes de publicar, sin afectar información asociada.</li>
 *   <li><b>Criterio 3:</b> crear una edición vinculada al evento base, con configuración y datos independientes.</li>
 *   <li><b>Criterio 4:</b> listar las ediciones ordenadas cronológicamente con su estado.</li>
 *   <li><b>Criterio 5:</b> rechazar datos incompletos y exigir confirmación para eliminar ediciones con información.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventoService {

    /** Índice único de la base de datos: un semestre por familia (changeset 006). */
    static final String INDICE_SEMESTRE_FAMILIA = "ux_eventos_familia_semestre";

    private final EventoRepository eventoRepository;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Lista eventos con filtros opcionales, del más reciente al más antiguo.
     *
     * @param estado   Estado (opcional).
     * @param semestre Semestre (opcional).
     * @return Eventos encontrados.
     */
    @Transactional(readOnly = true)
    public List<EventoResponse> listar(EstadoEvento estado, String semestre) {
        String semestreFiltro = (semestre == null || semestre.isBlank()) ? null : semestre.trim();

        Specification<Evento> filtro = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (estado != null) {
                condiciones.add(cb.equal(root.get("estado"), estado));
            }
            if (semestreFiltro != null) {
                condiciones.add(cb.equal(root.get("semestre"), semestreFiltro));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        Sort orden = Sort.by(Sort.Order.desc("fechaInicio"), Sort.Order.desc("id"));
        return eventoRepository.findAll(filtro, orden).stream()
                .map(EventoResponse::fromEntity)
                .toList();
    }

    /**
     * Obtiene un evento por su ID.
     *
     * @param id ID del evento.
     * @return Evento encontrado.
     */
    @Transactional(readOnly = true)
    public EventoResponse obtenerPorId(Long id) {
        return EventoResponse.fromEntity(buscarEvento(id));
    }

    /**
     * Lista el evento base y todas sus ediciones ordenadas cronológicamente (Criterio 4).
     * Puede invocarse con el ID del evento base o con el de cualquiera de sus ediciones.
     *
     * @param id ID del evento base o de una edición.
     * @return Listado de ediciones con su estado.
     */
    @Transactional(readOnly = true)
    public EdicionesEventoResponse listarEdiciones(Long id) {
        Evento raiz = resolverRaiz(buscarEvento(id));

        List<Evento> familia = new ArrayList<>();
        familia.add(raiz);
        familia.addAll(eventoRepository.findByEventoBase_IdOrderByFechaInicioAscIdAsc(raiz.getId()));
        familia.sort(Comparator.comparing(Evento::getFechaInicio).thenComparing(Evento::getId));

        List<EventoResponse> ediciones = familia.stream().map(EventoResponse::fromEntity).toList();
        return new EdicionesEventoResponse(raiz.getId(), raiz.getNombre(), ediciones.size(), ediciones);
    }

    // ------------------------------------------------------------------
    // Criterio 1: creación
    // ------------------------------------------------------------------

    /**
     * Crea un evento nuevo (evento base) en estado {@code en_configuracion}.
     *
     * @param request Datos generales del evento.
     * @return Evento creado.
     */
    @Transactional
    public EventoResponse crear(EventoRequest request) {
        validarRangoFechas(request.fechaInicio(), request.fechaFin());

        Evento evento = Evento.builder()
                .nombre(request.nombre().trim())
                .objetivo(limpiar(request.objetivo()))
                .descripcion(limpiar(request.descripcion()))
                .tipo(request.tipo().trim())
                .modalidad(request.modalidad())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .semestre(resolverSemestre(request.semestre(), request.fechaInicio()))
                .estado(EstadoEvento.en_configuracion)
                .build();

        Evento guardado = eventoRepository.save(evento);
        log.info("Evento creado: ID {}, nombre '{}', estado {}", guardado.getId(), guardado.getNombre(), guardado.getEstado());
        return EventoResponse.fromEntity(guardado);
    }

    // ------------------------------------------------------------------
    // Criterio 2: actualización
    // ------------------------------------------------------------------

    /**
     * Actualiza la configuración general de un evento o edición antes de su publicación.
     * <p>
     * Solo modifica columnas propias de {@code eventos}: no toca el vínculo con el evento base ni
     * la información de otros módulos asociada al evento, que permanece intacta.
     * </p>
     *
     * @param id      ID del evento.
     * @param request Nuevos datos.
     * @return Evento actualizado.
     */
    @Transactional
    public EventoResponse actualizar(Long id, EventoRequest request) {
        Evento evento = buscarEvento(id);

        if (evento.getEstado() != EstadoEvento.en_configuracion) {
            throw new OperacionNoPermitidaException(String.format(
                    "El evento '%s' ya no está en configuración (estado actual: %s). "
                            + "Solo puede modificarse su configuración antes de ser publicado.",
                    evento.getNombre(), evento.getEstado()));
        }

        validarRangoFechas(request.fechaInicio(), request.fechaFin());

        // El semestre debe ser único dentro de la familia (evento base + ediciones), igual que en
        // crearEdicion. Se valida siempre, contra toda la familia, excluyendo el propio registro.
        String nuevoSemestre = resolverSemestre(request.semestre(), request.fechaInicio());
        validarSemestreDisponible(resolverRaiz(evento).getId(), nuevoSemestre, evento.getId());

        evento.setNombre(request.nombre().trim());
        evento.setObjetivo(limpiar(request.objetivo()));
        evento.setDescripcion(limpiar(request.descripcion()));
        evento.setTipo(request.tipo().trim());
        evento.setModalidad(request.modalidad());
        evento.setFechaInicio(request.fechaInicio());
        evento.setFechaFin(request.fechaFin());
        evento.setSemestre(nuevoSemestre);

        Evento actualizado = guardarControlandoSemestre(evento);
        log.info("Evento actualizado: ID {}", actualizado.getId());
        return EventoResponse.fromEntity(actualizado);
    }

    // ------------------------------------------------------------------
    // Criterio 3: nueva edición
    // ------------------------------------------------------------------

    /**
     * Crea una nueva edición a partir de un evento existente.
     * <p>
     * La edición hereda la configuración general del evento origen (objetivo, descripción, tipo,
     * modalidad) pero es un registro independiente: nace en {@code en_configuracion}, con sus propias
     * fechas y semestre, y sin información de otros módulos (catálogos, comité, presupuesto, etc.).
     * Siempre queda vinculada al evento raíz de la familia.
     * </p>
     *
     * @param origenId ID del evento base o de una edición existente a tomar como plantilla.
     * @param request  Datos del nuevo periodo.
     * @return Edición creada.
     */
    @Transactional
    public EventoResponse crearEdicion(Long origenId, NuevaEdicionRequest request) {
        Evento origen = buscarEvento(origenId);
        Evento raiz = resolverRaiz(origen);

        validarRangoFechas(request.fechaInicio(), request.fechaFin());

        String semestre = resolverSemestre(request.semestre(), request.fechaInicio());
        validarSemestreDisponible(raiz.getId(), semestre, null);

        String nombre = (request.nombre() == null || request.nombre().isBlank())
                ? origen.getNombre()
                : request.nombre().trim();

        Evento edicion = Evento.builder()
                .nombre(nombre)
                .objetivo(origen.getObjetivo())
                .descripcion(origen.getDescripcion())
                .tipo(origen.getTipo())
                .modalidad(origen.getModalidad())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .semestre(semestre)
                .estado(EstadoEvento.en_configuracion)
                .eventoBase(raiz)
                .build();

        Evento guardada = guardarControlandoSemestre(edicion);

        log.info("Edición creada: ID {} a partir del evento {} (evento base {})",
                guardada.getId(), origen.getId(), raiz.getId());
        return EventoResponse.fromEntity(guardada);
    }

    // ------------------------------------------------------------------
    // Criterio 5: eliminación con integridad histórica
    // ------------------------------------------------------------------

    /**
     * Elimina un evento o edición.
     * <ul>
     *   <li>Solo se eliminan eventos en {@code en_configuracion} (permiso EVENTOS_ELIMINAR del catálogo).</li>
     *   <li>No se elimina un evento base que tenga ediciones derivadas, para conservar la integridad histórica.</li>
     *   <li>Siempre exige confirmación explícita ({@code confirmar = true}), porque la eliminación es
     *       irreversible y arrastra la información asociada ({@code ON DELETE CASCADE} del esquema).</li>
     * </ul>
     *
     * @param id        ID del evento.
     * @param confirmar Confirmación explícita del usuario.
     */
    @Transactional
    public void eliminar(Long id, boolean confirmar) {
        Evento evento = buscarEvento(id);

        if (evento.getEstado() != EstadoEvento.en_configuracion) {
            throw new OperacionNoPermitidaException(String.format(
                    "No es posible eliminar '%s' porque su estado es '%s'. Solo se pueden eliminar eventos "
                            + "en configuración, para conservar la integridad histórica.",
                    evento.getNombre(), evento.getEstado()));
        }

        long edicionesDerivadas = eventoRepository.countByEventoBase_Id(id);
        if (edicionesDerivadas > 0) {
            throw new OperacionNoPermitidaException(String.format(
                    "No es posible eliminar '%s' porque es el evento base de %d edición(es). "
                            + "Elimine primero las ediciones derivadas.",
                    evento.getNombre(), edicionesDerivadas));
        }

        if (!confirmar) {
            throw new ConfirmacionRequeridaException(String.format(
                    "Eliminar '%s' es irreversible y también eliminará la información registrada en el evento. "
                            + "Confirme la operación para continuar.",
                    evento.getNombre()));
        }

        eventoRepository.delete(evento);
        log.info("Evento eliminado: ID {}", id);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + id));
    }

    /**
     * Devuelve el evento raíz de la familia (el propio evento si no es edición).
     * <p>
     * La jerarquía tiene como máximo un nivel: {@link #crearEdicion} siempre vincula la nueva edición
     * al evento raíz y {@link #actualizar} nunca modifica el vínculo. Por eso basta con un solo salto,
     * sin recorrer la cadena; así tampoco hay riesgo de bucle infinito si la base de datos tuviera
     * una relación cíclica creada manualmente.
     * </p>
     */
    private Evento resolverRaiz(Evento evento) {
        return evento.getEventoBase() != null ? evento.getEventoBase() : evento;
    }

    private void validarRangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new OperacionNoPermitidaException("Las fechas de inicio y fin son obligatorias.");
        }
        if (fin.isBefore(inicio)) {
            throw new OperacionNoPermitidaException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
    }

    /**
     * Verifica que el semestre no esté ocupado en la familia (evento base + ediciones).
     *
     * @param raizId    ID del evento raíz de la familia.
     * @param semestre  Semestre a verificar.
     * @param excluirId ID del evento que se está actualizando (no cuenta como duplicado); {@code null} al crear.
     */
    private void validarSemestreDisponible(Long raizId, String semestre, Long excluirId) {
        boolean ocupado = excluirId == null
                ? eventoRepository.existsByIdAndSemestre(raizId, semestre)
                        || eventoRepository.existsByEventoBase_IdAndSemestre(raizId, semestre)
                : eventoRepository.existsByIdAndSemestreAndIdNot(raizId, semestre, excluirId)
                        || eventoRepository.existsByEventoBase_IdAndSemestreAndIdNot(raizId, semestre, excluirId);
        if (ocupado) {
            throw new OperacionNoPermitidaException(mensajeSemestreOcupado(semestre));
        }
    }

    /**
     * Guarda inmediatamente (flush) para que, si dos peticiones simultáneas pasaron la validación,
     * el índice único {@value #INDICE_SEMESTRE_FAMILIA} lo detecte aquí y se responda 409 en vez de 500.
     */
    private Evento guardarControlandoSemestre(Evento evento) {
        try {
            return eventoRepository.saveAndFlush(evento);
        } catch (DataIntegrityViolationException ex) {
            String detalle = ex.getMostSpecificCause().getMessage();
            if (detalle != null && detalle.contains(INDICE_SEMESTRE_FAMILIA)) {
                throw new OperacionNoPermitidaException(mensajeSemestreOcupado(evento.getSemestre()));
            }
            throw ex;
        }
    }

    private static String mensajeSemestreOcupado(String semestre) {
        return "Ya existe una edición de este evento para el semestre " + semestre + ".";
    }

    /**
     * Usa el semestre enviado o lo deriva de la fecha de inicio: enero–junio → AAAA-1, julio–diciembre → AAAA-2.
     */
    static String resolverSemestre(String semestre, LocalDate fechaInicio) {
        if (semestre != null && !semestre.isBlank()) {
            return semestre.trim();
        }
        int periodo = fechaInicio.getMonthValue() <= 6 ? 1 : 2;
        return fechaInicio.getYear() + "-" + periodo;
    }

    private String limpiar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
