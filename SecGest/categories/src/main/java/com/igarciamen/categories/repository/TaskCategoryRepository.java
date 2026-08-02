package com.igarciamen.categories.repository;

import com.igarciamen.categories.model.TaskCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskCategoryRepository extends JpaRepository<TaskCategory, Long> {
    boolean existsByNameIgnoreCase(String name);

    // Listado publico: solo categorias activas (catalogo de cara al cliente).
    List<TaskCategory> findAllByActiveTrueOrderByNameAsc();

    // Listado del admin: todas, activas e inactivas, para poder gestionarlas.
    List<TaskCategory> findAllByOrderByNameAsc();
}
