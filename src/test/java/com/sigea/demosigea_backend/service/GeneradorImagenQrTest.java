package com.sigea.demosigea_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de {@link GeneradorImagenQr} (HU-24) con ZXing real.
 */
class GeneradorImagenQrTest {

    private final GeneradorImagenQr generador = new GeneradorImagenQr();

    private BufferedImage leer(byte[] png) throws IOException {
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(imagen, "El resultado debe ser un PNG legible");
        return imagen;
    }

    @Test
    @DisplayName("Genera un PNG cuadrado del tamaño pedido con módulos negros y blancos")
    void generaPng() throws IOException {
        byte[] png = generador.generarPng(CodigoQrService.generarCodigo(), 300);

        assertEquals((byte) 0x89, png[0]);
        assertEquals('P', png[1]);
        BufferedImage imagen = leer(png);
        assertEquals(300, imagen.getWidth());
        assertEquals(300, imagen.getHeight());
        // La esquina es margen blanco y el patrón de posición superior izquierdo tiene módulos negros
        assertEquals(0xFFFFFF, imagen.getRGB(0, 0) & 0xFFFFFF);
        boolean hayNegro = false;
        for (int x = 0; x < 150 && !hayNegro; x++) {
            hayNegro = (imagen.getRGB(x, x) & 0xFFFFFF) == 0;
        }
        assertEquals(true, hayNegro);
    }

    @Test
    @DisplayName("El tamaño se ajusta al rango permitido")
    void ajustaTamano() throws IOException {
        assertEquals(GeneradorImagenQr.TAMANO_MINIMO, leer(generador.generarPng("SIGEA-x", 10)).getWidth());
        assertEquals(GeneradorImagenQr.TAMANO_MAXIMO, leer(generador.generarPng("SIGEA-x", 5000)).getWidth());
        assertEquals(GeneradorImagenQr.TAMANO_POR_DEFECTO, leer(generador.generarPng("SIGEA-x")).getWidth());
    }

    @Test
    @DisplayName("Códigos distintos producen imágenes distintas")
    void codigosDistintos() {
        assertNotEquals(java.util.Arrays.toString(generador.generarPng("SIGEA-aaaa")),
                java.util.Arrays.toString(generador.generarPng("SIGEA-bbbb")));
    }

    @Test
    @DisplayName("Rechaza contenido vacío")
    void rechazaVacio() {
        assertThrows(IllegalArgumentException.class, () -> generador.generarPng(" "));
        assertThrows(IllegalArgumentException.class, () -> generador.generarPng(null));
    }
}
