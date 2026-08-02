package com.igarciamen.documents.service;

import com.igarciamen.documents.model.Document;
import com.igarciamen.documents.repository.DocumentRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class DocumentService {

    // Limite deliberadamente conservador para un TFG: adjuntos de referencia
    // (contratos, guiones, entregables), no ficheros multimedia pesados.
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png", "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            // .zip: distintos navegadores/sistemas operativos mandan un content-type
            // ligeramente distinto para el mismo tipo de fichero, se aceptan los tres.
            "application/zip",
            "application/x-zip-compressed",
            "multipart/x-zip"
    );

    private final DocumentRepository documentRepo;
    private final DocumentStorageService storageService;
    private final TaskClient taskClient;

    public DocumentService(DocumentRepository documentRepo, DocumentStorageService storageService, TaskClient taskClient) {
        this.documentRepo = documentRepo;
        this.storageService = storageService;
        this.taskClient = taskClient;
    }

    public Document upload(Long taskId, Long uploaderUserId, String uploaderRole, MultipartFile file) {
        taskClient.verifyAccessOrThrow(taskId);
        validateFile(file);

        String storedFilename = storageService.store(taskId, file);

        Document document = new Document(taskId, uploaderUserId, uploaderRole,
                file.getOriginalFilename(), storedFilename, file.getContentType(), file.getSize());
        return documentRepo.save(document);
    }

    public List<Document> listForTask(Long taskId) {
        taskClient.verifyAccessOrThrow(taskId);
        return documentRepo.findByTaskIdOrderByUploadedAtDesc(taskId);
    }

    public DownloadedFile download(Long documentId) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        taskClient.verifyAccessOrThrow(document.getTaskId());

        Resource resource = storageService.load(document.getTaskId(), document.getStoredFilename());
        return new DownloadedFile(resource, document.getOriginalFilename(), document.getContentType());
    }

    // Solo quien subio el archivo, o un admin, puede borrarlo -- un cliente no
    // debe poder eliminar, por ejemplo, el entregable que subio la agencia.
    public void delete(Long documentId, Long requesterId, boolean isAdmin) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        taskClient.verifyAccessOrThrow(document.getTaskId());

        if (!isAdmin && !document.getUploaderUserId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo quien subio el documento (o un admin) puede eliminarlo");
        }

        storageService.delete(document.getTaskId(), document.getStoredFilename());
        documentRepo.delete(document);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha recibido ningun fichero");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "El fichero supera el tamano maximo permitido (10 MB)");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo de fichero no permitido: " + file.getContentType());
        }
    }

    public record DownloadedFile(Resource resource, String originalFilename, String contentType) {}
}
