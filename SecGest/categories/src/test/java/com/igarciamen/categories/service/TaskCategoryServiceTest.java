package com.igarciamen.categories.service;

import com.igarciamen.categories.model.TaskCategory;
import com.igarciamen.categories.payloads.request.CategoryRequest;
import com.igarciamen.categories.repository.TaskCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskCategoryServiceTest {

    @Mock
    private TaskCategoryRepository categoryRepo;

    @InjectMocks
    private TaskCategoryService categoryService;

    @Test
    void listPublic_devuelveSoloActivas() {
        List<TaskCategory> categorias = List.of(new TaskCategory("Agenda", "x"));
        when(categoryRepo.findAllByActiveTrueOrderByNameAsc()).thenReturn(categorias);

        List<TaskCategory> result = categoryService.listPublic();

        assertEquals(1, result.size());
    }

    @Test
    void listAllForAdmin_devuelveActivasEInactivas() {
        TaskCategory activa = new TaskCategory("Agenda", "x");
        TaskCategory inactiva = new TaskCategory("Vieja", "y");
        inactiva.setActive(false);
        when(categoryRepo.findAllByOrderByNameAsc()).thenReturn(List.of(activa, inactiva));

        List<TaskCategory> result = categoryService.listAllForAdmin();

        assertEquals(2, result.size());
    }

    @Test
    void getById_lanza404SiNoExiste() {
        when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.getById(99L));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void create_creaCategoriaActivaConCamposOpcionales() {
        CategoryRequest req = new CategoryRequest("Transcripcion", "Audio a texto");
        req.setIcon("bi-mic");
        req.setColorHex("#ff9900");
        req.setBasePrice(new BigDecimal("15.00"));
        req.setEstimatedMinutes(30);

        when(categoryRepo.existsByNameIgnoreCase("Transcripcion")).thenReturn(false);
        when(categoryRepo.save(any(TaskCategory.class))).thenAnswer(inv -> {
            TaskCategory c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        TaskCategory result = categoryService.create(req);

        assertEquals(10L, result.getId());
        assertTrue(result.isActive());
        assertEquals("bi-mic", result.getIcon());
        assertEquals("#ff9900", result.getColorHex());
        assertEquals(new BigDecimal("15.00"), result.getBasePrice());
        assertEquals(30, result.getEstimatedMinutes());
    }

    @Test
    void create_lanza409SiElNombreYaExiste() {
        CategoryRequest req = new CategoryRequest("Agenda", "Duplicada");
        when(categoryRepo.existsByNameIgnoreCase("Agenda")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.create(req));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void update_actualizaCategoriaExistenteSinTocarActive() {
        TaskCategory existente = new TaskCategory("Agenda", "Vieja descripcion");
        existente.setId(5L);
        existente.setActive(true);
        CategoryRequest req = new CategoryRequest("Agenda", "Nueva descripcion");

        when(categoryRepo.findById(5L)).thenReturn(Optional.of(existente));
        when(categoryRepo.save(any(TaskCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskCategory result = categoryService.update(5L, req);

        assertEquals("Nueva descripcion", result.getDescription());
        assertTrue(result.isActive());
    }

    @Test
    void deactivate_poneActiveAFalseSinBorrarLaFila() {
        TaskCategory existente = new TaskCategory("Agenda", "x");
        existente.setId(5L);
        existente.setActive(true);

        when(categoryRepo.findById(5L)).thenReturn(Optional.of(existente));
        when(categoryRepo.save(any(TaskCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskCategory result = categoryService.deactivate(5L);

        assertFalse(result.isActive());
    }

    @Test
    void activate_poneActiveATrue() {
        TaskCategory existente = new TaskCategory("Agenda", "x");
        existente.setId(5L);
        existente.setActive(false);

        when(categoryRepo.findById(5L)).thenReturn(Optional.of(existente));
        when(categoryRepo.save(any(TaskCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskCategory result = categoryService.activate(5L);

        assertTrue(result.isActive());
    }
}
