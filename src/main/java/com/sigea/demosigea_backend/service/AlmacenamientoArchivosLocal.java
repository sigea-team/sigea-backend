package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.exception.AlmacenamientoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implementación de {@link AlmacenamientoArchivos} sobre el sistema de archivos del servidor (HU-14).
 * <p>
 * El directorio raíz se configura con {@code app.almacenamiento.directorio} (variable de entorno
 * {@code APP_ALMACENAMIENTO_DIR}). En un servidor con disco efímero se debe montar un volumen persistente
 * en esa ruta, o reemplazar esta clase por una implementación sobre Object Storage (S3).
 * </p>
 * <p>
 * Los nombres de archivo los genera el sistema (UUID) y toda ruta se valida contra el directorio raíz,
 * de modo que un nombre o ruta manipulada no puede leer ni escribir fuera de él.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
public class AlmacenamientoArchivosLocal implements AlmacenamientoArchivos {

    private static final Pattern CARPETA_VALIDA = Pattern.compile("[A-Za-z0-9_\\-/]+");
    private static final Pattern EXTENSION_VALIDA = Pattern.compile("[a-z0-9]{1,10}");

    private final Path raiz;

    public AlmacenamientoArchivosLocal(
            @Value("${app.almacenamiento.directorio:./almacenamiento}") String directorio) {
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String carpeta, String extension, InputStream contenido) {
        if (carpeta == null || !CARPETA_VALIDA.matcher(carpeta).matches() || carpeta.contains("..")) {
            throw new AlmacenamientoException("Carpeta de almacenamiento inválida.");
        }
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!EXTENSION_VALIDA.matcher(ext).matches()) {
            throw new AlmacenamientoException("Extensión de archivo inválida.");
        }

        String rutaRelativa = carpeta.replaceAll("^/+|/+$", "") + "/" + UUID.randomUUID() + "." + ext;
        Path destino = resolverDentroDeRaiz(rutaRelativa);
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(contenido, destino);
            log.info("Archivo almacenado en {}", rutaRelativa);
            return rutaRelativa;
        } catch (IOException ex) {
            throw new AlmacenamientoException("No fue posible guardar el archivo en el almacenamiento.", ex);
        }
    }

    @Override
    public Resource cargar(String ruta) {
        Path archivo = resolverDentroDeRaiz(ruta);
        if (!Files.isRegularFile(archivo) || !Files.isReadable(archivo)) {
            throw new RecursoNoEncontradoException("El archivo solicitado no se encuentra en el almacenamiento.");
        }
        return new FileSystemResource(archivo);
    }

    @Override
    public void eliminar(String ruta) {
        try {
            Files.deleteIfExists(resolverDentroDeRaiz(ruta));
        } catch (IOException ex) {
            log.warn("No fue posible eliminar el archivo {}: {}", ruta, ex.getMessage());
        }
    }

    /**
     * Convierte una ruta relativa en absoluta y verifica que quede dentro del directorio raíz.
     */
    private Path resolverDentroDeRaiz(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new AlmacenamientoException("Ruta de archivo inválida.");
        }
        Path resuelta = raiz.resolve(rutaRelativa).normalize();
        if (!resuelta.startsWith(raiz)) {
            throw new AlmacenamientoException("Ruta de archivo inválida.");
        }
        return resuelta;
    }
}
