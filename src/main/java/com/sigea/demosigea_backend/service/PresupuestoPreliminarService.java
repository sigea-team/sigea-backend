package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.presupuesto.HistorialRubroResponse;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoPreliminarResponse;
import com.sigea.demosigea_backend.dto.presupuesto.RubroOperacionResponse;
import com.sigea.demosigea_backend.dto.presupuesto.RubroRequest;
import com.sigea.demosigea_backend.dto.presupuesto.RubroResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.RubroDuplicadoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.HistorialRubro;
import com.sigea.demosigea_backend.model.RubroPresupuestal;
import com.sigea.demosigea_backend.model.TipoOperacionRubro;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.HistorialRubroRepository;
import com.sigea.demosigea_backend.repository.RubroPresupuestalRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para elaborar y administrar el presupuesto preliminar de un evento (HU-07, RF06).
 * <ul>
 *   <li><b>Criterio 1:</b> agregar rubros con sus valores estimados; el total del presupuesto
 *       preliminar se calcula como la suma de {@code cantidad × valor unitario} de los rubros vigentes.</li>
 *   <li><b>Criterio 2:</b> editar o eliminar un rubro actualiza el total y deja un registro en
 *       {@code historial_rubros} con los valores anteriores y nuevos. Eliminar es un borrado lógico
 *       ({@code activo = false}).</li>
 *   <li><b>Criterio 3:</b> nombre vacío, valor vacío o valores negativos se rechazan con 400
 *       (Bean Validation en {@link RubroRequest}); el valor 0 se acepta.</li>
 * </ul>
 * <p>
 * <b>Reglas adicionales.</b>
 * </p>
 * <ul>
 *   <li>No puede haber dos rubros vigentes con el mismo nombre en un evento (sin distinguir
 *       mayúsculas). Respaldado por el índice parcial {@value #INDICE_NOMBRE_ACTIVO}.</li>
 *   <li>El presupuesto preliminar solo se modifica mientras el evento está en
 *       {@code en_configuracion} o {@code habilitado} ("antes de su ejecución") y mientras no exista
 *       un presupuesto aprobado (HU-08, flujo alterno de CU-29).</li>
 * </ul>
 * <p>
 * El cambio del rubro y su registro de historial se guardan en la misma transacción: si uno
 * falla, no se guarda ninguno.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresupuestoPreliminarService {

    /** Índice único parcial: un nombre por rubro vigente y evento (changeset hu07-001). */
    static final String INDICE_NOMBRE_ACTIVO = "ux_rubros_presupuestales_nombre_activo";

    /** SQLState estándar de violación de unicidad en PostgreSQL. */
    static final String SQLSTATE_VIOLACION_UNICIDAD = "23505";

    /** Longitud máxima del motivo (columna {@code historial_rubros.motivo}). */
    static final int LONGITUD_MAXIMA_MOTIVO = 255;

    /** Cantidad que se asume cuando el rubro se registra sin cantidad. */
    static final BigDecimal CANTIDAD_POR_DEFECTO = BigDecimal.ONE;

    /** Estados del evento en los que el presupuesto preliminar puede modificarse. */
    static final Set<EstadoEvento> ESTADOS_PRESUPUESTO_EDITABLE =
            EnumSet.of(EstadoEvento.en_configuracion, EstadoEvento.habilitado);

    private final RubroPresupuestalRepository rubroRepository;
    private final HistorialRubroRepository historialRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    // ------------------------------------------------------------------
    // Consulta (Criterio 1)
    // ------------------------------------------------------------------

    /**
     * Devuelve el presupuesto preliminar del evento con sus rubros y el total calculado.
     *
     * @param eventoId          ID del evento.
     * @param incluirEliminados {@code true} para listar también los rubros eliminados (no suman al total).
     * @return Rubros, total y si el presupuesto todavía es editable.
     * @throws RecursoNoEncontradoException si el evento no existe.
     */
    @Transactional(readOnly = true)
    public PresupuestoPreliminarResponse consultar(Long eventoId, boolean incluirEliminados) {
        Evento evento = buscarEvento(eventoId);
        List<RubroPresupuestal> vigentes = rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(eventoId);
        List<RubroPresupuestal> listado = incluirEliminados
                ? rubroRepository.findByEvento_IdOrderByActivoDescIdAsc(eventoId)
                : vigentes;
        boolean aprobado = rubroRepository.existePresupuestoAprobado(eventoId);

        return new PresupuestoPreliminarResponse(
                evento.getId(),
                evento.getNombre(),
                evento.getEstado(),
                listado.stream().map(RubroResponse::fromEntity).toList(),
                vigentes.size(),
                calcularTotal(vigentes),
                aprobado,
                !aprobado && ESTADOS_PRESUPUESTO_EDITABLE.contains(evento.getEstado())
        );
    }

    // ------------------------------------------------------------------
    // Criterio 1: agregar rubro
    // ------------------------------------------------------------------

    /**
     * Agrega un rubro al presupuesto preliminar y devuelve el total actualizado.
     *
     * @param eventoId ID del evento.
     * @param request  Nombre, cantidad (opcional, por defecto 1) y valor unitario del rubro.
     * @return Rubro creado y total del presupuesto.
     * @throws RecursoNoEncontradoException  si el evento no existe.
     * @throws OperacionNoPermitidaException si el presupuesto ya no se puede modificar.
     * @throws RubroDuplicadoException       si ya existe un rubro vigente con ese nombre.
     */
    @Transactional
    public RubroOperacionResponse agregar(Long eventoId, RubroRequest request) {
        Evento evento = buscarEvento(eventoId);
        validarPresupuestoEditable(evento);

        String nombre = normalizarNombre(request.nombre());
        if (rubroRepository.existsByEvento_IdAndNombreIgnoreCaseAndActivoTrue(eventoId, nombre)) {
            throw duplicado(nombre, evento);
        }

        List<RubroPresupuestal> vigentes = rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(eventoId);
        BigDecimal totalAnterior = calcularTotal(vigentes);

        RubroPresupuestal rubro = RubroPresupuestal.builder()
                .evento(evento)
                .nombre(nombre)
                .cantidad(escalar(request.cantidad() != null ? request.cantidad() : CANTIDAD_POR_DEFECTO))
                .valorUnitarioProyectado(escalar(request.valorUnitarioProyectado()))
                .activo(true)
                .build();
        RubroPresupuestal guardado = guardarControlandoDuplicado(rubro, evento);

        BigDecimal totalNuevo = totalAnterior.add(guardado.getSubtotal());
        registrarHistorial(TipoOperacionRubro.creacion, guardado, evento, null, Valores.de(guardado),
                totalAnterior, totalNuevo, request.motivo());

        log.info("Rubro {} agregado al presupuesto del evento {}: '{}' subtotal {} (total {} -> {})",
                guardado.getId(), evento.getId(), guardado.getNombre(), guardado.getSubtotal(), totalAnterior, totalNuevo);
        return new RubroOperacionResponse(RubroResponse.fromEntity(guardado), totalNuevo, vigentes.size() + 1);
    }

    // ------------------------------------------------------------------
    // Criterio 2: editar rubro
    // ------------------------------------------------------------------

    /**
     * Edita un rubro vigente, recalcula el total y registra los valores anteriores y nuevos en el
     * historial. Si no cambió ningún valor, no se registra historial.
     *
     * @param eventoId ID del evento.
     * @param rubroId  ID del rubro.
     * @param request  Nuevos valores del rubro.
     * @return Rubro editado y total del presupuesto.
     * @throws RecursoNoEncontradoException  si el evento o el rubro no existen.
     * @throws OperacionNoPermitidaException si el rubro fue eliminado o el presupuesto ya no se puede modificar.
     * @throws RubroDuplicadoException       si otro rubro vigente ya tiene el nuevo nombre.
     */
    @Transactional
    public RubroOperacionResponse actualizar(Long eventoId, Long rubroId, RubroRequest request) {
        Evento evento = buscarEvento(eventoId);
        validarPresupuestoEditable(evento);
        RubroPresupuestal rubro = buscarRubroVigente(eventoId, rubroId);

        String nombre = normalizarNombre(request.nombre());
        BigDecimal cantidad = escalar(request.cantidad() != null ? request.cantidad() : CANTIDAD_POR_DEFECTO);
        BigDecimal valorUnitario = escalar(request.valorUnitarioProyectado());

        List<RubroPresupuestal> vigentes = rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(eventoId);
        // Se calcula ANTES de modificar el rubro: la lista contiene la misma instancia gestionada.
        BigDecimal totalAnterior = calcularTotal(vigentes);
        Valores antes = Valores.de(rubro);

        if (antes.nombre().equals(nombre) && antes.cantidad().compareTo(cantidad) == 0
                && antes.valorUnitario().compareTo(valorUnitario) == 0) {
            log.debug("Edición sin cambios del rubro {}: no se registra historial", rubroId);
            return new RubroOperacionResponse(RubroResponse.fromEntity(rubro), totalAnterior, vigentes.size());
        }

        if (rubroRepository.existsByEvento_IdAndNombreIgnoreCaseAndActivoTrueAndIdNot(eventoId, nombre, rubroId)) {
            throw duplicado(nombre, evento);
        }

        rubro.setNombre(nombre);
        rubro.setCantidad(cantidad);
        rubro.setValorUnitarioProyectado(valorUnitario);
        RubroPresupuestal actualizado = guardarControlandoDuplicado(rubro, evento);

        BigDecimal totalNuevo = totalAnterior.subtract(antes.subtotal()).add(actualizado.getSubtotal());
        registrarHistorial(TipoOperacionRubro.edicion, actualizado, evento, antes, Valores.de(actualizado),
                totalAnterior, totalNuevo, request.motivo());

        log.info("Rubro {} editado en el evento {} (total {} -> {})", rubroId, evento.getId(), totalAnterior, totalNuevo);
        return new RubroOperacionResponse(RubroResponse.fromEntity(actualizado), totalNuevo, vigentes.size());
    }

    // ------------------------------------------------------------------
    // Criterio 2: eliminar rubro
    // ------------------------------------------------------------------

    /**
     * Elimina un rubro del presupuesto vigente (borrado lógico), recalcula el total y registra en el
     * historial los valores que tenía.
     *
     * @param eventoId ID del evento.
     * @param rubroId  ID del rubro.
     * @param motivo   Justificación opcional.
     * @return Rubro eliminado ({@code activo = false}) y total del presupuesto.
     * @throws RecursoNoEncontradoException  si el evento o el rubro no existen.
     * @throws OperacionNoPermitidaException si el rubro ya estaba eliminado o el presupuesto no se puede modificar.
     * @throws SolicitudInvalidaException    si el motivo supera los 255 caracteres.
     */
    @Transactional
    public RubroOperacionResponse eliminar(Long eventoId, Long rubroId, String motivo) {
        if (motivo != null && motivo.length() > LONGITUD_MAXIMA_MOTIVO) {
            throw new SolicitudInvalidaException(
                    "El motivo no puede superar los " + LONGITUD_MAXIMA_MOTIVO + " caracteres.");
        }
        Evento evento = buscarEvento(eventoId);
        validarPresupuestoEditable(evento);
        RubroPresupuestal rubro = buscarRubroVigente(eventoId, rubroId);

        List<RubroPresupuestal> vigentes = rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(eventoId);
        BigDecimal totalAnterior = calcularTotal(vigentes);
        Valores antes = Valores.de(rubro);

        rubro.desactivar();
        RubroPresupuestal eliminado = rubroRepository.save(rubro);

        BigDecimal totalNuevo = totalAnterior.subtract(antes.subtotal());
        registrarHistorial(TipoOperacionRubro.eliminacion, eliminado, evento, antes, null,
                totalAnterior, totalNuevo, motivo);

        log.info("Rubro {} eliminado del presupuesto del evento {} (total {} -> {})",
                rubroId, evento.getId(), totalAnterior, totalNuevo);
        return new RubroOperacionResponse(RubroResponse.fromEntity(eliminado), totalNuevo,
                Math.max(0, vigentes.size() - 1));
    }

    // ------------------------------------------------------------------
    // Historial (Criterio 2)
    // ------------------------------------------------------------------

    /**
     * Historial completo del presupuesto preliminar del evento (incluye rubros eliminados).
     *
     * @param eventoId ID del evento.
     * @return Registros del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public List<HistorialRubroResponse> historialPresupuesto(Long eventoId) {
        buscarEvento(eventoId);
        return historialRepository.findByEvento_IdOrderByFechaHoraDescIdDesc(eventoId).stream()
                .map(HistorialRubroResponse::fromEntity)
                .toList();
    }

    /**
     * Historial de un rubro (vigente o eliminado).
     *
     * @param eventoId ID del evento.
     * @param rubroId  ID del rubro.
     * @return Registros del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public List<HistorialRubroResponse> historialRubro(Long eventoId, Long rubroId) {
        buscarEvento(eventoId);
        buscarRubro(eventoId, rubroId);
        return historialRepository.findByRubro_IdOrderByFechaHoraDescIdDesc(rubroId).stream()
                .map(HistorialRubroResponse::fromEntity)
                .toList();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /** Valores de un rubro en un momento dado (para el historial y el recálculo del total). */
    record Valores(String nombre, BigDecimal cantidad, BigDecimal valorUnitario) {
        static Valores de(RubroPresupuestal rubro) {
            return new Valores(rubro.getNombre(), rubro.getCantidad(), rubro.getValorUnitarioProyectado());
        }

        BigDecimal subtotal() {
            return RubroPresupuestal.calcularSubtotal(cantidad, valorUnitario);
        }
    }

    /**
     * Total del presupuesto: suma de los subtotales (ya redondeados) de los rubros vigentes, para que
     * el total coincida exactamente con la suma de lo que el usuario ve en cada fila.
     */
    static BigDecimal calcularTotal(List<RubroPresupuestal> vigentes) {
        return vigentes.stream()
                .filter(RubroPresupuestal::isActivo)
                .map(RubroPresupuestal::getSubtotal)
                .reduce(BigDecimal.ZERO.setScale(RubroPresupuestal.ESCALA), BigDecimal::add);
    }

    private void registrarHistorial(TipoOperacionRubro tipo, RubroPresupuestal rubro, Evento evento,
                                    Valores antes, Valores despues,
                                    BigDecimal totalAnterior, BigDecimal totalNuevo, String motivo) {
        HistorialRubro registro = HistorialRubro.builder()
                .rubro(rubro)
                .evento(evento)
                .tipoOperacion(tipo)
                .nombreAnterior(antes != null ? antes.nombre() : null)
                .cantidadAnterior(antes != null ? antes.cantidad() : null)
                .valorUnitarioAnterior(antes != null ? antes.valorUnitario() : null)
                .nombreNuevo(despues != null ? despues.nombre() : null)
                .cantidadNueva(despues != null ? despues.cantidad() : null)
                .valorUnitarioNuevo(despues != null ? despues.valorUnitario() : null)
                .totalPresupuestoAnterior(totalAnterior)
                .totalPresupuestoNuevo(totalNuevo)
                .motivo(StringUtils.hasText(motivo) ? motivo.trim() : null)
                .usuario(obtenerUsuarioAutenticado().orElse(null))
                .build();
        historialRepository.save(registro);
    }

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + id));
    }

    private RubroPresupuestal buscarRubro(Long eventoId, Long rubroId) {
        return rubroRepository.findByIdAndEvento_Id(rubroId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "No se encontró el rubro con ID %d en el presupuesto del evento %d.", rubroId, eventoId)));
    }

    private RubroPresupuestal buscarRubroVigente(Long eventoId, Long rubroId) {
        RubroPresupuestal rubro = buscarRubro(eventoId, rubroId);
        if (!rubro.isActivo()) {
            throw new OperacionNoPermitidaException(String.format(
                    "El rubro '%s' ya fue eliminado del presupuesto preliminar.", rubro.getNombre()));
        }
        return rubro;
    }

    /**
     * El presupuesto preliminar solo se modifica antes de la ejecución del evento y mientras no
     * exista un presupuesto aprobado.
     */
    private void validarPresupuestoEditable(Evento evento) {
        if (!ESTADOS_PRESUPUESTO_EDITABLE.contains(evento.getEstado())) {
            String permitidos = ESTADOS_PRESUPUESTO_EDITABLE.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(" o "));
            throw new OperacionNoPermitidaException(String.format(
                    "El presupuesto preliminar del evento '%s' ya no puede modificarse (estado actual: %s). "
                            + "Solo se permiten cambios mientras el evento está en %s.",
                    evento.getNombre(), evento.getEstado(), permitidos));
        }
        if (rubroRepository.existePresupuestoAprobado(evento.getId())) {
            throw new OperacionNoPermitidaException(String.format(
                    "El evento '%s' ya tiene un presupuesto aprobado; el presupuesto preliminar no puede modificarse.",
                    evento.getNombre()));
        }
    }

    /** Quita espacios al inicio y al final y reduce los espacios repetidos a uno solo. */
    static String normalizarNombre(String nombre) {
        return nombre == null ? null : nombre.trim().replaceAll("\\s+", " ");
    }

    private static BigDecimal escalar(BigDecimal valor) {
        return valor.setScale(RubroPresupuestal.ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * Guarda con flush inmediato: si dos peticiones simultáneas pasaron la validación de nombre
     * duplicado, el índice {@value #INDICE_NOMBRE_ACTIVO} lo detecta aquí y se responde 409 en vez de 500.
     */
    private RubroPresupuestal guardarControlandoDuplicado(RubroPresupuestal rubro, Evento evento) {
        try {
            return rubroRepository.saveAndFlush(rubro);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionNombreActivo(ex)) {
                log.warn("El índice {} rechazó un rubro duplicado en el evento {}. Se responde 409.",
                        INDICE_NOMBRE_ACTIVO, evento.getId());
                throw duplicado(rubro.getNombre(), evento);
            }
            throw ex;
        }
    }

    /**
     * Determina si la excepción corresponde al índice {@value #INDICE_NOMBRE_ACTIVO}, usando el
     * SQLState y el nombre de la restricción (no el texto del mensaje).
     */
    static boolean esViolacionNombreActivo(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause() == causa ? null : causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                String restriccion = violacion.getConstraintName();
                return SQLSTATE_VIOLACION_UNICIDAD.equals(violacion.getSQLState())
                        && restriccion != null
                        && INDICE_NOMBRE_ACTIVO.equalsIgnoreCase(restriccion.replace("\"", ""));
            }
        }
        return false;
    }

    private static RubroDuplicadoException duplicado(String nombre, Evento evento) {
        return new RubroDuplicadoException(String.format(
                "Ya existe un rubro llamado '%s' en el presupuesto preliminar del evento '%s'.",
                nombre, evento.getNombre()));
    }

    /**
     * Usuario autenticado de la petición. El filtro JWT deja el correo como principal
     * (ver {@code JwtAuthenticationFilter}).
     */
    private Optional<Usuario> obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName());
    }
}
