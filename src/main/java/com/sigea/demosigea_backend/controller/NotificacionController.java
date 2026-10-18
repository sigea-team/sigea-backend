package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.notificacion.NotificacionResponse;
import com.sigea.demosigea_backend.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Notificaciones del usuario autenticado, para la campana del frontend (HU-11, Criterio 2).
 * <p>
 * Cualquier usuario con sesión iniciada consulta y marca <b>sus propias</b> notificaciones; no requiere un
 * permiso específico.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Notificaciones del usuario autenticado (campana)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class NotificacionController {

    private final NotificacionService notificacionService;

    @Operation(summary = "Listar mis notificaciones",
            description = "Devuelve las notificaciones del usuario autenticado, de la más reciente a la más antigua.")
    @ApiResponse(responseCode = "200", description = "Listado de notificaciones",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificacionResponse.class))))
    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar(
            @Parameter(description = "Solo las no leídas", example = "false")
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @Parameter(description = "Cantidad máxima (1 a 50)", example = "20")
            @RequestParam(defaultValue = "20") int limite
    ) {
        return ResponseEntity.ok(notificacionService.listarMias(soloNoLeidas, limite));
    }

    @Operation(summary = "Contar mis notificaciones no leídas", description = "Número que muestra la campana.")
    @ApiResponse(responseCode = "200", description = "Cantidad de notificaciones sin leer")
    @GetMapping("/no-leidas/conteo")
    public ResponseEntity<Map<String, Long>> contarNoLeidas() {
        return ResponseEntity.ok(Map.of("noLeidas", notificacionService.contarNoLeidas()));
    }

    @Operation(summary = "Marcar una notificación como leída")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificación marcada como leída",
                    content = @Content(schema = @Schema(implementation = NotificacionResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe o no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/leida")
    public ResponseEntity<NotificacionResponse> marcarComoLeida(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.marcarComoLeida(id));
    }

    @Operation(summary = "Marcar todas mis notificaciones como leídas")
    @ApiResponse(responseCode = "200", description = "Cantidad de notificaciones actualizadas")
    @PatchMapping("/leidas")
    public ResponseEntity<Map<String, Integer>> marcarTodasComoLeidas() {
        return ResponseEntity.ok(Map.of("actualizadas", notificacionService.marcarTodasComoLeidas()));
    }
}
