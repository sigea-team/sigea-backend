package com.sigea.demosigea_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoEstadoSchedulerTest {

    @Mock
    private EventoService eventoService;

    @InjectMocks
    private EventoEstadoScheduler scheduler;

    @Test
    @DisplayName("Al arrancar y en la revisión diaria actualiza los estados de los eventos")
    void alArrancarYDiariamente_actualizanEstados() {
        scheduler.alArrancar();
        scheduler.diariamente();

        verify(eventoService, times(2)).actualizarEstadosPorFecha();
    }

    @Test
    @DisplayName("Un fallo (por ejemplo, base de datos no disponible) no detiene la aplicación")
    void fallo_noSePropaga() {
        when(eventoService.actualizarEstadosPorFecha()).thenThrow(new IllegalStateException("BD no disponible"));

        assertDoesNotThrow(() -> scheduler.alArrancar());
    }
}
