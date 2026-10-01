package com.sigea.demosigea_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;

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
     * @param ex Excepción de confirmación requerida.
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código CONFIRMACION_REQUERIDA.
     */
    @ExceptionHandler(ConfirmacionRequeridaException.class)
    public ResponseEntity<ErrorResponse> handleConfirmacionRequerida(ConfirmacionRequeridaException ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                "CONFIRMACION_REQUERIDA",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Cuerpo JSON malformado o con valores no convertibles (p. ej. una modalidad que no existe
     * o una fecha con formato inválido).
     *
     * @param ex Excepción de lectura del cuerpo HTTP.
     * @return {@link ResponseEntity} con estado 400 BAD_REQUEST.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMensajeIlegible(HttpMessageNotReadableException ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El cuerpo de la petición es inválido: revise el formato JSON, las fechas (AAAA-MM-DD) y los valores permitidos.",
                "VALIDACION_FALLIDA",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Parámetros de ruta o de consulta con un tipo/valor no válido (p. ej. ?estado=inexistente).
     *
     * @param ex Excepción de conversión de argumento.
     * @return {@link ResponseEntity} con estado 400 BAD_REQUEST.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoArgumentoInvalido(MethodArgumentTypeMismatchException ex) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El valor '" + ex.getValue() + "' no es válido para el parámetro '" + ex.getName() + "'.",
                "VALIDACION_FALLIDA",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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