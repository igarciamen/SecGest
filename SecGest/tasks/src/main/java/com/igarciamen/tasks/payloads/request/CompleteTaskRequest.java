package com.igarciamen.tasks.payloads.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// El cliente puede confirmar la entrega sin valorar (rating null), o valorar
// con una puntuacion de 1 a 5 y un comentario opcional.
public class CompleteTaskRequest {

    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 1000)
    private String ratingComment;

    public CompleteTaskRequest() {}

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getRatingComment() { return ratingComment; }
    public void setRatingComment(String ratingComment) { this.ratingComment = ratingComment; }
}
