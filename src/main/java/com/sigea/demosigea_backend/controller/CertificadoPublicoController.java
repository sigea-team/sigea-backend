package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.certificado.VerificacionCertificadoResponse;
import com.sigea.demosigea_backend.service.CertificadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verificación pública de certificados por su código (HU-26). No requiere sesión ({@code GET /api/v1/publico/**}).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/publico/certificados")
@RequiredArgsConstructor
@Tag(name = "Certificados (público)", description = "HU-26: comprobar la autenticidad de un certificado")
public class CertificadoPublicoController {

    private final CertificadoService certificadoService;

    @Operation(summary = "Verificar un certificado",
            description = "Con el código impreso en el certificado devuelve el nombre, el rol, el evento y la fecha de "
                    + "emisión. El documento se muestra parcialmente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado auténtico"),
            @ApiResponse(responseCode = "404", description = "No existe un certificado con ese código",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{codigo}")
    public ResponseEntity<VerificacionCertificadoResponse> verificar(@PathVariable String codigo) {
        return ResponseEntity.ok(certificadoService.verificar(codigo));
    }
}
