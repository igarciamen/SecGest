package com.igarciamen.messages.dto;

import com.igarciamen.messages.model.Message;

import java.time.LocalDateTime;

public class MessageDto {
    public Long id;
    public Long senderId;
    public String senderRole;
    public String content;
    public LocalDateTime createdAt;
    public LocalDateTime readAt;

    public MessageDto() {}

    public MessageDto(Long id, Long senderId, String senderRole, String content,
                       LocalDateTime createdAt, LocalDateTime readAt) {
        this.id = id;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.content = content;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public static MessageDto from(Message m) {
        return new MessageDto(m.getId(), m.getSenderId(), m.getSenderRole(), m.getContent(),
                m.getCreatedAt(), m.getReadAt());
    }
}
