package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.auth.LoginRequest;
import com.sigea.demosigea_backend.dto.auth.LoginResponse;
import com.sigea.demosigea_backend.dto.auth.ReenviarVerificacionRequest;
import com.sigea.demosigea_backend.dto.auth.ReenviarVerificacionResponse;
import com.sigea.demosigea_backend.dto.auth.RegistroRequest;
import com.sigea.demosigea_backend.dto.auth.RegistroResponse;
import com.sigea.demosigea_backend.dto.auth.VerificarCorreoRequest;
import com.sigea.demosigea_backend.dto.auth.VerificarCorreoResponse;
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

/**
 * Controlador REST para la autenticación y el registro de usuarios en la plataforma SIGEA.
 * <p>
 * Expone los endpoints públicos bajo {@code /api/v1/auth} para las operaciones de:
 * registro de nuevos usuarios, inicio de sesión (login), verificación de correo electrónico
 * y reenvío del enlace de verificación.
 * </p>
 * <p>
 * Todos los endpoints de este controlador son de acceso público (no requieren autenticación JWT).
 * </p>
 *
 * @author SIGEA Team
 * @version 1.1
 * @see AuthService
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación y Registro", description = "Endpoints de registro de personas, autenticación (login) y verificación de correo electrónico")
public class AuthController {

    /** Servicio de autenticación que contiene la lógica de negocio. */
    private final AuthService authService;

    /**
     * Registra un nuevo usuario en la plataforma SIGEA.
     * <p>
     * Crea el registro de persona y su cuenta de usuario asociada con el rol inicial por defecto.
     * Envía un correo electrónico con un token de verificación para habilitar el acceso.
     * </p>
     *
     * @param request datos del formulario de registro validados con Bean Validation
     * @return {@link RegistroResponse} con los datos del usuario creado y mensaje informativo (HTTP 201)
     */
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

    /**
     * Autentica un usuario en la plataforma mediante correo electrónico y contraseña.
     * <p>
     * Valida las credenciales, verifica que el correo esté confirmado y que la cuenta esté activa.
     * Si la autenticación es exitosa, genera y retorna un token JWT Bearer.
     * </p>
     *
     * @param request credenciales de inicio de sesión (correo + contraseña)
     * @return {@link LoginResponse} con el token JWT y datos del usuario autenticado (HTTP 200)
     */
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

    /**
     * Verifica el correo electrónico de un usuario mediante un enlace GET.
     * <p>
     * Este endpoint es invocado cuando el usuario hace clic en el enlace de verificación
     * recibido en su correo electrónico tras el registro.
     * </p>
     *
     * @param token token UUID de verificación recibido como parámetro de consulta
     * @return {@link VerificarCorreoResponse} con el resultado de la verificación (HTTP 200)
     */
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

    /**
     * Verifica el correo electrónico de un usuario enviando el token en el cuerpo de la petición.
     * <p>
     * Alternativa al endpoint GET para verificar el correo cuando el frontend prefiere
     * enviar el token vía POST en lugar de parámetro de consulta.
     * </p>
     *
     * @param request objeto con el token de verificación
     * @return {@link VerificarCorreoResponse} con el resultado de la verificación (HTTP 200)
     */
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

    /**
     * Reenvía el enlace de verificación de correo electrónico a una cuenta pendiente de verificar.
     * <p>
     * Invalida los tokens de verificación previos que no hayan sido usados, genera uno nuevo
     * con vigencia de 24 horas y envía el correo electrónico correspondiente.
     * </p>
     *
     * @param request datos con el correo de la cuenta
     * @return {@link ReenviarVerificacionResponse} con mensaje de confirmación y correo destino (HTTP 200)
     */
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

    /**
     * Obtiene el catálogo de afiliaciones institucionales.
     * Endpoint público para poblar el combobox en el formulario de registro.
     *
     * @return Lista de {@link com.sigea.demosigea_backend.model.Afiliacion} (HTTP 200)
     */
    @Operation(
            summary = "Obtener catálogo de afiliaciones institucionales",
            description = "Devuelve la lista de afiliaciones institucionales registradas en el sistema para consumo en formularios de registro."
    )
    @ApiResponse(responseCode = "200", description = "Lista de afiliaciones obtenida exitosamente")
    @GetMapping("/afiliaciones")
    public ResponseEntity<java.util.List<com.sigea.demosigea_backend.model.Afiliacion>> obtenerAfiliaciones() {
        return ResponseEntity.ok(authService.obtenerAfiliaciones());
    }
}


