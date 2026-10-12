package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoAprobadoRequest;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoAprobadoResponse;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoResumenResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.EstadoPresupuestoAprobado;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.PresupuestoAprobado;
import com.sigea.demosigea_backend.model.RubroPresupuestal;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.PresupuestoAprobadoRepository;
import com.sigea.demosigea_backend.repository.RubroPresupuestalRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para el registro y gestión del presupuesto aprobado de un evento (HU-08, RF07).
 * <ul>
 *   <li><b>Criterio 1:</b> Si existe un presupuesto preliminar, registrar el presupuesto aprobado
 *       (que puede diferir del preliminar) con su soporte, guardándolo como versión oficial vinculada al evento.</li>
 *   <li><b>Criterio 2:</b> Si ya existe un presupuesto aprobado registrado, conservar la versión
 *       anterior y guardar la nueva, con historial de cambios y trazabilidad en auditoría.</li>
 *   <li><b>Criterio 3:</b> Exigir obligatoriamente la evidencia documental de aprobación antes de guardar.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresupuestoAprobadoService {

    /** Nombre de la entidad en el log de auditoría. */
    static final String ENTIDAD_PRESUPUESTO = "presupuestos_aprobados";

    private final PresupuestoAprobadoRepository presupuestoRepository;
    private final RubroPresupuestalRepository rubroRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    // ------------------------------------------------------------------
    // Criterios 1, 2 y 3: Registro y Actualización del Presupuesto Aprobado
    // ------------------------------------------------------------------

    /**
     * Registra o actualiza el presupuesto aprobado formalmente para un evento.
     * <p>
     * Cumple los siguientes criterios de aceptación:
     * <ul>
     *   <li><b>Criterio 1:</b> Valida que exista un presupuesto preliminar (rubros presupuestales activos).
     *       El monto aprobado puede ser diferente al total preliminar.</li>
     *   <li><b>Criterio 2:</b> Si ya existe un presupuesto oficial previo, pasa la versión anterior a
     *       {@code historico} y guarda la nueva versión como {@code aprobado}, registrando el cambio
     *       y las diferencias en la auditoría.</li>
     *   <li><b>Criterio 3:</b> Exige que el soporte o URL de evidencia esté presente y no sea vacío.</li>
     * </ul>
     * </p>
     *
     * @param eventoId ID del evento al que se le vincula el presupuesto aprobado.
     * @param request  Datos del presupuesto aprobado (monto, evidencia documental, usuario opcional).
     * @return DTO con la versión oficial guardada y la comparación frente al presupuesto preliminar.
     * @throws RecursoNoEncontradoException  si el evento no existe o el usuario especificado no existe.
     * @throws OperacionNoPermitidaException si el evento no tiene presupuesto preliminar o está cerrado.
     * @throws SolicitudInvalidaException    si falta la evidencia de soporte o el monto es negativo.
     */
    @Transactional
    public PresupuestoAprobadoResponse registrarOActualizar(Long eventoId, PresupuestoAprobadoRequest request) {
        Evento evento = buscarEvento(eventoId);
        validarEventoModificable(evento);

        // Criterio 3: Exigir evidencia documental antes de guardar
        validarEvidencia(request.evidenciaUrl());

        // Validar monto aprobado
        validarMonto(request.valorTotalAprobado());

        // Criterio 1: Precondición - Dado que existe un presupuesto preliminar
        validarExistenciaPresupuestoPreliminar(eventoId);
        BigDecimal totalPreliminar = rubroRepository.calcularTotalPreliminarPorEvento(eventoId);

        // Resolver usuario que autoriza
        Usuario usuarioAprobador = resolverUsuario(request.aprobadoPorId());

        // Criterio 2: Verificar si ya existe una versión oficial previa
        Optional<PresupuestoAprobado> oficialActualOpt =
                presupuestoRepository.findByEvento_IdAndEstado(eventoId, EstadoPresupuestoAprobado.aprobado);

        PresupuestoAprobado guardado;
        if (oficialActualOpt.isPresent()) {
            // Actualización: conservar versión anterior como 'historico'
            PresupuestoAprobado anterior = oficialActualOpt.get();
            anterior.pasarAHistorico();
            presupuestoRepository.save(anterior);

            // Crear y guardar la nueva versión oficial
            PresupuestoAprobado nuevo = PresupuestoAprobado.builder()
                    .evento(evento)
                    .valorTotalAprobado(request.valorTotalAprobado())
                    .evidenciaUrl(request.evidenciaUrl().trim())
                    .fechaAprobacion(LocalDateTime.now())
                    .aprobadoPor(usuarioAprobador)
                    .estado(EstadoPresupuestoAprobado.aprobado)
                    .build();

            guardado = presupuestoRepository.save(nuevo);

            // Registrar en auditoría el cambio de versión con historial
            auditoriaService.registrar(
                    TipoOperacionAuditoria.PRESUPUESTO_ACTUALIZADO,
                    ENTIDAD_PRESUPUESTO,
                    guardado.getId(),
                    DetalleAuditoria.de(
                            "eventoId", evento.getId(),
                            "aprobacionIdAnterior", anterior.getId(),
                            "valorAnterior", anterior.getValorTotalAprobado(),
                            "evidenciaAnterior", anterior.getEvidenciaUrl(),
                            "fechaAprobacionAnterior", anterior.getFechaAprobacion(),
                            "aprobacionIdNueva", guardado.getId(),
                            "valorNuevo", guardado.getValorTotalAprobado(),
                            "evidenciaNueva", guardado.getEvidenciaUrl(),
                            "totalPreliminar", totalPreliminar,
                            "esActualizacion", true
                    )
            );

            log.info("Presupuesto aprobado actualizado para evento {}: versión previa #{} marcada como histórica, nueva versión oficial #{} con monto {}",
                    eventoId, anterior.getId(), guardado.getId(), guardado.getValorTotalAprobado());

        } else {
            // Primer registro oficial
            PresupuestoAprobado nuevo = PresupuestoAprobado.builder()
                    .evento(evento)
                    .valorTotalAprobado(request.valorTotalAprobado())
                    .evidenciaUrl(request.evidenciaUrl().trim())
                    .fechaAprobacion(LocalDateTime.now())
                    .aprobadoPor(usuarioAprobador)
                    .estado(EstadoPresupuestoAprobado.aprobado)
                    .build();

            guardado = presupuestoRepository.save(nuevo);

            auditoriaService.registrar(
                    TipoOperacionAuditoria.PRESUPUESTO_APROBADO,
                    ENTIDAD_PRESUPUESTO,
                    guardado.getId(),
                    DetalleAuditoria.de(
                            "eventoId", evento.getId(),
                            "aprobacionId", guardado.getId(),
                            "valorTotalAprobado", guardado.getValorTotalAprobado(),
                            "totalPreliminar", totalPreliminar,
                            "evidenciaUrl", guardado.getEvidenciaUrl(),
                            "esActualizacion", false
                    )
            );

            log.info("Presupuesto aprobado registrado por primera vez para evento {}: versión oficial #{} con monto {}",
                    eventoId, guardado.getId(), guardado.getValorTotalAprobado());
        }

        return PresupuestoAprobadoResponse.fromEntity(guardado, totalPreliminar);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Obtiene la versión oficial vigente del presupuesto aprobado para el evento.
     *
     * @param eventoId ID del evento.
     * @return DTO del presupuesto aprobado oficial.
     * @throws RecursoNoEncontradoException si el evento no existe o no tiene presupuesto aprobado.
     */
    @Transactional(readOnly = true)
    public PresupuestoAprobadoResponse obtenerOficial(Long eventoId) {
        buscarEvento(eventoId);
        PresupuestoAprobado oficial = presupuestoRepository
                .findByEvento_IdAndEstado(eventoId, EstadoPresupuestoAprobado.aprobado)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El evento con ID " + eventoId + " no tiene un presupuesto aprobado oficial registrado."));

        BigDecimal totalPreliminar = rubroRepository.calcularTotalPreliminarPorEvento(eventoId);
        return PresupuestoAprobadoResponse.fromEntity(oficial, totalPreliminar);
    }

    /**
     * Consulta el historial completo de versiones de presupuestos aprobados del evento
     * (tanto vigentes como históricas), ordenado del más reciente al más antiguo (Criterio 2).
     *
     * @param eventoId ID del evento.
     * @return Lista cronológica de versiones de presupuestos aprobados.
     */
    @Transactional(readOnly = true)
    public List<PresupuestoAprobadoResponse> listarHistorial(Long eventoId) {
        buscarEvento(eventoId);
        BigDecimal totalPreliminar = rubroRepository.calcularTotalPreliminarPorEvento(eventoId);
        List<PresupuestoAprobado> historial =
                presupuestoRepository.findByEvento_IdOrderByFechaAprobacionDescIdDesc(eventoId);

        return historial.stream()
                .map(pa -> PresupuestoAprobadoResponse.fromEntity(pa, totalPreliminar))
                .toList();
    }

    /**
     * Obtiene el resumen comparativo del evento: presupuesto preliminar proyectado,
     * versión oficial vigente y el historial de cambios registrados.
     *
     * @param eventoId ID del evento.
     * @return Resumen presupuestal completo.
     */
    @Transactional(readOnly = true)
    public PresupuestoResumenResponse obtenerResumen(Long eventoId) {
        Evento evento = buscarEvento(eventoId);
        List<RubroPresupuestal> rubros = rubroRepository.findByEvento_IdAndActivoTrue(eventoId);
        BigDecimal totalPreliminar = rubroRepository.calcularTotalPreliminarPorEvento(eventoId);

        PresupuestoAprobadoResponse oficial = presupuestoRepository
                .findByEvento_IdAndEstado(eventoId, EstadoPresupuestoAprobado.aprobado)
                .map(pa -> PresupuestoAprobadoResponse.fromEntity(pa, totalPreliminar))
                .orElse(null);

        List<PresupuestoAprobadoResponse> historial = presupuestoRepository
                .findByEvento_IdOrderByFechaAprobacionDescIdDesc(eventoId)
                .stream()
                .map(pa -> PresupuestoAprobadoResponse.fromEntity(pa, totalPreliminar))
                .toList();

        return new PresupuestoResumenResponse(
                evento.getId(),
                evento.getNombre(),
                totalPreliminar,
                rubros.size(),
                oficial,
                historial
        );
    }

    // ------------------------------------------------------------------
    // Validaciones y Métodos Auxiliares
    // ------------------------------------------------------------------

    private Evento buscarEvento(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId));
    }

    private void validarEventoModificable(Evento evento) {
        if (evento.getEstado() == EstadoEvento.cerrado) {
            throw new OperacionNoPermitidaException("No se puede registrar o actualizar el presupuesto de un evento cerrado.");
        }
    }

    private void validarEvidencia(String evidenciaUrl) {
        if (!StringUtils.hasText(evidenciaUrl)) {
            throw new SolicitudInvalidaException("La evidencia de aprobación es obligatoria para registrar el presupuesto aprobado.");
        }
    }

    private void validarMonto(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new SolicitudInvalidaException("El valor total aprobado debe ser mayor o igual a cero.");
        }
    }

    private void validarExistenciaPresupuestoPreliminar(Long eventoId) {
        boolean existePreliminar = rubroRepository.existsByEvento_IdAndActivoTrue(eventoId);
        if (!existePreliminar) {
            throw new OperacionNoPermitidaException(
                    "No se puede registrar el presupuesto aprobado porque el evento no cuenta con un presupuesto preliminar registrado.");
        }
    }

    private Usuario resolverUsuario(Long aprobadoPorId) {
        if (aprobadoPorId != null) {
            return usuarioRepository.findById(aprobadoPorId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró el usuario con ID: " + aprobadoPorId));
        }

        return obtenerUsuarioAutenticado()
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "No se pudo determinar el usuario que aprueba el presupuesto. Inicie sesión o especifique 'aprobadoPorId'."));
    }

    private Optional<Usuario> obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName());
    }
}
