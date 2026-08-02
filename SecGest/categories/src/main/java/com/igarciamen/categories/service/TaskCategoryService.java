package com.igarciamen.categories.service;

import com.igarciamen.categories.model.TaskCategory;
import com.igarciamen.categories.payloads.request.CategoryRequest;
import com.igarciamen.categories.repository.TaskCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskCategoryService {

    private final TaskCategoryRepository categoryRepo;

    public TaskCategoryService(TaskCategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    // Catalogo publico: solo categorias activas.
    public List<TaskCategory> listPublic() {
        return categoryRepo.findAllByActiveTrueOrderByNameAsc();
    }

    // Panel del admin: todas, activas e inactivas.
    public List<TaskCategory> listAllForAdmin() {
        return categoryRepo.findAllByOrderByNameAsc();
    }

    public TaskCategory getById(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found: " + id));
    }

    public TaskCategory create(CategoryRequest req) {
        if (categoryRepo.existsByNameIgnoreCase(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Category already exists: " + req.getName());
        }
        TaskCategory category = new TaskCategory(req.getName(), req.getDescription());
        applyOptionalFields(category, req);
        category.setActive(true);
        return categoryRepo.save(category);
    }

    public TaskCategory update(Long id, CategoryRequest req) {
        TaskCategory category = getById(id);
        category.setName(req.getName());
        category.setDescription(req.getDescription());
        applyOptionalFields(category, req);
        return categoryRepo.save(category);
    }

    // Baja logica: no se borra la fila, para que las tareas que ya referencian
    // esta categoria (por id) sigan pudiendo resolver su nombre via CategoryClient.
    public TaskCategory deactivate(Long id) {
        TaskCategory category = getById(id);
        category.setActive(false);
        return categoryRepo.save(category);
    }

    public TaskCategory activate(Long id) {
        TaskCategory category = getById(id);
        category.setActive(true);
        return categoryRepo.save(category);
    }

    private void applyOptionalFields(TaskCategory category, CategoryRequest req) {
        category.setIcon(req.getIcon());
        category.setColorHex(req.getColorHex());
        category.setImageUrl(req.getImageUrl());
        category.setBasePrice(req.getBasePrice());
        category.setEstimatedMinutes(req.getEstimatedMinutes());
    }
}
