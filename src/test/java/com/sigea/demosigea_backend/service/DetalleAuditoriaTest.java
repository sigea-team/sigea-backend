package com.sigea.demosigea_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DetalleAuditoriaTest {

    @Test
    @DisplayName("Serializa mapas anidados, listas, números, booleanos y nulos en orden de inserción")
    void serializa_estructuras() {
        String json = DetalleAuditoria.aJson(DetalleAuditoria.de(
                "nombre", "ADMIN",
                "permisos", List.of("ROLES_VER", "ROLES_CREAR"),
                "activo", true,
                "total", 3,
                "descripcion", null,
                "extra", DetalleAuditoria.de("a", 1)
        ));
        assertEquals("{\"nombre\":\"ADMIN\",\"permisos\":[\"ROLES_VER\",\"ROLES_CREAR\"],\"activo\":true,"
                + "\"total\":3,\"descripcion\":null,\"extra\":{\"a\":1}}", json);
    }

    @Test
    @DisplayName("Escapa comillas, barras y saltos de línea")
    void escapa_caracteres() {
        assertEquals("{\"t\":\"dijo \\\"hola\\\"\\n\\\\fin\"}",
                DetalleAuditoria.aJson(DetalleAuditoria.de("t", "dijo \"hola\"\n\\fin")));
    }

    @Test
    @DisplayName("Rechaza pares clave/valor incompletos")
    void paresIncompletos() {
        assertThrows(IllegalArgumentException.class, () -> DetalleAuditoria.de("solo-clave"));
    }

    @Test
    @DisplayName("Lista con nulos")
    void listaConNulos() {
        assertEquals("[null,\"x\"]", DetalleAuditoria.aJson(Arrays.asList(null, "x")));
    }
}
