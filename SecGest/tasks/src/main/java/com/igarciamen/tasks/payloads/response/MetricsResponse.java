package com.igarciamen.tasks.payloads.response;

import java.math.BigDecimal;
import java.util.Map;

public class MetricsResponse {

    private Map<String, Long> tasksByStatus;
    private BigDecimal totalRevenue;
    private long upcomingDueCount;
    private Double averageRating;

    public MetricsResponse() {}

    public MetricsResponse(Map<String, Long> tasksByStatus, BigDecimal totalRevenue,
                            long upcomingDueCount, Double averageRating) {
        this.tasksByStatus = tasksByStatus;
        this.totalRevenue = totalRevenue;
        this.upcomingDueCount = upcomingDueCount;
        this.averageRating = averageRating;
    }

    public Map<String, Long> getTasksByStatus() { return tasksByStatus; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public long getUpcomingDueCount() { return upcomingDueCount; }
    public Double getAverageRating() { return averageRating; }
}