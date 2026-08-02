package com.igarciamen.messages.controller;

import com.igarciamen.messages.dto.MessageDto;
import com.igarciamen.messages.dto.ThreadDto;
import com.igarciamen.messages.service.MessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessagingController {

    private final MessagingService service;

    public MessagingController(MessagingService service) {
        this.service = service;
    }

    @Operation(
            summary = "Obtiene el hilo de mensajes de una tarea (dueño de la tarea, o admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/tasks/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ThreadDto> thread(@AuthenticationPrincipal Jwt jwt, @PathVariable Long taskId) {
        return ResponseEntity.ok(service.getThread(taskId, extractUserId(jwt)));
    }

    @Operation(
            summary = "Envia un mensaje en el hilo de una tarea (dueño de la tarea, o admin)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(path = "/tasks/{taskId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageDto> send(@AuthenticationPrincipal Jwt jwt, @PathVariable Long taskId,
                                            @RequestBody Map<String, String> body) {
        MessageDto saved = service.sendMessage(taskId, extractUserId(jwt), extractRole(jwt), body.get("content"));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(
            summary = "Borra un mensaje propio (solo quien lo escribio)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/tasks/{taskId}/{messageId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long taskId,
                                        @PathVariable Long messageId) {
        service.deleteMessage(taskId, messageId, extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Marca como leidos los mensajes del otro lado en esta tarea",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/tasks/{taskId}/read", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> markAsRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long taskId) {
        int updated = service.markAsRead(taskId, extractUserId(jwt));
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @Operation(
            summary = "Numero de mensajes sin leer en esta tarea, para el propio usuario",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/tasks/{taskId}/unread-count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal Jwt jwt, @PathVariable Long taskId) {
        return ResponseEntity.ok(Map.of("count", service.unreadCount(taskId, extractUserId(jwt))));
    }

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new IllegalStateException("El token no contiene el claim 'userId'");
        }
        return ((Number) claim).longValue();
    }

    private String extractRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return (roles != null && roles.contains("ROLE_ADMIN")) ? "ROLE_ADMIN" : "ROLE_USER";
    }
}
