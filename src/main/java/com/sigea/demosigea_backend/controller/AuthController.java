package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.ErrorResponse;
import com.sigea.demosigea_backend.dto.LoginRequest;
import com.sigea.demosigea_backend.dto.LoginResponse;
import com.sigea.demosigea_backend.dto.ReenviarVerificacionRequest;
import com.sigea.demosigea_backend.dto.ReenviarVerificacionResponse;
import com.sigea.demosigea_backend.dto.RegistroRequest;
import com.sigea.demosigea_backend.dto.RegistroResponse;
import com.sigea.demosigea_backend.dto.VerificarCorreoRequest;
import com.sigea.demosigea_backend.dto.VerificarCorreoResponse;
import com.sigea.demosigea_backend.service.AuthService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación y Registro", description = "Endpoints de registro de personas, autenticación (login) y verificación de correo electrónico")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Registro de usuario inicial",
            description = "Registra los datos básicos de una persona y crea una cuenta de usuario con rol inicial. Envía un correo con token de verificación para habilitar el acceso."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada exitosamente",
                    content = @Content(schema = @Schema(implementation = RegistroResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o contraseña no cumple con la política de seguridad",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El correo o documento ya existe en el sistema",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<RegistroResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        RegistroResponse response = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Inicio de sesión (Login)",
            description = "Valida credenciales y estado de verificación del correo. Si el correo no está verificado, rechaza con HTTP 403 y ofrece reenviar el enlace."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Correo no verificado (impide el acceso y ofrece reenviar verificación)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Verificar correo electrónico por enlace (GET)",
            description = "Endpoint invocado cuando el usuario hace clic en el enlace de verificación enviado a su correo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Correo verificado exitosamente",
                    content = @Content(schema = @Schema(implementation = VerificarCorreoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token inválido, expirado o ya utilizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/verify-email")
    public ResponseEntity<VerificarCorreoResponse> verificarCorreoPorGet(@RequestParam("token") String token) {
        VerificarCorreoResponse response = authService.verificarCorreo(token);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Verificar correo electrónico por body (POST)",
            description = "Alternativa para verificar el correo enviando el token en el cuerpo de la petición."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Correo verificado exitosamente",
                    content = @Content(schema = @Schema(implementation = VerificarCorreoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token inválido, expirado o ya utilizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/verify-email")
    public ResponseEntity<VerificarCorreoResponse> verificarCorreoPorPost(@Valid @RequestBody VerificarCorreoRequest request) {
        VerificarCorreoResponse response = authService.verificarCorreo(request.token());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reenviar enlace de verificación de correo",
            description = "Genera un nuevo token de verificación y lo envía por correo electrónico a la cuenta que aún no se encuentra verificada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enlace reenviado satisfactoriamente",
                    content = @Content(schema = @Schema(implementation = ReenviarVerificacionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o correo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<ReenviarVerificacionResponse> reenviarVerificacion(@Valid @RequestBody ReenviarVerificacionRequest request) {
        ReenviarVerificacionResponse response = authService.reenviarVerificacion(request);
        return ResponseEntity.ok(response);
    }
}
