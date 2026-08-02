package com.igarciamen.documents.payloads.response;

import com.igarciamen.documents.model.Document;

import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private Long taskId;
    private Long uploaderUserId;
    private String uploaderRole;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private LocalDateTime uploadedAt;

    public DocumentResponse() {}

    public DocumentResponse(Long id, Long taskId, Long uploaderUserId, String uploaderRole, String originalFilename,
                             String contentType, long sizeBytes, LocalDateTime uploadedAt) {
        this.id = id;
        this.taskId = taskId;
        this.uploaderUserId = uploaderUserId;
        this.uploaderRole = uploaderRole;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
    }

    public static DocumentResponse from(Document d) {
        return new DocumentResponse(d.getId(), d.getTaskId(), d.getUploaderUserId(), d.getUploaderRole(),
                d.getOriginalFilename(), d.getContentType(), d.getSizeBytes(), d.getUploadedAt());
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public Long getUploaderUserId() { return uploaderUserId; }
    public String getUploaderRole() { return uploaderRole; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
