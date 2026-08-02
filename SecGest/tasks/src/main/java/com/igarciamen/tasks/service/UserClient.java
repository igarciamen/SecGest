package com.igarciamen.tasks.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

// Igual que CategoryClient, pero llamando a "users" para obtener los datos del
// cliente (username/email) que necesita el correo de presupuesto. El token del
// admin que hace la peticion se reenvia automaticamente (RestTemplateConfig),
// asi que llega autenticado a GET /api/user/{id} en users.
@Component
public class UserClient {

    private final RestTemplate http;
    private final String usersBase;

    public UserClient(RestTemplate http, @Value("${users.base-url}") String usersBaseUrl) {
        this.http = http;
        this.usersBase = usersBaseUrl;
    }

    public Map<String, Object> fetchUserOrThrow(Long id) {
        try {
            var body = http.getForObject(usersBase + "/" + id, Map.class);
            if (body == null || !Objects.equals(((Number) body.getOrDefault("id", -1)).longValue(), id)) {
                throw new EntityNotFoundException("User not found: " + id);
            }
            return body;
        } catch (RestClientException e) {
            throw new IllegalStateException("Error calling the users service: " + e.getMessage(), e);
        }
    }
}
