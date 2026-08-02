package com.igarciamen.tasks.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

// Equivalente a "ExternalClients" en tus proyectos anteriores: aisla las llamadas
// HTTP a otros microservicios (aqui, "categories") del resto del codigo de negocio.
@Component
public class CategoryClient {

    private final RestTemplate http;
    private final String categoriesBase;

    public CategoryClient(RestTemplate http,
                          @Value("${categories.base-url}") String categoriesBaseUrl) {
        this.http = http;
        this.categoriesBase = categoriesBaseUrl;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchCategoryOrThrow(Long id) {
        try {
            var body = http.getForObject(categoriesBase + "/" + id, Map.class);
            if (body == null || !Objects.equals(((Number) body.getOrDefault("id", -1)).longValue(), id)) {
                throw new EntityNotFoundException("Category not found: " + id);
            }
            return body;
        } catch (RestClientException e) {
            throw new IllegalStateException("Error calling the categories service: " + e.getMessage(), e);
        }
    }
}
