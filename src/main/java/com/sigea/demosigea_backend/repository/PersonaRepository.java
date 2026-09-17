package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByNumeroDocumento(String numeroDocumento);

    Optional<Persona> findByCorreoIgnoreCase(String correo);

    Optional<Persona> findByNumeroDocumento(String numeroDocumento);
}
