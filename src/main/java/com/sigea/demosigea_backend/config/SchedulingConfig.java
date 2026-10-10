package com.sigea.demosigea_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Habilita las tareas programadas ({@code @Scheduled}) y define el reloj del sistema.
 * <p>
 * El reloj usa la zona horaria del programa ({@code app.zona-horaria}, por defecto America/Bogota) y no la
 * del servidor: en Railway el servidor está en UTC, y sin esto un evento empezaría a las
 * 7:00 p. m. del día anterior, hora de Colombia. Los servicios lo reciben inyectado para que las pruebas
 * usen un reloj fijo.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * @param zona Zona horaria del programa.
     * @return Reloj del sistema en esa zona.
     */
    @Bean
    public Clock clock(@Value("${app.zona-horaria:America/Bogota}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
