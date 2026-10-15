package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA del catálogo de tipos de evento.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface TipoEventoRepository extends JpaRepository<TipoEvento, Long> {

    /** Catálogo completo ordenado por nombre. */
    List<TipoEvento> findAllByOrderByNombreAscIdAsc();

    /** ¿Ya existe un tipo con ese nombre (sin distinguir mayúsculas)? */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Igual que el anterior, sin contar el propio registro (al actualizar). */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long excluirId);
}
