package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryAmbiguityTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("findByIdentificador con '@' invoca exclusivamente la búsqueda por correo")
    void findByIdentificador_ConArroba_BuscaSoloPorCorreo() {
        String correo = "victima@correo.com";
        Usuario usuarioEsperado = Usuario.builder().id(10L).build();

        when(usuarioRepository.findByIdentificador(correo)).thenCallRealMethod();
        when(usuarioRepository.findByPersona_CorreoIgnoreCase(correo)).thenReturn(Optional.of(usuarioEsperado));

        Optional<Usuario> resultado = usuarioRepository.findByIdentificador(correo);

        assertTrue(resultado.isPresent());
        assertEquals(10L, resultado.get().getId());
        verify(usuarioRepository).findByPersona_CorreoIgnoreCase(correo);
    }

    @Test
    @DisplayName("findByIdentificador sin '@' invoca exclusivamente la búsqueda por nombreUsuario")
    void findByIdentificador_SinArroba_BuscaSoloPorNombreUsuario() {
        String username = "usuario123";
        Usuario usuarioEsperado = Usuario.builder().id(20L).build();

        when(usuarioRepository.findByIdentificador(username)).thenCallRealMethod();
        when(usuarioRepository.findByNombreUsuarioIgnoreCase(username)).thenReturn(Optional.of(usuarioEsperado));

        Optional<Usuario> resultado = usuarioRepository.findByIdentificador(username);

        assertTrue(resultado.isPresent());
        assertEquals(20L, resultado.get().getId());
        verify(usuarioRepository).findByNombreUsuarioIgnoreCase(username);
    }
}
