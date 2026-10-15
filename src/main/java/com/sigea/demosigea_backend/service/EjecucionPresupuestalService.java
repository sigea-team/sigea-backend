package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.presupuesto.EjecucionPresupuestalResponse;
import com.sigea.demosigea_backend.dto.presupuesto.EjecucionRubroResponse;
import com.sigea.demosigea_backend.dto.presupuesto.GastoEjecutadoRequest;
import com.sigea.demosigea_backend.dto.presupuesto.GastoEjecutadoResponse;
import com.sigea.demosigea_backend.dto.presupuesto.GastoRegistradoResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.EstadoPresupuestoAprobado;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.GastoEjecutado;
import com.sigea.demosigea_backend.model.PresupuestoAprobado;
import com.sigea.demosigea_backend.model.RubroPresupuestal;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.GastoEjecutadoRepository;
import com.sigea.demosigea_backend.repository.GastoEjecutadoRepository.AcumuladoRubro;
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para registrar gastos ejecutados y calcular la ejecución presupuestal de un
 * evento (HU-09, RF11 + RF12).
 * <ul>
 *   <li><b>Criterio 1:</b> con un presupuesto aprobado vigente (HU-08), registrar un gasto contra un
 *       rubro vigente lo guarda y devuelve el acumulado actualizado del rubro y el porcentaje de
 *       ejecución recalculado del evento.</li>
 *   <li><b>Gastos que superan el rubro:</b> se permiten siempre. El gasto se registra y la respuesta
 *       lo marca ({@code excedioDisponible}, {@code advertencia} y {@code excedido} en el rubro) solo
 *       como información.</li>
 *   <li><b>Criterio 3:</b> gasto sin valor (o con valor negativo) → 400. El valor 0 se acepta y el soporte es opcional. Bean
 *       Validation en {@link GastoEjecutadoRequest}; el servicio repite la regla por si se invoca sin
 *       {@code @Valid}.</li>
 *   <li><b>Criterio 4:</b> sin gastos, la ejecución es 0 % y no se genera error.</li>
 * </ul>
 * <p>
 * Los acumulados y porcentajes no se persisten: se calculan siempre con {@code SUM} sobre
 * {@code gastos_ejecutados}, por lo que no pueden quedar desactualizados.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EjecucionPresupuestalService {

    /** Nombre de la entidad en el log de auditoría. */
    static final String ENTIDAD_GASTO = "gastos_ejecutados";

    private static final int ESCALA = 2;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA);

    private final GastoEjecutadoRepository gastoRepository;
    private final RubroPresupuestalRepository rubroRepository;
    private final PresupuestoAprobadoRepository presupuestoAprobadoRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    // ------------------------------------------------------------------
    // Criterios 1 y 3: registrar gasto
    // ------------------------------------------------------------------

    /**
     * Registra un gasto ejecutado contra un rubro del evento. Si el gasto supera el disponible del
     * rubro se registra igual y la respuesta lo indica.
     *
     * @param eventoId ID del evento.
     * @param request  Datos del gasto.
     * @return Gasto guardado con el acumulado del rubro y la ejecución del evento recalculados.
     * @throws RecursoNoEncontradoException  si el evento o el rubro no existen.
     * @throws OperacionNoPermitidaException si el evento está cerrado, no tiene presupuesto aprobado
     *                                       o el rubro fue eliminado.
     * @throws SolicitudInvalidaException    si falta el valor o no hay usuario autenticado.
     */
    @Transactional
    public GastoRegistradoResponse registrarGasto(Long eventoId, GastoEjecutadoRequest request) {
        validarDatosGasto(request);

        Evento evento = buscarEvento(eventoId);
        if (evento.getEstado() == EstadoEvento.cerrado) {
            throw new OperacionNoPermitidaException("No se pueden registrar gastos en un evento cerrado.");
        }
        PresupuestoAprobado aprobado = presupuestoAprobadoRepository
                .findByEvento_IdAndEstado(eventoId, EstadoPresupuestoAprobado.aprobado)
                .orElseThrow(() -> new OperacionNoPermitidaException(
                        "No se pueden registrar gastos: el evento no tiene un presupuesto aprobado (HU-08)."));

        RubroPresupuestal rubro = rubroRepository.findByIdAndEvento_Id(request.rubroId(), eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el rubro con ID " + request.rubroId() + " en el evento " + eventoId + "."));
        if (!rubro.isActivo()) {
            throw new OperacionNoPermitidaException(
                    "El rubro '" + rubro.getNombre() + "' fue eliminado del presupuesto y no admite gastos.");
        }

        Usuario usuario = obtenerUsuarioAutenticado()
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "No se pudo determinar el usuario que registra el gasto. Inicie sesión nuevamente."));

        BigDecimal valor = escalar(request.valor());
        BigDecimal presupuestado = escalar(rubro.getSubtotal());
        BigDecimal ejecutadoAntes = escalar(gastoRepository.sumarPorRubro(rubro.getId()));
        BigDecimal disponibleAntes = presupuestado.subtract(ejecutadoAntes);

        GastoEjecutado gasto = gastoRepository.save(GastoEjecutado.builder()
                .rubro(rubro)
                .valor(valor)
                .fechaGasto(request.fechaGasto())
                .soporteUrl(textoOpcional(request.soporteUrl()))
                .descripcion(textoOpcional(request.descripcion()))
                .registradoPor(usuario)
                .build());

        // Criterio 1: acumulado del rubro y ejecución del evento actualizados
        BigDecimal ejecutadoRubro = ejecutadoAntes.add(valor);
        long gastosRubro = gastoRepository.countByRubro_Id(rubro.getId());
        EjecucionRubroResponse ejecucionRubro = ejecucionRubro(rubro, ejecutadoRubro, gastosRubro);

        BigDecimal valorAprobado = escalar(aprobado.getValorTotalAprobado());
        BigDecimal totalEjecutado = escalar(gastoRepository.sumarPorEvento(eventoId));
        BigDecimal porcentaje = porcentaje(totalEjecutado, valorAprobado);

        // Exceso sobre el disponible del rubro: permitido, solo se informa
        boolean excede = valor.compareTo(disponibleAntes) > 0;
        String advertencia = null;
        if (excede) {
            BigDecimal exceso = valor.subtract(disponibleAntes.max(BigDecimal.ZERO));
            advertencia = "El gasto de $" + pesos(valor) + " supera el disponible del rubro '" + rubro.getNombre()
                    + "' ($" + pesos(disponibleAntes) + ") en $" + pesos(exceso) + ".";
        }

        auditoriaService.registrar(
                TipoOperacionAuditoria.GASTO_REGISTRADO,
                ENTIDAD_GASTO,
                gasto.getId(),
                DetalleAuditoria.de(
                        "eventoId", eventoId,
                        "rubroId", rubro.getId(),
                        "rubro", rubro.getNombre(),
                        "valor", valor,
                        "fechaGasto", gasto.getFechaGasto(),
                        "soporteUrl", gasto.getSoporteUrl(),
                        "ejecutadoRubroAnterior", ejecutadoAntes,
                        "ejecutadoRubroNuevo", ejecutadoRubro,
                        "presupuestadoRubro", presupuestado,
                        "excedioDisponible", excede,
                        "totalEjecutadoEvento", totalEjecutado,
                        "porcentajeEjecucionEvento", porcentaje
                )
        );

        log.info("Gasto #{} de {} registrado en el rubro {} del evento {} (ejecución {}%, exceso={})",
                gasto.getId(), valor, rubro.getId(), eventoId, porcentaje, excede);

        return new GastoRegistradoResponse(
                GastoEjecutadoResponse.fromEntity(gasto),
                ejecucionRubro,
                valorAprobado,
                totalEjecutado,
                porcentaje,
                excede,
                advertencia
        );
    }

    // ------------------------------------------------------------------
    // Criterio 4: consultar la ejecución
    // ------------------------------------------------------------------

    /**
     * Calcula el indicador de ejecución presupuestal del evento y la ejecución de cada rubro vigente.
     * Sin gastos o sin presupuesto aprobado devuelve 0 % sin error (Criterio 4).
     *
     * @param eventoId ID del evento.
     * @return Indicador de ejecución.
     * @throws RecursoNoEncontradoException si el evento no existe.
     */
    @Transactional(readOnly = true)
    public EjecucionPresupuestalResponse consultarEjecucion(Long eventoId) {
        Evento evento = buscarEvento(eventoId);

        Optional<PresupuestoAprobado> aprobado = presupuestoAprobadoRepository
                .findByEvento_IdAndEstado(eventoId, EstadoPresupuestoAprobado.aprobado);
        BigDecimal valorAprobado = aprobado.map(pa -> escalar(pa.getValorTotalAprobado())).orElse(CERO);

        Map<Long, AcumuladoRubro> acumulados = gastoRepository.acumuladosPorRubro(eventoId).stream()
                .collect(Collectors.toMap(AcumuladoRubro::getRubroId, Function.identity()));

        List<RubroPresupuestal> rubros = rubroRepository.findByEvento_IdAndActivoTrueOrderByIdAsc(eventoId);
        List<EjecucionRubroResponse> ejecucionRubros = rubros.stream()
                .map(rubro -> {
                    AcumuladoRubro acumulado = acumulados.get(rubro.getId());
                    BigDecimal ejecutado = acumulado != null ? escalar(acumulado.getTotalEjecutado()) : CERO;
                    long cantidad = acumulado != null && acumulado.getCantidadGastos() != null
                            ? acumulado.getCantidadGastos() : 0L;
                    return ejecucionRubro(rubro, ejecutado, cantidad);
                })
                .toList();

        BigDecimal totalPresupuestado = ejecucionRubros.stream()
                .map(EjecucionRubroResponse::presupuestado)
                .reduce(CERO, BigDecimal::add);
        BigDecimal totalEjecutado = escalar(gastoRepository.sumarPorEvento(eventoId));
        long cantidadGastos = acumulados.values().stream()
                .map(AcumuladoRubro::getCantidadGastos)
                .filter(c -> c != null)
                .mapToLong(Long::longValue)
                .sum();

        return new EjecucionPresupuestalResponse(
                evento.getId(),
                evento.getNombre(),
                aprobado.isPresent(),
                valorAprobado,
                totalPresupuestado,
                totalEjecutado,
                valorAprobado.subtract(totalEjecutado),
                porcentaje(totalEjecutado, valorAprobado),
                cantidadGastos,
                ejecucionRubros
        );
    }

    /**
     * Lista los gastos del evento, o solo los de un rubro, del más reciente al más antiguo.
     *
     * @param eventoId ID del evento.
     * @param rubroId  ID del rubro (opcional).
     * @return Gastos registrados (lista vacía si no hay).
     * @throws RecursoNoEncontradoException si el evento o el rubro no existen.
     */
    @Transactional(readOnly = true)
    public List<GastoEjecutadoResponse> listarGastos(Long eventoId, Long rubroId) {
        buscarEvento(eventoId);
        List<GastoEjecutado> gastos;
        if (rubroId != null) {
            rubroRepository.findByIdAndEvento_Id(rubroId, eventoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró el rubro con ID " + rubroId + " en el evento " + eventoId + "."));
            gastos = gastoRepository.findByRubro_IdOrderByFechaGastoDescIdDesc(rubroId);
        } else {
            gastos = gastoRepository.findByRubro_Evento_IdOrderByFechaGastoDescIdDesc(eventoId);
        }
        return gastos.stream().map(GastoEjecutadoResponse::fromEntity).toList();
    }

    /**
     * Consulta un gasto del evento.
     *
     * @param eventoId ID del evento.
     * @param gastoId  ID del gasto.
     * @return Gasto registrado.
     * @throws RecursoNoEncontradoException si el gasto no existe en el evento.
     */
    @Transactional(readOnly = true)
    public GastoEjecutadoResponse obtenerGasto(Long eventoId, Long gastoId) {
        return gastoRepository.findByIdAndRubro_Evento_Id(gastoId, eventoId)
                .map(GastoEjecutadoResponse::fromEntity)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el gasto con ID " + gastoId + " en el evento " + eventoId + "."));
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /** Criterio 3, repetido en el servicio por si se llama sin Bean Validation. */
    private static void validarDatosGasto(GastoEjecutadoRequest request) {
        if (request == null || request.rubroId() == null) {
            throw new SolicitudInvalidaException("El rubro del gasto es obligatorio.");
        }
        if (request.valor() == null) {
            throw new SolicitudInvalidaException("El valor del gasto es obligatorio.");
        }
        if (request.valor().compareTo(BigDecimal.ZERO) < 0) {
            throw new SolicitudInvalidaException("El valor del gasto no puede ser negativo.");
        }
        if (request.fechaGasto() == null) {
            throw new SolicitudInvalidaException("La fecha del gasto es obligatoria.");
        }
        if (request.fechaGasto().isAfter(LocalDate.now())) {
            throw new SolicitudInvalidaException("La fecha del gasto no puede ser futura.");
        }
    }

    private static EjecucionRubroResponse ejecucionRubro(RubroPresupuestal rubro, BigDecimal ejecutado, long cantidad) {
        BigDecimal presupuestado = escalar(rubro.getSubtotal());
        BigDecimal ejecutadoEscalado = escalar(ejecutado);
        return new EjecucionRubroResponse(
                rubro.getId(),
                rubro.getNombre(),
                presupuestado,
                ejecutadoEscalado,
                presupuestado.subtract(ejecutadoEscalado),
                porcentaje(ejecutadoEscalado, presupuestado),
                ejecutadoEscalado.compareTo(presupuestado) > 0,
                cantidad
        );
    }

    /**
     * {@code parte / base × 100} con 2 decimales; 0 cuando la base es 0 (Criterio 4: sin división por cero).
     */
    static BigDecimal porcentaje(BigDecimal parte, BigDecimal base) {
        if (parte == null || base == null || base.signum() <= 0) {
            return CERO;
        }
        return parte.multiply(CIEN).divide(base, ESCALA, RoundingMode.HALF_UP);
    }

    private static BigDecimal escalar(BigDecimal valor) {
        return valor == null ? CERO : valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /** Texto recortado, o {@code null} si llega vacío o solo con espacios. */
    private static String textoOpcional(String texto) {
        return StringUtils.hasText(texto) ? texto.trim() : null;
    }

    /** Formato de pesos para los mensajes: 1.250.000,5 */
    private static String pesos(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO")));
        return formato.format(valor);
    }

    private Evento buscarEvento(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId));
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
