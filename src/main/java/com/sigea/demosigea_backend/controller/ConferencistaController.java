package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.conferencista.ActualizarConferencistaRequest;
import com.sigea.demosigea_backend.dto.conferencista.AsociarConferencistaRequest;
import com.sigea.demosigea_backend.dto.conferencista.CoincidenciaConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistaEventoResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistaRegistradoResponse;
import com.sigea.demosigea_backend.dto.conferencista.PerfilConferencistaResponse;
import com.sigea.demosigea_backend.service.ConferencistaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

/**
 * Controlador REST del módulo de conferencistas (HU-12, RF16 y RF17).
 * Los conferencistas nuevos se invitan por correo ({@link InvitacionConferencistaController}); aquí se
 * vinculan los ya registrados y se administra su trayectoria.
 * <ul>
 *   <li>{@code /api/v1/eventos/{eventoId}/conferencistas}: conferencistas asociados a un evento.</li>
 *   <li>{@code /api/v1/conferencistas}: perfil de la persona (datos, trayectoria e historial).</li>
 * </ul>
 * Usa los permisos del módulo EVENTOS, igual que el comité organizador (HU-06).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see ConferencistaService
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Conferencistas", description = "Registro de conferencistas y administración de su trayectoria (HU-12)")
@SecurityRequirement(name = "bearerAuth")
public class ConferencistaController {

    private final ConferencistaService conferencistaService;

    @Operation(summary = "Listar conferencistas del evento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conferencistas del evento",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConferencistaEventoResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eventos/{eventoId}/conferencistas")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<ConferencistaEventoResponse>> listar(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId) {
        return ResponseEntity.ok(conferencistaService.listarPorEvento(eventoId));
    }

    @Operation(summary = "Listar conferencistas registrados",
            description = "Criterio 3: conferencistas que ya tienen trayectoria en SIGEA, para elegir a quién vincular a "
                    + "un evento sin volver a pedir sus datos. Filtra por nombre, documento o correo (máximo 50). Con "
                    + "eventoId indica quién ya está en ese evento.")
    @ApiResponse(responseCode = "200", description = "Conferencistas registrados",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConferencistaRegistradoResponse.class))))
    @GetMapping("/conferencistas")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<ConferencistaRegistradoResponse>> listarRegistrados(
            @Parameter(description = "Texto a buscar en nombre, documento o correo", example = "pérez")
            @RequestParam(required = false) String busqueda,
            @Parameter(description = "Evento actual, para marcar quién ya está asociado", example = "3")
            @RequestParam(required = false) Long eventoId) {
        return ResponseEntity.ok(conferencistaService.listarRegistrados(busqueda, eventoId));
    }

    @Operation(summary = "Asociar al evento un conferencista que ya existe",
            description = "Criterio 3: asocia la persona sin duplicar sus datos. Si todavía no tiene trayectoria, "
                    + "'trayectoria' es obligatoria. Si ya está en el evento → 409 CONFERENCISTA_YA_ASOCIADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conferencista asociado",
                    content = @Content(schema = @Schema(implementation = ConferencistaEventoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falta la trayectoria o hay datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o persona no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya asociado o el evento no admite cambios",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/eventos/{eventoId}/conferencistas/asociar")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ConferencistaEventoResponse> asociar(
            @PathVariable Long eventoId,
            @Valid @RequestBody AsociarConferencistaRequest request) {
        ConferencistaEventoResponse asociado = conferencistaService.asociar(eventoId, request);
        return ResponseEntity.created(ubicacion(eventoId, asociado)).body(asociado);
    }

    @Operation(summary = "Retirar conferencista del evento",
            description = "Quita la asociación con el evento; la persona y su trayectoria se conservan.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conferencista retirado del evento"),
            @ApiResponse(responseCode = "404", description = "Evento o asociación no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El evento está en ejecución o cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/eventos/{eventoId}/conferencistas/{participacionId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> desasociar(
            @PathVariable Long eventoId,
            @Parameter(description = "ID de la asociación (participacionId)", example = "7") @PathVariable Long participacionId) {
        conferencistaService.desasociar(eventoId, participacionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar conferencista existente por documento o correo",
            description = "Criterio 3: permite avisar antes de registrar un duplicado. 200 con la persona encontrada "
                    + "o 204 si no existe. Con eventoId indica si ya está asociada a ese evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Persona encontrada",
                    content = @Content(schema = @Schema(implementation = CoincidenciaConferencistaResponse.class))),
            @ApiResponse(responseCode = "204", description = "No hay coincidencias"),
            @ApiResponse(responseCode = "400", description = "No se envió documento ni correo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/conferencistas/buscar")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<CoincidenciaConferencistaResponse> buscar(
            @RequestParam(required = false) String numeroDocumento,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) Long eventoId) {
        return conferencistaService.buscarCoincidencia(numeroDocumento, correo, eventoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Mi perfil de conferencista",
            description = "Perfil de ingreso 'Conferencista': el usuario de la sesión consulta sus datos, su trayectoria "
                    + "y los eventos en los que participa. 404 si el organizador aún no lo ha registrado como conferencista.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil del conferencista de la sesión",
                    content = @Content(schema = @Schema(implementation = PerfilConferencistaResponse.class))),
            @ApiResponse(responseCode = "404", description = "El usuario aún no es conferencista",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/conferencistas/mi-perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PerfilConferencistaResponse> obtenerMiPerfil(Principal principal) {
        return ResponseEntity.ok(conferencistaService.obtenerMiPerfil(principal.getName()));
    }

    @Operation(summary = "Actualizar mi perfil de conferencista",
            description = "El conferencista de la sesión actualiza sus datos y su trayectoria; el cambio se refleja en la "
                    + "vista pública (Criterio 4). Su correo no se puede cambiar porque es su usuario de inicio de sesión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado",
                    content = @Content(schema = @Schema(implementation = PerfilConferencistaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "El usuario aún no es conferencista",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Documento de otra persona",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/conferencistas/mi-perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PerfilConferencistaResponse> actualizarMiPerfil(
            Principal principal,
            @Valid @RequestBody ActualizarConferencistaRequest request) {
        return ResponseEntity.ok(conferencistaService.actualizarMiPerfil(principal.getName(), request));
    }

    @Operation(summary = "Consultar perfil del conferencista",
            description = "Datos personales, de contacto, trayectoria e historial de participaciones en los eventos (RF17).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil del conferencista",
                    content = @Content(schema = @Schema(implementation = PerfilConferencistaResponse.class))),
            @ApiResponse(responseCode = "404", description = "La persona no existe o no es conferencista",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/conferencistas/{personaId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<PerfilConferencistaResponse> obtener(
            @Parameter(description = "ID de la persona", example = "12") @PathVariable Long personaId) {
        return ResponseEntity.ok(conferencistaService.obtenerPerfil(personaId));
    }

    @Operation(summary = "Actualizar datos y trayectoria del conferencista",
            description = "Criterio 4: el cambio se refleja de inmediato en la vista pública de los eventos publicados "
                    + "(ver 'visiblePublicamente' en participaciones). Documento o correo de otra persona → 409 "
                    + "DATOS_PERSONA_DUPLICADOS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado",
                    content = @Content(schema = @Schema(implementation = PerfilConferencistaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La persona no existe o no es conferencista",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Documento o correo de otra persona",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/conferencistas/{personaId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<PerfilConferencistaResponse> actualizar(
            @PathVariable Long personaId,
            @Valid @RequestBody ActualizarConferencistaRequest request) {
        return ResponseEntity.ok(conferencistaService.actualizar(personaId, request));
    }

    private static URI ubicacion(Long eventoId, ConferencistaEventoResponse conferencista) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/eventos/{eventoId}/conferencistas/{participacionId}")
                .buildAndExpand(eventoId, conferencista.participacionId())
                .toUri();
    }
}
