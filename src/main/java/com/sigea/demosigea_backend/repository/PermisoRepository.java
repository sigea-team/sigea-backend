package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link Permiso}.
 */
@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    Optional<Permiso> findByCodigoIgnoreCase(String codigo);

    List<Permiso> findByModuloIgnoreCaseOrderByCodigoAsc(String modulo);

    List<Permiso> findAllByOrderByModuloAscCodigoAsc();
}
