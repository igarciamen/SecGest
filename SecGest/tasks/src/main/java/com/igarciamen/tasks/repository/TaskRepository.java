package com.igarciamen.tasks.repository;

import com.igarciamen.tasks.enums.TaskStatus;
import com.igarciamen.tasks.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByClientUserIdOrderByCreatedAtDesc(Long clientUserId);

    // Panel del admin: todas las tareas de todos los clientes en un estado dado
    // (de momento, PENDIENTE_REVISION para presupuestar).
    List<Task> findByStatusOrderByCreatedAtAsc(TaskStatus status);

    // Vista general del admin (Bloque 7): todas las tareas, o filtradas por estado
    // si se indica uno (ej. ACEPTADA, pendientes de cobro).
    List<Task> findAllByOrderByCreatedAtDesc();

    List<Task> findByStatusOrderByCreatedAtDesc(TaskStatus status);
    List<Task> findByDueDateBetweenOrderByDueDateAsc(java.time.LocalDateTime from, java.time.LocalDateTime to);
}
