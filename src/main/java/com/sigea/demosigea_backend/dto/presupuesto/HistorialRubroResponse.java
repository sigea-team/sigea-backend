package com.sigea.demosigea_backend.dto.presupuesto;

import com.sigea.demosigea_backend.model.HistorialRubro;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.RubroPresupuestal;
import com.sigea.demosigea_backend.model.TipoOperacionRubro;
import com.sigea.demosigea_backend.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DTO con un registro del historial de modificaciones de un rubro (HU-07, Criterio 2).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Registro del historial de un rubro presupuestal")
public record HistorialRubroResponse(
        @Schema(example = "15") Long id,
        @Schema(example = "7") Long rubroId,
        @Schema(example = "edicion") TipoOperacionRubro tipoOperacion,
        @Schema(example = "Transporte") String nombreAnterior,
        @Schema(example = "2.00") BigDecimal cantidadAnterior,
        @Schema(example = "300000.00") BigDecimal valorUnitarioAnterior,
        @Schema(example = "600000.00") BigDecimal subtotalAnterior,
        @Schema(example = "Transporte de conferencistas") String nombreNuevo,
        @Schema(example = "2.00") BigDecimal cantidadNueva,
        @Schema(example = "350000.00") BigDecimal valorUnitarioNuevo,
        @Schema(example = "700000.00") BigDecimal subtotalNuevo,
        @Schema(example = "5150000.00") BigDecimal totalPresupuestoAnterior,
        @Schema(example = "5250000.00") BigDecimal totalPresupuestoNuevo,
        @Schema(example = "Ajuste por tarifa 2026") String motivo,
        @Schema(example = "2") Long usuarioId,
        @Schema(example = "Cristian Rodríguez") String usuarioNombre,
        LocalDateTime fechaHora
) {
    /**
     * Construye el DTO a partir de la entidad (con el usuario y su persona cargados).
     *
     * @param h Registro de historial.
     * @return DTO inmutable.
     */
    public static HistorialRubroResponse fromEntity(HistorialRubro h) {
        Usuario usuario = h.getUsuario();
        boolean tieneAnterior = h.getNombreAnterior() != null;
        boolean tieneNuevo = h.getNombreNuevo() != null;
        return new HistorialRubroResponse(
                h.getId(),
                h.getRubro() != null ? h.getRubro().getId() : null,
                h.getTipoOperacion(),
                h.getNombreAnterior(),
                h.getCantidadAnterior(),
                h.getValorUnitarioAnterior(),
                tieneAnterior ? RubroPresupuestal.calcularSubtotal(h.getCantidadAnterior(), h.getValorUnitarioAnterior()) : null,
                h.getNombreNuevo(),
                h.getCantidadNueva(),
                h.getValorUnitarioNuevo(),
                tieneNuevo ? RubroPresupuestal.calcularSubtotal(h.getCantidadNueva(), h.getValorUnitarioNuevo()) : null,
                h.getTotalPresupuestoAnterior(),
                h.getTotalPresupuestoNuevo(),
                h.getMotivo(),
                usuario != null ? usuario.getId() : null,
                usuario != null ? nombreCompleto(usuario.getPersona()) : null,
                h.getFechaHora()
        );
    }

    private static String nombreCompleto(Persona persona) {
        if (persona == null) {
            return null;
        }
        return Stream.of(persona.getNombres(), persona.getApellidos())
                .filter(parte -> parte != null && !parte.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }
}
