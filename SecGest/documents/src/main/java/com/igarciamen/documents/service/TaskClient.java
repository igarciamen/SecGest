package com.igarciamen.documents.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// Llama a "tasks" (GET /api/tasks/{id}) para comprobar si quien esta subiendo o
// descargando un archivo tiene permiso sobre esa tarea. No se duplica esa logica
// aqui: "tasks" ya sabe quien es el dueno de cada tarea, documents solo pregunta.
@Component
public class TaskClient {

    private final RestTemplate http;
    private final String tasksBase;

    public TaskClient(RestTemplate http, @Value("${tasks.base-url}") String tasksBaseUrl) {
        this.http = http;
        this.tasksBase = tasksBaseUrl;
    }

    // Lanza 403/404 (los mismos que devolvio "tasks") si no hay acceso, o 503 si
    // "tasks" no responde. Si no lanza nada, el llamante tiene acceso confirmado.
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
