package com.sigea.demosigea_backend.exception;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.memoria.CierreConPendientesResponse;
import com.sigea.demosigea_backend.dto.memoria.PendienteCierreResponse;
import com.sigea.demosigea_backend.service.RegistroIntentosHistoricosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * HU-28: respuestas y auditoría de los intentos rechazados sobre la memoria histórica.
 */
class GlobalExceptionHandlerMemoriaHistoricaTest {

    private RegistroIntentosHistoricosService registro;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        registro = mock(RegistroIntentosHistoricosService.class);
        handler = new GlobalExceptionHandler();
        handler.setRegistroIntentosHistoricos(registro);
    }

    private static RuntimeException errorTrigger(String operacion) {
        return new RuntimeException("could not execute statement", new SQLException(
                "ERROR: La información del evento 12 está consolidada en la memoria histórica y no se puede "
                        + "modificar (" + operacion + " sobre actividades).", "SG028"));
    }

    @Test
    @DisplayName("Criterio 3: el error SG028 de la base de datos responde 409 EVENTO_ARCHIVADO y queda en la auditoría")
    void errorDeTriggerEnEliminacion() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/eventos/12/actividades/3");

        ResponseEntity<ErrorResponse> respuesta = handler.handleGeneralException(errorTrigger("UPDATE"), request);

        assertEquals(409, respuesta.getStatusCode().value());
        assertEquals(EventoArchivadoException.CODIGO, respuesta.getBody().codigo());
        // El método DELETE marca el intento como eliminación aunque el trigger haya visto un UPDATE.
        verify(registro).registrarIntentoRechazado(eq(12L), eq(true), eq("DELETE"),
                eq("/api/v1/eventos/12/actividades/3"), anyString());
    }

    @Test
    @DisplayName("Criterio 2: una modificación rechazada queda en la auditoría como modificación")
    void errorDeTriggerEnModificacion() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/eventos/12/actividades/3");

        handler.handleGeneralException(errorTrigger("UPDATE"), request);

        verify(registro).registrarIntentoRechazado(eq(12L), eq(false), eq("PUT"), anyString(), anyString());
    }

    @Test
    @DisplayName("Rechazo desde el backend (EventoArchivadoException) también se audita")
    void rechazoDelBackend() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/eventos/4/inscripciones");

        ResponseEntity<ErrorResponse> respuesta = handler.handleEventoArchivado(
                new EventoArchivadoException("solo consulta", 4L, false), request);

        assertEquals(409, respuesta.getStatusCode().value());
        assertEquals("solo consulta", respuesta.getBody().message());
        verify(registro).registrarIntentoRechazado(4L, false, "POST", "/api/v1/eventos/4/inscripciones",
                "solo consulta");
    }

    @Test
    @DisplayName("Si la auditoría falla, la respuesta sigue siendo 409")
    void auditoriaFalla() {
        doThrow(new IllegalStateException("bd caída")).when(registro)
                .registrarIntentoRechazado(any(), anyBoolean(), any(), any(), any());

        ResponseEntity<ErrorResponse> respuesta = handler.handleGeneralException(errorTrigger("DELETE"),
                new MockHttpServletRequest("DELETE", "/api/v1/eventos/12"));

        assertEquals(409, respuesta.getStatusCode().value());
    }

    @Test
    @DisplayName("Sin el servicio de registro (pruebas con new GlobalExceptionHandler()) responde 409 igual")
    void sinRegistro() {
        ResponseEntity<ErrorResponse> respuesta = new GlobalExceptionHandler()
                .handleGeneralException(errorTrigger("DELETE"), new MockHttpServletRequest("DELETE", "/x"));

        assertEquals(409, respuesta.getStatusCode().value());
    }

    @Test
    @DisplayName("Otros errores siguen respondiendo 500 sin auditoría de memoria histórica")
    void otrosErrores() {
        ResponseEntity<ErrorResponse> respuesta = handler.handleGeneralException(
                new RuntimeException(new SQLException("otro", "XX000")), new MockHttpServletRequest("GET", "/x"));

        assertEquals(500, respuesta.getStatusCode().value());
        verify(registro, never()).registrarIntentoRechazado(any(), anyBoolean(), any(), any(), any());
    }

    @Test
    @DisplayName("CU-32: el cierre con pendientes responde 409 con la lista")
    void cierreConPendientes() {
        CierreConPendientesException ex = new CierreConPendientesException("pendientes",
                List.of(new PendienteCierreResponse("EVALUACIONES_SIN_FINALIZAR", 2, "Asignaciones pendientes")));

        ResponseEntity<CierreConPendientesResponse> respuesta = handler.handleCierreConPendientes(ex,
                new MockHttpServletRequest("POST", "/api/v1/eventos/1/memoria-historica"));

        assertEquals(409, respuesta.getStatusCode().value());
        assertEquals(CierreConPendientesException.CODIGO, respuesta.getBody().codigo());
        assertEquals(1, respuesta.getBody().pendientes().size());
    }
}
