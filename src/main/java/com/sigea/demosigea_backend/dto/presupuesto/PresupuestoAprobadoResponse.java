package com.sigea.demosigea_backend.dto.presupuesto;

import com.sigea.demosigea_backend.model.EstadoPresupuestoAprobado;
import com.sigea.demosigea_backend.model.PresupuestoAprobado;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta con la información del presupuesto aprobado (versión oficial o histórica) de un evento (HU-08).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Información detallada de una versión del presupuesto aprobado")
public record PresupuestoAprobadoResponse(

        @Schema(description = "Identificador único de la aprobación", example = "1")
        Long id,

        @Schema(description = "ID del evento", example = "10")
        Long eventoId,

        @Schema(description = "Nombre del evento", example = "Congreso de Ingeniería de Sistemas")
        String eventoNombre,

        @Schema(description = "Monto oficial aprobado para la versión", example = "50000000.00")
        BigDecimal valorTotalAprobado,

        @Schema(description = "Total del presupuesto preliminar proyectado del evento", example = "48500000.00")
        BigDecimal valorPreliminarProyectado,

        @Schema(description = "Diferencia respecto al presupuesto preliminar (aprobado - preliminar)", example = "1500000.00")
        BigDecimal diferenciaConPreliminar,

        @Schema(description = "URL o soporte documental de aprobación", example = "https://archivos.ufps.edu.co/presupuestos/acta-2026.pdf")
        String evidenciaUrl,

        @Schema(description = "Fecha y hora en que se formalizó la aprobación", example = "2026-10-11T12:00:00")
        LocalDateTime fechaAprobacion,

        @Schema(description = "ID del usuario institucional que aprobó", example = "1")
        Long aprobadoPorId,

        @Schema(description = "Nombre o correo del usuario que aprobó", example = "Carlos Gómez")
        String aprobadoPorNombre,

        @Schema(description = "Estado de la versión: 'aprobado' (oficial vigente) o 'historico' (previa)", example = "aprobado")
        EstadoPresupuestoAprobado estado,

        @Schema(description = "Indica si corresponde a la versión oficial vigente del evento", example = "true")
        boolean esOficial
) {

    /**
     * Construye la respuesta a partir de la entidad {@link PresupuestoAprobado} y el valor preliminar.
     *
     * @param pa         Entidad de presupuesto aprobado.
     * @param preliminar Monto preliminar proyectado del evento (puede ser {@code null}).
     * @return DTO de respuesta.
     */
    public static PresupuestoAprobadoResponse fromEntity(PresupuestoAprobado pa, BigDecimal preliminar) {
        String nombreAprobador = null;
        if (pa.getAprobadoPor() != null && pa.getAprobadoPor().getPersona() != null) {
            String nombres = pa.getAprobadoPor().getPersona().getNombres();
            String apellidos = pa.getAprobadoPor().getPersona().getApellidos();
            nombreAprobador = ((nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "")).trim();
            if (nombreAprobador.isEmpty()) {
                nombreAprobador = pa.getAprobadoPor().getPersona().getCorreo();
            }
        }

        BigDecimal diff = null;
        if (preliminar != null && pa.getValorTotalAprobado() != null) {
            diff = pa.getValorTotalAprobado().subtract(preliminar);
        }

        return new PresupuestoAprobadoResponse(
                pa.getId(),
                pa.getEvento() != null ? pa.getEvento().getId() : null,
                pa.getEvento() != null ? pa.getEvento().getNombre() : null,
                pa.getValorTotalAprobado(),
                preliminar,
                diff,
                pa.getEvidenciaUrl(),
                pa.getFechaAprobacion(),
                pa.getAprobadoPor() != null ? pa.getAprobadoPor().getId() : null,
                nombreAprobador,
                pa.getEstado(),
                pa.esOficial()
        );
    }
}
