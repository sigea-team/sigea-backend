package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);

    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

    Optional<Usuario> findByPersona_CorreoIgnoreCase(String correo);

    @Query("SELECT u FROM Usuario u JOIN u.persona p WHERE LOWER(u.nombreUsuario) = LOWER(:identificador) OR LOWER(p.correo) = LOWER(:identificador)")
    Optional<Usuario> findByIdentificador(@Param("identificador") String identificador);
}
