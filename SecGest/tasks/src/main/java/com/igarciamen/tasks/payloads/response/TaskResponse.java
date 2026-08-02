package com.igarciamen.tasks.payloads.response;

import com.igarciamen.tasks.model.Task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TaskResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
    private String contactPhone;
    private String relevantUrl;
    private String confidentialityLevel;
    private BigDecimal price;
    private List<BigDecimal> priceHistory;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private Integer rating;
    private String ratingComment;

    public TaskResponse() {}

    public TaskResponse(Long id, Long categoryId, String categoryName, String title, String description,
                        String status, LocalDateTime createdAt, LocalDateTime dueDate,
                        String contactPhone, String relevantUrl, String confidentialityLevel, BigDecimal price,
                        List<BigDecimal> priceHistory) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.dueDate = dueDate;
        this.contactPhone = contactPhone;
        this.relevantUrl = relevantUrl;
        this.confidentialityLevel = confidentialityLevel;
        this.price = price;
        this.priceHistory = priceHistory;
    }

    // categoryName llega ya resuelto desde el controller (via CategoryClient), porque
    // Task ya no la tiene embebida (vive en otro microservicio/BBDD).
    public static TaskResponse from(Task task, String categoryName) {
        TaskResponse response = new TaskResponse(
                task.getId(),
                task.getCategoryId(),
                categoryName,
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getCreatedAt(),
                task.getDueDate(),
                task.getContactPhone(),
                task.getRelevantUrl(),
                task.getConfidentialityLevel() != null ? task.getConfidentialityLevel().name() : null,
                task.getPrice(),
                task.getPriceHistory()
        );
        response.deliveredAt = task.getDeliveredAt();
        response.completedAt = task.getCompletedAt();
        response.rating = task.getRating();
        response.ratingComment = task.getRatingComment();
        return response;
    }

    public Long getId() { return id; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDueDate() { return dueDate; }
    public String getContactPhone() { return contactPhone; }
    public String getRelevantUrl() { return relevantUrl; }
    public String getConfidentialityLevel() { return confidentialityLevel; }
    public BigDecimal getPrice() { return price; }
    public List<BigDecimal> getPriceHistory() { return priceHistory; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Integer getRating() { return rating; }
    public String getRatingComment() { return ratingComment; }
}
