package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.model.TipoCertificado;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;

/**
 * Genera el PDF de un certificado de participación (HU-26, RF47): una página carta horizontal con el rol, el nombre
 * y documento de la persona, el evento, sus fechas, el detalle de la participación, la fecha de expedición y el
 * código de verificación. El diseño predeterminado lleva el logo institucional ({@value #RECURSO_LOGO}). Con la plantilla
 * del evento, la imagen de fondo cubre la página (en lugar del marco y el
 * encabezado institucional) y de 1 a {@value #MAXIMO_FIRMAS} firmas se reparten en el pie.
 * <p>
 * Escribe el PDF directamente (sin dependencias): usa las fuentes estándar Helvetica y Helvetica-Bold con
 * codificación WinAnsi (tildes, eñes, comillas angulares), por eso no incrusta fuentes. Los caracteres fuera de
 * Windows-1252 se reemplazan por {@code ?}. La salida es determinista: los mismos datos producen los mismos bytes,
 * así que un certificado reconstruido es idéntico al original (Criterio 3).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Component
public class GeneradorPdfCertificado {

    static final float ANCHO = 792f;
    static final float ALTO = 612f;
    private static final float MARGEN_TEXTO = 80f;
    private static final float ANCHO_TEXTO = ANCHO - 2 * MARGEN_TEXTO;
    /** Máximo de firmas en el pie. */
    public static final int MAXIMO_FIRMAS = 4;
    private static final float Y_LINEA_FIRMA = 150f;
    private static final float ALTO_FIRMA = 52f;
    /** Recurso con el logo institucional del diseño predeterminado (logo vertical del login del frontend). */
    static final String RECURSO_LOGO = "/certificados/logo-ufps.png";
    private static final float Y_TOPE_LOGO = 574f;
    private static final float ALTO_LOGO = 62f;

    /** Con fondo, línea base del título «CERTIFICADO»: deja libre la franja superior para el encabezado del fondo. */
    static final float Y_INICIO_CON_FONDO = 455f;

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");
    private static final String COLOR_INSTITUCIONAL = "0.667 0.098 0.086";
    private static final String COLOR_TEXTO = "0.15 0.15 0.15";
    private static final String COLOR_SECUNDARIO = "0.38 0.38 0.38";

    private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
            "septiembre", "octubre", "noviembre", "diciembre"};

    /** Anchos de Helvetica (milésimas de em) para los códigos WinAnsi 32 a 255. */
    private static final int[] ANCHOS_HELVETICA = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556, 556, 556,
            556, 556, 556, 556, 278, 278, 584, 584, 584, 556, 1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667,
            556, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556, 333, 556,
            556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556, 556, 556, 333, 500, 278, 556, 500, 722,
            500, 500, 500, 334, 260, 334, 584, 350, 556, 350, 222, 556, 333, 1000, 556, 556, 333, 1000, 667, 333, 1000,
            350, 611, 350, 350, 222, 222, 333, 333, 350, 556, 1000, 333, 1000, 500, 333, 944, 350, 500, 667, 278, 333, 556,
            556, 556, 556, 260, 556, 333, 737, 370, 556, 584, 333, 737, 333, 400, 584, 333, 333, 333, 556, 537, 278, 333,
            333, 365, 556, 834, 834, 834, 611, 667, 667, 667, 667, 667, 667, 1000, 722, 667, 667, 667, 667, 278, 278, 278,
            278, 722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611, 556, 556, 556, 556, 556,
            556, 889, 500, 556, 556, 556, 556, 278, 278, 278, 278, 556, 556, 556, 556, 556, 556, 556, 584, 611, 556, 556,
            556, 556, 500, 556, 500};

    /** Anchos de Helvetica-Bold (milésimas de em) para los códigos WinAnsi 32 a 255. */
    private static final int[] ANCHOS_HELVETICA_BOLD = {
            278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556, 556, 556,
            556, 556, 556, 556, 333, 333, 584, 584, 584, 611, 975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722,
            611, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556, 333, 556,
            611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611, 611, 611, 389, 556, 333, 611, 556, 778,
            556, 556, 500, 389, 280, 389, 584, 350, 556, 350, 278, 556, 500, 1000, 556, 556, 333, 1000, 667, 333, 1000,
            350, 611, 350, 350, 278, 278, 500, 500, 350, 556, 1000, 333, 1000, 556, 333, 944, 350, 500, 667, 278, 333, 556,
            556, 556, 556, 280, 556, 333, 737, 370, 556, 584, 333, 737, 333, 400, 584, 333, 333, 333, 611, 556, 278, 333,
            333, 365, 556, 834, 834, 834, 611, 722, 722, 722, 722, 722, 722, 1000, 722, 667, 667, 667, 667, 278, 278, 278,
            278, 722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611, 556, 556, 556, 556, 556,
            556, 889, 556, 556, 556, 556, 556, 278, 278, 278, 278, 611, 611, 611, 611, 611, 611, 611, 584, 611, 611, 611,
            611, 611, 556, 611, 556};

    /** Fuentes de la página: F1 = Helvetica, F2 = Helvetica-Bold. */
    enum Fuente {
        NORMAL("F1", ANCHOS_HELVETICA),
        NEGRITA("F2", ANCHOS_HELVETICA_BOLD);

        private final String recurso;
        private final int[] anchos;

        Fuente(String recurso, int[] anchos) {
            this.recurso = recurso;
            this.anchos = anchos;
        }
    }

    /**
     * Datos impresos en el certificado.
     *
     * @param tipo                rol certificado.
     * @param nombreParticipante  nombre completo.
     * @param tipoDocumento       tipo de documento (CC, TI, CE...), puede ser null.
     * @param numeroDocumento     número de documento, puede ser null.
     * @param nombreEvento        nombre del evento.
     * @param fechaInicioEvento   fecha de inicio del evento.
     * @param fechaFinEvento      fecha de fin del evento.
     * @param detalle             detalle de la participación (puede ser null).
     * @param fechaGeneracion     fecha de expedición.
     * @param codigoVerificacion  código para comprobar la autenticidad.
     * @param urlVerificacion     dirección donde se comprueba el código (puede ser null).
     * @param fondoJpeg           imagen de fondo del evento en JPEG (carta horizontal), o null para el diseño
     *                            predeterminado.
     * @param firmas              firmantes del evento, en orden (0 a {@value #MAXIMO_FIRMAS}).
     */
    public record DatosCertificado(
            TipoCertificado tipo,
            String nombreParticipante,
            String tipoDocumento,
            String numeroDocumento,
            String nombreEvento,
            LocalDate fechaInicioEvento,
            LocalDate fechaFinEvento,
            String detalle,
            LocalDateTime fechaGeneracion,
            String codigoVerificacion,
            String urlVerificacion,
            byte[] fondoJpeg,
            List<Firma> firmas
    ) {
        public DatosCertificado {
            firmas = firmas == null ? List.of() : List.copyOf(firmas);
        }

        /** Sin plantilla: diseño predeterminado y sin firmas. */
        public DatosCertificado(TipoCertificado tipo, String nombreParticipante, String tipoDocumento,
                                String numeroDocumento, String nombreEvento, LocalDate fechaInicioEvento,
                                LocalDate fechaFinEvento, String detalle, LocalDateTime fechaGeneracion,
                                String codigoVerificacion, String urlVerificacion) {
            this(tipo, nombreParticipante, tipoDocumento, numeroDocumento, nombreEvento, fechaInicioEvento,
                    fechaFinEvento, detalle, fechaGeneracion, codigoVerificacion, urlVerificacion, null, List.of());
        }
    }

    /**
     * Firma impresa en el certificado.
     *
     * @param nombre    nombre del firmante.
     * @param cargo     cargo del firmante.
     * @param imagenPng imagen de la firma en PNG (con o sin transparencia); null para dejar solo la línea.
     */
    public record Firma(String nombre, String cargo, byte[] imagenPng) {
    }

    /**
     * Genera el PDF.
     *
     * @param datos datos del certificado.
     * @return bytes del documento PDF.
     */
    public byte[] generar(DatosCertificado datos) {
        if (datos == null || datos.tipo() == null) {
            throw new IllegalArgumentException("Faltan los datos del certificado.");
        }
        List<Imagen> imagenes = new ArrayList<>();
        Imagen fondo = datos.fondoJpeg() == null ? null : Imagen.desdeJpeg("Fondo", datos.fondoJpeg());
        if (fondo != null) {
            imagenes.add(fondo);
        }
        List<Firma> firmas = datos.firmas().size() > MAXIMO_FIRMAS
                ? datos.firmas().subList(0, MAXIMO_FIRMAS) : datos.firmas();
        boolean conFirmas = !firmas.isEmpty();

        Contenido c = new Contenido();
        float y;
        if (fondo != null) {
            // El fondo cubre la página; trae el marco y la identidad institucional, así que no se dibujan. La franja
            // superior (de Y_INICIO_CON_FONDO hacia arriba, unos 5,4 cm) queda libre para el logo o el encabezado del fondo.
            c.imagen(fondo.nombre, 0, 0, ANCHO, ALTO);
            y = Y_INICIO_CON_FONDO;
        } else {
            dibujarMarco(c);
            Imagen logo = logoInstitucional();
            if (logo != null) {
                // Logo vertical de la UFPS (el mismo del login); ya trae el nombre de la universidad.
                imagenes.add(logo);
                float w = logo.ancho * ALTO_LOGO / logo.alto;
                c.imagen(logo.nombre, (ANCHO - w) / 2, Y_TOPE_LOGO - ALTO_LOGO, w, ALTO_LOGO);
                y = Y_TOPE_LOGO - ALTO_LOGO - 12;
            } else {
                y = conFirmas ? 548f : 540f;
                c.textoCentrado("UNIVERSIDAD FRANCISCO DE PAULA SANTANDER", Fuente.NEGRITA, 13, y, COLOR_TEXTO);
                y -= conFirmas ? 15 : 17;
            }
            c.textoCentrado("SIGEA · Sistema de gestión de eventos académicos", Fuente.NORMAL, 10, y, COLOR_SECUNDARIO);
            y = logo != null ? (conFirmas ? Y_INICIO_CON_FONDO : 458f) : y - (conFirmas ? 50 : 62);
        }

        // Con firmas el pie ocupa más espacio: el cuerpo se compacta y limita el evento y el detalle a 2 líneas.
        c.textoCentrado("CERTIFICADO", Fuente.NEGRITA, 36, y, COLOR_INSTITUCIONAL);
        y -= conFirmas ? 23 : 26;
        c.textoCentrado(subtitulo(datos.tipo()), Fuente.NORMAL, 14, y, COLOR_INSTITUCIONAL);
        y -= conFirmas ? 12 : 14;
        c.linea(ANCHO / 2 - 70, y, ANCHO / 2 + 70, y, 1.2f, COLOR_INSTITUCIONAL);

        y -= conFirmas ? 27 : 34;
        c.textoCentrado("Se certifica que", Fuente.NORMAL, 13, y, COLOR_TEXTO);

        y -= conFirmas ? 31 : 36;
        String nombre = texto(datos.nombreParticipante(), "Participante").toUpperCase(Locale.of("es", "CO"));
        float tamanoNombre = ajustarTamano(nombre, Fuente.NEGRITA, 26, 15, ANCHO_TEXTO);
        if (ancho(nombre, Fuente.NEGRITA, tamanoNombre) > ANCHO_TEXTO) {
            nombre = recortar(nombre, Fuente.NEGRITA, tamanoNombre, ANCHO_TEXTO);
        }
        c.textoCentrado(nombre, Fuente.NEGRITA, tamanoNombre, y, COLOR_TEXTO);

        String documento = documento(datos.tipoDocumento(), datos.numeroDocumento());
        if (documento != null) {
            y -= conFirmas ? 19 : 22;
            c.textoCentrado("identificado(a) con " + documento, Fuente.NORMAL, 12, y, COLOR_SECUNDARIO);
        }

        y -= conFirmas ? 27 : 32;
        c.textoCentrado("participó como " + datos.tipo().getRolEnTexto() + " en el evento", Fuente.NORMAL, 13, y,
                COLOR_TEXTO);

        y -= conFirmas ? 23 : 26;
        float pasoEvento = conFirmas ? 20 : 22;
        for (String linea : partir(texto(datos.nombreEvento(), "Evento"), Fuente.NEGRITA, 18, ANCHO_TEXTO,
                conFirmas ? 2 : 3)) {
            c.textoCentrado(linea, Fuente.NEGRITA, 18, y, COLOR_INSTITUCIONAL);
            y -= pasoEvento;
        }

        String fechas = rangoFechas(datos.fechaInicioEvento(), datos.fechaFinEvento());
        if (fechas != null) {
            y -= 2;
            c.textoCentrado(fechas, Fuente.NORMAL, 12, y, COLOR_TEXTO);
            y -= conFirmas ? 16 : 18;
        }

        String detalle = datos.detalle() == null ? null : datos.detalle().trim();
        if (detalle != null && !detalle.isEmpty()) {
            y -= conFirmas ? 6 : 8;
            for (String linea : partir(detalle, Fuente.NORMAL, conFirmas ? 10 : 11, ANCHO_TEXTO, conFirmas ? 2 : 4)) {
                c.textoCentrado(linea, Fuente.NORMAL, conFirmas ? 10 : 11, y, COLOR_SECUNDARIO);
                y -= conFirmas ? 13 : 15;
            }
        }

        if (conFirmas) {
            dibujarFirmas(c, firmas, imagenes);
        }

        // Pie: fecha de expedición y código de verificación.
        c.linea(70, 104, ANCHO - 70, 104, 0.5f, COLOR_SECUNDARIO);
        String expedicion = datos.fechaGeneracion() == null ? null
                : "Expedido el " + fechaLarga(datos.fechaGeneracion().toLocalDate());
        if (expedicion != null) {
            c.texto(expedicion, Fuente.NORMAL, 10, 70, 86, COLOR_TEXTO);
        }
        String codigo = "Código de verificación: " + texto(datos.codigoVerificacion(), "-");
        c.texto(codigo, Fuente.NEGRITA, 10, ANCHO - 70 - ancho(codigo, Fuente.NEGRITA, 10), 86, COLOR_TEXTO);
        if (datos.urlVerificacion() != null && !datos.urlVerificacion().isBlank()) {
            String url = "Verifique la autenticidad de este certificado en " + datos.urlVerificacion().trim();
            float tamanoUrl = ajustarTamano(url, Fuente.NORMAL, 8, 6, ANCHO - 140);
            c.textoCentrado(recortar(url, Fuente.NORMAL, tamanoUrl, ANCHO - 140), Fuente.NORMAL, tamanoUrl, 66,
                    COLOR_SECUNDARIO);
        }

        return armarDocumento(c.bytes(), datos, imagenes);
    }

    /**
     * Firmas en el pie, repartidas en columnas iguales según cuántas haya: imagen de la firma sobre la línea, nombre y
     * cargo debajo.
     */
    private static void dibujarFirmas(Contenido c, List<Firma> firmas, List<Imagen> imagenes) {
        int n = firmas.size();
        float anchoColumna = Math.min(200f, ANCHO_TEXTO / n);
        float inicio = (ANCHO - anchoColumna * n) / 2;
        float anchoLinea = anchoColumna - 24;
        for (int i = 0; i < n; i++) {
            Firma firma = firmas.get(i);
            float centro = inicio + anchoColumna * i + anchoColumna / 2;
            Imagen imagen = firma.imagenPng() == null ? null : Imagen.desdePng("Firma" + (i + 1), firma.imagenPng());
            if (imagen != null) {
                imagenes.add(imagen);
                // Cabe en una caja del ancho de la línea y ALTO_FIRMA de alto, apoyada sobre la línea.
                float escala = Math.min(anchoLinea / imagen.ancho, ALTO_FIRMA / imagen.alto);
                float w = imagen.ancho * escala;
                float h = imagen.alto * escala;
                c.imagen(imagen.nombre, centro - w / 2, Y_LINEA_FIRMA + 1, w, h);
            }
            c.linea(centro - anchoLinea / 2, Y_LINEA_FIRMA, centro + anchoLinea / 2, Y_LINEA_FIRMA, 0.7f, COLOR_TEXTO);
            String nombre = texto(firma.nombre(), "");
            float tNombre = ajustarTamano(nombre, Fuente.NEGRITA, 10, 7, anchoLinea);
            c.textoCentradoEn(recortar(nombre, Fuente.NEGRITA, tNombre, anchoLinea), Fuente.NEGRITA, tNombre, centro,
                    Y_LINEA_FIRMA - 13, COLOR_TEXTO);
            String cargo = texto(firma.cargo(), "");
            float tCargo = ajustarTamano(cargo, Fuente.NORMAL, 9, 6.5f, anchoLinea);
            c.textoCentradoEn(recortar(cargo, Fuente.NORMAL, tCargo, anchoLinea), Fuente.NORMAL, tCargo, centro,
                    Y_LINEA_FIRMA - 24, COLOR_SECUNDARIO);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Textos
    // ──────────────────────────────────────────────────────────────────────────────

    static String subtitulo(TipoCertificado tipo) {
        return switch (tipo) {
            case asistente -> "DE ASISTENCIA";
            case ponente -> "DE PARTICIPACIÓN COMO PONENTE";
            case conferencista -> "DE PARTICIPACIÓN COMO CONFERENCISTA";
            case evaluador -> "DE PARTICIPACIÓN COMO EVALUADOR";
            case organizador -> "DE PARTICIPACIÓN COMO ORGANIZADOR";
        };
    }

    private static String documento(String tipo, String numero) {
        if (numero == null || numero.isBlank()) {
            return null;
        }
        String t = tipo == null || tipo.isBlank() ? "documento" : tipo.trim().toUpperCase(Locale.ROOT);
        return t + " " + numero.trim();
    }

    /** «el 10 de noviembre de 2026», «del 10 al 12 de noviembre de 2026», «del 30 de noviembre al 2 de diciembre de 2026». */
    static String rangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null && fin == null) {
            return null;
        }
        if (inicio == null || fin == null || inicio.equals(fin)) {
            return "realizado el " + fechaLarga(inicio != null ? inicio : fin);
        }
        if (inicio.getYear() != fin.getYear()) {
            return "realizado del " + fechaLarga(inicio) + " al " + fechaLarga(fin);
        }
        if (inicio.getMonthValue() != fin.getMonthValue()) {
            return "realizado del " + inicio.getDayOfMonth() + " de " + MESES[inicio.getMonthValue() - 1] + " al "
                    + fechaLarga(fin);
        }
        return "realizado del " + inicio.getDayOfMonth() + " al " + fechaLarga(fin);
    }

    static String fechaLarga(LocalDate fecha) {
        return fecha.getDayOfMonth() + " de " + MESES[fecha.getMonthValue() - 1] + " de " + fecha.getYear();
    }

    private static String texto(String valor, String porDefecto) {
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        return valor.trim().replaceAll("\\s+", " ");
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Medidas
    // ──────────────────────────────────────────────────────────────────────────────

    /** Ancho del texto en puntos. */
    static float ancho(String texto, Fuente fuente, float tamano) {
        int total = 0;
        for (byte b : codificar(texto)) {
            int codigo = b & 0xFF;
            total += codigo >= 32 ? fuente.anchos[codigo - 32] : 0;
        }
        return total * tamano / 1000f;
    }

    private static float ajustarTamano(String texto, Fuente fuente, float maximo, float minimo, float anchoDisponible) {
        float tamano = maximo;
        while (tamano > minimo && ancho(texto, fuente, tamano) > anchoDisponible) {
            tamano -= 0.5f;
        }
        return tamano;
    }

    /** Parte el texto en líneas que caben en el ancho; si sobran líneas, la última termina en «…». */
    static List<String> partir(String texto, Fuente fuente, float tamano, float anchoDisponible, int maximoLineas) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        String[] palabras = texto.trim().split("\\s+");
        int i = 0;
        while (i < palabras.length) {
            String palabra = palabras[i];
            String candidata = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (ancho(candidata, fuente, tamano) <= anchoDisponible) {
                actual.setLength(0);
                actual.append(candidata);
                i++;
                continue;
            }
            if (actual.isEmpty()) {
                // Una sola palabra más ancha que la línea: se corta.
                lineas.add(recortar(palabra, fuente, tamano, anchoDisponible));
                i++;
            } else {
                lineas.add(actual.toString());
                actual.setLength(0);
            }
            if (lineas.size() == maximoLineas) {
                break;
            }
        }
        if (lineas.size() < maximoLineas && !actual.isEmpty()) {
            lineas.add(actual.toString());
        } else if (i < palabras.length || !actual.isEmpty()) {
            int ultima = lineas.size() - 1;
            lineas.set(ultima, recortar(lineas.get(ultima) + " …", fuente, tamano, anchoDisponible, true));
        }
        return lineas;
    }

    private static String recortar(String texto, Fuente fuente, float tamano, float anchoDisponible) {
        return recortar(texto, fuente, tamano, anchoDisponible, false);
    }

    private static String recortar(String texto, Fuente fuente, float tamano, float anchoDisponible, boolean forzar) {
        if (!forzar && ancho(texto, fuente, tamano) <= anchoDisponible) {
            return texto;
        }
        String base = texto.endsWith(" …") ? texto.substring(0, texto.length() - 2) : texto;
        if (forzar && ancho(base + " …", fuente, tamano) <= anchoDisponible) {
            return base + " …";
        }
        int fin = base.length();
        while (fin > 0 && ancho(base.substring(0, fin).trim() + "…", fuente, tamano) > anchoDisponible) {
            fin--;
        }
        return base.substring(0, fin).trim() + "…";
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // PDF
    // ──────────────────────────────────────────────────────────────────────────────

    private static void dibujarMarco(Contenido c) {
        c.rectangulo(22, 22, ANCHO - 44, ALTO - 44, 3f, COLOR_INSTITUCIONAL);
        c.rectangulo(32, 32, ANCHO - 64, ALTO - 64, 0.8f, COLOR_INSTITUCIONAL);
    }

    private static byte[] armarDocumento(byte[] contenido, DatosCertificado datos, List<Imagen> imagenes) {
        byte[] comprimido = comprimir(contenido);
        String titulo = "Certificado de " + datos.tipo().getRolEnTexto() + " - " + texto(datos.nombreEvento(), "Evento");
        String fecha = datos.fechaGeneracion() == null ? null
                : datos.fechaGeneracion().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // Objetos 1-7 fijos; desde el 8, cada imagen (y su máscara de transparencia, si tiene).
        int siguiente = 8;
        StringBuilder recursosImagenes = new StringBuilder();
        List<Integer> numeros = new ArrayList<>();
        for (Imagen imagen : imagenes) {
            numeros.add(siguiente);
            recursosImagenes.append(" /").append(imagen.nombre).append(' ').append(siguiente).append(" 0 R");
            siguiente += imagen.mascara != null ? 2 : 1;
        }
        int cantidad = siguiente - 1;

        Escritor pdf = new Escritor();
        pdf.ascii("%PDF-1.4\n");
        pdf.bytes(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        pdf.objeto(1, "<< /Type /Catalog /Pages 2 0 R >>");
        pdf.objeto(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        pdf.objeto(3, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + numero(ANCHO) + " " + numero(ALTO) + "] "
                + "/Resources << /Font << /F1 4 0 R /F2 5 0 R >>"
                + (imagenes.isEmpty() ? "" : " /XObject <<" + recursosImagenes + " >>")
                + " >> /Contents 6 0 R >>");
        pdf.objeto(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        pdf.objeto(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        pdf.flujo(6, comprimido);
        pdf.objeto(7, "<< /Title " + textoUnicode(titulo) + " /Author (SIGEA - UFPS) /Producer (SIGEA)"
                + (fecha != null ? " /CreationDate (D:" + fecha + ")" : "") + " >>");
        for (int k = 0; k < imagenes.size(); k++) {
            Imagen imagen = imagenes.get(k);
            int num = numeros.get(k);
            String base = "/Type /XObject /Subtype /Image /Width " + imagen.ancho + " /Height " + imagen.alto
                    + " /ColorSpace /" + imagen.espacioColor + " /BitsPerComponent 8";
            pdf.flujo(num, imagen.datos, base + " /Filter /" + imagen.filtro
                    + (imagen.mascara != null ? " /SMask " + (num + 1) + " 0 R" : ""));
            if (imagen.mascara != null) {
                pdf.flujo(num + 1, imagen.mascara, "/Type /XObject /Subtype /Image /Width " + imagen.ancho
                        + " /Height " + imagen.alto + " /ColorSpace /DeviceGray /BitsPerComponent 8 /Filter /FlateDecode");
            }
        }
        return pdf.cerrar(cantidad, 1, 7);
    }

    /** Logo leído una sola vez del classpath; null si el recurso no está (se usa el encabezado en texto). */
    private static final class LogoCache {
        private static final byte[] PNG = leerRecurso();

        private static byte[] leerRecurso() {
            try (java.io.InputStream in = GeneradorPdfCertificado.class.getResourceAsStream(RECURSO_LOGO)) {
                return in == null ? null : in.readAllBytes();
            } catch (java.io.IOException ex) {
                return null;
            }
        }
    }

    private static Imagen logoInstitucional() {
        return LogoCache.PNG == null ? null : Imagen.desdePng("Logo", LogoCache.PNG);
    }

    /** Imagen lista para incrustar: JPEG tal cual (DCTDecode) o píxeles comprimidos (FlateDecode) con máscara alfa. */
    static final class Imagen {
        final String nombre;
        final int ancho;
        final int alto;
        final String espacioColor;
        final String filtro;
        final byte[] datos;
        final byte[] mascara;

        private Imagen(String nombre, int ancho, int alto, String espacioColor, String filtro, byte[] datos,
                       byte[] mascara) {
            this.nombre = nombre;
            this.ancho = ancho;
            this.alto = alto;
            this.espacioColor = espacioColor;
            this.filtro = filtro;
            this.datos = datos;
            this.mascara = mascara;
        }

        /** JPEG de 1 o 3 componentes (las dimensiones salen del marcador SOF); null si no es un JPEG utilizable. */
        static Imagen desdeJpeg(String nombre, byte[] jpeg) {
            if (jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
                return null;
            }
            int i = 2;
            while (i + 9 < jpeg.length) {
                if ((jpeg[i] & 0xFF) != 0xFF) {
                    return null;
                }
                int marcador = jpeg[i + 1] & 0xFF;
                if (marcador == 0xFF) {
                    i++;
                    continue;
                }
                int largo = ((jpeg[i + 2] & 0xFF) << 8) | (jpeg[i + 3] & 0xFF);
                boolean sof = marcador >= 0xC0 && marcador <= 0xCF
                        && marcador != 0xC4 && marcador != 0xC8 && marcador != 0xCC;
                if (sof) {
                    int alto = ((jpeg[i + 5] & 0xFF) << 8) | (jpeg[i + 6] & 0xFF);
                    int ancho = ((jpeg[i + 7] & 0xFF) << 8) | (jpeg[i + 8] & 0xFF);
                    int componentes = jpeg[i + 9] & 0xFF;
                    if (ancho <= 0 || alto <= 0 || (componentes != 1 && componentes != 3)) {
                        return null;
                    }
                    return new Imagen(nombre, ancho, alto, componentes == 1 ? "DeviceGray" : "DeviceRGB",
                            "DCTDecode", jpeg, null);
                }
                i += 2 + largo;
            }
            return null;
        }

        /** PNG (u otro formato que lea ImageIO) con transparencia; null si no se puede leer. */
        static Imagen desdePng(String nombre, byte[] png) {
            java.awt.image.BufferedImage img;
            try {
                img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
            } catch (java.io.IOException | RuntimeException ex) {
                return null;
            }
            if (img == null) {
                return null;
            }
            int w = img.getWidth();
            int h = img.getHeight();
            byte[] rgb = new byte[w * h * 3];
            byte[] alfa = new byte[w * h];
            boolean transparente = false;
            int k = 0;
            for (int fila = 0; fila < h; fila++) {
                for (int col = 0; col < w; col++) {
                    int argb = img.getRGB(col, fila);
                    int a = (argb >>> 24) & 0xFF;
                    rgb[k * 3] = (byte) (argb >> 16);
                    rgb[k * 3 + 1] = (byte) (argb >> 8);
                    rgb[k * 3 + 2] = (byte) argb;
                    alfa[k] = (byte) a;
                    transparente |= a != 0xFF;
                    k++;
                }
            }
            return new Imagen(nombre, w, h, "DeviceRGB", "FlateDecode", comprimir(rgb),
                    transparente ? comprimir(alfa) : null);
        }
    }

    private static byte[] comprimir(byte[] datos) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(datos);
            deflater.finish();
            ByteArrayOutputStream salida = new ByteArrayOutputStream(datos.length);
            byte[] buffer = new byte[4096];
            while (!deflater.finished()) {
                int n = deflater.deflate(buffer);
                salida.write(buffer, 0, n);
            }
            return salida.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /** Texto en Windows-1252; lo que no se puede representar queda como {@code ?}. Quita caracteres de control. */
    static byte[] codificar(String texto) {
        CharsetEncoder encoder = WIN_ANSI.newEncoder();
        ByteArrayOutputStream salida = new ByteArrayOutputStream(texto.length());
        texto.codePoints().forEach(cp -> {
            if (Character.isISOControl(cp)) {
                return;
            }
            String caracter = new String(Character.toChars(cp));
            if (encoder.canEncode(caracter)) {
                byte[] b = caracter.getBytes(WIN_ANSI);
                salida.write(b, 0, b.length);
            } else {
                salida.write('?');
            }
        });
        return salida.toByteArray();
    }

    /** Cadena literal de PDF con los caracteres especiales escapados. */
    private static byte[] cadenaPdf(String texto) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write('(');
        for (byte b : codificar(texto)) {
            if (b == '(' || b == ')' || b == '\\') {
                salida.write('\\');
            }
            salida.write(b);
        }
        salida.write(')');
        return salida.toByteArray();
    }

    /** Cadena de texto de los metadatos en UTF-16BE (hexadecimal con BOM), para conservar las tildes. */
    private static String textoUnicode(String texto) {
        StringBuilder hex = new StringBuilder("<FEFF");
        for (byte b : texto.getBytes(StandardCharsets.UTF_16BE)) {
            hex.append(String.format("%02X", b & 0xFF));
        }
        return hex.append('>').toString();
    }

    private static String numero(float valor) {
        if (valor == Math.rint(valor)) {
            return Integer.toString((int) valor);
        }
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    /** Operadores de la página. */
    private static final class Contenido {
        private final ByteArrayOutputStream salida = new ByteArrayOutputStream();

        void textoCentrado(String texto, Fuente fuente, float tamano, float y, String color) {
            texto(texto, fuente, tamano, (ANCHO - ancho(texto, fuente, tamano)) / 2, y, color);
        }

        void textoCentradoEn(String texto, Fuente fuente, float tamano, float centro, float y, String color) {
            texto(texto, fuente, tamano, centro - ancho(texto, fuente, tamano) / 2, y, color);
        }

        void imagen(String nombre, float x, float y, float ancho, float alto) {
            ascii("q " + numero(ancho) + " 0 0 " + numero(alto) + " " + numero(x) + " " + numero(y) + " cm /" + nombre
                    + " Do Q\n");
        }

        void texto(String texto, Fuente fuente, float tamano, float x, float y, String color) {
            ascii(color + " rg BT /" + fuente.recurso + " " + numero(tamano) + " Tf 1 0 0 1 " + numero(x) + " "
                    + numero(y) + " Tm ");
            byte[] cadena = cadenaPdf(texto);
            salida.write(cadena, 0, cadena.length);
            ascii(" Tj ET\n");
        }

        void linea(float x1, float y1, float x2, float y2, float grosor, String color) {
            ascii(color + " RG " + numero(grosor) + " w " + numero(x1) + " " + numero(y1) + " m " + numero(x2) + " "
                    + numero(y2) + " l S\n");
        }

        void rectangulo(float x, float y, float ancho, float alto, float grosor, String color) {
            ascii(color + " RG " + numero(grosor) + " w " + numero(x) + " " + numero(y) + " " + numero(ancho) + " "
                    + numero(alto) + " re S\n");
        }

        private void ascii(String s) {
            byte[] b = s.getBytes(StandardCharsets.US_ASCII);
            salida.write(b, 0, b.length);
        }

        byte[] bytes() {
            return salida.toByteArray();
        }
    }

    /** Escribe los objetos y la tabla de referencias cruzadas. */
    private static final class Escritor {
        private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
        private final List<Integer> posiciones = new ArrayList<>();

        void ascii(String s) {
            bytes(s.getBytes(StandardCharsets.US_ASCII));
        }

        void bytes(byte[] b) {
            salida.write(b, 0, b.length);
        }

        void objeto(int numero, String cuerpo) {
            registrar(numero);
            ascii(numero + " 0 obj\n" + cuerpo + "\nendobj\n");
        }

        void flujo(int numero, byte[] datos) {
            flujo(numero, datos, "/Filter /FlateDecode");
        }

        void flujo(int numero, byte[] datos, String diccionario) {
            registrar(numero);
            ascii(numero + " 0 obj\n<< " + diccionario + " /Length " + datos.length + " >>\nstream\n");
            bytes(datos);
            ascii("\nendstream\nendobj\n");
        }

        private void registrar(int numero) {
            if (posiciones.size() != numero - 1) {
                throw new IllegalStateException("Los objetos del PDF deben escribirse en orden.");
            }
            posiciones.add(salida.size());
        }

        byte[] cerrar(int cantidad, int raiz, int info) {
            int inicioXref = salida.size();
            StringBuilder xref = new StringBuilder("xref\n0 ").append(cantidad + 1).append('\n');
            xref.append("0000000000 65535 f \n");
            for (int posicion : posiciones) {
                xref.append(String.format(Locale.ROOT, "%010d 00000 n \n", posicion));
            }
            xref.append("trailer\n<< /Size ").append(cantidad + 1).append(" /Root ").append(raiz).append(" 0 R /Info ")
                    .append(info).append(" 0 R >>\nstartxref\n").append(inicioXref).append("\n%%EOF\n");
            ascii(xref.toString());
            return salida.toByteArray();
        }
    }
}
