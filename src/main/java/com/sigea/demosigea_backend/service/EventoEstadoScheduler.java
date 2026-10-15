package com.sigea.demosigea_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cambia automáticamente el estado de los eventos según sus fechas:
 * {@code habilitado} → {@code en_ejecucion} al llegar la fecha de inicio. El cierre del evento
 * ({@code cerrado}) no es automático: corresponde al cierre formal (CU-32).
 * <p>
 * Se ejecuta:
 * <ul>
 *   <li>al arrancar la aplicación, para ponerse al día si el servidor estuvo apagado o dormido;</li>
 *   <li>una vez al día, a las 00:01 ({@code app.eventos.estados.cron}), en la zona del programa: las fechas del evento no tienen hora, así que el estado solo puede cambiar al empezar un día nuevo.</li>
 * </ul>
 * La regla está en {@link EventoService#actualizarEstadosPorFecha()} y es idempotente. Se puede
 * desactivar con {@code app.eventos.estados.automaticos=false}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.eventos.estados.automaticos", havingValue = "true", matchIfMissing = true)
public class EventoEstadoScheduler {

    private final EventoService eventoService;

    /** Ponerse al día al arrancar (Railway puede reiniciar o suspender el servicio). */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        ejecutar();
    }

    /** Revisión diaria, al empezar el día en la zona del programa. */
    @Scheduled(cron = "${app.eventos.estados.cron:0 1 0 * * *}", zone = "${app.zona-horaria:America/Bogota}")
    public void diariamente() {
        ejecutar();
    }

    /**
     * Un fallo (por ejemplo, la base de datos no disponible) se registra y no detiene la aplicación;
     * la siguiente ejecución lo vuelve a intentar.
     */
    void ejecutar() {
        try {
            eventoService.actualizarEstadosPorFecha();
        } catch (RuntimeException ex) {
            log.error("No se pudieron actualizar los estados de los eventos: {}", ex.getMessage());
        }
    }
}
