package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Perfil completo del conferencista: datos personales, de contacto, trayectoria e historial de
 * participaciones en los eventos del Programa (HU-12, RF16 y RF17).
 *
 * @param tieneCuentaUsuario {@code true} si la persona inicia sesión en SIGEA (su correo no se cambia desde aquí).
 * @param trayectoria        {@code null} si la persona todavía no tiene trayectoria registrada.
 * @param participaciones    Historial de eventos, del más reciente al más antiguo.
 */
@Schema(description = "Perfil completo del conferencista con su historial de participaciones")
public record PerfilConferencistaResponse(
        Long personaId,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String correo,
        String telefono,
        boolean tieneCuentaUsuario,
        TrayectoriaResponse trayectoria,
        List<ParticipacionHistorialResponse> participaciones
) {
}
