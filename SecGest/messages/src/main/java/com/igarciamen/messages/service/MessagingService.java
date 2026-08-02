package com.igarciamen.messages.service;

import com.igarciamen.messages.dto.MessageDto;
import com.igarciamen.messages.dto.ThreadDto;
import com.igarciamen.messages.model.Conversation;
import com.igarciamen.messages.model.Message;
import com.igarciamen.messages.repository.ConversationRepository;
import com.igarciamen.messages.repository.MessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class MessagingService {

    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final TaskClient taskClient;

    public MessagingService(ConversationRepository convRepo, MessageRepository msgRepo, TaskClient taskClient) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.taskClient = taskClient;
    }

    // No hay "iniciar conversacion" explicito: se crea sola, la primera vez que
    // hace falta (al leer el hilo por primera vez, o al mandar el primer mensaje).
    private Conversation getOrCreateConversation(Long taskId) {
        return convRepo.findByTaskId(taskId)
                .orElseGet(() -> convRepo.save(new Conversation(taskId)));
    }

    public ThreadDto getThread(Long taskId, Long requesterId) {
        taskClient.verifyAccessOrThrow(taskId);
        Conversation conversation = getOrCreateConversation(taskId);

        List<MessageDto> messages = msgRepo.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream().map(MessageDto::from).toList();

        return new ThreadDto(taskId, messages);
    }

    public MessageDto sendMessage(Long taskId, Long senderId, String senderRole, String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje no puede estar vacio");
        }
        taskClient.verifyAccessOrThrow(taskId);
        Conversation conversation = getOrCreateConversation(taskId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setContent(content.trim());
        message = msgRepo.save(message);

        conversation.setLastMessageAt(LocalDateTime.now());

        return MessageDto.from(message);
    }

    // Solo quien mando el mensaje puede borrarlo (ni siquiera el admin puede
    // borrar un mensaje del cliente, ni al reves).
    public void deleteMessage(Long taskId, Long messageId, Long requesterId) {
        taskClient.verifyAccessOrThrow(taskId);
        Conversation conversation = convRepo.findByTaskId(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay conversacion para la tarea " + taskId));

        Message message = msgRepo.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + messageId));

        if (!Objects.equals(message.getConversation().getId(), conversation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + messageId);
        }
        if (!Objects.equals(message.getSenderId(), requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes borrar tus propios mensajes");
        }

        msgRepo.delete(message);
    }

    public int markAsRead(Long taskId, Long requesterId) {
        taskClient.verifyAccessOrThrow(taskId);
        Conversation conversation = convRepo.findByTaskId(taskId).orElse(null);
        if (conversation == null) {
            return 0;
        }
        return msgRepo.markAsRead(conversation.getId(), requesterId, LocalDateTime.now());
    }

    public long unreadCount(Long taskId, Long requesterId) {
        taskClient.verifyAccessOrThrow(taskId);
        Conversation conversation = convRepo.findByTaskId(taskId).orElse(null);
        if (conversation == null) {
            return 0;
        }
        return msgRepo.countUnread(conversation.getId(), requesterId);
    }
}
