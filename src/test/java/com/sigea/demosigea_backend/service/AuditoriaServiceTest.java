package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.auditoria.AuditoriaResponse;
import com.sigea.demosigea_backend.dto.auditoria.FiltroAuditoria;
import com.sigea.demosigea_backend.dto.auditoria.PaginaResponse;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Auditoria;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AuditoriaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuditoriaService auditoriaService;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuario(Long id, String correo) {
        Persona persona = Persona.builder().id(id).nombres("Ana").apellidos("Pérez").correo(correo).build();
        return Usuario.builder().id(id).persona(persona).build();
    }

    // ------------------------------------------------------------ Criterio 1

    @Test
    @DisplayName("Criterio 1: registra usuario autenticado, acción, entidad y datos afectados")
    void registrar_conUsuarioAutenticado() {
        Usuario admin = usuario(3L, "admin@ufps.edu.co");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@ufps.edu.co", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(usuarioRepository.findByPersona_CorreoIgnoreCase("admin@ufps.edu.co")).thenReturn(Optional.of(admin));

        auditoriaService.registrar(TipoOperacionAuditoria.ROL_ACTUALIZADO, "roles", 4L,
                DetalleAuditoria.de("antes", DetalleAuditoria.de("nombre", "EVALUADOR"),
                        "despues", DetalleAuditoria.de("nombre", "EVALUADOR_PAR")));

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria guardada = captor.getValue();
        assertSame(admin, guardada.getUsuario());
        assertEquals("ROL_ACTUALIZADO", guardada.getAccion());
        assertEquals("roles", guardada.getEntidad());
        assertEquals(4L, guardada.getEntidadId());
        assertEquals("{\"antes\":{\"nombre\":\"EVALUADOR\"},\"despues\":{\"nombre\":\"EVALUADOR_PAR\"}}",
                guardada.getDetalle());
    }

    @Test
    @DisplayName("Criterio 1: sin sesión autenticada el registro queda sin usuario (operación del sistema)")
    void registrar_sinAutenticacion() {
        auditoriaService.registrar(TipoOperacionAuditoria.ROL_CREADO, "roles", 1L, null);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertNull(captor.getValue().getUsuario());
        assertNull(captor.getValue().getDetalle());
    }

    @Test
    @DisplayName("Criterio 1: en flujos públicos se registra el actor explícito")
    void registrar_conActorExplicito() {
        Usuario nuevo = usuario(9L, "nuevo@ufps.edu.co");

        auditoriaService.registrar(TipoOperacionAuditoria.USUARIO_REGISTRADO, "usuarios", 9L,
                DetalleAuditoria.de("correo", "nuevo@ufps.edu.co"), nuevo);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertSame(nuevo, captor.getValue().getUsuario());
        verify(usuarioRepository, never()).findByPersona_CorreoIgnoreCase(any());
    }

    // ------------------------------------------------------------ Criterio 2

    @Test
    @DisplayName("Criterio 2: aplica los filtros recibidos y devuelve la página mapeada")
    void buscar_aplicaFiltros() {
        FiltroAuditoria filtro = new FiltroAuditoria(3L, null, "rol_actualizado", "roles",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        Auditoria registro = Auditoria.builder().id(15L).usuario(usuario(3L, "admin@ufps.edu.co"))
                .accion("ROL_ACTUALIZADO").entidad("roles").entidadId(4L).detalle("{}").build();
        when(auditoriaRepository.buscar(eq(filtro), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(registro), PageRequest.of(0, 20), 1));

        PaginaResponse<AuditoriaResponse> pagina = auditoriaService.buscar(filtro, 0, 20);

        assertEquals(1, pagina.totalElementos());
        AuditoriaResponse r = pagina.contenido().get(0);
        assertEquals(15L, r.id());
        assertEquals(3L, r.usuarioId());
        assertEquals("admin@ufps.edu.co", r.usuarioCorreo());
        assertEquals("Ana Pérez", r.usuarioNombre());
        assertEquals("Modificación de un rol o de sus permisos", r.accionDescripcion());
        verify(auditoriaRepository).buscar(filtro, PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("Criterio 2: rechaza un rango de fechas invertido")
    void buscar_rangoInvertido() {
        FiltroAuditoria filtro = new FiltroAuditoria(null, null, null, null,
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1));

        assertThrows(SolicitudInvalidaException.class, () -> auditoriaService.buscar(filtro, 0, 20));
        verify(auditoriaRepository, never()).buscar(any(), any());
    }

    @Test
    @DisplayName("Criterio 2: rechaza un tipo de operación inexistente")
    void buscar_tipoOperacionInexistente() {
        FiltroAuditoria filtro = new FiltroAuditoria(null, null, "BORRAR_TODO", null, null, null);

        assertThrows(SolicitudInvalidaException.class, () -> auditoriaService.buscar(filtro, 0, 20));
    }

    @Test
    @DisplayName("Criterio 2: valida el tamaño de página")
    void buscar_tamanoInvalido() {
        FiltroAuditoria vacio = new FiltroAuditoria(null, null, null, null, null, null);
        assertThrows(SolicitudInvalidaException.class, () -> auditoriaService.buscar(vacio, 0, 0));
        assertThrows(SolicitudInvalidaException.class, () -> auditoriaService.buscar(vacio, 0, 101));
        assertThrows(SolicitudInvalidaException.class, () -> auditoriaService.buscar(vacio, -1, 20));
    }

    @Test
    @DisplayName("Consulta por ID inexistente responde recurso no encontrado")
    void obtenerPorId_inexistente() {
        when(auditoriaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> auditoriaService.obtenerPorId(99L));
    }

    @Test
    @DisplayName("El catálogo incluye las operaciones críticas de roles")
    void listarTiposOperacion() {
        assertTrue(auditoriaService.listarTiposOperacion().stream()
                .anyMatch(t -> t.codigo().equals("ROL_ACTUALIZADO")));
    }
}
