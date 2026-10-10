package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.tipoevento.TipoEventoRequest;
import com.sigea.demosigea_backend.dto.tipoevento.TipoEventoResponse;
import com.sigea.demosigea_backend.dto.tipoevento.UsoTipoEventoResponse;
import com.sigea.demosigea_backend.service.TipoEventoService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST del catálogo global de tipos de evento. Lo usa el formulario de eventos para
 * llenar el combobox de tipo, y el administrador para mantener el catálogo.
 * <p>
 * Usa los permisos del módulo EVENTOS ya sembrados (changeset 003): consultar con
 * {@code EVENTOS_VER} y modificar con {@code EVENTOS_EDITAR}, igual que los catálogos de la HU-05.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see TipoEventoService
 */
@RestController
@RequestMapping("/api/v1/tipos-evento")
@RequiredArgsConstructor
@Tag(name = "Tipos de Evento", description = "Catálogo de tipos de evento (congreso, seminario, taller, etc.)")
@SecurityRequirement(name = "bearerAuth")
public class TipoEventoController {

    private final TipoEventoService tipoService;

    @Operation(summary = "Listar tipos de evento", description = "Devuelve el catálogo ordenado por nombre.")
    @ApiResponse(responseCode = "200", description = "Catálogo de tipos de evento",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TipoEventoResponse.class))))
    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<TipoEventoResponse>> listar() {
        return ResponseEntity.ok(tipoService.listar());
    }

    @Operation(summary = "Consultar un tipo de evento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo encontrado",
                    content = @Content(schema = @Schema(implementation = TipoEventoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tipo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<TipoEventoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(tipoService.obtener(id));
    }

    @Operation(summary = "Consultar el uso (impacto de eliminar)",
            description = "Indica cuántos eventos usan el tipo y si puede eliminarse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uso del tipo",
                    content = @Content(schema = @Schema(implementation = UsoTipoEventoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tipo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/uso")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<UsoTipoEventoResponse> consultarUso(@PathVariable Long id) {
        return ResponseEntity.ok(tipoService.consultarUso(id));
    }

    @Operation(summary = "Crear tipo de evento",
            description = "Si el nombre ya existe, sin distinguir mayúsculas ni espacios → 409 PARAMETRO_DUPLICADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tipo creado",
                    content = @Content(schema = @Schema(implementation = TipoEventoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<TipoEventoResponse> crear(@Valid @RequestBody TipoEventoRequest request) {
        TipoEventoResponse creado = tipoService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @Operation(summary = "Actualizar tipo de evento",
            description = "Modifica nombre y descripción. Los eventos que lo usan muestran el nombre nuevo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo actualizado",
                    content = @Content(schema = @Schema(implementation = TipoEventoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tipo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<TipoEventoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody TipoEventoRequest request) {
        return ResponseEntity.ok(tipoService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar tipo de evento",
            description = "Elimina el tipo si ningún evento lo usa. Si está en uso → 409 PARAMETRO_EN_USO.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tipo eliminado"),
            @ApiResponse(responseCode = "404", description = "Tipo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Tipo en uso",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
