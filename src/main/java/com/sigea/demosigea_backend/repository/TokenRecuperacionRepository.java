package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.TipoToken;
import com.sigea.demosigea_backend.model.TokenRecuperacion;
import com.sigea.demosigea_backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByTokenAndTipo(String token, TipoToken tipo);

    List<TokenRecuperacion> findByUsuarioAndTipoAndUsadoFalse(Usuario usuario, TipoToken tipo);
}
