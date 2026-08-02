package com.igarciamen.documents.service;

import com.igarciamen.documents.model.Document;
import com.igarciamen.documents.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepo;

    @Mock
    private DocumentStorageService storageService;

    @Mock
    private TaskClient taskClient;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void upload_guardaElFicheroYLosMetadatosSiTieneAcceso() {
        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", "contenido".getBytes());

        when(storageService.store(10L, file)).thenReturn("uuid-generado.pdf");
        when(documentRepo.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        Document result = documentService.upload(10L, 5L, "ROLE_USER", file);

        assertEquals(1L, result.getId());
        assertEquals("contrato.pdf", result.getOriginalFilename());
        assertEquals("uuid-generado.pdf", result.getStoredFilename());
        verify(taskClient).verifyAccessOrThrow(10L);
    }

    @Test
    void upload_propagaElRechazoDeTaskClient() {
        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "x".getBytes());
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(taskClient).verifyAccessOrThrow(10L);

        assertThrows(ResponseStatusException.class, () -> documentService.upload(10L, 999L, "ROLE_USER", file));
        verify(storageService, never()).store(any(), any());
    }

    @Test
    void upload_rechazaUnTipoDeFicheroNoPermitido() {
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/x-msdownload", "x".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.upload(10L, 5L, "ROLE_USER", file));

        assertEquals(415, ex.getStatusCode().value());
        verify(storageService, never()).store(any(), any());
    }

    @Test
    void upload_aceptaUnZip() {
        MockMultipartFile file = new MockMultipartFile("file", "documentos.zip", "application/zip", "contenido comprimido".getBytes());

        when(storageService.store(10L, file)).thenReturn("uuid-generado.zip");
        when(documentRepo.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(2L);
            return d;
        });

        Document result = documentService.upload(10L, 5L, "ROLE_USER", file);

        assertEquals("documentos.zip", result.getOriginalFilename());
    }

    @Test
    void upload_rechazaUnFicheroDemasiadoGrande() {
        byte[] contenidoGrande = new byte[11 * 1024 * 1024]; // 11 MB > limite de 10 MB
        MockMultipartFile file = new MockMultipartFile("file", "grande.pdf", "application/pdf", contenidoGrande);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.upload(10L, 5L, "ROLE_USER", file));

        assertEquals(413, ex.getStatusCode().value());
    }

    @Test
    void upload_rechazaUnFicheroVacio() {
        MockMultipartFile file = new MockMultipartFile("file", "vacio.pdf", "application/pdf", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.upload(10L, 5L, "ROLE_USER", file));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void listForTask_comprueboAccesoYDevuelveElListado() {
        Document d = new Document(10L, 5L, "ROLE_USER", "a.pdf", "uuid.pdf", "application/pdf", 100);
        when(documentRepo.findByTaskIdOrderByUploadedAtDesc(10L)).thenReturn(List.of(d));

        List<Document> result = documentService.listForTask(10L);

        assertEquals(1, result.size());
        verify(taskClient).verifyAccessOrThrow(10L);
    }

    @Test
    void download_lanza404SiElDocumentoNoExiste() {
        when(documentRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.download(99L));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void download_comprueboAccesoALaTareaDelDocumento() {
        Document d = new Document(10L, 5L, "ROLE_USER", "a.pdf", "uuid.pdf", "application/pdf", 100);
        d.setId(1L);
        when(documentRepo.findById(1L)).thenReturn(Optional.of(d));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(taskClient).verifyAccessOrThrow(10L);

        assertThrows(ResponseStatusException.class, () -> documentService.download(1L));
    }

    @Test
    void delete_elPropioSubidorPuedeBorrarlo() {
        Document d = new Document(10L, 5L, "ROLE_USER", "a.pdf", "uuid.pdf", "application/pdf", 100);
        d.setId(1L);
        when(documentRepo.findById(1L)).thenReturn(Optional.of(d));

        documentService.delete(1L, 5L, false);

        verify(storageService).delete(10L, "uuid.pdf");
        verify(documentRepo).delete(d);
    }

    @Test
    void delete_unAdminPuedeBorrarCualquiera() {
        Document d = new Document(10L, 5L, "ROLE_USER", "a.pdf", "uuid.pdf", "application/pdf", 100);
        d.setId(1L);
        when(documentRepo.findById(1L)).thenReturn(Optional.of(d));

        documentService.delete(1L, 999L, true);

        verify(documentRepo).delete(d);
    }

    @Test
    void delete_otroClienteQueNoLoSubioNiEsAdmin_lanza403() {
        Document d = new Document(10L, 5L, "ROLE_USER", "a.pdf", "uuid.pdf", "application/pdf", 100);
        d.setId(1L);
        when(documentRepo.findById(1L)).thenReturn(Optional.of(d));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.delete(1L, 999L, false));

        assertEquals(403, ex.getStatusCode().value());
        verify(documentRepo, never()).delete(any(Document.class));
    }

    @Test
    void delete_lanza404SiNoExiste() {
        when(documentRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> documentService.delete(99L, 5L, false));

        assertEquals(404, ex.getStatusCode().value());
    }
}
