package com.igarciamen.categories.controller;

import com.igarciamen.categories.model.TaskCategory;
import com.igarciamen.categories.payloads.request.CategoryRequest;
import com.igarciamen.categories.payloads.response.CategoryResponse;
import com.igarciamen.categories.service.TaskCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class TaskCategoryController {

    private final TaskCategoryService categoryService;

    public TaskCategoryController(TaskCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Lista las categorias activas (publico, sin necesidad de token)")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CategoryResponse>> listAll() {
        return ResponseEntity.ok(toResponseList(categoryService.listPublic()));
    }

    @Operation(
            summary = "Lista TODAS las categorias, activas e inactivas (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CategoryResponse>> listAllForAdmin() {
        return ResponseEntity.ok(toResponseList(categoryService.listAllForAdmin()));
    }

    // Publico tambien: lo consulta el frontend y, sobre todo, "tasks" (RestTemplate)
    // para validar la categoria al crear una tarea. Devuelve la categoria aunque
    // este desactivada: una tarea antigua debe poder seguir mostrando su nombre.
    @Operation(summary = "Obtiene una categoria por id (publico, sin necesidad de token)")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.getById(id)));
    }

    @Operation(
            summary = "Crea una categoria (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest req) {
        TaskCategory created = categoryService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(created));
    }

    @Operation(
            summary = "Edita una categoria existente (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        TaskCategory updated = categoryService.update(id, req);
        return ResponseEntity.ok(CategoryResponse.from(updated));
    }

    @Operation(
            summary = "Desactiva una categoria (baja logica, solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<CategoryResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.deactivate(id)));
    }

    @Operation(
            summary = "Reactiva una categoria (solo ROLE_ADMIN)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping(path = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.activate(id)));
    }

    private List<CategoryResponse> toResponseList(List<TaskCategory> categories) {
        return categories.stream().map(CategoryResponse::from).collect(Collectors.toList());
    }
}
