package com.igarciamen.messages.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// Mismo patron que en "documents": no se reimplementa aqui "es el dueño o es
// admin", se le pregunta a "tasks" (que ya sabe la respuesta) reenviando el
// token de quien esta llamando.
@Component
public class TaskClient {

    private final RestTemplate http;
    private final String tasksBase;

    public TaskClient(RestTemplate http, @Value("${tasks.base-url}") String tasksBaseUrl) {
        this.http = http;
        this.tasksBase = tasksBaseUrl;
    }

    public void verifyAccessOrThrow(Long taskId) {
        try {
            http.getForObject(tasksBase + "/" + taskId, Map.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()),
                    "Sin acceso a la tarea " + taskId);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo comprobar el acceso a la tarea: " + e.getMessage());
        }
    }
}
