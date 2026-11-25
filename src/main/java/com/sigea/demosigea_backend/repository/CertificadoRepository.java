package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Certificado;
import com.sigea.demosigea_backend.model.TipoCertificado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de los certificados (HU-26, RF47) y de los datos con los que se decide si una persona cumple los
 * requisitos de su rol.
 * <p>
 * Actividades que cuentan: las del evento que no están canceladas ni por programar (HU-21/HU-22). Para el asistente
 * se consideran las actividades en las que estaba inscrito (HU-23); para el ponente, las que tenía a cargo.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

    /** Criterio 3: certificado de todo el evento ya emitido a la persona con ese tipo. */
    @EntityGraph(attributePaths = {"evento"})
    Optional<Certificado> findByPersona_IdAndEvento_IdAndTipoAndActividadIsNull(Long personaId, Long eventoId,
                                                                               TipoCertificado tipo);

    /** Certificados de la persona en un evento. */
    @EntityGraph(attributePaths = {"evento"})
    List<Certificado> findByPersona_IdAndEvento_IdOrderByFechaGeneracionAscIdAsc(Long personaId, Long eventoId);

    /** Certificados de la persona en todos los eventos, del más reciente al más antiguo (perfil del participante). */
    @EntityGraph(attributePaths = {"evento"})
    List<Certificado> findByPersona_IdOrderByFechaGeneracionDescIdDesc(Long personaId);

    /** Certificados emitidos en un evento. */
    @EntityGraph(attributePaths = {"evento", "persona"})
    List<Certificado> findByEvento_IdOrderByTipoAscNombreParticipanteAscIdAsc(Long eventoId);

    @EntityGraph(attributePaths = {"evento", "persona"})
    Optional<Certificado> findWithEventoAndPersonaById(Long id);

    /** Consulta pública de autenticidad. */
    @EntityGraph(attributePaths = {"evento"})
    Optional<Certificado> findByCodigoVerificacion(String codigoVerificacion);

    boolean existsByCodigoVerificacion(String codigoVerificacion);

    /** Certificados de asistente ya emitidos en el evento (con ellos el porcentaje mínimo ya no se cambia). */
    long countByEvento_IdAndTipo(Long eventoId, TipoCertificado tipo);

    long countByEvento_Id(Long eventoId);

    /**
     * Serializa la emisión para una persona en un evento: dos solicitudes simultáneas no generan dos certificados (el
     * índice único es la última garantía). El bloqueo dura hasta el fin de la transacción.
     *
     * @return siempre 1.
     */
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtext('certificado:' || CAST(:personaId AS TEXT) || ':' "
            + "|| CAST(:eventoId AS TEXT)))", nativeQuery = true)
    Integer bloquearEmision(@Param("personaId") Long personaId, @Param("eventoId") Long eventoId);

    /**
     * Bloqueo exclusivo de la plantilla del evento mientras se modifica (fondo y firmantes). Espera a que terminen las
     * emisiones en curso, así la plantilla no cambia después de emitir el primer certificado.
     */
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtext('plantilla-certificado:' || CAST(:eventoId AS TEXT)))",
            nativeQuery = true)
    Integer bloquearPlantilla(@Param("eventoId") Long eventoId);

    /** Bloqueo compartido de la plantilla durante una emisión (varias emisiones pueden ir a la vez). */
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock_shared(hashtext('plantilla-certificado:' "
            + "|| CAST(:eventoId AS TEXT)))", nativeQuery = true)
    Integer bloquearPlantillaParaEmitir(@Param("eventoId") Long eventoId);

    /**
     * Asistencia de una persona: actividades del evento en las que estaba inscrita (no canceladas ni por programar) y
     * cuántas de ellas tienen asistencia registrada (HU-24), con los minutos asistidos. Sin inscripción al evento no
     * devuelve fila.
     */
    @Query(value = """
            SELECT CAST(ie.persona_id AS BIGINT) AS "personaId",
                   CAST(COUNT(act.actividad_id) AS BIGINT) AS "consideradas",
                   CAST(COUNT(a.asistencia_id) AS BIGINT) AS "asistidas",
                   CAST(COALESCE(SUM(CASE WHEN a.asistencia_id IS NOT NULL
                       THEN EXTRACT(EPOCH FROM (act.hora_fin - act.hora_inicio)) / 60 END), 0) AS BIGINT) AS "minutosAsistidos"
            FROM inscripciones_evento ie
            LEFT JOIN inscripciones_actividad ia ON ia.inscripcion_id = ie.inscripcion_id
            LEFT JOIN actividades act ON act.actividad_id = ia.actividad_id
                 AND act.evento_id = ie.evento_id
                 AND lower(act.estado) NOT IN ('cancelada', 'por_programar')
            LEFT JOIN asistencias a ON a.inscripcion_id = ie.inscripcion_id AND a.actividad_id = act.actividad_id
            WHERE ie.evento_id = :eventoId AND ie.persona_id = :personaId
            GROUP BY ie.persona_id
            """, nativeQuery = true)
    Optional<ResumenAsistencia> resumenAsistencia(@Param("eventoId") Long eventoId, @Param("personaId") Long personaId);

    /** Lo mismo que {@link #resumenAsistencia} para todos los inscritos del evento (generación masiva, CU-23). */
    @Query(value = """
            SELECT CAST(ie.persona_id AS BIGINT) AS "personaId",
                   CAST(COUNT(act.actividad_id) AS BIGINT) AS "consideradas",
                   CAST(COUNT(a.asistencia_id) AS BIGINT) AS "asistidas",
                   CAST(COALESCE(SUM(CASE WHEN a.asistencia_id IS NOT NULL
                       THEN EXTRACT(EPOCH FROM (act.hora_fin - act.hora_inicio)) / 60 END), 0) AS BIGINT) AS "minutosAsistidos"
            FROM inscripciones_evento ie
            LEFT JOIN inscripciones_actividad ia ON ia.inscripcion_id = ie.inscripcion_id
            LEFT JOIN actividades act ON act.actividad_id = ia.actividad_id
                 AND act.evento_id = ie.evento_id
                 AND lower(act.estado) NOT IN ('cancelada', 'por_programar')
            LEFT JOIN asistencias a ON a.inscripcion_id = ie.inscripcion_id AND a.actividad_id = act.actividad_id
            WHERE ie.evento_id = :eventoId
            GROUP BY ie.persona_id
            ORDER BY ie.persona_id
            """, nativeQuery = true)
    List<ResumenAsistencia> resumenAsistenciaDelEvento(@Param("eventoId") Long eventoId);

    /**
     * Eventos con los que la persona tiene relación para certificados: inscrita, ponente de una actividad realizada,
     * conferencista registrado o con algún certificado emitido.
     */
    @Query(value = """
            SELECT CAST(x.evento_id AS BIGINT) FROM (
                SELECT ie.evento_id FROM inscripciones_evento ie WHERE ie.persona_id = :personaId
                UNION
                SELECT act.evento_id FROM actividades act WHERE act.ponente_id = :personaId
                       AND lower(act.estado) NOT IN ('cancelada', 'por_programar')
                UNION
                SELECT pc.evento_id FROM participaciones_conferencista pc WHERE pc.persona_id = :personaId
                UNION
                SELECT c.evento_id FROM certificados c WHERE c.persona_id = :personaId
            ) x
            """, nativeQuery = true)
    List<Long> eventosDeLaPersona(@Param("personaId") Long personaId);

    /** Actividades realizadas del evento que la persona tuvo a cargo como ponente, en orden cronológico. */
    @Query("SELECT a FROM Actividad a WHERE a.evento.id = :eventoId AND a.ponente.id = :personaId "
            + "AND LOWER(a.estado) NOT IN ('cancelada', 'por_programar') "
            + "ORDER BY a.fecha, a.horaInicio, a.id")
    List<Actividad> actividadesComoPonente(@Param("eventoId") Long eventoId, @Param("personaId") Long personaId);

    /** Actividades realizadas del evento con ponente asignado (generación masiva, CU-23). */
    @Query("SELECT a FROM Actividad a JOIN FETCH a.ponente WHERE a.evento.id = :eventoId "
            + "AND LOWER(a.estado) NOT IN ('cancelada', 'por_programar') "
            + "ORDER BY a.ponente.id, a.fecha, a.horaInicio, a.id")
    List<Actividad> actividadesConPonente(@Param("eventoId") Long eventoId);

    /** Asistencia de una persona en un evento ({@code consideradas} = actividades inscritas que cuentan). */
    interface ResumenAsistencia {
        Long getPersonaId();

        Long getConsideradas();

        Long getAsistidas();

        /** Minutos de las actividades asistidas (intensidad horaria impresa en el certificado). */
        Long getMinutosAsistidos();
    }
}
