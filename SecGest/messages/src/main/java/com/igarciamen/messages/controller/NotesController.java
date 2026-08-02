package com.igarciamen.messages.controller;

import com.igarciamen.messages.dto.InternalNoteDto;
import com.igarciamen.messages.service.NotesService;
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

// Todo este controller exige ROLE_ADMIN (ver SecurityConfig): el cliente ni
// siquiera puede llegar a intentarlo.
@RestController
@RequestMapping("/api/notes")
public class NotesController {

    private final NotesService service;

    public NotesController(NotesService service) {
        this.service = service;
    }

    @Operation(
            summary = "Lista las notas internas de una tarea (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/tasks/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InternalNoteDto>> list(@PathVariable Long taskId) {
        return ResponseEntity.ok(service.listNotes(taskId));
    }

    @Operation(
            summary = "Añade una nota interna a una tarea (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(path = "/tasks/{taskId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InternalNoteDto> add(@AuthenticationPrincipal Jwt jwt, @PathVariable Long taskId,
                                                @RequestBody Map<String, String> body) {
        InternalNoteDto saved = service.addNote(taskId, extractUserId(jwt), body.get("content"));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new IllegalStateException("El token no contiene el claim 'userId'");
        }
        return ((Number) claim).longValue();
    }
}
