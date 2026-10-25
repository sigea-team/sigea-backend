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
import com.sigea.demosigea_backend.dto.conferencista.ConflictoConferencistaResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
            String field = error.getField();
            if (field.contains(".")) {
                String subField = field.substring(field.lastIndexOf('.') + 1);
                errores.putIfAbsent(subField, error.getDefaultMessage());
            }
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
     * HU-07: ya existe un rubro vigente con ese nombre en el presupuesto preliminar del evento.
     * Al ser subclase de {@link RecursoDuplicadoException}, Spring elige este manejador por ser
     * el más específico.
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código RUBRO_DUPLICADO.
     */
    @ExceptionHandler(RubroDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleRubroDuplicado(RubroDuplicadoException ex,
                                                              HttpServletRequest request) {
           return construir(HttpStatus.CONFLICT, ex.getMessage(), "RUBRO_DUPLICADO", request);
    }

    /**
     * HU-05, Criterio 3: ya existe un tipo de actividad o una línea temática con ese nombre en el evento.
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código PARAMETRO_DUPLICADO.
     */
    @ExceptionHandler(ParametroEventoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleParametroDuplicado(ParametroEventoDuplicadoException ex,
                                                                  HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "PARAMETRO_DUPLICADO", request);
    }

    /**
     * HU-05, Criterio 2: el tipo de actividad o la línea temática está en uso y no puede eliminarse.
     * Al ser subclase de {@link OperacionNoPermitidaException}, Spring elige este manejador por ser
     * el más específico.
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código PARAMETRO_EN_USO.
     */
    @ExceptionHandler(ParametroEventoEnUsoException.class)
    public ResponseEntity<ErrorResponse> handleParametroEnUso(ParametroEventoEnUsoException ex,
                                                              HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "PARAMETRO_EN_USO", request);
    }

    /**
     * HU-12, Criterio 3: el documento o el correo ya pertenecen a una persona registrada. Además del
     * mensaje se devuelve la persona encontrada ({@code coincidencia}) para que el frontend ofrezca
     * asociarla al evento en lugar de crear un duplicado.
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código CONFERENCISTA_EXISTENTE.
     */
    @ExceptionHandler(ConferencistaExistenteException.class)
    public ResponseEntity<ConflictoConferencistaResponse> handleConferencistaExistente(
            ConferencistaExistenteException ex, HttpServletRequest request) {
        ConflictoConferencistaResponse response = new ConflictoConferencistaResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                "CONFERENCISTA_EXISTENTE",
                request != null ? request.getRequestURI() : null,
                ex.getCoincidencia()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * HU-12: ya hay una invitación vigente para ese correo en el evento.
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código INVITACION_PENDIENTE.
     */
    @ExceptionHandler(InvitacionPendienteException.class)
    public ResponseEntity<ErrorResponse> handleInvitacionPendiente(InvitacionPendienteException ex,
                                                                   HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "INVITACION_PENDIENTE", request);
    }

    /**
     * HU-12: el enlace de invitación venció, ya se usó, fue cancelado o el evento ya no recibe
     * conferencistas.
     *
     * @return {@link ResponseEntity} con estado 410 GONE y el código de la causa.
     */
    @ExceptionHandler(InvitacionNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> handleInvitacionNoDisponible(InvitacionNoDisponibleException ex,
                                                                      HttpServletRequest request) {
        return construir(HttpStatus.GONE, ex.getMessage(), ex.getCodigo(), request);
    }

    /**
     * HU-12, Criterio 3: el conferencista ya está asociado al evento.
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código CONFERENCISTA_YA_ASOCIADO.
     */
    @ExceptionHandler(ConferencistaYaAsociadoException.class)
    public ResponseEntity<ErrorResponse> handleConferencistaYaAsociado(ConferencistaYaAsociadoException ex,
                                                                       HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "CONFERENCISTA_YA_ASOCIADO", request);
    }

    /**
     * HU-12: al actualizar un conferencista, el documento o el correo pertenecen a otra persona.
     * En {@code erroresValidacion} se indica el campo en conflicto.
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código DATOS_PERSONA_DUPLICADOS.
     */
    @ExceptionHandler(DatosPersonaDuplicadosException.class)
    public ResponseEntity<ErrorResponse> handleDatosPersonaDuplicados(DatosPersonaDuplicadosException ex,
                                                                      HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                "DATOS_PERSONA_DUPLICADOS",
                null,
                ex.getCampo() != null ? Map.of(ex.getCampo(), ex.getMessage()) : null,
                request != null ? request.getRequestURI() : null,
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * HU-12, Criterio 2: reglas de los datos del conferencista que no cubre Bean Validation
     * (años futuros, trayectoria faltante, correo vinculado a una cuenta). Se responde igual que
     * un error de validación, con el detalle por campo.
     *
     * @return {@link ResponseEntity} con estado 400 BAD_REQUEST y código VALIDACION_FALLIDA.
     */
    @ExceptionHandler(DatosConferencistaInvalidosException.class)
    public ResponseEntity<ErrorResponse> handleDatosConferencistaInvalidos(DatosConferencistaInvalidosException ex,
                                                                           HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                "VALIDACION_FALLIDA",
                null,
                ex.getCampos(),
                request != null ? request.getRequestURI() : null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
     * Conflicto de interés que impide asignar la propuesta al evaluador (HU-16, Criterio 2).
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código {@code CONFLICTO_INTERES}.
     */
    @ExceptionHandler(ConflictoInteresException.class)
    public ResponseEntity<ErrorResponse> handleConflictoInteres(ConflictoInteresException ex,
                                                                HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "CONFLICTO_INTERES", request);
    }

    /**
     * La propuesta ya está asignada al evaluador indicado (HU-16).
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código {@code ASIGNACION_DUPLICADA}.
     */
    @ExceptionHandler(AsignacionDuplicadaException.class)
    public ResponseEntity<ErrorResponse> handleAsignacionDuplicada(AsignacionDuplicadaException ex,
                                                                   HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "ASIGNACION_DUPLICADA", request);
    }

    /**
     * Captura operaciones denegadas por reglas de negocio o violaciones de integridad referencial controlada.
     *
     * @param ex Excepción de operación no permitida.
     * @return {@link ResponseEntity} con estado 409 CONFLICT.
     */
    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ErrorResponse> handleOperacionNoPermitida(OperacionNoPermitidaException ex,
                                                                    HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "OPERACION_NO_PERMITIDA", request);
    }

    /**
     * HU-17, Criterio 4: el evaluador intenta modificar una evaluación ya enviada cuando el
     * periodo de evaluación de la convocatoria está cerrado.
     *
     * @return {@link ResponseEntity} con estado 409 CONFLICT y código EVALUACION_CERRADA.
     */
    @ExceptionHandler(EvaluacionCerradaException.class)
    public ResponseEntity<ErrorResponse> handleEvaluacionCerrada(EvaluacionCerradaException ex,
                                                                 HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), "EVALUACION_CERRADA", request);
    }

    /**
     * HU-17, Criterios 2 y 3: se intentó enviar una evaluación con criterios sin calificar,
     * o la propuesta tiene asignaciones de evaluación pendientes.
     * <p>
     * Incluye el detalle de los criterios faltantes en {@code erroresValidacion}.
     * </p>
     *
     * @return {@link ResponseEntity} con estado 400 BAD_REQUEST y código EVALUACION_INCOMPLETA.
     */
    @ExceptionHandler(EvaluacionIncompletaException.class)
    public ResponseEntity<ErrorResponse> handleEvaluacionIncompleta(EvaluacionIncompletaException ex,
                                                                    HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (ex.getCriteriosFaltantes() != null) {
            for (int i = 0; i < ex.getCriteriosFaltantes().size(); i++) {
                errores.put("criterioFaltante[" + i + "]", ex.getCriteriosFaltantes().get(i));
            }
        }
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                "EVALUACION_INCOMPLETA",
                null,
                errores,
                request.getRequestURI(),
                null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Captura el intento de envío de propuesta sobre una convocatoria cerrada o fuera del rango de fechas.
     *
     * @return {@link ResponseEntity} con estado 400 BAD_REQUEST y código CONVOCATORIA_CERRADA.
     */
    @ExceptionHandler(ConvocatoriaCerradaException.class)
    public ResponseEntity<ErrorResponse> handleConvocatoriaCerrada(ConvocatoriaCerradaException ex,
                                                                   HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), "CONVOCATORIA_CERRADA", request);
    }


    /**
     * HU-11, Criterio 3: se intenta publicar una convocatoria con datos obligatorios sin completar
     * (p. ej. sin fecha límite de recepción de propuestas). En {@code erroresValidacion} se indican
     * los campos que el organizador debe completar.
     *
     * @return {@link ResponseEntity} con estado 400 BAD_REQUEST y código CONVOCATORIA_INCOMPLETA.
     */
    @ExceptionHandler(ConvocatoriaIncompletaException.class)
    public ResponseEntity<ErrorResponse> handleConvocatoriaIncompleta(ConvocatoriaIncompletaException ex,
                                                                      HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                "CONVOCATORIA_INCOMPLETA",
                null,
                ex.getCamposFaltantes(),
                request != null ? request.getRequestURI() : null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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