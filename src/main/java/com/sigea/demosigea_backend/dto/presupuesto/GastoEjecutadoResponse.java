package com.sigea.demosigea_backend.dto.presupuesto;

import com.sigea.demosigea_backend.model.GastoEjecutado;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.RubroPresupuestal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Gasto ejecutado registrado contra un rubro (HU-09).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Gasto ejecutado")
public record GastoEjecutadoResponse(
        @Schema(example = "15") Long id,
        @Schema(example = "3") Long eventoId,
        @Schema(example = "7") Long rubroId,
        @Schema(example = "Transporte de conferencistas") String rubroNombre,
        @Schema(example = "Tiquetes aéreos del conferencista invitado") String descripcion,
        @Schema(example = "350000.00") BigDecimal valor,
        @Schema(example = "2026-10-14") LocalDate fechaGasto,
        @Schema(example = "https://archivos.ufps.edu.co/gastos/factura-001.pdf") String soporteUrl,
        @Schema(example = "1") Long registradoPorId,
        @Schema(example = "Carlos Gómez") String registradoPorNombre
) {

    /**
     * Construye el DTO a partir de la entidad.
     *
     * @param gasto Entidad JPA.
     * @return DTO inmutable.
     */
    public static GastoEjecutadoResponse fromEntity(GastoEjecutado gasto) {
        RubroPresupuestal rubro = gasto.getRubro();
        Long registradoPorId = null;
        String registradoPorNombre = null;
        if (gasto.getRegistradoPor() != null) {
            registradoPorId = gasto.getRegistradoPor().getId();
            registradoPorNombre = nombre(gasto.getRegistradoPor().getPersona());
        }
        return new GastoEjecutadoResponse(
                gasto.getId(),
                rubro != null && rubro.getEvento() != null ? rubro.getEvento().getId() : null,
                rubro != null ? rubro.getId() : null,
                rubro != null ? rubro.getNombre() : null,
                gasto.getDescripcion(),
                gasto.getValor(),
                gasto.getFechaGasto(),
                gasto.getSoporteUrl(),
                registradoPorId,
                registradoPorNombre
        );
    }

    private static String nombre(Persona persona) {
        if (persona == null) {
            return null;
        }
        String nombres = persona.getNombres() != null ? persona.getNombres() : "";
        String apellidos = persona.getApellidos() != null ? persona.getApellidos() : "";
        String completo = (nombres + " " + apellidos).trim();
        return completo.isEmpty() ? persona.getCorreo() : completo;
    }
}
