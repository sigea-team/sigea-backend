package com.sigea.demosigea_backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dibuja la imagen PNG de un código QR (HU-24). Usa ZXing solo para calcular los módulos del QR y
 * {@link ImageIO} para escribir el PNG, sin depender del módulo {@code javase} de ZXing.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Component
public class GeneradorImagenQr {

    /** Tamaño por defecto de la imagen, en píxeles. */
    public static final int TAMANO_POR_DEFECTO = 320;
    /** Tamaño mínimo y máximo aceptados. */
    public static final int TAMANO_MINIMO = 120;
    public static final int TAMANO_MAXIMO = 1024;

    private static final int NEGRO = 0xFF000000;
    private static final int BLANCO = 0xFFFFFFFF;

    /**
     * Genera el PNG del QR que codifica {@code texto}.
     *
     * @param texto  contenido del QR.
     * @param tamano ancho y alto en píxeles; se ajusta al rango [{@value #TAMANO_MINIMO}, {@value #TAMANO_MAXIMO}].
     * @return bytes del PNG.
     */
    public byte[] generarPng(String texto, int tamano) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El contenido del código QR es obligatorio.");
        }
        int lado = Math.max(TAMANO_MINIMO, Math.min(TAMANO_MAXIMO, tamano));

        Map<EncodeHintType, Object> opciones = new EnumMap<>(EncodeHintType.class);
        opciones.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        opciones.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        opciones.put(EncodeHintType.MARGIN, 2);

        BitMatrix matriz;
        try {
            matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, lado, lado, opciones);
        } catch (WriterException ex) {
            throw new IllegalStateException("No se pudo generar el código QR: " + ex.getMessage(), ex);
        }

        int ancho = matriz.getWidth();
        int alto = matriz.getHeight();
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                imagen.setRGB(x, y, matriz.get(x, y) ? NEGRO : BLANCO);
            }
        }

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo escribir la imagen del código QR.", ex);
        }
    }

    /** Genera el PNG con el tamaño por defecto. */
    public byte[] generarPng(String texto) {
        return generarPng(texto, TAMANO_POR_DEFECTO);
    }
}
