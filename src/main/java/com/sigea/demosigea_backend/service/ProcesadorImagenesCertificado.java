package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.exception.ArchivoInvalidoException;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Valida y normaliza las imágenes de la plantilla de certificados (HU-26): el fondo y las firmas.
 * <ul>
 *   <li>Fondo: PNG o JPG horizontal de al menos {@value #ANCHO_MINIMO_FONDO} px de ancho. Se recorta al centro a la
 *       proporción de una carta horizontal (11 × 8,5) y se guarda como JPEG de {@value #ANCHO_FONDO} ×
 *       {@value #ALTO_FONDO} px, así el PDF lo incrusta tal cual y no se deforma.</li>
 *   <li>Firma: PNG o JPG. Si no tiene transparencia (p. ej. una firma escaneada en papel blanco), el blanco se vuelve
 *       transparente para que no tape el fondo. Se recortan los bordes vacíos, se limita a
 *       {@value #ANCHO_MAXIMO_FIRMA} × {@value #ALTO_MAXIMO_FIRMA} px y se guarda como PNG.</li>
 * </ul>
 * Solo se aceptan archivos que realmente son PNG o JPEG (se revisan los primeros bytes, no la extensión).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Component
public class ProcesadorImagenesCertificado {

    static final long TAMANO_MAXIMO_FONDO = 5L * 1024 * 1024;
    static final long TAMANO_MAXIMO_FIRMA = 2L * 1024 * 1024;
    static final int ANCHO_MINIMO_FONDO = 1000;
    static final int ANCHO_FONDO = 2200;
    static final int ALTO_FONDO = 1700;
    static final int ANCHO_MAXIMO_FIRMA = 900;
    static final int ALTO_MAXIMO_FIRMA = 360;
    /** Por encima de este valor en los tres canales, un píxel de una firma sin transparencia se considera papel. */
    static final int UMBRAL_BLANCO = 225;

    /**
     * @param contenido bytes del archivo subido.
     * @return JPEG listo para el PDF.
     * @throws ArchivoInvalidoException si no es PNG/JPG, pesa más de 5 MB, no es horizontal o es muy pequeño.
     */
    public byte[] normalizarFondo(byte[] contenido) {
        BufferedImage original = leer(contenido, TAMANO_MAXIMO_FONDO, "fondo");
        int w = original.getWidth();
        int h = original.getHeight();
        if (w <= h) {
            throw invalido("fondo", "El fondo debe ser horizontal (carta apaisada, proporción 11 × 8,5).");
        }
        if (w < ANCHO_MINIMO_FONDO) {
            throw invalido("fondo", "El fondo debe tener al menos " + ANCHO_MINIMO_FONDO + " px de ancho para "
                    + "imprimirse con buena calidad.");
        }
        // Recorte centrado a la proporción ANCHO_FONDO:ALTO_FONDO.
        int recorteW = w;
        int recorteH = (int) Math.round((double) w * ALTO_FONDO / ANCHO_FONDO);
        if (recorteH > h) {
            recorteH = h;
            recorteW = (int) Math.round((double) h * ANCHO_FONDO / ALTO_FONDO);
        }
        int x0 = (w - recorteW) / 2;
        int y0 = (h - recorteH) / 2;

        BufferedImage destino = new BufferedImage(ANCHO_FONDO, ALTO_FONDO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ANCHO_FONDO, ALTO_FONDO);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(original, 0, 0, ANCHO_FONDO, ALTO_FONDO, x0, y0, x0 + recorteW, y0 + recorteH, null);
        } finally {
            g.dispose();
        }
        return escribirJpeg(destino, 0.88f);
    }

    /**
     * @param contenido bytes del archivo subido.
     * @return PNG con transparencia, recortado y limitado de tamaño.
     * @throws ArchivoInvalidoException si no es PNG/JPG, pesa más de 2 MB o no tiene trazo.
     */
    public byte[] normalizarFirma(byte[] contenido) {
        BufferedImage original = leer(contenido, TAMANO_MAXIMO_FIRMA, "firma");
        int w = original.getWidth();
        int h = original.getHeight();
        boolean conAlfa = original.getColorModel().hasAlpha();

        BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = original.getRGB(x, y);
                int a = conAlfa ? (p >>> 24) & 0xFF : 0xFF;
                int r = (p >> 16) & 0xFF;
                int gr = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                if (!conAlfa && r > UMBRAL_BLANCO && gr > UMBRAL_BLANCO && b > UMBRAL_BLANCO) {
                    a = 0;
                }
                argb.setRGB(x, y, (a << 24) | (r << 16) | (gr << 8) | b);
                if (a > 16) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            throw invalido("firma", "La imagen de la firma está vacía (no tiene trazo visible).");
        }
        BufferedImage recortada = argb.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);

        double escala = Math.min(1.0, Math.min((double) ANCHO_MAXIMO_FIRMA / recortada.getWidth(),
                (double) ALTO_MAXIMO_FIRMA / recortada.getHeight()));
        int nw = Math.max(1, (int) Math.round(recortada.getWidth() * escala));
        int nh = Math.max(1, (int) Math.round(recortada.getHeight() * escala));
        BufferedImage destino = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(recortada, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            ImageIO.write(destino, "png", salida);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo procesar la imagen de la firma.", ex);
        }
        return salida.toByteArray();
    }

    // ──────────────────────────────────────────────────────────────────────────────

    private static BufferedImage leer(byte[] contenido, long maximo, String campo) {
        if (contenido == null || contenido.length == 0) {
            throw invalido(campo, "Adjunte la imagen (PNG o JPG).");
        }
        if (contenido.length > maximo) {
            throw invalido(campo, "La imagen supera el tamaño máximo de " + (maximo / (1024 * 1024)) + " MB.");
        }
        if (!esPng(contenido) && !esJpeg(contenido)) {
            throw invalido(campo, "Solo se aceptan imágenes PNG o JPG.");
        }
        BufferedImage imagen;
        try {
            imagen = ImageIO.read(new ByteArrayInputStream(contenido));
        } catch (IOException | RuntimeException ex) {
            imagen = null;
        }
        if (imagen == null || imagen.getWidth() < 1 || imagen.getHeight() < 1) {
            throw invalido(campo, "No se pudo leer la imagen. Si es un JPG en CMYK, guárdelo en RGB o como PNG.");
        }
        return imagen;
    }

    static boolean esPng(byte[] b) {
        return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    static boolean esJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static byte[] escribirJpeg(BufferedImage imagen, float calidad) {
        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpeg");
        if (!escritores.hasNext()) {
            throw new IllegalStateException("No hay codificador JPEG disponible.");
        }
        ImageWriter escritor = escritores.next();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ImageOutputStream flujo = ImageIO.createImageOutputStream(salida)) {
            escritor.setOutput(flujo);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(calidad);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo procesar la imagen de fondo.", ex);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }

    private static ArchivoInvalidoException invalido(String campo, String mensaje) {
        return new ArchivoInvalidoException(mensaje, Map.of(campo, mensaje));
    }
}
