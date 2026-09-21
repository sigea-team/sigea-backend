package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Afiliacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión del acceso a datos de la entidad {@link Afiliacion}.
 * <p>
 * Proporciona operaciones CRUD y consultas sobre el catálogo de afiliaciones institucionales
 * disponibles para ser asignadas a las personas registradas en la plataforma SIGEA.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface AfiliacionRepository extends JpaRepository<Afiliacion, Long> {

    /**
     * Busca una afiliación por su nombre descriptivo, ignorando mayúsculas y minúsculas.
     *
     * @param nombreAfiliacion Nombre de la afiliación a buscar.
     * @return Un {@link Optional} con la {@link Afiliacion} encontrada, o vacío si no existe.
     */
    Optional<Afiliacion> findByNombreAfiliacionIgnoreCase(String nombreAfiliacion);
}
