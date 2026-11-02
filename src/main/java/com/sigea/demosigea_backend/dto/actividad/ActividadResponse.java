package com.sigea.demosigea_backend.dto.actividad;

import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.ModalidadEvento;
import com.sigea.demosigea_backend.model.Persona;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Actividad programada en la agenda del evento (HU-21).
 *
 * @param id                  ID de la actividad.
 * @param eventoId            ID del evento.
 * @param nombre              Nombre de la actividad.
 * @param tipoActividadId     ID del tipo de actividad.
 * @param tipoActividadNombre Nombre del tipo de actividad.
 * @param fecha               Fecha.
 * @param horaInicio          Hora de inicio.
 * @param horaFin             Hora de fin.
 * @param salaId              ID de la sala.
 * @param salaNombre          Nombre de la sala.
 * @param ponenteId           ID de la persona ponente (puede ser null).
 * @param ponenteNombre       Nombre del ponente (puede ser null).
 * @param propuestaId         ID de la propuesta presentada (puede ser null).
 * @param lineaTematicaId     ID de la línea temática (puede ser null).
 * @param modalidad           Modalidad de la actividad.
 * @param simultanea          Si se marcó como simultánea.
 * @param estado              Estado de la actividad.
 * @param advertencias        Advertencias aceptadas al guardar (vacía en los listados).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Actividad de la agenda del evento")
public record ActividadResponse(
        Long id,
        Long eventoId,
        String nombre,
        Long tipoActividadId,
        String tipoActividadNombre,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        Long salaId,
        String salaNombre,
        Long ponenteId,
        String ponenteNombre,
        Long propuestaId,
        Long lineaTematicaId,
        ModalidadEvento modalidad,
        boolean simultanea,
        String estado,
        List<AdvertenciaActividadResponse> advertencias
) {

    /** Construye la respuesta sin advertencias (listados y consultas). */
    public static ActividadResponse fromEntity(Actividad a) {
        return fromEntity(a, List.of());
    }

    /** Construye la respuesta con las advertencias aceptadas al guardar. */
    public static ActividadResponse fromEntity(Actividad a, List<AdvertenciaActividadResponse> advertencias) {
        return new ActividadResponse(
                a.getId(),
                a.getEvento() != null ? a.getEvento().getId() : null,
                a.getNombre(),
                a.getTipoActividad() != null ? a.getTipoActividad().getId() : null,
                a.getTipoActividad() != null ? a.getTipoActividad().getNombre() : null,
                a.getFecha(),
                a.getHoraInicio(),
                a.getHoraFin(),
                a.getSala() != null ? a.getSala().getId() : null,
                a.getSala() != null ? a.getSala().getNombre() : null,
                a.getPonente() != null ? a.getPonente().getId() : null,
                nombreCompleto(a.getPonente()),
                a.getPropuesta() != null ? a.getPropuesta().getId() : null,
                a.getLineaTematica() != null ? a.getLineaTematica().getId() : null,
                a.getModalidad(),
                a.isPermiteSimultaneidad(),
                a.getEstado(),
                advertencias == null ? List.of() : List.copyOf(advertencias)
        );
    }

    /** Nombre completo de una persona, o null si no hay. */
    public static String nombreCompleto(Persona p) {
        if (p == null) {
            return null;
        }
        return ((p.getNombres() == null ? "" : p.getNombres()) + " "
                + (p.getApellidos() == null ? "" : p.getApellidos())).trim();
    }
}
