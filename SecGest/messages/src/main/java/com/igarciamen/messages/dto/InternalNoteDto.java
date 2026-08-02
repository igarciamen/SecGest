package com.igarciamen.messages.dto;

import com.igarciamen.messages.model.InternalNote;

import java.time.LocalDateTime;

public class InternalNoteDto {
    public Long id;
    public Long authorUserId;
    public String content;
    public LocalDateTime createdAt;

    public InternalNoteDto() {}

    public InternalNoteDto(Long id, Long authorUserId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.authorUserId = authorUserId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static InternalNoteDto from(InternalNote n) {
        return new InternalNoteDto(n.getId(), n.getAuthorUserId(), n.getContent(), n.getCreatedAt());
    }
}
