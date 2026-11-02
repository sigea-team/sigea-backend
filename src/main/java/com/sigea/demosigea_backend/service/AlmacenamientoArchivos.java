package com.sigea.demosigea_backend.service;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Almacenamiento de los archivos que suben los usuarios (HU-14: documentos de las propuestas).
 * <p>
 * La base de datos solo guarda la ruta relativa que devuelve {@link #guardar}; el contenido vive en el
 * almacenamiento. La implementación actual es {@link AlmacenamientoArchivosLocal} (disco del servidor).
 * Para usar el Object Storage compatible con S3 definido en la arquitectura basta con crear otra
 * implementación de esta interfaz, sin tocar los servicios de negocio.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public interface AlmacenamientoArchivos {

    /**
     * Guarda un archivo con un nombre único generado por el sistema.
     *
     * @param carpeta   Carpeta lógica relativa (ej. {@code propuestas/15/v2}). No debe contener {@code ..}.
     * @param extension Extensión del archivo sin punto (ej. {@code pdf}).
     * @param contenido Contenido del archivo. El llamador es responsable de cerrarlo.
     * @return Ruta relativa con la que el archivo se recupera después.
     */
    String guardar(String carpeta, String extension, InputStream contenido);

    /**
     * Recupera un archivo previamente guardado.
     *
     * @param ruta Ruta relativa devuelta por {@link #guardar}.
     * @return Recurso legible con el contenido del archivo.
     */
    Resource cargar(String ruta);

    /**
     * Elimina un archivo. Si no existe, no hace nada.
     *
     * @param ruta Ruta relativa devuelta por {@link #guardar}.
     */
    void eliminar(String ruta);
}
