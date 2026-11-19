package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.RespuestaEncuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de las respuestas de las encuestas (HU-25). Las consultas de consolidación agrupan en la base de
 * datos para no cargar todas las respuestas en memoria (Criterio 4).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface RespuestaEncuestaRepository extends JpaRepository<RespuestaEncuesta, Long> {

    /** Conteo por valor de las preguntas de escala: [preguntaId, valor, cantidad]. */
    @Query("SELECT r.pregunta.id, r.valorEscala, COUNT(r) FROM RespuestaEncuesta r "
            + "WHERE r.pregunta.encuesta.id = :encuestaId AND r.valorEscala IS NOT NULL "
            + "GROUP BY r.pregunta.id, r.valorEscala")
    List<Object[]> contarPorValorEscala(@Param("encuestaId") Long encuestaId);

    /** Conteo por opción de las preguntas de opción: [opcionId, cantidad]. */
    @Query("SELECT r.opcion.id, COUNT(r) FROM RespuestaEncuesta r "
            + "WHERE r.pregunta.encuesta.id = :encuestaId AND r.opcion IS NOT NULL GROUP BY r.opcion.id")
    List<Object[]> contarPorOpcion(@Param("encuestaId") Long encuestaId);

    /** Respuestas de texto libre de la encuesta, en orden de llegada (con la persona si es identificada). */
    @Query("SELECT r FROM RespuestaEncuesta r JOIN FETCH r.pregunta p LEFT JOIN FETCH r.persona "
            + "WHERE p.encuesta.id = :encuestaId "
            + "AND p.tipoRespuesta = com.sigea.demosigea_backend.model.TipoRespuestaEncuesta.texto "
            + "ORDER BY r.fechaRespuesta ASC, r.id ASC")
    List<RespuestaEncuesta> findTextosDeEncuesta(@Param("encuestaId") Long encuestaId);

    /** Todas las respuestas identificadas de la encuesta con su persona, pregunta y opción. */
    @Query("SELECT r FROM RespuestaEncuesta r JOIN FETCH r.persona JOIN FETCH r.pregunta p LEFT JOIN FETCH r.opcion "
            + "WHERE p.encuesta.id = :encuestaId ORDER BY p.orden ASC, p.id ASC")
    List<RespuestaEncuesta> findIdentificadasDeEncuesta(@Param("encuestaId") Long encuestaId);
}
