package com.igarciamen.messages.service;

import com.igarciamen.messages.dto.MessageDto;
import com.igarciamen.messages.dto.ThreadDto;
import com.igarciamen.messages.model.Conversation;
import com.igarciamen.messages.model.Message;
import com.igarciamen.messages.repository.ConversationRepository;
import com.igarciamen.messages.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

    @Mock
    private ConversationRepository convRepo;

    @Mock
    private MessageRepository msgRepo;

    @Mock
    private TaskClient taskClient;

    @InjectMocks
    private MessagingService service;

    private Conversation conversation(long id, long taskId) {
        Conversation c = new Conversation(taskId);
        // el id lo pone Hibernate normalmente; para el test lo forzamos via el mock de save
        return c;
    }

    @Test
    void getThread_creaLaConversacionLaPrimeraVezYDevuelveVacio() {
        when(convRepo.findByTaskId(10L)).thenReturn(Optional.empty());
        Conversation created = conversation(1, 10L);
        when(convRepo.save(any(Conversation.class))).thenReturn(created);
        when(msgRepo.findByConversationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        ThreadDto result = service.getThread(10L, 5L);

        assertEquals(10L, result.taskId);
        assertTrue(result.messages.isEmpty());
        verify(taskClient).verifyAccessOrThrow(10L);
        verify(convRepo).save(any(Conversation.class));
    }

    @Test
    void getThread_reutilizaLaConversacionExistente() {
        Conversation existing = conversation(1, 10L);
        when(convRepo.findByTaskId(10L)).thenReturn(Optional.of(existing));
        when(msgRepo.findByConversationIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        service.getThread(10L, 5L);

        verify(convRepo, never()).save(any());
    }

    @Test
    void sendMessage_guardaElMensajeConElRolDelRemitente() {
        Conversation existing = conversation(1, 10L);
        when(convRepo.findByTaskId(10L)).thenReturn(Optional.of(existing));
        when(msgRepo.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageDto result = service.sendMessage(10L, 5L, "ROLE_USER", "Hola, ¿va todo bien?");

        assertEquals(5L, result.senderId);
        assertEquals("ROLE_USER", result.senderRole);
        assertEquals("Hola, ¿va todo bien?", result.content);
        verify(taskClient).verifyAccessOrThrow(10L);
    }

    @Test
    void sendMessage_rechazaContenidoVacio() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.sendMessage(10L, 5L, "ROLE_USER", "   "));

        assertEquals(400, ex.getStatusCode().value());
        verify(msgRepo, never()).save(any());
    }

    @Test
    void deleteMessage_elPropioRemitentePuedeBorrarlo() {
        Conversation existing = conversation(1, 10L);
        when(convRepo.findByTaskId(10L)).thenReturn(Optional.of(existing));

        Message message = new Message();
        message.setId(3L);
        message.setSenderId(5L);
        message.setConversation(existing);
        when(msgRepo.findById(3L)).thenReturn(Optional.of(message));

        service.deleteMessage(10L, 3L, 5L);

        verify(msgRepo).delete(message);
    }

    @Test
    void deleteMessage_otroUsuarioNoPuedeBorrarlo() {
        Conversation existing = conversation(1, 10L);
        when(convRepo.findByTaskId(10L)).thenReturn(Optional.of(existing));

        Message message = new Message();
        message.setId(3L);
        message.setSenderId(5L);
        message.setConversation(existing);
        when(msgRepo.findById(3L)).thenReturn(Optional.of(message));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteMessage(10L, 3L, 999L));

        assertEquals(403, ex.getStatusCode().value());
        verify(msgRepo, never()).delete(any(Message.class));
    }

    @Test
    void markAsRead_sinConversacionDevuelveCero() {
        when(convRepo.findByTaskId(10L)).thenReturn(Optional.empty());

        int updated = service.markAsRead(10L, 5L);

        assertEquals(0, updated);
        verify(msgRepo, never()).markAsRead(any(), any(), any());
    }

    @Test
    void unreadCount_delegaEnElRepositorio() {
        Conversation existing = conversation(1, 10L);
        when(convRepo.findByTaskId(10L)).thenReturn(Optional.of(existing));
        when(msgRepo.countUnread(any(), eq(5L))).thenReturn(3L);

        long count = service.unreadCount(10L, 5L);

        assertEquals(3L, count);
    }
}
