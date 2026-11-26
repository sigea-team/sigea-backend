package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.model.TipoCertificado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link GeneradorPdfCertificado} (HU-26, Criterios 1 y 3).
 */
class GeneradorPdfCertificadoTest {

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    private final GeneradorPdfCertificado generador = new GeneradorPdfCertificado();

    private static GeneradorPdfCertificado.DatosCertificado datos(TipoCertificado tipo, String nombre, String evento,
                                                                 String detalle) {
        return new GeneradorPdfCertificado.DatosCertificado(tipo, nombre, "CC", "1090123456", evento,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12), detalle,
                LocalDateTime.of(2026, 11, 24, 18, 30), "CERT-3F9A1C07B2D44E5A9C61B0D2",
                "https://sigea.ufps.edu.co/api/v1/publico/certificados/CERT-3F9A1C07B2D44E5A9C61B0D2");
    }

    @Test
    @DisplayName("Criterio 1: el PDF lleva la universidad, el nombre, el documento, el rol, el evento, las fechas y el código")
    void contenido() throws Exception {
        byte[] pdf = generador.generar(datos(TipoCertificado.asistente, "Ana María Pérez Núñez",
                "Semana de Ingeniería de Sistemas", "Asistió a 4 de las 5 actividades en las que se inscribió (80 %)."));

        String inicio = new String(pdf, 0, 8, StandardCharsets.US_ASCII);
        assertEquals("%PDF-1.4", inicio);
        String completo = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(completo.endsWith("%%EOF\n"));
        assertTrue(completo.contains("/BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding"));

        String texto = textoDeLaPagina(pdf);
        assertTrue(texto.contains("LA UNIVERSIDAD FRANCISCO DE PAULA SANTANDER"));
        assertTrue(texto.contains("CERTIFICA QUE"));
        assertTrue(texto.contains("ANA MARÍA PÉREZ NÚÑEZ"), texto);
        assertTrue(texto.contains("identificado(a) con CC 1090123456"));
        assertTrue(texto.contains("asistió al evento"));
        assertTrue(texto.contains("Semana de Ingeniería de Sistemas"));
        assertTrue(texto.contains("realizado del 10 al 12 de noviembre de 2026"));
        assertTrue(texto.contains("(80 %)."));
        assertTrue(texto.contains("Expedido el 24 de noviembre de 2026"));
        assertTrue(texto.contains("Código de verificación: CERT-3F9A1C07B2D44E5A9C61B0D2"));
    }

    @Test
    @DisplayName("Lleva el logo vertical de la UFPS (el del login) sobre «La Universidad ... certifica que»")
    void logoInstitucional() throws Exception {
        assertTrue(GeneradorPdfCertificado.class.getResource(GeneradorPdfCertificado.RECURSO_LOGO) != null);
        byte[] pdf = generador.generar(datos(TipoCertificado.asistente, "Ana", "Congreso", null));
        String s = new String(pdf, StandardCharsets.ISO_8859_1);

        assertTrue(s.contains("/XObject << /Logo 8 0 R >>"));
        assertTrue(s.contains("/SMask 9 0 R"));
        String texto = textoDeLaPagina(pdf);
        assertTrue(texto.contains("LA UNIVERSIDAD FRANCISCO DE PAULA SANTANDER"));
        assertFalse(texto.contains("SIGEA · Sistema de gestión de eventos académicos"));
    }

    @Test
    @DisplayName("La tabla de referencias cruzadas apunta al inicio de cada objeto")
    void tablaDeReferencias() {
        byte[] pdf = generador.generar(datos(TipoCertificado.ponente, "Darío", "Congreso", null));
        String s = new String(pdf, StandardCharsets.ISO_8859_1);

        int inicioXref = Integer.parseInt(s.substring(s.lastIndexOf("startxref\n") + 10, s.lastIndexOf("\n%%EOF")).trim());
        assertTrue(s.startsWith("xref\n0 ", inicioXref));
        int declarados = Integer.parseInt(s.substring(inicioXref + 7, s.indexOf('\n', inicioXref + 7)));
        Matcher entradas = Pattern.compile("(\\d{10}) 00000 n ").matcher(s.substring(inicioXref));
        int objeto = 1;
        while (entradas.find()) {
            int posicion = Integer.parseInt(entradas.group(1));
            assertTrue(s.startsWith(objeto + " 0 obj\n", posicion), "objeto " + objeto);
            objeto++;
        }
        assertEquals(declarados, objeto);
        assertTrue(s.contains("/Size " + declarados + " "));
    }

    @Test
    @DisplayName("Criterio 3: los mismos datos producen exactamente el mismo PDF")
    void determinista() {
        var d = datos(TipoCertificado.conferencista, "Elena", "Congreso", "A cargo de la actividad «Apertura».");
        assertArrayEquals(generador.generar(d), generador.generar(d));
    }

    @Test
    @DisplayName("La frase de participación cambia según el tipo de certificado")
    void segunElTipo() throws Exception {
        String ponente = textoDeLaPagina(generador.generar(datos(TipoCertificado.ponente, "Darío", "Congreso", null)));
        assertTrue(ponente.contains("participó como ponente en el evento"));
        assertFalse(ponente.contains("asistió al evento"));

        String conferencista = textoDeLaPagina(generador.generar(
                datos(TipoCertificado.conferencista, "Elena", "Congreso", null)));
        assertTrue(conferencista.contains("participó como conferencista en el evento"));
    }

    @Test
    @DisplayName("Escapa paréntesis y barras, y reemplaza los caracteres fuera de WinAnsi")
    void escapes() throws Exception {
        byte[] pdf = generador.generar(datos(TipoCertificado.asistente, "Ana (la de \\ sistemas) 漢",
                "Evento", null));
        String contenido = contenidoDescomprimido(pdf);
        assertTrue(contenido.contains("\\(LA DE \\\\ SISTEMAS\\) ?"), contenido);
    }

    @Test
    @DisplayName("Los textos largos se parten en líneas que caben en la página y se recortan con «…»")
    void textosLargos() {
        String largo = "Congreso internacional de ingeniería de sistemas, computación, telecomunicaciones y "
                + "ciencia de datos aplicada a la transformación digital de las regiones fronterizas de Colombia";
        List<String> lineas = GeneradorPdfCertificado.partir(largo, GeneradorPdfCertificado.Fuente.NEGRITA, 18, 632, 2);
        assertEquals(2, lineas.size());
        for (String linea : lineas) {
            assertTrue(GeneradorPdfCertificado.ancho(linea, GeneradorPdfCertificado.Fuente.NEGRITA, 18) <= 632, linea);
        }
        assertTrue(lineas.get(1).endsWith("…"));

        List<String> corto = GeneradorPdfCertificado.partir("Congreso", GeneradorPdfCertificado.Fuente.NORMAL, 12, 632, 3);
        assertEquals(List.of("Congreso"), corto);

        byte[] pdf = generador.generar(datos(TipoCertificado.asistente, "Nombre ".repeat(40), largo, largo + " " + largo));
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("Ancho de texto con las métricas de Helvetica")
    void ancho() {
        // «A» = 667 y «a» = 556 milésimas en Helvetica; «A» = 722 en negrita
        assertEquals(6.67f, GeneradorPdfCertificado.ancho("A", GeneradorPdfCertificado.Fuente.NORMAL, 10), 0.001f);
        assertEquals(5.56f, GeneradorPdfCertificado.ancho("á", GeneradorPdfCertificado.Fuente.NORMAL, 10), 0.001f);
        assertEquals(7.22f, GeneradorPdfCertificado.ancho("A", GeneradorPdfCertificado.Fuente.NEGRITA, 10), 0.001f);
    }

    @Test
    @DisplayName("Rangos de fechas en español")
    void fechas() {
        assertEquals("realizado el 10 de noviembre de 2026",
                GeneradorPdfCertificado.rangoFechas(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10)));
        assertEquals("realizado del 30 de noviembre al 2 de diciembre de 2026",
                GeneradorPdfCertificado.rangoFechas(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 2)));
        assertEquals("realizado del 30 de diciembre de 2026 al 2 de enero de 2027",
                GeneradorPdfCertificado.rangoFechas(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2)));
    }

    @Test
    @DisplayName("Plantilla: incrusta el fondo (JPEG tal cual) y las firmas (PNG con máscara de transparencia)")
    void conFondoYFirmas() throws Exception {
        byte[] fondo = new ProcesadorImagenesCertificado().normalizarFondo(imagen(2200, 1700, java.awt.Color.LIGHT_GRAY, "png"));
        byte[] firma = new ProcesadorImagenesCertificado().normalizarFirma(firmaConHuecos());
        var d = new GeneradorPdfCertificado.DatosCertificado(TipoCertificado.asistente, "Ana", "CC", "1", "Congreso",
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12), "Detalle", LocalDateTime.of(2026, 11, 24, 18, 30),
                "CERT-X", null, fondo, List.of(
                        new GeneradorPdfCertificado.Firma("Judith Rodríguez", "Directora del programa", firma),
                        new GeneradorPdfCertificado.Firma("Carlos Pérez", "Decano", firma),
                        new GeneradorPdfCertificado.Firma("Sin imagen", "Coordinador", null)));

        byte[] pdf = generador.generar(d);
        String s = new String(pdf, StandardCharsets.ISO_8859_1);

        assertTrue(s.contains("/XObject << /Fondo 8 0 R /Logo 9 0 R /Firma1 11 0 R /Firma2 13 0 R >>"), s.substring(0, 600));
        assertTrue(s.contains("/Filter /DCTDecode"));
        assertTrue(s.contains("/Width 2200 /Height 1700"));
        assertTrue(s.contains("/SMask 10 0 R"));
        assertTrue(s.contains("/SMask 12 0 R"));
        assertTrue(s.contains("xref\n0 15\n"));
        String texto = textoDeLaPagina(pdf);
        assertTrue(texto.contains("Judith Rodríguez") && texto.contains("Directora del programa"));
        assertTrue(texto.contains("Sin imagen") && texto.contains("Coordinador"));
        // Con fondo también van el logo y el encabezado de la universidad.
        assertTrue(texto.contains("LA UNIVERSIDAD FRANCISCO DE PAULA SANTANDER"));
        assertArrayEquals(pdf, generador.generar(d));
    }

    @Test
    @DisplayName("Plantilla: más de 4 firmas → se imprimen las 4 primeras; un fondo que no es JPEG se ignora")
    void limites() throws Exception {
        var cinco = java.util.stream.IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new GeneradorPdfCertificado.Firma("Firmante " + i, "Cargo", null)).toList();
        var d = new GeneradorPdfCertificado.DatosCertificado(TipoCertificado.ponente, "Ana", null, null, "Congreso",
                null, null, null, null, "CERT-X", null, "no es jpeg".getBytes(), cinco);
        String texto = textoDeLaPagina(generador.generar(d));
        assertTrue(texto.contains("Firmante 4"));
        assertFalse(texto.contains("Firmante 5"));
        // Fondo inválido → diseño predeterminado, con el logo institucional.
        assertTrue(new String(generador.generar(d), StandardCharsets.ISO_8859_1).contains("/XObject << /Logo 8 0 R"));
    }

    /** Trazo en forma de «L» sobre blanco: al normalizarlo queda con zonas transparentes (necesita máscara). */
    private static byte[] firmaConHuecos() throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(400, 120, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, 400, 120);
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(20, 10, 15, 100);
        g.fillRect(20, 95, 300, 15);
        g.dispose();
        java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", salida);
        return salida.toByteArray();
    }

    private static byte[] imagen(int w, int h, java.awt.Color color, String formato) throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(color);
        g.fillRect(w / 10, h / 3, w * 8 / 10, h / 3);
        g.dispose();
        java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, formato, salida);
        return salida.toByteArray();
    }

    @Test
    @DisplayName("Sin tipo de certificado no se genera")
    void sinTipo() {
        assertThrows(IllegalArgumentException.class, () -> generador.generar(null));
        assertEquals("participó como evaluador en el evento", GeneradorPdfCertificado.frasePrincipal(TipoCertificado.evaluador));
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Texto de los operadores Tj de la página (sin escapes), una línea por operador. */
    private static String textoDeLaPagina(byte[] pdf) throws Exception {
        String contenido = contenidoDescomprimido(pdf);
        StringBuilder texto = new StringBuilder();
        Matcher m = Pattern.compile("\\(((?:\\\\.|[^\\\\)])*)\\) Tj").matcher(contenido);
        while (m.find()) {
            texto.append(m.group(1).replaceAll("\\\\(.)", "$1")).append('\n');
        }
        return texto.toString();
    }

    private static String contenidoDescomprimido(byte[] pdf) throws Exception {
        String s = new String(pdf, StandardCharsets.ISO_8859_1);
        int inicio = s.indexOf("stream\n") + "stream\n".length();
        int fin = s.indexOf("\nendstream");
        Inflater inflater = new Inflater();
        inflater.setInput(pdf, inicio, fin - inicio);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        while (!inflater.finished()) {
            int n = inflater.inflate(buffer);
            if (n == 0 && inflater.needsInput()) {
                break;
            }
            salida.write(buffer, 0, n);
        }
        inflater.end();
        return salida.toString(WIN_ANSI);
    }
}
