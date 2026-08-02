package com.igarciamen.tasks.payloads.request;

import com.igarciamen.tasks.enums.ConfidentialityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CreateTaskRequest {

    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(max = 120)
    private String title;

    @Size(max = 1000)
    private String description;

    // Todos los siguientes son opcionales: el cliente puede rellenarlos o no.
    private LocalDateTime dueDate;

    @Size(max = 30)
    private String contactPhone;

    @Size(max = 500)
    private String relevantUrl;

    private ConfidentialityLevel confidentialityLevel;

    public CreateTaskRequest() {}

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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
}
