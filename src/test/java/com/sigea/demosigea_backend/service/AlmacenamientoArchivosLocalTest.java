package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.exception.AlmacenamientoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmacenamientoArchivosLocalTest {

    @TempDir
    Path directorio;

    @Test
    @DisplayName("Guarda, carga y elimina un archivo dentro del directorio configurado")
    void guardarCargarEliminar() throws Exception {
        AlmacenamientoArchivosLocal almacenamiento = new AlmacenamientoArchivosLocal(directorio.toString());
        byte[] contenido = "%PDF-1.7 prueba".getBytes();

        String ruta = almacenamiento.guardar("propuestas/50/v1", "pdf", new ByteArrayInputStream(contenido));

        assertTrue(ruta.startsWith("propuestas/50/v1/"));
        assertTrue(ruta.endsWith(".pdf"));
        assertTrue(Files.exists(directorio.resolve(ruta)));

        Resource recurso = almacenamiento.cargar(ruta);
        try (InputStream in = recurso.getInputStream()) {
            assertArrayEquals(contenido, in.readAllBytes());
        }

        almacenamiento.eliminar(ruta);
        assertFalse(Files.exists(directorio.resolve(ruta)));
        assertThrows(RecursoNoEncontradoException.class, () -> almacenamiento.cargar(ruta));
    }

    @Test
    @DisplayName("Rechaza rutas que intentan salir del directorio de almacenamiento")
    void rutasManipuladas_SeRechazan() {
        AlmacenamientoArchivosLocal almacenamiento = new AlmacenamientoArchivosLocal(directorio.toString());

        assertThrows(AlmacenamientoException.class,
                () -> almacenamiento.guardar("../fuera", "pdf", new ByteArrayInputStream(new byte[]{1})));
        assertThrows(AlmacenamientoException.class,
                () -> almacenamiento.guardar("propuestas", "p/df", new ByteArrayInputStream(new byte[]{1})));
        assertThrows(AlmacenamientoException.class, () -> almacenamiento.cargar("../../etc/passwd"));
    }
}
