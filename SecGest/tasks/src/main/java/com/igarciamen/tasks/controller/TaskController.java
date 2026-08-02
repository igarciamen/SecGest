package com.igarciamen.tasks.controller;

import com.igarciamen.tasks.enums.TaskStatus;
import com.igarciamen.tasks.model.Task;
import com.igarciamen.tasks.payloads.request.BudgetTaskRequest;
import com.igarciamen.tasks.payloads.request.CompleteTaskRequest;
import com.igarciamen.tasks.payloads.request.CreateTaskRequest;
import com.igarciamen.tasks.payloads.request.UpdateTaskRequest;
import com.igarciamen.tasks.payloads.response.RedsysFormResponse;
import com.igarciamen.tasks.payloads.response.TaskResponse;
import com.igarciamen.tasks.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.igarciamen.tasks.payloads.response.MetricsResponse;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            summary = "Solicita una tarea nueva (solo ROLE_USER)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateTaskRequest req) {
        Long clientUserId = extractUserId(jwt);
        Task created = taskService.create(clientUserId, req);
        String categoryName = taskService.fetchCategoryName(created.getCategoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(created, categoryName));
    }

    @Operation(
            summary = "Lista las tareas del usuario autenticado (\"Mis tareas\")",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/mine", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TaskResponse>> mine(@AuthenticationPrincipal Jwt jwt) {
        Long clientUserId = extractUserId(jwt);
        List<TaskResponse> body = taskService.listMine(clientUserId).stream()
                .map(t -> TaskResponse.from(t, taskService.fetchCategoryName(t.getCategoryId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Detalle de una tarea concreta (el dueño, o cualquier ROLE_ADMIN). Lo usa tambien el microservicio 'documents' para comprobar permisos.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> getOne(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        boolean isAdmin = jwt.getClaimAsStringList("roles") != null
                && jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN");
        Task task = taskService.getOne(id, extractUserId(jwt), isAdmin);
        String categoryName = taskService.fetchCategoryName(task.getCategoryId());
        return ResponseEntity.ok(TaskResponse.from(task, categoryName));
    }

    @Operation(
            summary = "Lista las tareas pendientes de presupuestar, de todos los clientes (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TaskResponse>> pending() {
        List<TaskResponse> body = taskService.listPending().stream()
                .map(t -> TaskResponse.from(t, taskService.fetchCategoryName(t.getCategoryId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Lista todas las tareas, de todos los clientes; opcionalmente filtradas por estado (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TaskResponse>> all(@RequestParam(required = false) TaskStatus status) {
        List<TaskResponse> body = taskService.listAllForAdmin(status).stream()
                .map(t -> TaskResponse.from(t, taskService.fetchCategoryName(t.getCategoryId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Tareas con fecha limite dentro de un rango, para el calendario del admin (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/calendar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TaskResponse>> calendar(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to) {
        List<TaskResponse> body = taskService.listForCalendar(from, to).stream()
                .map(t -> TaskResponse.from(t, taskService.fetchCategoryName(t.getCategoryId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Fija el presupuesto de una tarea PENDIENTE_REVISION (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}/budget", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> budget(@PathVariable Long id, @Valid @RequestBody BudgetTaskRequest req) {
        Task updated = taskService.budget(id, req);
        String categoryName = taskService.fetchCategoryName(updated.getCategoryId());
        return ResponseEntity.ok(TaskResponse.from(updated, categoryName));
    }

    @Operation(
            summary = "Acepta el presupuesto de una tarea propia (solo ROLE_USER, dueño de la tarea)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Task updated = taskService.accept(id, extractUserId(jwt));
        String categoryName = taskService.fetchCategoryName(updated.getCategoryId());
        return ResponseEntity.ok(TaskResponse.from(updated, categoryName));
    }

    @Operation(
            summary = "Rechaza el presupuesto de una tarea propia; vuelve a PENDIENTE_REVISION (solo ROLE_USER, dueño de la tarea)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Task updated = taskService.reject(id, extractUserId(jwt));
        String categoryName = taskService.fetchCategoryName(updated.getCategoryId());
        return ResponseEntity.ok(TaskResponse.from(updated, categoryName));
    }

    @Operation(
            summary = "Edita una tarea propia mientras esta en PENDIENTE_REVISION (solo ROLE_USER, dueño de la tarea)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                               @Valid @RequestBody UpdateTaskRequest req) {
        Task updated = taskService.update(id, extractUserId(jwt), req);
        String categoryName = taskService.fetchCategoryName(updated.getCategoryId());
        return ResponseEntity.ok(TaskResponse.from(updated, categoryName));
    }

    @Operation(
            summary = "Paga una tarea propia ACEPTADA (simulado, solo ROLE_USER, dueño de la tarea)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}/pay", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> pay(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Task updated = taskService.pay(id, extractUserId(jwt));
        String categoryName = taskService.fetchCategoryName(updated.getCategoryId());
        return ResponseEntity.ok(TaskResponse.from(updated, categoryName));
    }

    @Operation(
            summary = "Inicia un pago real con el TPV (Redsys/BBVA, entorno de pruebas). Devuelve los datos firmados para redirigir al cliente a la pasarela (solo ROLE_USER, dueño de la tarea)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(path = "/{id}/pay/redsys/start", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RedsysFormResponse> startRedsysPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        RedsysFormResponse form = taskService.startRedsysPayment(id, extractUserId(jwt));
        return ResponseEntity.ok(form);
    }

    @Operation(
            summary = "Marca una tarea PAGADA como entregada (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}/deliver", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> deliver(@PathVariable Long id) {
        Task updated = taskService.deliver(id);
        String categoryName = taskService.fetchCategoryName(updated.getCategoryId());
        return ResponseEntity.ok(TaskResponse.from(updated, categoryName));
    }

    @Operation(
            summary = "Confirma la recepcion de una tarea ENTREGADA propia, con valoracion opcional (solo ROLE_USER, dueño de la tarea)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}/complete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> complete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                 @Valid @RequestBody(required = false) CompleteTaskRequest req) {
        Task updated = taskService.complete(id, extractUserId(jwt), req);
        String categoryName = taskService.fetchCategoryName(updated.getCategoryId());
        return ResponseEntity.ok(TaskResponse.from(updated, categoryName));
    }

    // El claim "userId" lo pone JwtUtils (en users) como numero; segun el parser puede
    // llegar como Long o Integer, por eso se convierte via Number en vez de castear directo.
    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim == null) {
            throw new IllegalStateException("El token no contiene el claim 'userId'");
        }
        return ((Number) claim).longValue();
    }

    @Operation(
            summary = "Metricas del panel de administracion (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MetricsResponse> metrics() {
        return ResponseEntity.ok(taskService.getMetrics());
    }
}
