package com.sigea.demosigea_backend.dto.sala;

import com.sigea.demosigea_backend.model.Sala;
import com.sigea.demosigea_backend.model.TipoSala;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;

/**
 * DTO de respuesta con los datos de una sala, sus franjas de disponibilidad,
 * eventos a los que está vinculada y horarios ocupados por actividades — HU-Salas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Sala con su disponibilidad horaria, eventos asociados y ocupaciones")
public record SalaResponse(
        @Schema(description = "ID de la sala", example = "3")
        Long id,

        @Schema(description = "ID del evento principal (opcional si es institucional)", example = "1")
        Long eventoId,

        @Schema(description = "Nombre de la sala", example = "Aula Magna")
        String nombre,

        @Schema(description = "Ubicación física o URL", example = "Edificio A, piso 2")
        String ubicacion,

        @Schema(description = "Tipo de sala: fisica o virtual")
        TipoSala tipo,

        @Schema(description = "Aforo máximo", example = "200")
        Integer aforoMaximo,

        @Schema(description = "Franjas de disponibilidad horaria registradas")
        List<DisponibilidadSalaResponse> disponibilidades,

        @Schema(description = "HU-21: la sala admite actividades simultáneas", example = "false")
        boolean permiteSimultaneidad,

        @Schema(description = "Eventos a los que la sala está vinculada actualmente")
        List<EventoSalaInfo> eventosVinculados,

        @Schema(description = "Franjas ocupadas por actividades asignadas en eventos")
        List<OcupacionSalaResponse> ocupaciones,

        @Schema(description = "HU-20 Criterio 5: actividades que quedaron fuera de la nueva disponibilidad al editar "
                + "la sala. Vacía en las demás operaciones; no bloquea el guardado.")
        List<ConflictoDisponibilidadResponse> conflictosDisponibilidad
) {
    /**
     * Constructor de conveniencia retrocompatible con 10 argumentos (sin conflictos de disponibilidad).
     */
    public SalaResponse(
            Long id,
            Long eventoId,
            String nombre,
            String ubicacion,
            TipoSala tipo,
            Integer aforoMaximo,
            List<DisponibilidadSalaResponse> disponibilidades,
            boolean permiteSimultaneidad,
            List<EventoSalaInfo> eventosVinculados,
            List<OcupacionSalaResponse> ocupaciones
    ) {
        this(id, eventoId, nombre, ubicacion, tipo, aforoMaximo, disponibilidades, permiteSimultaneidad,
                eventosVinculados, ocupaciones, Collections.emptyList());
    }

    /**
     * Constructor de conveniencia retrocompatible con 8 argumentos.
     */
    public SalaResponse(
            Long id,
            Long eventoId,
            String nombre,
            String ubicacion,
            TipoSala tipo,
            Integer aforoMaximo,
            List<DisponibilidadSalaResponse> disponibilidades,
            boolean permiteSimultaneidad
    ) {
        this(id, eventoId, nombre, ubicacion, tipo, aforoMaximo, disponibilidades, permiteSimultaneidad,
                Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Copia del DTO con los conflictos de disponibilidad detectados al editar la sala (HU-20, Criterio 5).
     *
     * @param conflictos actividades que quedaron fuera de la nueva disponibilidad.
     * @return nuevo DTO con los conflictos.
     */
    public SalaResponse conConflictos(List<ConflictoDisponibilidadResponse> conflictos) {
        return new SalaResponse(id, eventoId, nombre, ubicacion, tipo, aforoMaximo, disponibilidades,
                permiteSimultaneidad, eventosVinculados, ocupaciones,
                conflictos != null ? conflictos : Collections.emptyList());
    }

    /**
     * Construye el DTO desde la entidad JPA con sus disponibilidades ya cargadas.
     *
     * @param sala             Entidad {@link Sala}.
     * @param disponibilidades Lista de franjas de disponibilidad.
     * @return DTO inmutable.
     */
    public static SalaResponse fromEntity(Sala sala, List<DisponibilidadSalaResponse> disponibilidades) {
        return fromEntity(sala, disponibilidades, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Construye el DTO completo con disponibilidades, eventos vinculados y ocupaciones activas.
     */
    public static SalaResponse fromEntity(
            Sala sala,
            List<DisponibilidadSalaResponse> disponibilidades,
            List<EventoSalaInfo> eventosVinculados,
            List<OcupacionSalaResponse> ocupaciones
    ) {
        Long evId = sala.getEvento() != null ? sala.getEvento().getId() : null;
        return new SalaResponse(
                sala.getId(),
                evId,
                sala.getNombre(),
                sala.getUbicacion(),
                sala.getTipo(),
                sala.getAforoMaximo(),
                disponibilidades != null ? disponibilidades : Collections.emptyList(),
                sala.isPermiteSimultaneidad(),
                eventosVinculados != null ? eventosVinculados : Collections.emptyList(),
                ocupaciones != null ? ocupaciones : Collections.emptyList()
        );
    }
}
