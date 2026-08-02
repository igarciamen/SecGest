package com.igarciamen.documents.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Solo METADATOS aqui; el fichero en si se guarda en el sistema de archivos
// (ver DocumentStorageService), no en la base de datos.
@Entity
@Table(name = "documents", schema = "public")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long uploaderUserId;

    // "ROLE_USER" (el cliente) o "ROLE_ADMIN" (la agencia): para poder mostrar
    // en el frontend quien subio cada archivo, sin tener que llamar a "users".
    @Column(nullable = false, length = 20)
    private String uploaderRole;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    // Nombre real del fichero en disco: un UUID, nunca el nombre original --
    // evita colisiones y problemas de seguridad (path traversal, caracteres raros).
    @Column(nullable = false, length = 100, unique = true)
    private String storedFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public Document() {}

    public Document(Long taskId, Long uploaderUserId, String uploaderRole, String originalFilename,
                     String storedFilename, String contentType, long sizeBytes) {
        this.taskId = taskId;
        this.uploaderUserId = uploaderUserId;
        this.uploaderRole = uploaderRole;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUploaderUserId() { return uploaderUserId; }
    public void setUploaderUserId(Long uploaderUserId) { this.uploaderUserId = uploaderUserId; }

    public String getUploaderRole() { return uploaderRole; }
    public void setUploaderRole(String uploaderRole) { this.uploaderRole = uploaderRole; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
