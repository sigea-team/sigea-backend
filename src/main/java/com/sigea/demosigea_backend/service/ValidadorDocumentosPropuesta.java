package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.exception.ArchivoInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valida los documentos que se adjuntan a una propuesta (HU-14, Criterio 2).
 * <p>
 * Reglas, configurables en {@code application.properties}:
 * <ul>
 *   <li>{@code app.propuestas.documentos.extensiones-permitidas} (por defecto {@code pdf,doc,docx}).</li>
 *   <li>{@code app.propuestas.documentos.tamano-maximo-mb} por archivo (por defecto 10 MB).</li>
 *   <li>{@code app.propuestas.documentos.maximo-por-version}: cantidad máxima de documentos (por defecto 5).</li>
 * </ul>
 * Además de la extensión, para PDF, DOC y DOCX se revisan los primeros bytes del archivo, de modo que
 * un archivo renombrado (ej. un {@code .exe} con extensión {@code .pdf}) también se rechaza.
 * Se revisan todos los archivos y se informan juntos todos los errores encontrados.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Component
public class ValidadorDocumentosPropuesta {

    private static final int LONGITUD_MAXIMA_NOMBRE = 255;

    private static final byte[] FIRMA_PDF = {0x25, 0x50, 0x44, 0x46, 0x2D};                       // %PDF-
    private static final byte[] FIRMA_ZIP = {0x50, 0x4B, 0x03, 0x04};                             // PK.. (docx)
    private static final byte[] FIRMA_OLE = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};                                          // doc

    private static final Map<String, String> TIPOS_CONTENIDO = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final Set<String> extensionesPermitidas;
    private final long tamanoMaximoBytes;
    private final long tamanoMaximoMb;
    private final int maximoPorVersion;

    public ValidadorDocumentosPropuesta(
            @Value("${app.propuestas.documentos.extensiones-permitidas:pdf,doc,docx}") String extensiones,
            @Value("${app.propuestas.documentos.tamano-maximo-mb:10}") long tamanoMaximoMb,
            @Value("${app.propuestas.documentos.maximo-por-version:5}") int maximoPorVersion) {
        this.extensionesPermitidas = Arrays.stream(extensiones.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT).replace(".", ""))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.tamanoMaximoMb = tamanoMaximoMb;
        this.tamanoMaximoBytes = tamanoMaximoMb * 1024L * 1024L;
        this.maximoPorVersion = maximoPorVersion;
    }

    /**
     * Documento que pasó la validación, con su nombre limpio, extensión y tipo de contenido.
     *
     * @param archivo        Archivo recibido.
     * @param nombreOriginal Nombre del archivo sin rutas.
     * @param extension      Extensión en minúsculas, sin punto.
     * @param tipoContenido  Tipo MIME asignado según la extensión.
     */
    public record DocumentoValidado(MultipartFile archivo, String nombreOriginal, String extension,
                                    String tipoContenido) {
    }

    /**
     * Valida la lista de archivos recibida.
     *
     * @param archivos    Archivos adjuntos (puede ser {@code null}).
     * @param obligatorio Si es {@code true}, exige al menos un documento.
     * @return Documentos válidos, en el mismo orden en que llegaron. Los vacíos sin nombre se ignoran.
     * @throws ArchivoInvalidoException si algún archivo no cumple las reglas o faltan documentos obligatorios.
     */
    public List<DocumentoValidado> validar(List<MultipartFile> archivos, boolean obligatorio) {
        List<MultipartFile> recibidos = archivos == null ? List.of() : archivos.stream()
                .filter(a -> a != null && !(a.isEmpty() && !StringUtils.hasText(a.getOriginalFilename())))
                .toList();

        if (recibidos.isEmpty()) {
            if (obligatorio) {
                throw new ArchivoInvalidoException(
                        "Debe adjuntar al menos un documento de la propuesta.",
                        Map.of("documentos", "Documento obligatorio. Formatos permitidos: " + formatosTexto() + "."));
            }
            return List.of();
        }

        Map<String, String> errores = new LinkedHashMap<>();
        if (recibidos.size() > maximoPorVersion) {
            errores.put("documentos", "Se permiten como máximo " + maximoPorVersion + " documentos por versión.");
        }

        List<DocumentoValidado> validos = new ArrayList<>();
        for (int i = 0; i < recibidos.size(); i++) {
            MultipartFile archivo = recibidos.get(i);
            String clave = "documentos[" + i + "]";
            String nombre = limpiarNombre(archivo.getOriginalFilename());
            String extension = StringUtils.getFilenameExtension(nombre);
            extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);

            String error = validarArchivo(archivo, nombre, extension);
            if (error != null) {
                errores.put(clave, (nombre.isEmpty() ? "Archivo sin nombre" : nombre) + ": " + error);
            } else {
                validos.add(new DocumentoValidado(archivo, nombre, extension,
                        TIPOS_CONTENIDO.getOrDefault(extension, "application/octet-stream")));
            }
        }

        if (!errores.isEmpty()) {
            throw new ArchivoInvalidoException(
                    "Uno o varios documentos no cumplen las condiciones de carga.", errores);
        }
        return validos;
    }

    /**
     * Verifica que la versión resultante (documentos conservados + nuevos) no supere el máximo permitido.
     *
     * @param total Cantidad total de documentos de la versión.
     */
    public void validarCantidadTotal(int total) {
        if (total > maximoPorVersion) {
            throw new ArchivoInvalidoException(
                    "La versión supera la cantidad máxima de documentos permitida.",
                    Map.of("documentos", "Se permiten como máximo " + maximoPorVersion + " documentos por versión."));
        }
    }

    private String validarArchivo(MultipartFile archivo, String nombre, String extension) {
        if (nombre.isEmpty()) {
            return "el archivo no tiene nombre.";
        }
        if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
            return "el nombre del archivo supera " + LONGITUD_MAXIMA_NOMBRE + " caracteres.";
        }
        if (!extensionesPermitidas.contains(extension)) {
            return "formato no permitido. Formatos permitidos: " + formatosTexto() + ".";
        }
        if (archivo.isEmpty() || archivo.getSize() <= 0) {
            return "el archivo está vacío.";
        }
        if (archivo.getSize() > tamanoMaximoBytes) {
            return "supera el tamaño máximo permitido de " + tamanoMaximoMb + " MB.";
        }
        if (!contenidoCoincideConExtension(archivo, extension)) {
            return "el contenido del archivo no corresponde al formato ." + extension + ".";
        }
        return null;
    }

    private boolean contenidoCoincideConExtension(MultipartFile archivo, String extension) {
        byte[] firma = switch (extension) {
            case "pdf" -> FIRMA_PDF;
            case "docx" -> FIRMA_ZIP;
            case "doc" -> FIRMA_OLE;
            default -> null;
        };
        if (firma == null) {
            return true;
        }
        try (InputStream in = archivo.getInputStream()) {
            byte[] inicio = in.readNBytes(firma.length);
            return Arrays.equals(inicio, firma);
        } catch (IOException ex) {
            return false;
        }
    }

    private static String limpiarNombre(String original) {
        if (original == null) {
            return "";
        }
        String sinRuta = original.replace('\\', '/');
        sinRuta = sinRuta.substring(sinRuta.lastIndexOf('/') + 1);
        return sinRuta.replaceAll("[\\p{Cntrl}]", "").trim();
    }

    private String formatosTexto() {
        return extensionesPermitidas.stream().map(e -> "." + e).collect(Collectors.joining(", "));
    }
}
