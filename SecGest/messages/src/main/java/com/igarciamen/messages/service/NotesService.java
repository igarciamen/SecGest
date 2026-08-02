package com.igarciamen.messages.service;

import com.igarciamen.messages.dto.InternalNoteDto;
import com.igarciamen.messages.model.InternalNote;
import com.igarciamen.messages.repository.InternalNoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Solo el admin llega hasta aqui (lo exige SecurityConfig), pero se comprueba
// igualmente el acceso a la tarea via TaskClient, por consistencia con el
// resto del proyecto y como segunda capa de defensa.
@Service
public class NotesService {

    private final InternalNoteRepository noteRepo;
    private final TaskClient taskClient;

    public NotesService(InternalNoteRepository noteRepo, TaskClient taskClient) {
        this.noteRepo = noteRepo;
        this.taskClient = taskClient;
    }

    public List<InternalNoteDto> listNotes(Long taskId) {
        taskClient.verifyAccessOrThrow(taskId);
        return noteRepo.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(InternalNoteDto::from).toList();
    }

    public InternalNoteDto addNote(Long taskId, Long authorUserId, String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nota no puede estar vacia");
        }
        taskClient.verifyAccessOrThrow(taskId);
        InternalNote note = new InternalNote(taskId, authorUserId, content.trim());
        return InternalNoteDto.from(noteRepo.save(note));
    }
}
