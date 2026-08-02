package com.igarciamen.messages.service;

import com.igarciamen.messages.dto.InternalNoteDto;
import com.igarciamen.messages.model.InternalNote;
import com.igarciamen.messages.repository.InternalNoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotesServiceTest {

    @Mock
    private InternalNoteRepository noteRepo;

    @Mock
    private TaskClient taskClient;

    @InjectMocks
    private NotesService service;

    @Test
    void listNotes_comprueboAccesoYDevuelveLasNotas() {
        InternalNote note = new InternalNote(10L, 1L, "Cliente prefiere videollamada");
        when(noteRepo.findByTaskIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(note));

        List<InternalNoteDto> result = service.listNotes(10L);

        assertEquals(1, result.size());
        verify(taskClient).verifyAccessOrThrow(10L);
    }

    @Test
    void addNote_guardaLaNotaConElAutor() {
        when(noteRepo.save(any(InternalNote.class))).thenAnswer(inv -> inv.getArgument(0));

        InternalNoteDto result = service.addNote(10L, 1L, "No llamar antes de las 10h");

        assertEquals(1L, result.authorUserId);
        assertEquals("No llamar antes de las 10h", result.content);
        verify(taskClient).verifyAccessOrThrow(10L);
    }

    @Test
    void addNote_rechazaContenidoVacio() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.addNote(10L, 1L, ""));

        assertEquals(400, ex.getStatusCode().value());
        verify(noteRepo, never()).save(any());
    }
}
