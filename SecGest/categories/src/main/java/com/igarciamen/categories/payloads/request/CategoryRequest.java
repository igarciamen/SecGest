package com.igarciamen.categories.payloads.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CategoryRequest {

    @NotBlank
    @Size(max = 80)
    private String name;

    @Size(max = 255)
    private String description;

    @Size(max = 60)
    private String icon;

    @Size(max = 7)
    private String colorHex;

    @Size(max = 500)
    private String imageUrl;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal basePrice;

    @Min(0)
    private Integer estimatedMinutes;

    public CategoryRequest() {}

    public CategoryRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
}