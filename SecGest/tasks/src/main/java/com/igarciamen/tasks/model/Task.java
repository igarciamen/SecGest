package com.igarciamen.tasks.model;

import com.igarciamen.tasks.enums.ConfidentialityLevel;
import com.igarciamen.tasks.enums.TaskStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks", schema = "public")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El cliente vive en el microservicio "users"; aqui solo guardamos su id
    // (extraido del claim "userId" del JWT), sin llamar a users para esto.
    @Column(nullable = false)
    private Long clientUserId;

    // La categoria vive en el microservicio "categories" (base de datos distinta),
    // por eso aqui NO hay una relacion @ManyToOne, solo el id. La validez del id
    // y el nombre de la categoria se resuelven via CategoryClient (RestTemplate).
    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status = TaskStatus.PENDIENTE_REVISION;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Fecha y hora limite deseada por el cliente. Opcional: alimenta el futuro
    // bloque de calendario, no bloquea la creacion de la tarea si no se indica.
    private LocalDateTime dueDate;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 500)
    private String relevantUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConfidentialityLevel confidentialityLevel = ConfidentialityLevel.NORMAL;

    // Precio fijado por el admin al presupuestar (Bloque 5). Null hasta entonces,
    // y tambien null de nuevo si el cliente rechaza el presupuesto (Bloque 6).
    @Column(precision = 8, scale = 2)
    private java.math.BigDecimal price;

    // Historial simple de presupuestos rechazados (Bloque 6): cada vez que el
    // cliente rechaza, el precio que tenia en ese momento se archiva aqui antes
    // de ponerlo a null, para no perder esa informacion.
    @ElementCollection
    @CollectionTable(name = "task_price_history", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "price", precision = 8, scale = 2)
    private java.util.List<java.math.BigDecimal> priceHistory = new java.util.ArrayList<>();

    public Task() {}

    public Task(Long clientUserId, Long categoryId, String title, String description) {
        this.clientUserId = clientUserId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClientUserId() { return clientUserId; }
    public void setClientUserId(Long clientUserId) { this.clientUserId = clientUserId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getRelevantUrl() { return relevantUrl; }
    public void setRelevantUrl(String relevantUrl) { this.relevantUrl = relevantUrl; }

    public ConfidentialityLevel getConfidentialityLevel() { return confidentialityLevel; }
    public void setConfidentialityLevel(ConfidentialityLevel confidentialityLevel) {
        this.confidentialityLevel = confidentialityLevel;
    }

    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }

    public java.util.List<java.math.BigDecimal> getPriceHistory() { return priceHistory; }
    public void setPriceHistory(java.util.List<java.math.BigDecimal> priceHistory) { this.priceHistory = priceHistory; }

    // Bloque 9: entrega y cierre.
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;

    // 1 a 5, opcional (el cliente puede confirmar sin valorar). Comentario libre,
    // tambien opcional.
    private Integer rating;

    @Column(length = 1000)
    private String ratingComment;

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getRatingComment() { return ratingComment; }
    public void setRatingComment(String ratingComment) { this.ratingComment = ratingComment; }
}
