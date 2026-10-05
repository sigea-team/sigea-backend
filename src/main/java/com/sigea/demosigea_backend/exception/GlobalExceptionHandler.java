package com.sigea.demosigea_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para la API REST de SIGEA.
 * <p>
 * Captura y procesa las excepciones lanzadas por los controladores y la lógica
 * de negocio, transformándolas en respuestas HTTP estandarizadas mediante el DTO
 * {@link ErrorResponse}. Garantiza una estructura uniforme de errores para el consumo
 * por parte del cliente frontend.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura las fallas de validación de Bean Validation (campos obligatorios, formatos, etc.).
     * Mapea cada campo no válido con su respectivo mensaje de error.
     *
     * @param ex Excepción lanzada cuando la validación de un argumento de método falla.
     * @return {@link ResponseEntity} con estado 400 BAD_REQUEST y el detalle de los campos inválidos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Los datos enviados no cumplen con los requisitos o políticas exigidas.",
                "VALIDACION_FALLIDA",
                null,
                errores
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Gestiona los intentos de registro o creación de recursos repetidos en el sistema.
     *
     * @param ex Excepción personalizada para recursos duplicados (ej. correo o documento registrado).
     * @return {@link ResponseEntity} con estado 409 CONFLICT y el mensaje descriptivo.
     */
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoDuplicado(RecursoDuplicadoException ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                "CUENTA_YA_EXISTE",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * HU-06, Criterio 2: la persona ya es miembro vigente del comité organizador del evento.
     * Al ser subclase de {@link RecursoDuplicadoException}, Spring elige este manejador por ser
     * el más específico, y se responde con un código propio en lugar de CUENTA_YA_EXISTE.
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código MIEMBRO_COMITE_DUPLICADO.
     */
    @ExceptionHandler(MiembroComiteDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleMiembroComiteDuplicado(MiembroComiteDuplicadoException ex,
                                                                      HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "MIEMBRO_COMITE_DUPLICADO", request);
    }

    /**
     * Criterio 2: ya existe una línea temática con el mismo nombre en el evento.
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código LINEA_TEMATICA_DUPLICADA.
     */
    @ExceptionHandler(LineaTematicaDuplicadaException.class)
    public ResponseEntity<ErrorResponse> handleLineaTematicaDuplicada(LineaTematicaDuplicadaException ex,
                                                                      HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "LINEA_TEMATICA_DUPLICADA", request);
    }

    /**
     * Maneja el acceso o autenticación de cuentas cuyos correos no han completado el flujo de verificación.
     * Adjunta la dirección de correo involucrada para facilitar reenvíos desde la interfaz gráfica.
     *
     * @param ex Excepción de correo no verificado.
     * @return {@link ResponseEntity} con estado 403 FORBIDDEN, el correo del usuario y el código de error.
     */
    @ExceptionHandler(CorreoNoVerificadoException.class)
    public ResponseEntity<ErrorResponse> handleCorreoNoVerificado(CorreoNoVerificadoException ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                "CORREO_NO_VERIFICADO",
                ex.getCorreo(),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Procesa los fallos de autenticación cuando el usuario o clave ingresados son incorrectos.
     *
     * @param ex Excepción lanzada al ingresar credenciales no válidas.
     * @return {@link ResponseEntity} con estado 401 UNAUTHORIZED.
     */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage(),
                "CREDENCIALES_INVALIDAS",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * Captura las peticiones de inicio de sesión sobre cuentas que se encuentran bloqueadas
     * temporalmente tras haber superado el número máximo de intentos fallidos permitidos
     * (HU-01, Criterio 3).
     * <p>
     * Además del mensaje legible, devuelve {@code bloqueadoHasta} para que el frontend pueda
     * mostrar la hora de desbloqueo o un contador, y el encabezado estándar {@code Retry-After}
     * con los segundos restantes del bloqueo.
     * </p>
     *
     * @param ex      Excepción lanzada cuando la cuenta está en periodo de bloqueo temporal.
     * @param request Petición HTTP recibida, usada para informar la URI solicitada.
     * @return {@link ResponseEntity} con estado 423 LOCKED y el detalle del bloqueo.
     */
    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<ErrorResponse> handleCuentaBloqueada(CuentaBloqueadaException ex,
                                                               HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.LOCKED.value(),
                HttpStatus.LOCKED.getReasonPhrase(),
                ex.getMessage(),
                "CUENTA_BLOQUEADA",
                null,
                null,
                request.getRequestURI(),
                ex.getBloqueadoHasta()
        );

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.LOCKED);
        if (ex.getBloqueadoHasta() != null) {
            long segundosRestantes = Math.max(0,
                    Duration.between(LocalDateTime.now(), ex.getBloqueadoHasta()).getSeconds());
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(segundosRestantes));
        }
        return builder.body(response);
    }
    

    /**
     * Captura errores relacionados con tokens JWT o de verificación corruptos, expirados o malformados.
     *
     * @param ex Excepción de token no válido.
     * @return {@link ResponseEntity} con estado 400 BAD_REQUEST.
     */
    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleTokenInvalido(TokenInvalidoException ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                "TOKEN_INVALIDO",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maneja las búsquedas fallidas de entidades inexistentes en la base de datos.
     *
     * @param ex Excepción lanzada al no encontrar un registro específico.
     * @return {@link ResponseEntity} con estado 404 NOT_FOUND.
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                "RECURSO_NO_ENCONTRADO",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Captura operaciones denegadas por reglas de negocio o violaciones de integridad referencial controlada.
     *
     * @param ex Excepción de operación no permitida.
     * @return {@link ResponseEntity} con estado 409 CONFLICT.
     */
    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ErrorResponse> handleOperacionNoPermitida(OperacionNoPermitidaException ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                "OPERACION_NO_PERMITIDA",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Operaciones que requieren confirmación explícita del usuario (HU-04, Criterio 5).
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código CONFIRMACION_REQUERIDA.
     */
    @ExceptionHandler(ConfirmacionRequeridaException.class)
    public ResponseEntity<ErrorResponse> handleConfirmacionRequerida(ConfirmacionRequeridaException ex,
                                                                     HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "CONFIRMACION_REQUERIDA", request);
    }

    /**
     * Cuerpo JSON malformado o con valores no convertibles (p. ej. una modalidad que no existe
     * o una fecha con formato inválido).
     *
     * @return {@link ResponseEntity} con estado 400 BAD REQUEST.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMensajeIlegible(HttpMessageNotReadableException ex,
                                                               HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición es inválido: revise el formato JSON, las fechas (AAAA-MM-DD) y los valores permitidos.",
                "VALIDACION_FALLIDA", request);
    }

    /**
     * HU-03, Criterio 3: intento de modificar o eliminar un registro de auditoría.
     *
     * @return {@link ResponseEntity} con estado 405 METHOD NOT ALLOWED.
     */
    @ExceptionHandler(RegistroAuditoriaInmutableException.class)
    public ResponseEntity<ErrorResponse> handleAuditoriaInmutable(RegistroAuditoriaInmutableException ex,
                                                                  HttpServletRequest request) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), "AUDITORIA_INMUTABLE", request);
    }

    /**
     * Parámetros de consulta incoherentes (ej. rango de fechas invertido en el filtro de auditoría).
     *
     * @return {@link ResponseEntity} con estado 400 BAD REQUEST.
     */
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleSolicitudInvalida(SolicitudInvalidaException ex,
                                                                 HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), "SOLICITUD_INVALIDA", request);
    }

    /**
     * Parámetro con formato inválido (ej. fecha que no cumple yyyy-MM-dd o un estado de evento inexistente).
     *
     * @return {@link ResponseEntity} con estado 400 BAD REQUEST.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoParametro(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        String mensaje = "El parámetro '" + ex.getName() + "' tiene un formato inválido.";
        return construir(HttpStatus.BAD_REQUEST, mensaje, "PARAMETRO_INVALIDO", request);
    }

    /**
     * Acceso denegado por {@code @PreAuthorize}. Sin este manejador la excepción caería en el
     * manejador genérico y se respondería 500 en lugar de 403.
     *
     * @return {@link ResponseEntity} con estado 403 FORBIDDEN.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccessDeniedException ex,
                                                              HttpServletRequest request) {
        return construir(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación.",
                "ACCESO_DENEGADO", request);
    }

    /**
     * Método HTTP no soportado por el endpoint (sin este manejador se respondería 500).
     *
     * @return {@link ResponseEntity} con estado 405 METHOD NOT ALLOWED.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodoNoSoportado(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest request) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED,
                "El método " + ex.getMethod() + " no está permitido en este recurso.",
                "METODO_NO_PERMITIDO", request);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje, String codigo,
                                                    HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                codigo,
                null,
                null,
                request != null ? request.getRequestURI() : null,
                null
        );
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Manejador de último recurso (*fallback*) para capturar cualquier error no controlado explícitamente.
     *
     * @param ex Excepción general del sistema.
     * @return {@link ResponseEntity} con estado 500 INTERNAL_SERVER_ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ha ocurrido un error inesperado en el servidor: " + ex.getMessage(),
                "ERROR_INTERNO",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}