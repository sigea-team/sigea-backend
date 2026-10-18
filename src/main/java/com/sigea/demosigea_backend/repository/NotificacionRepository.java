package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Notificacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de {@link Notificacion} (HU-11, Criterio 2).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Notificaciones del usuario, de la más reciente a la más antigua. */
    List<Notificacion> findByUsuario_IdOrderByFechaCreacionDescIdDesc(Long usuarioId, Pageable pagina);

    /** Notificaciones sin leer del usuario, de la más reciente a la más antigua. */
    List<Notificacion> findByUsuario_IdAndLeidaFalseOrderByFechaCreacionDescIdDesc(Long usuarioId, Pageable pagina);

    /** Cantidad de notificaciones sin leer del usuario (número de la campana). */
    long countByUsuario_IdAndLeidaFalse(Long usuarioId);

    /** Una notificación solo si pertenece al usuario indicado. */
    Optional<Notificacion> findByIdAndUsuario_Id(Long id, Long usuarioId);

    /**
     * Crea en una sola sentencia la misma notificación para todos los usuarios con cuenta activa y correo
     * verificado. Es una inserción masiva en la base de datos: no carga los usuarios en memoria.
     *
     * @return Número de notificaciones creadas (una por usuario).
     */
    @Modifying
    @Query(value = """
            INSERT INTO notificaciones (usuario_id, tipo, titulo, mensaje, entidad, entidad_id, leida, fecha_creacion)
            SELECT u.usuario_id, :tipo, :titulo, :mensaje, :entidad, :entidadId, false, :fechaCreacion
            FROM usuarios u
            WHERE u.estado = 'activo' AND u.correo_verificado = true
            """, nativeQuery = true)
    int crearParaUsuariosActivos(@Param("tipo") String tipo,
                                 @Param("titulo") String titulo,
                                 @Param("mensaje") String mensaje,
                                 @Param("entidad") String entidad,
                                 @Param("entidadId") Long entidadId,
                                 @Param("fechaCreacion") LocalDateTime fechaCreacion);

    /** Marca como leídas todas las notificaciones pendientes del usuario. */
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLectura = :fecha "
            + "WHERE n.usuario.id = :usuarioId AND n.leida = false")
    int marcarTodasComoLeidas(@Param("usuarioId") Long usuarioId, @Param("fecha") LocalDateTime fecha);
}
