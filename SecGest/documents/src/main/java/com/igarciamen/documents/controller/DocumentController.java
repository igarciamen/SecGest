package com.igarciamen.documents.controller;

import com.igarciamen.documents.model.Document;
import com.igarciamen.documents.payloads.response.DocumentResponse;
import com.igarciamen.documents.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(
            summary = "Sube un documento vinculado a una tarea (cliente dueño o admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(path = "/tasks/{taskId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResponse> upload(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable Long taskId,
                                                   @RequestParam("file") MultipartFile file) {
        Document saved = documentService.upload(taskId, extractUserId(jwt), extractRole(jwt), file);
        return ResponseEntity.status(201).body(DocumentResponse.from(saved));
    }

    @Operation(
            summary = "Lista los documentos de una tarea (cliente dueño o admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/tasks/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DocumentResponse>> listForTask(@PathVariable Long taskId) {
        List<DocumentResponse> body = documentService.listForTask(taskId).stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Descarga un documento por id (cliente dueño de la tarea, o admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DocumentService.DownloadedFile file = documentService.download(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.originalFilename()).build().toString())
                .body(file.resource());
    }

    @Operation(
            summary = "Elimina un documento (solo quien lo subio, o un admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        documentService.delete(id, extractUserId(jwt), isAdmin(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new IllegalStateException("El token no contiene el claim 'userId'");
        }
        return ((Number) claim).longValue();
    }

    private String extractRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return (roles != null && roles.contains("ROLE_ADMIN")) ? "ROLE_ADMIN" : "ROLE_USER";
    }

    private boolean isAdmin(Jwt jwt) {
        return "ROLE_ADMIN".equals(extractRole(jwt));
    }
}
