package com.igarciamen.messages.dto;

import java.util.List;

public class ThreadDto {
    public Long taskId;
    public List<MessageDto> messages;

    public ThreadDto() {}

    public ThreadDto(Long taskId, List<MessageDto> messages) {
        this.taskId = taskId;
        this.messages = messages;
    }
}
