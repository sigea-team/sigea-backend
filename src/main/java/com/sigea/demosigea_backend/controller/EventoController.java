package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.evento.EdicionesEventoResponse;
import com.sigea.demosigea_backend.dto.evento.EventoRequest;
import com.sigea.demosigea_backend.dto.evento.EventoResponse;
import com.sigea.demosigea_backend.dto.evento.NuevaEdicionRequest;
import com.sigea.demosigea_backend.exception.ConfirmacionRequeridaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.service.EventoService;
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
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de eventos y sus ediciones (HU-04: RF03 + RF04).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see EventoService
 */
@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
@Tag(name = "Gestión de Eventos y Ediciones", description = "Creación, configuración y versionado por ediciones de eventos académicos (HU-04)")
@SecurityRequirement(name = "bearerAuth")
public class EventoController {

    private final EventoService eventoService;

    @Operation(summary = "Listar eventos",
            description = "Lista eventos base y ediciones juntos, del más reciente al más antiguo, filtrando opcionalmente "
                    + "por estado y semestre. Cada registro indica su tipo con eventoBaseId (null = evento base; "
                    + "con valor = edición de ese evento) y esEdicion, que se deriva de eventoBaseId. "
                    + "Para obtener solo la familia de un evento use GET /api/v1/eventos/{id}/ediciones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de eventos",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventoResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Filtro inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<EventoResponse>> listar(
            @Parameter(description = "Estado del evento", example = "en_configuracion")
            @RequestParam(required = false) EstadoEvento estado,
            @Parameter(description = "Semestre académico", example = "2026-2")
            @RequestParam(required = false) String semestre
    ) {
        return ResponseEntity.ok(eventoService.listar(estado, semestre));
    }

    @Operation(summary = "Consultar evento por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento encontrado",
                    content = @Content(schema = @Schema(implementation = EventoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<EventoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerPorId(id));
    }

    @Operation(summary = "Crear evento",
            description = "Registra un evento con sus datos generales. El evento queda en estado 'en_configuracion' (Criterio 1). "
                    + "Datos obligatorios incompletos → 400 (Criterio 5).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento creado",
                    content = @Content(schema = @Schema(implementation = EventoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos obligatorios incompletos o inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_CREAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<EventoResponse> crear(@Valid @RequestBody EventoRequest request) {
        EventoResponse creado = eventoService.crear(request);
        return ResponseEntity.created(ubicacion(creado.id())).body(creado);
    }

    @Operation(summary = "Modificar configuración del evento",
            description = "Actualiza los parámetros generales de un evento en configuración, sin afectar la información asociada (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento actualizado",
                    content = @Content(schema = @Schema(implementation = EventoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El evento ya no está en configuración o el semestre ya existe en otra edición",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<EventoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody EventoRequest request) {
        return ResponseEntity.ok(eventoService.actualizar(id, request));
    }

    @Operation(summary = "Publicar evento",
            description = "Publica un evento o edición que se encuentre en configuración, cambiando su estado a 'habilitado'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento publicado exitosamente",
                    content = @Content(schema = @Schema(implementation = EventoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El evento ya no está en configuración",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/publicar")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<EventoResponse> publicar(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.publicar(id));
    }

    @Operation(summary = "Crear nueva edición",
            description = "Genera una edición vinculada al evento base a partir del evento indicado, heredando su configuración general "
                    + "pero con datos independientes (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Edición creada",
                    content = @Content(schema = @Schema(implementation = EventoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento origen no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una edición para ese semestre",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/ediciones")
    @PreAuthorize("hasAnyAuthority('EVENTOS_CREAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<EventoResponse> crearEdicion(
            @Parameter(description = "ID del evento base o de una edición a usar como plantilla", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody NuevaEdicionRequest request
    ) {
        EventoResponse edicion = eventoService.crearEdicion(id, request);
        return ResponseEntity.created(ubicacion(edicion.id())).body(edicion);
    }

    @Operation(summary = "Listar ediciones de un evento",
            description = "Devuelve el evento base y sus ediciones ordenados cronológicamente con su estado (Criterio 4). "
                    + "Acepta el ID del evento base o de cualquiera de sus ediciones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ediciones encontradas",
                    content = @Content(schema = @Schema(implementation = EdicionesEventoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/ediciones")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<EdicionesEventoResponse> listarEdiciones(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.listarEdiciones(id));
    }

    @Operation(summary = "Eliminar evento o edición",
            description = "Elimina un evento en configuración. Siempre exige confirmar=true; "
                    + "si es base de otras ediciones o ya no está en configuración, se rechaza (Criterio 5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento eliminado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Requiere confirmación (CONFIRMACION_REQUERIDA) u operación no permitida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_ELIMINAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> eliminar(
            @PathVariable Long id,
            @Parameter(description = "Confirmación explícita de la eliminación: solo se acepta el valor true", example = "true")
            @RequestParam(required = false) String confirmar
    ) {
        eventoService.eliminar(id, esConfirmacionExplicita(confirmar));
        return ResponseEntity.ok(Map.of(
                "mensaje", "Evento eliminado exitosamente.",
                "eventoId", String.valueOf(id)
        ));
    }

    /**
     * URL del recurso creado, para el encabezado {@code Location} de las respuestas 201.
     *
     * @param id ID del evento o edición creado.
     * @return URI absoluta, p. ej. {@code http://localhost:8080/api/v1/eventos/5}.
     */
    private URI ubicacion(Long id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/eventos/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    /**
     * Interpreta el parámetro {@code confirmar} de forma estricta: solo {@code "true"} confirma.
     * Sin parámetro o con {@code "false"} no hay confirmación; cualquier otro valor (p. ej. {@code yes}
     * o {@code 1}, que Spring aceptaría como verdadero) se rechaza para evitar ambigüedad.
     *
     * @param confirmar Valor recibido en la consulta.
     * @return {@code true} solo si se recibió exactamente {@code "true"}.
     */
    private boolean esConfirmacionExplicita(String confirmar) {
        if (confirmar == null || "false".equals(confirmar)) {
            return false;
        }
        if ("true".equals(confirmar)) {
            return true;
        }
        throw new ConfirmacionRequeridaException(
                "El parámetro 'confirmar' solo admite el valor true. Envíe confirmar=true para confirmar la eliminación.");
    }
}
