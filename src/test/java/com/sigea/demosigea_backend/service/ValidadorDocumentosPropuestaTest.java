package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.exception.ArchivoInvalidoException;
import com.sigea.demosigea_backend.service.ValidadorDocumentosPropuesta.DocumentoValidado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorDocumentosPropuestaTest {

    /** Validador con 1 MB por archivo y máximo 3 documentos. */
    private final ValidadorDocumentosPropuesta validador = new ValidadorDocumentosPropuesta("pdf,doc,docx", 1, 3);

    private static MockMultipartFile pdf(String nombre) {
        return new MockMultipartFile("documentos", nombre, "application/pdf", "%PDF-1.7\ncontenido".getBytes());
    }

    private static MockMultipartFile docx(String nombre) {
        byte[] contenido = {0x50, 0x4B, 0x03, 0x04, 0x14, 0x00};
        return new MockMultipartFile("documentos", nombre, "application/octet-stream", contenido);
    }

    @Test
    @DisplayName("Criterio 1: acepta PDF y DOCX válidos y asigna su tipo de contenido")
    void validar_ArchivosValidos_Acepta() {
        List<DocumentoValidado> validos = validador.validar(List.of(pdf("ponencia.PDF"), docx("anexo.docx")), true);

        assertEquals(2, validos.size());
        assertEquals("pdf", validos.get(0).extension());
        assertEquals("application/pdf", validos.get(0).tipoContenido());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                validos.get(1).tipoContenido());
    }

    @Test
    @DisplayName("Criterio 1: limpia la ruta del nombre original del archivo")
    void validar_NombreConRuta_SeLimpia() {
        List<DocumentoValidado> validos = validador.validar(List.of(pdf("C:\\Users\\autor\\ponencia.pdf")), true);
        assertEquals("ponencia.pdf", validos.get(0).nombreOriginal());
    }

    @Test
    @DisplayName("Criterio 2: exige al menos un documento cuando es obligatorio")
    void validar_SinDocumentosObligatorios_Rechaza() {
        ArchivoInvalidoException ex = assertThrows(ArchivoInvalidoException.class,
                () -> validador.validar(List.of(), true));
        assertTrue(ex.getErrores().containsKey("documentos"));
        assertThrows(ArchivoInvalidoException.class, () -> validador.validar(null, true));
    }

    @Test
    @DisplayName("Criterio 2: si no es obligatorio, la lista vacía es válida")
    void validar_SinDocumentosOpcionales_DevuelveVacio() {
        assertTrue(validador.validar(null, false).isEmpty());
    }

    @Test
    @DisplayName("Criterio 2: rechaza un formato no permitido indicando el archivo")
    void validar_FormatoNoPermitido_Rechaza() {
        MockMultipartFile imagen = new MockMultipartFile("documentos", "foto.png", "image/png", new byte[]{1, 2, 3});

        ArchivoInvalidoException ex = assertThrows(ArchivoInvalidoException.class,
                () -> validador.validar(List.of(pdf("ok.pdf"), imagen), true));

        assertTrue(ex.getErrores().get("documentos[1]").contains("formato no permitido"));
        assertEquals(1, ex.getErrores().size());
    }

    @Test
    @DisplayName("Criterio 2: rechaza un archivo que supera el tamaño máximo")
    void validar_TamanoExcedido_Rechaza() {
        byte[] grande = new byte[1024 * 1024 + 10];
        System.arraycopy("%PDF-".getBytes(), 0, grande, 0, 5);
        MockMultipartFile archivo = new MockMultipartFile("documentos", "grande.pdf", "application/pdf", grande);

        ArchivoInvalidoException ex = assertThrows(ArchivoInvalidoException.class,
                () -> validador.validar(List.of(archivo), true));

        assertTrue(ex.getErrores().get("documentos[0]").contains("tamaño máximo"));
    }

    @Test
    @DisplayName("Criterio 2: rechaza un archivo vacío")
    void validar_ArchivoVacio_Rechaza() {
        MockMultipartFile vacio = new MockMultipartFile("documentos", "vacio.pdf", "application/pdf", new byte[0]);

        ArchivoInvalidoException ex = assertThrows(ArchivoInvalidoException.class,
                () -> validador.validar(List.of(vacio), true));

        assertTrue(ex.getErrores().get("documentos[0]").contains("vacío"));
    }

    @Test
    @DisplayName("Criterio 2: rechaza un archivo renombrado cuyo contenido no corresponde a la extensión")
    void validar_ContenidoNoCoincide_Rechaza() {
        MockMultipartFile falso = new MockMultipartFile("documentos", "virus.pdf", "application/pdf",
                "MZ ejecutable".getBytes());

        ArchivoInvalidoException ex = assertThrows(ArchivoInvalidoException.class,
                () -> validador.validar(List.of(falso), true));

        assertTrue(ex.getErrores().get("documentos[0]").contains("no corresponde"));
    }

    @Test
    @DisplayName("Criterio 2: rechaza más documentos de los permitidos por versión")
    void validar_ExcedeCantidad_Rechaza() {
        List<MultipartFile> archivos = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            archivos.add(pdf("doc" + i + ".pdf"));
        }

        ArchivoInvalidoException ex = assertThrows(ArchivoInvalidoException.class,
                () -> validador.validar(archivos, true));

        assertTrue(ex.getErrores().get("documentos").contains("máximo 3"));
        assertThrows(ArchivoInvalidoException.class, () -> validador.validarCantidadTotal(4));
    }
}
