package com.igarciamen.categories.payloads.response;

import com.igarciamen.categories.model.TaskCategory;

import java.math.BigDecimal;

public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String icon;
    private String colorHex;
    private String imageUrl;
    private boolean active;
    private BigDecimal basePrice;
    private Integer estimatedMinutes;

    public CategoryResponse() {}

    public CategoryResponse(Long id, String name, String description, String icon, String colorHex,
                            String imageUrl, boolean active, BigDecimal basePrice, Integer estimatedMinutes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.colorHex = colorHex;
        this.imageUrl = imageUrl;
        this.active = active;
        this.basePrice = basePrice;
        this.estimatedMinutes = estimatedMinutes;
    }

    public static CategoryResponse from(TaskCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getColorHex(),
                category.getImageUrl(),
                category.isActive(),
                category.getBasePrice(),
                category.getEstimatedMinutes()
        );
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public String getColorHex() { return colorHex; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActive() { return active; }
    public BigDecimal getBasePrice() { return basePrice; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
}