package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.exception.ArchivoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link ProcesadorImagenesCertificado} (HU-26, plantilla de certificados).
 */
class ProcesadorImagenesCertificadoTest {

    private final ProcesadorImagenesCertificado procesador = new ProcesadorImagenesCertificado();

    @Test
    @DisplayName("Fondo: se recorta a carta horizontal y se guarda como JPEG de 2200 × 1700")
    void fondoNormalizado() throws Exception {
        byte[] jpeg = procesador.normalizarFondo(imagen(3000, 1500, BufferedImage.TYPE_INT_RGB, "png", Color.BLUE));

        assertTrue(ProcesadorImagenesCertificado.esJpeg(jpeg));
        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(jpeg));
        assertEquals(2200, leida.getWidth());
        assertEquals(1700, leida.getHeight());
    }

    @Test
    @DisplayName("Fondo: vertical, pequeño, de otro formato o vacío → 400 ARCHIVO_INVALIDO")
    void fondoInvalido() throws Exception {
        assertThrows(ArchivoInvalidoException.class,
                () -> procesador.normalizarFondo(imagen(1200, 1600, BufferedImage.TYPE_INT_RGB, "png", Color.RED)));
        assertThrows(ArchivoInvalidoException.class,
                () -> procesador.normalizarFondo(imagen(800, 600, BufferedImage.TYPE_INT_RGB, "jpg", Color.RED)));
        assertThrows(ArchivoInvalidoException.class,
                () -> procesador.normalizarFondo(imagen(1400, 1000, BufferedImage.TYPE_INT_RGB, "bmp", Color.RED)));
        assertThrows(ArchivoInvalidoException.class, () -> procesador.normalizarFondo(new byte[0]));
        assertThrows(ArchivoInvalidoException.class, () -> procesador.normalizarFondo("%PDF-1.4".getBytes()));
    }

    @Test
    @DisplayName("Firma escaneada (sin transparencia): el blanco se vuelve transparente y se recortan los bordes")
    void firmaSinTransparencia() throws Exception {
        BufferedImage hoja = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = hoja.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 600, 300);
        g.setColor(new Color(20, 30, 120));
        g.fillRect(100, 120, 300, 40);
        g.dispose();

        byte[] png = procesador.normalizarFirma(codificar(hoja, "jpg"));

        assertTrue(ProcesadorImagenesCertificado.esPng(png));
        BufferedImage firma = ImageIO.read(new ByteArrayInputStream(png));
        assertTrue(firma.getColorModel().hasAlpha());
        // Recortada al trazo (con algo de tolerancia por la compresión JPEG).
        assertTrue(firma.getWidth() >= 300 && firma.getWidth() <= 306, "ancho " + firma.getWidth());
        assertTrue(firma.getHeight() >= 40 && firma.getHeight() <= 46, "alto " + firma.getHeight());
        assertEquals(255, (firma.getRGB(firma.getWidth() / 2, firma.getHeight() / 2) >>> 24) & 0xFF);
    }

    @Test
    @DisplayName("Firma grande: se limita a 900 × 360 conservando la proporción")
    void firmaGrande() throws Exception {
        byte[] png = procesador.normalizarFirma(imagen(1800, 360, BufferedImage.TYPE_INT_ARGB, "png", Color.BLACK));
        BufferedImage firma = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(900, firma.getWidth());
        assertEquals(180, firma.getHeight());
    }

    @Test
    @DisplayName("Firma vacía (todo blanco o transparente) → 400")
    void firmaVacia() throws Exception {
        assertThrows(ArchivoInvalidoException.class,
                () -> procesador.normalizarFirma(imagen(300, 100, BufferedImage.TYPE_INT_RGB, "png", Color.WHITE)));
        assertThrows(ArchivoInvalidoException.class,
                () -> procesador.normalizarFirma(codificar(new BufferedImage(300, 100, BufferedImage.TYPE_INT_ARGB), "png")));
    }

    private static byte[] imagen(int w, int h, int tipo, String formato, Color color) throws Exception {
        BufferedImage img = new BufferedImage(w, h, tipo);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return codificar(img, formato);
    }

    private static byte[] codificar(BufferedImage img, String formato) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(img, formato, salida);
        return salida.toByteArray();
    }
}
