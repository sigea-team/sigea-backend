package com.sigea.demosigea_backend.dto;

import com.sigea.demosigea_backend.dto.auditoria.AuditoriaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * HU-03: la fecha de la auditoría se muestra en hora de Colombia aunque el servidor esté en UTC.
 */
class AuditoriaZonaHorariaTest {

    @Test
    @DisplayName("La hora del servidor se convierte a hora de Colombia")
    void aHoraColombia_convierteDesdeZonaDelServidor() {
        LocalDateTime servidor = LocalDateTime.of(2026, 10, 21, 1, 30);
        LocalDateTime esperado = servidor.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("America/Bogota"))
                .toLocalDateTime();

        assertEquals(esperado, AuditoriaResponse.aHoraColombia(servidor));
    }

    @Test
    @DisplayName("Una fecha nula se mantiene nula")
    void aHoraColombia_nulo() {
        assertNull(AuditoriaResponse.aHoraColombia(null));
    }
}
