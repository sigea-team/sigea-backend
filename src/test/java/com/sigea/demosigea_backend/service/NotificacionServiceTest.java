package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.notificacion.NotificacionResponse;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Notificacion;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.NotificacionRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de las notificaciones de la campana (HU-11, Criterio 2).
 */
@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 17, 12, 0);
    private static final String CORREO = "ana@ufps.edu.co";

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private NotificacionService notificacionService;

    private final Usuario usuario = Usuario.builder().id(7L).build();

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
        notificacionService = new NotificacionService(notificacionRepository, usuarioRepository, reloj);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void iniciarSesion() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CORREO, null, List.of()));
        when(usuarioRepository.findByPersona_CorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
    }

    private Notificacion notificacion(Long id, boolean leida) {
        return Notificacion.builder()
                .id(id)
                .usuario(usuario)
                .tipo(TipoNotificacion.CONVOCATORIA_FECHA_MODIFICADA)
                .titulo("Nueva fecha límite: Convocatoria")
                .mensaje("Mensaje")
                .entidad("convocatorias")
                .entidadId(10L)
                .leida(leida)
                .fechaCreacion(AHORA.minusHours(1))
                .build();
    }

    @Test
    @DisplayName("HU-11 Criterio 2: notificar a todos crea una notificación por usuario activo en una sola inserción")
    void notificarATodos_insercionMasiva_retornaCantidad() {
        when(notificacionRepository.crearParaUsuariosActivos("CONVOCATORIA_FECHA_MODIFICADA",
                "Nueva fecha límite: Convocatoria", "Mensaje", "convocatorias", 10L, AHORA)).thenReturn(25);

        int creadas = notificacionService.notificarATodos(TipoNotificacion.CONVOCATORIA_FECHA_MODIFICADA,
                "Nueva fecha límite: Convocatoria", "Mensaje", "convocatorias", 10L);

        assertEquals(25, creadas);
    }

    @Test
    @DisplayName("Un título más largo que la columna (150) se recorta")
    void notificarATodos_tituloLargo_seRecortaA150() {
        String largo = "x".repeat(200);
        ArgumentCaptor<String> titulo = ArgumentCaptor.forClass(String.class);
        when(notificacionRepository.crearParaUsuariosActivos(anyString(), titulo.capture(), anyString(),
                anyString(), any(), any())).thenReturn(1);

        notificacionService.notificarATodos(TipoNotificacion.CONVOCATORIA_FECHA_MODIFICADA, largo, "Mensaje",
                "convocatorias", 10L);

        assertEquals(150, titulo.getValue().length());
    }

    @Test
    @DisplayName("Listar devuelve solo las notificaciones del usuario autenticado, limitando la cantidad")
    void listarMias_usuarioAutenticado_consultaSusNotificaciones() {
        iniciarSesion();
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        when(notificacionRepository.findByUsuario_IdOrderByFechaCreacionDescIdDesc(eq(7L), pagina.capture()))
                .thenReturn(List.of(notificacion(1L, false)));

        List<NotificacionResponse> lista = notificacionService.listarMias(false, 500);

        assertEquals(1, lista.size());
        assertEquals("convocatorias", lista.get(0).entidad());
        assertEquals(NotificacionService.LIMITE_MAXIMO, pagina.getValue().getPageSize());
    }

    @Test
    @DisplayName("Listar con soloNoLeidas consulta únicamente las pendientes")
    void listarMias_soloNoLeidas_consultaPendientes() {
        iniciarSesion();
        when(notificacionRepository.findByUsuario_IdAndLeidaFalseOrderByFechaCreacionDescIdDesc(eq(7L), any()))
                .thenReturn(List.of(notificacion(2L, false)));

        List<NotificacionResponse> lista = notificacionService.listarMias(true, 20);

        assertEquals(1, lista.size());
        assertEquals(false, lista.get(0).leida());
    }

    @Test
    @DisplayName("Contar no leídas devuelve el número de la campana")
    void contarNoLeidas_retornaConteo() {
        iniciarSesion();
        when(notificacionRepository.countByUsuario_IdAndLeidaFalse(7L)).thenReturn(3L);

        assertEquals(3L, notificacionService.contarNoLeidas());
    }

    @Test
    @DisplayName("Marcar como leída registra la fecha de lectura")
    void marcarComoLeida_propia_quedaLeida() {
        iniciarSesion();
        Notificacion pendiente = notificacion(5L, false);
        when(notificacionRepository.findByIdAndUsuario_Id(5L, 7L)).thenReturn(Optional.of(pendiente));
        when(notificacionRepository.save(pendiente)).thenReturn(pendiente);

        NotificacionResponse res = notificacionService.marcarComoLeida(5L);

        assertTrue(res.leida());
        assertEquals(AHORA, res.fechaLectura());
    }

    @Test
    @DisplayName("Marcar una notificación ya leída no la vuelve a guardar")
    void marcarComoLeida_yaLeida_noGuarda() {
        iniciarSesion();
        when(notificacionRepository.findByIdAndUsuario_Id(5L, 7L)).thenReturn(Optional.of(notificacion(5L, true)));

        notificacionService.marcarComoLeida(5L);

        verify(notificacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una notificación de otro usuario se responde como no encontrada")
    void marcarComoLeida_deOtroUsuario_lanzaRecursoNoEncontrado() {
        iniciarSesion();
        when(notificacionRepository.findByIdAndUsuario_Id(9L, 7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> notificacionService.marcarComoLeida(9L));
    }

    @Test
    @DisplayName("Marcar todas como leídas actualiza las pendientes del usuario")
    void marcarTodasComoLeidas_retornaActualizadas() {
        iniciarSesion();
        when(notificacionRepository.marcarTodasComoLeidas(7L, AHORA)).thenReturn(4);

        assertEquals(4, notificacionService.marcarTodasComoLeidas());
    }

    @Test
    @DisplayName("Sin sesión iniciada no se pueden consultar notificaciones")
    void contarNoLeidas_sinSesion_lanzaAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> notificacionService.contarNoLeidas());
    }

    @Test
    @DisplayName("HU-16 Criterio 1: notificar a un usuario guarda una notificación no leída con la fecha del reloj")
    void notificarAUsuario_guardaNotificacionNoLeida() {
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Notificacion creada = notificacionService.notificarAUsuario(usuario, TipoNotificacion.PROPUESTA_ASIGNADA,
                "x".repeat(200), "Se te asignó la propuesta", "asignaciones_evaluacion", 12L);

        assertEquals(usuario, creada.getUsuario());
        assertEquals(TipoNotificacion.PROPUESTA_ASIGNADA, creada.getTipo());
        assertEquals(150, creada.getTitulo().length());
        assertEquals(AHORA, creada.getFechaCreacion());
        assertEquals("asignaciones_evaluacion", creada.getEntidad());
        assertEquals(12L, creada.getEntidadId());
        assertEquals(Boolean.FALSE, creada.getLeida());
    }
}
