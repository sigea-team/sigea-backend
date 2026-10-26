package com.sigea.demosigea_backend.dto;

import com.sigea.demosigea_backend.dto.auth.PoliticaContrasena;
import com.sigea.demosigea_backend.dto.auth.RegistroRequest;
import com.sigea.demosigea_backend.dto.auth.RestablecerContrasenaRequest;
import com.sigea.demosigea_backend.dto.conferencista.CompletarInvitacionRequest;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Evita que la política de contraseñas se desalinee: el registro, el restablecimiento y la cuenta del
 * conferencista invitado (HU-12) deben usar exactamente la misma regla y el mismo mensaje.
 */
class PoliticaContrasenaTest {

    @ParameterizedTest(name = "{0}.{1}")
    @CsvSource({
            "RegistroRequest, contrasena",
            "RestablecerContrasenaRequest, nuevaContrasena",
            "CompletarInvitacionRequest, contrasena"
    })
    @DisplayName("Los tres formularios con contraseña usan la misma política y el mismo mensaje")
    void todosUsanLaMismaPolitica(String clase, String campo) throws Exception {
        Class<?> tipo = switch (clase) {
            case "RegistroRequest" -> RegistroRequest.class;
            case "RestablecerContrasenaRequest" -> RestablecerContrasenaRequest.class;
            default -> CompletarInvitacionRequest.class;
        };
        Pattern patron = tipo.getDeclaredField(campo).getAnnotation(Pattern.class);
        assertNotNull(patron, clase + "." + campo + " debe validar la política de contraseña");
        assertEquals(PoliticaContrasena.PATRON, patron.regexp());
        assertEquals(PoliticaContrasena.MENSAJE, patron.message());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Clave123*", "Segura#2026", "aB3$aB3$"})
    @DisplayName("Acepta contraseñas que cumplen la política")
    void aceptaValidas(String valida) {
        assertTrue(valida.matches(PoliticaContrasena.PATRON));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "clave123*", "CLAVE123*", "Clave1234", "Clave*sin", "Cl1*"})
    @DisplayName("Rechaza contraseñas cortas, sin mayúscula, sin minúscula, sin número o sin símbolo")
    void rechazaInvalidas(String invalida) {
        assertFalse(invalida.matches(PoliticaContrasena.PATRON));
    }
}
