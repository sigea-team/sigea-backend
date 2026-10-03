package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.auditoria.AuditoriaResponse;
import com.sigea.demosigea_backend.dto.auditoria.FiltroAuditoria;
import com.sigea.demosigea_backend.dto.auditoria.PaginaResponse;
import com.sigea.demosigea_backend.dto.auditoria.TipoOperacionResponse;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Auditoria;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AuditoriaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio de auditoría de operaciones críticas (HU-03, RF56).
 * <p>
 * <b>Criterio 1 — registro automático solo si la operación termina con éxito.</b>
 * Los métodos {@code registrar(...)} usan {@link Propagation#MANDATORY}: deben invocarse dentro de la
 * transacción del servicio de negocio. Así el registro de auditoría y el cambio de negocio se
 * confirman o se revierten juntos:
 * <ul>
 *   <li>Si la operación falla (excepción → rollback), su registro de auditoría también se descarta.</li>
 *   <li>Si la auditoría no puede guardarse, la operación crítica tampoco se confirma
 *       (no queda ninguna operación crítica sin rastro).</li>
 * </ul>
 * Si alguien llama a {@code registrar} fuera de una transacción, Spring lanza
 * {@code IllegalTransactionStateException}, lo que evita usos incorrectos.
 * </p>
 * <p>
 * <b>Criterio 2</b> — {@link #buscar(FiltroAuditoria, int, int)}. <b>Criterio 3</b> — este servicio
 * no ofrece ninguna operación de actualización ni borrado.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    /** Tamaño máximo de página permitido en la consulta del log. */
    public static final int TAMANO_MAXIMO_PAGINA = 100;

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Registra una operación crítica ejecutada por el usuario autenticado en la petición actual.
     *
     * @param accion    Tipo de operación crítica.
     * @param entidad   Tabla/recurso afectado (ej. {@code "roles"}).
     * @param entidadId ID del registro afectado.
     * @param datos     Datos afectados (se guardan como JSON en {@code detalle}). Puede ser {@code null}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(TipoOperacionAuditoria accion, String entidad, Long entidadId, Map<String, ?> datos) {
        registrar(accion, entidad, entidadId, datos, obtenerUsuarioAutenticado().orElse(null));
    }

    /**
     * Registra una operación crítica indicando explícitamente el usuario actor. Se usa en flujos
     * públicos sin JWT (ej. registro de cuenta o restablecimiento de contraseña), donde el actor es
     * el propio usuario afectado.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(TipoOperacionAuditoria accion, String entidad, Long entidadId,
                          Map<String, ?> datos, Usuario actor) {
        Auditoria registro = Auditoria.builder()
                .usuario(actor)
                .accion(accion.name())
                .entidad(entidad)
                .entidadId(entidadId)
                .detalle(datos == null ? null : DetalleAuditoria.aJson(datos))
                .build();
        auditoriaRepository.save(registro);
        log.info("Auditoría: {} sobre {} #{} por usuario {}", accion, entidad, entidadId,
                actor != null ? actor.getId() : "SISTEMA");
    }

    /**
     * Consulta paginada del log aplicando los filtros recibidos (Criterio 2).
     *
     * @throws SolicitudInvalidaException si los parámetros son incoherentes.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaResponse> buscar(FiltroAuditoria filtro, int pagina, int tamano) {
        validar(filtro, pagina, tamano);
        return PaginaResponse.de(
                auditoriaRepository.buscar(filtro, PageRequest.of(pagina, tamano)),
                AuditoriaResponse::fromEntity
        );
    }

    /** Obtiene un registro puntual del log. */
    @Transactional(readOnly = true)
    public AuditoriaResponse obtenerPorId(Long id) {
        return auditoriaRepository.findById(id)
                .map(AuditoriaResponse::fromEntity)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el registro de auditoría con ID: " + id));
    }

    /** Catálogo de tipos de operación auditables, para el filtro del frontend. */
    public List<TipoOperacionResponse> listarTiposOperacion() {
        return Arrays.stream(TipoOperacionAuditoria.values())
                .map(TipoOperacionResponse::fromEnum)
                .toList();
    }

    private void validar(FiltroAuditoria filtro, int pagina, int tamano) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException("El número de página no puede ser negativo.");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO_PAGINA) {
            throw new SolicitudInvalidaException(
                    "El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO_PAGINA + ".");
        }
        if (filtro == null) {
            return;
        }
        if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
            throw new SolicitudInvalidaException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }
        if (StringUtils.hasText(filtro.accion())) {
            String accion = filtro.accion().trim().toUpperCase();
            boolean existe = Arrays.stream(TipoOperacionAuditoria.values()).anyMatch(t -> t.name().equals(accion));
            if (!existe) {
                throw new SolicitudInvalidaException("El tipo de operación '" + filtro.accion()
                        + "' no existe. Consulte GET /api/v1/auditoria/tipos-operacion.");
            }
        }
    }

    /**
     * Resuelve el usuario autenticado a partir del contexto de seguridad. El filtro JWT deja el
     * correo del usuario como principal (ver {@code JwtAuthenticationFilter}).
     */
    private Optional<Usuario> obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName());
    }
}
