package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Actividad}.
 * <p>
 * Contiene consultas usadas por HU-05 (Criterio 2), HU-Salas (Criterios 4 y 5), HU-21 y HU-22.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    /** HU-05, Criterio 2: actividades de la agenda que usan el tipo de actividad. */
    long countByTipoActividad_Id(Long tipoActividadId);

    /** HU-05, Criterio 2: actividades de la agenda clasificadas en la línea temática. */
    long countByLineaTematica_Id(Long lineaTematicaId);

    /** HU-Salas, Criterio 4: número de actividades programadas en la sala (para detectar si tiene uso). */
    long countBySala_Id(Long salaId);

    /**
     * HU-Salas, Criterio 5: actividades de una sala en un evento específico.
     * Se usa para detectar conflictos al actualizar la disponibilidad horaria de la sala.
     */
    List<Actividad> findBySala_IdAndEvento_Id(Long salaId, Long eventoId);

    /**
     * Actividades activas en una sala en una fecha dada a través de todos los eventos.
     * Permite controlar cruces y solapamientos en salas reutilizables entre eventos.
     */
    @Query("SELECT a FROM Actividad a JOIN FETCH a.evento LEFT JOIN FETCH a.sala "
            + "WHERE a.sala.id = :salaId AND a.fecha = :fecha AND LOWER(a.estado) != 'cancelada' ORDER BY a.horaInicio, a.id")
    List<Actividad> findActividadesPorSalaYFecha(@Param("salaId") Long salaId, @Param("fecha") LocalDate fecha);

    /**
     * Todas las actividades activas en una sala a través de todos los eventos.
     */
    @Query("SELECT a FROM Actividad a JOIN FETCH a.evento LEFT JOIN FETCH a.sala "
            + "WHERE a.sala.id = :salaId AND LOWER(a.estado) != 'cancelada' ORDER BY a.fecha, a.horaInicio, a.id")
    List<Actividad> findActividadesPorSala(@Param("salaId") Long salaId);

    /**
     * HU-Salas, Criterio 5: actividades de un ponente en un evento específico.
     * Se usa para detectar conflictos al actualizar la disponibilidad horaria del ponente.
     */
    List<Actividad> findByPonente_IdAndEvento_Id(Long ponenteId, Long eventoId);

    /**
     * HU-21 Criterio 1: agenda completa del evento en orden cronológico, con sala, ponente y tipo cargados.
     *
     * @param eventoId ID del evento.
     * @return actividades ordenadas por fecha, hora de inicio e ID.
     */
    @Query("SELECT a FROM Actividad a JOIN FETCH a.tipoActividad LEFT JOIN FETCH a.sala LEFT JOIN FETCH a.ponente "
            + "WHERE a.evento.id = :eventoId ORDER BY a.fecha, a.horaInicio, a.id")
    List<Actividad> findAgendaDelEvento(@Param("eventoId") Long eventoId);

    /**
     * HU-21 Criterios 2 y 5: actividades del evento en una fecha, para revisar los cruces.
     *
     * @param eventoId ID del evento.
     * @param fecha    fecha de la actividad a revisar.
     * @return actividades de ese día en orden cronológico.
     */
    @Query("SELECT a FROM Actividad a JOIN FETCH a.tipoActividad LEFT JOIN FETCH a.sala LEFT JOIN FETCH a.ponente "
            + "WHERE a.evento.id = :eventoId AND a.fecha = :fecha ORDER BY a.horaInicio, a.id")
    List<Actividad> findAgendaDelEventoEnFecha(@Param("eventoId") Long eventoId, @Param("fecha") LocalDate fecha);

    /**
     * HU-22 (salas reutilizables): actividades de OTROS eventos que usan las salas indicadas entre dos fechas, sin
     * las canceladas. Ocupan esas salas al generar y al publicar la agenda del evento.
     *
     * @param eventoId evento cuya agenda se genera (se excluye).
     * @param salaIds  salas vinculadas al evento.
     * @param desde    primera fecha.
     * @param hasta    última fecha.
     * @return actividades con evento y sala cargados.
     */
    @Query("SELECT a FROM Actividad a JOIN FETCH a.evento JOIN FETCH a.sala "
            + "WHERE a.sala.id IN :salaIds AND a.evento.id <> :eventoId AND a.fecha BETWEEN :desde AND :hasta "
            + "AND LOWER(a.estado) <> 'cancelada' ORDER BY a.fecha, a.horaInicio, a.id")
    List<Actividad> findDeOtrosEventosEnSalas(@Param("eventoId") Long eventoId,
                                              @Param("salaIds") Collection<Long> salaIds,
                                              @Param("desde") LocalDate desde,
                                              @Param("hasta") LocalDate hasta);

    /**
     * HU-22 Criterio 3: actividades con horario de los días indicados (los publicados), sin las canceladas ni las
     * por programar, organizadas por día y hora.
     *
     * @param eventoId ID del evento.
     * @param fechas   días a incluir.
     * @param excluir  estados que no se muestran.
     * @return actividades con tipo, sala, ponente y línea temática cargados.
     */
    @Query("SELECT a FROM Actividad a JOIN FETCH a.tipoActividad LEFT JOIN FETCH a.sala LEFT JOIN FETCH a.ponente "
            + "LEFT JOIN FETCH a.lineaTematica "
            + "WHERE a.evento.id = :eventoId AND a.fecha IN :fechas AND a.estado NOT IN :excluir "
            + "ORDER BY a.fecha, a.horaInicio, a.id")
    List<Actividad> findEnFechasSinEstados(@Param("eventoId") Long eventoId,
                                           @Param("fechas") Collection<LocalDate> fechas,
                                           @Param("excluir") Collection<String> excluir);

    /**
     * Busca una actividad dentro de su evento.
     *
     * @param id       ID de la actividad.
     * @param eventoId ID del evento.
     * @return la actividad si pertenece al evento.
     */
    Optional<Actividad> findByIdAndEvento_Id(Long id, Long eventoId);

    /** HU-19: actividades del evento vinculadas a una propuesta (para saber cuáles ya están programadas). */
    List<Actividad> findByEvento_IdAndPropuestaIsNotNull(Long eventoId);
}
