package com.sigea.demosigea_backend.dto.conferencista;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo que ve el invitado al abrir el enlace del correo (HU-12): el evento, el nombre con el que fue
 * invitado, su correo enmascarado (p. ej. {@code "an•••@unal.edu.co"}) y, si ya existe en SIGEA, sus
 * datos para precargar el formulario. El enlace no exige sesión, así que no se devuelve ningún dato
 * de contacto completo (Criterio 6). {@code requiereContrasena} es {@code true} si la persona aún no
 * tiene cuenta en SIGEA: el formulario le pide crear una contraseña para ingresar luego con el
 * perfil «Conferencista».
 */
@Schema(description = "Invitación vista por el conferencista desde el enlace del correo")
public record InvitacionPublicaResponse(
        Long eventoId,
        String eventoNombre,
        String semestre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String nombre,
        String correo,
        LocalDateTime fechaExpiracion,
        DatosRegistradosResponse datosRegistrados,
        boolean requiereContrasena
) {
}
