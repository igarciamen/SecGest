package com.igarciamen.messages.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Tabla separada de Message a proposito: una nota interna nunca debe poder
// mezclarse ni filtrarse por error con los mensajes que si ve el cliente.
// Solo la escribe y la lee el admin.
@Entity
@Table(name = "internal_notes", schema = "public")
public class InternalNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public InternalNote() {}

    public InternalNote(Long taskId, Long authorUserId, String content) {
        this.taskId = taskId;
        this.authorUserId = authorUserId;
        this.content = content;
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public Long getAuthorUserId() { return authorUserId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
