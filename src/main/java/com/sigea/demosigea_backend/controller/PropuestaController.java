package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.propuesta.ActualizarAutorRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorResponse;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaResponse;
import com.sigea.demosigea_backend.service.PropuestaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.List;

/**
 * Controlador REST para el envío de propuestas académicas y el registro de autores y ponentes (HU-13).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Tag(name = "Propuestas y Autores", description = "Endpoints para postulación de propuestas académicas y vinculación de autores y ponentes (HU-13)")
@RestController
@RequestMapping("/api/v1/propuestas")
@RequiredArgsConstructor
public class PropuestaController {

    private final PropuestaService propuestaService;

    // ------------------------------------------------------------------
    // Envío de Propuesta (Criterio 1, 2 y 3)
    // ------------------------------------------------------------------

    @Operation(summary = "Enviar o registrar una propuesta académica",
            description = "Registra una propuesta para una convocatoria abierta, asociando los autores o ponentes postulados con su rol específico (Criterios 1, 2 y 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Propuesta y autores registrados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos obligatorios faltantes o convocatoria cerrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convocatoria o línea temática no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Autor duplicado en la misma propuesta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PropuestaResponse> crearPropuesta(@Valid @RequestBody PropuestaRequest request) {
        PropuestaResponse respuesta = propuestaService.crearPropuesta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(summary = "Obtener el detalle de una propuesta con sus autores",
            description = "Devuelve la información de la propuesta académica junto con la lista de autores postulantes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Propuesta obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PropuestaResponse> obtenerPropuesta(@PathVariable Long id) {
        return ResponseEntity.ok(propuestaService.obtenerPropuesta(id));
    }

    // ------------------------------------------------------------------
    // Gestión de Autores y Ponentes (Criterio 1, 2 y 3)
    // ------------------------------------------------------------------

    @Operation(summary = "Registrar y vincular un autor o ponente a una propuesta",
            description = "Asocia los datos de una persona como autor o ponente a la propuesta enviada, validando campos obligatorios (nombre, correo, filiación) y rol específico (Criterios 1, 2 y 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Autor registrado y asociado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes (nombre, correo, filiación o rol)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La persona ya se encuentra registrada en la propuesta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/autores")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AutorResponse> agregarAutor(
            @PathVariable Long id,
            @Valid @RequestBody AutorRequest request) {
        AutorResponse respuesta = propuestaService.agregarAutor(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(summary = "Registrar múltiples autores o ponentes en lote a una propuesta",
            description = "Permite registrar una lista de autores con sus roles específicos (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Autores registrados y asociados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes en alguno de los autores",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/autores/lote")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AutorResponse>> agregarAutoresEnLote(
            @PathVariable Long id,
            @RequestBody List<@Valid AutorRequest> requests) {
        List<AutorResponse> respuesta = propuestaService.agregarAutoresEnLote(id, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(summary = "Listar los autores y ponentes de una propuesta",
            description = "Devuelve los autores postulantes con sus respectivos roles y orden de autoría.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de autores obtenida"),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/autores")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AutorResponse>> listarAutores(@PathVariable Long id) {
        return ResponseEntity.ok(propuestaService.listarAutores(id));
    }

    @Operation(summary = "Actualizar el rol u orden de un autor en la propuesta",
            description = "Modifica el rol específico (autor principal, coautor, ponente) o prelación de autoría (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autor actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Propuesta o autor no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/autores/{personaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AutorResponse> actualizarAutor(
            @PathVariable Long id,
            @PathVariable Long personaId,
            @Valid @RequestBody ActualizarAutorRequest request) {
        return ResponseEntity.ok(propuestaService.actualizarAutor(id, personaId, request));
    }

    @Operation(summary = "Desvincular un autor de una propuesta",
            description = "Elimina la asociación del autor con la propuesta académica.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Autor desvinculado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Propuesta o autor no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}/autores/{personaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> eliminarAutor(
            @PathVariable Long id,
            @PathVariable Long personaId) {
        propuestaService.eliminarAutor(id, personaId);
        return ResponseEntity.noContent().build();
    }
}
