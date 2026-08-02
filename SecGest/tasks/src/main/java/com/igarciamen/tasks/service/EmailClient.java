package com.igarciamen.tasks.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

// Llama al microservicio "notifications" para disparar el correo de presupuesto.
// Un fallo aqui NO debe impedir que la tarea quede presupuestada (ver TaskService):
// por eso este cliente lanza una excepcion propia que TaskService puede capturar
// y solo loguear, sin deshacer el presupuesto ya guardado.
@Component
public class EmailClient {

    private final RestTemplate http;
    private final String notificationsBase;

    public EmailClient(RestTemplate http, @Value("${notifications.base-url}") String notificationsBaseUrl) {
        this.http = http;
        this.notificationsBase = notificationsBaseUrl;
    }

    public void sendBudgetEmail(String destinatario, String nombreCliente, String tituloTarea,
                                String nombreCategoria, BigDecimal precio) {
        try {
            Map<String, Object> body = Map.of(
                    "destinatario", destinatario,
                    "nombreCliente", nombreCliente,
                    "tituloTarea", tituloTarea,
                    "nombreCategoria", nombreCategoria,
                    "precio", precio
            );
            http.postForObject(notificationsBase + "/notificar-presupuesto", body, String.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Error calling the notifications service: " + e.getMessage(), e);
        }
    }

    // Correo generico (plantilla email.html en notifications), reutilizable para
    // cualquier aviso simple: confirmacion de aceptacion, y en bloques futuros
    // (rechazo, pago, entrega...).
    public void sendGenericEmail(String destinatario, String asunto, String mensaje) {
        try {
            Map<String, Object> body = Map.of(
                    "destinatario", destinatario,
                    "asunto", asunto,
                    "mensaje", mensaje
            );
            http.postForObject(notificationsBase + "/enviar-correo", body, String.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Error calling the notifications service: " + e.getMessage(), e);
        }
    }
}
