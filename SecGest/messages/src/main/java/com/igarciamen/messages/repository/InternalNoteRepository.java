package com.igarciamen.messages.repository;

import com.igarciamen.messages.model.InternalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InternalNoteRepository extends JpaRepository<InternalNote, Long> {
    List<InternalNote> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}
