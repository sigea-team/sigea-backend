package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.sala.ActualizacionDisponibilidadSalaResponse;
import com.sigea.demosigea_backend.dto.sala.DisponibilidadHorariaRequest;
import com.sigea.demosigea_backend.dto.sala.DisponibilidadSalaResponse;
import com.sigea.demosigea_backend.dto.sala.OcupacionSalaResponse;
import com.sigea.demosigea_backend.dto.sala.SalaRequest;
import com.sigea.demosigea_backend.dto.sala.SalaResponse;
import com.sigea.demosigea_backend.service.SalaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para la gestión del catálogo institucional de salas y espacios desvinculados de eventos.
 * Permite que las salas sean reutilizables y vinculables posteriormente a múltiples eventos.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/salas")
@RequiredArgsConstructor
@Tag(name = "Salas Globales", description = "Gestión institucional de salas, espacios reutilizables y disponibilidad")
@SecurityRequirement(name = "bearerAuth")
public class SalaGlobalController {

    private final SalaService salaService;

    @Operation(summary = "Listar todas las salas institucionales",
            description = "Devuelve todas las salas registradas en el sistema con su disponibilidad horaria, "
                    + "eventos a los que está vinculada y horarios ocupados por actividades.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de salas institucionales",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SalaResponse.class))))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<SalaResponse>> listarTodas() {
        return ResponseEntity.ok(salaService.listarTodas());
    }

    @Operation(summary = "Consultar una sala por su ID",
            description = "Obtiene los detalles de una sala institucional, franjas disponibles y actividades programadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sala encontrada",
                    content = @Content(schema = @Schema(implementation = SalaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{salaId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<SalaResponse> obtener(@PathVariable Long salaId) {
        return ResponseEntity.ok(salaService.obtenerGlobal(salaId));
    }

    @Operation(summary = "Crear sala institucional desvinculada",
            description = "Registra una nueva sala sin asignarla de antemano a ningún evento. "
                    + "Permite configurar su nombre, ubicación, tipo, aforo y franjas de disponibilidad iniciales.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sala creada exitosamente",
                    content = @Content(schema = @Schema(implementation = SalaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (aforo ≤ 0, etc.)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre de sala ya existente en el catálogo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<SalaResponse> crear(@Valid @RequestBody SalaRequest request) {
        SalaResponse creada = salaService.crearGlobal(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @Operation(summary = "Actualizar sala institucional",
            description = "Modifica los datos y/o disponibilidad de una sala institucional existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sala actualizada exitosamente",
                    content = @Content(schema = @Schema(implementation = SalaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado o aforo inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{salaId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<SalaResponse> actualizar(
            @PathVariable Long salaId,
            @Valid @RequestBody SalaRequest request
    ) {
        return ResponseEntity.ok(salaService.actualizarGlobal(salaId, request));
    }

    @Operation(summary = "Eliminar sala institucional",
            description = "Elimina una sala si no tiene actividades programadas en ningún evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sala eliminada"),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La sala tiene actividades programadas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{salaId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long salaId) {
        salaService.eliminarGlobal(salaId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consultar disponibilidad horaria de una sala",
            description = "Obtiene las franjas de disponibilidad horaria registradas para la sala.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Franjas de disponibilidad",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DisponibilidadSalaResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{salaId}/disponibilidad")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<DisponibilidadSalaResponse>> obtenerDisponibilidad(@PathVariable Long salaId) {
        return ResponseEntity.ok(salaService.obtenerDisponibilidadGlobal(salaId));
    }

    @Operation(summary = "Actualizar disponibilidad horaria de una sala",
            description = "Reemplaza las franjas de disponibilidad de la sala y devuelve los conflictos con actividades existentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad actualizada",
                    content = @Content(schema = @Schema(implementation = ActualizacionDisponibilidadSalaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Franjas con datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{salaId}/disponibilidad")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ActualizacionDisponibilidadSalaResponse> actualizarDisponibilidad(
            @PathVariable Long salaId,
            @Valid @RequestBody List<@Valid DisponibilidadHorariaRequest> franjas
    ) {
        return ResponseEntity.ok(salaService.actualizarDisponibilidadGlobal(salaId, franjas));
    }

    @Operation(summary = "Consultar horarios ocupados de una sala",
            description = "Devuelve las franjas horarias ocupadas por eventos o actividades programadas en la sala, "
                    + "para que dejen de figurar disponibles para futuras asignaciones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Franjas ocupadas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OcupacionSalaResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{salaId}/ocupaciones")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<OcupacionSalaResponse>> obtenerOcupaciones(
            @PathVariable Long salaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(salaService.obtenerOcupacionesGlobal(salaId, fecha));
    }
}
