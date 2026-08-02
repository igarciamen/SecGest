package com.igarciamen.messages.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Una conversacion por tarea, no por par de usuarios: en SecreGest cada tarea
// tiene como mucho un cliente y un admin, asi que taskId ya identifica el hilo
// sin ambiguedad -- no hace falta guardar aqui quien es el cliente ni el admin,
// eso ya lo sabe "tasks" y se comprueba en cada peticion via TaskClient.
@Entity
@Table(name = "conversations", schema = "public", uniqueConstraints = @UniqueConstraint(columnNames = "task_id"))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();

    public Conversation() {}

    public Conversation(Long taskId) {
        this.taskId = taskId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public List<Message> getMessages() { return messages; }
}
