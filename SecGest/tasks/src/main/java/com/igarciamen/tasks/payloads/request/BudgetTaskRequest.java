package com.igarciamen.tasks.payloads.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BudgetTaskRequest {

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal price;

    public BudgetTaskRequest() {}

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
