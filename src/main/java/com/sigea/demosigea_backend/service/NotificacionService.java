package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.notificacion.NotificacionResponse;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Notificacion;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.NotificacionRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Notificaciones dentro del sistema, visibles en la campana del frontend (HU-11, Criterio 2).
 * <p>
 * Reemplaza el aviso por correo: no consume el cupo diario de Brevo y no hace esperar al organizador, porque
 * crear las notificaciones es una sola inserción masiva en la base de datos.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    /** Máximo de notificaciones que se devuelven por consulta. */
    public static final int LIMITE_MAXIMO = 50;

    /** Longitud máxima de {@code notificaciones.titulo}. */
    private static final int LARGO_TITULO = 150;

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    /**
     * Crea la misma notificación para todos los usuarios con cuenta activa y correo verificado. Debe llamarse
     * dentro de la transacción de la operación que la origina: si esa operación falla, tampoco quedan
     * notificaciones.
     *
     * @return Número de usuarios notificados.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int notificarATodos(TipoNotificacion tipo, String titulo, String mensaje, String entidad, Long entidadId) {
        String tituloAjustado = titulo.length() > LARGO_TITULO ? titulo.substring(0, LARGO_TITULO - 1) + "…" : titulo;
        int creadas = notificacionRepository.crearParaUsuariosActivos(tipo.name(), tituloAjustado, mensaje,
                entidad, entidadId, LocalDateTime.now(clock));
        log.info("Notificación {} creada para {} usuario(s) ({} #{})", tipo, creadas, entidad, entidadId);
        return creadas;
    }

    /**
     * Crea una notificación para un único usuario (p. ej. el evaluador al que se asigna una propuesta, HU-16).
     * Debe llamarse dentro de la transacción de la operación que la origina.
     *
     * @param destinatario Usuario que verá la notificación en su campana.
     * @return La notificación guardada.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Notificacion notificarAUsuario(Usuario destinatario, TipoNotificacion tipo, String titulo, String mensaje,
                                          String entidad, Long entidadId) {
        String tituloAjustado = titulo.length() > LARGO_TITULO ? titulo.substring(0, LARGO_TITULO - 1) + "…" : titulo;
        Notificacion notificacion = Notificacion.builder()
                .usuario(destinatario)
                .tipo(tipo)
                .titulo(tituloAjustado)
                .mensaje(mensaje)
                .entidad(entidad)
                .entidadId(entidadId)
                .leida(false)
                .fechaCreacion(LocalDateTime.now(clock))
                .build();
        Notificacion guardada = notificacionRepository.save(notificacion);
        log.info("Notificación {} creada para el usuario {} ({} #{})", tipo, destinatario.getId(), entidad, entidadId);
        return guardada;
    }

    /**
     * Notificaciones del usuario autenticado, de la más reciente a la más antigua.
     *
     * @param soloNoLeidas Si es {@code true}, devuelve solo las pendientes.
     * @param limite       Cantidad máxima a devolver (entre 1 y {@value #LIMITE_MAXIMO}).
     */
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarMias(boolean soloNoLeidas, int limite) {
        Long usuarioId = usuarioActual().getId();
        PageRequest pagina = PageRequest.of(0, Math.max(1, Math.min(limite, LIMITE_MAXIMO)));
        List<Notificacion> notificaciones = soloNoLeidas
                ? notificacionRepository.findByUsuario_IdAndLeidaFalseOrderByFechaCreacionDescIdDesc(usuarioId, pagina)
                : notificacionRepository.findByUsuario_IdOrderByFechaCreacionDescIdDesc(usuarioId, pagina);
        return notificaciones.stream().map(NotificacionResponse::fromEntity).toList();
    }

    /** Cantidad de notificaciones sin leer del usuario autenticado (el número de la campana). */
    @Transactional(readOnly = true)
    public long contarNoLeidas() {
        return notificacionRepository.countByUsuario_IdAndLeidaFalse(usuarioActual().getId());
    }

    /**
     * Marca como leída una notificación del usuario autenticado. Si la notificación es de otro usuario se
     * responde como no encontrada, para no revelar que existe.
     *
     * @throws RecursoNoEncontradoException si no existe o no pertenece al usuario.
     */
    @Transactional
    public NotificacionResponse marcarComoLeida(Long id) {
        Notificacion notificacion = notificacionRepository.findByIdAndUsuario_Id(id, usuarioActual().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la notificación con ID: " + id));
        if (!Boolean.TRUE.equals(notificacion.getLeida())) {
            notificacion.setLeida(true);
            notificacion.setFechaLectura(LocalDateTime.now(clock));
            notificacion = notificacionRepository.save(notificacion);
        }
        return NotificacionResponse.fromEntity(notificacion);
    }

    /**
     * Marca como leídas todas las notificaciones pendientes del usuario autenticado.
     *
     * @return Número de notificaciones actualizadas.
     */
    @Transactional
    public int marcarTodasComoLeidas() {
        return notificacionRepository.marcarTodasComoLeidas(usuarioActual().getId(), LocalDateTime.now(clock));
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debe iniciar sesión para consultar sus notificaciones.");
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("No se encontró el usuario autenticado."));
    }
}
