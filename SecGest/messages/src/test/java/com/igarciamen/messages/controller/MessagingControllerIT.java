package com.igarciamen.messages.controller;

import tools.jackson.databind.ObjectMapper;
import com.igarciamen.messages.repository.ConversationRepository;
import com.igarciamen.messages.repository.MessageRepository;
import com.igarciamen.messages.service.TaskClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// TaskClient se sustituye por un mock: "tasks" no esta levantado durante los tests.
@SpringBootTest
@AutoConfigureMockMvc
class MessagingControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationRepository convRepo;

    @Autowired
    private MessageRepository msgRepo;

    @MockitoBean
    private TaskClient taskClient;

    @BeforeEach
    void setUp() {
        doNothing().when(taskClient).verifyAccessOrThrow(10L);
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(taskClient).verifyAccessOrThrow(999L);
    }

    @AfterEach
    void cleanUp() {
        msgRepo.deleteAll();
        convRepo.deleteAll();
    }

    @Test
    void thread_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/messages/tasks/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void thread_sinAccesoALaTarea_devuelve403() throws Exception {
        mockMvc.perform(get("/api/messages/tasks/999")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isForbidden());
    }

    @Test
    void thread_conAcceso_creaLaConversacionVacia() throws Exception {
        mockMvc.perform(get("/api/messages/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(10))
                .andExpect(jsonPath("$.messages.length()").value(0));
    }

    @Test
    void send_conRolUser_creaElMensaje() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "Hola, ¿va todo bien con el encargo?"));

        mockMvc.perform(post("/api/messages/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderRole").value("ROLE_USER"))
                .andExpect(jsonPath("$.content").value("Hola, ¿va todo bien con el encargo?"));
    }

    @Test
    void send_conRolAdmin_creaElMensajeConEseRol() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "Todo en orden, gracias"));

        mockMvc.perform(post("/api/messages/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", 1).claim("roles", java.util.List.of("ROLE_ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderRole").value("ROLE_ADMIN"));
    }

    @Test
    void send_contenidoVacio_devuelve400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "   "));

        mockMvc.perform(post("/api/messages/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_elPropioRemitentePuedeBorrarlo() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "Mensaje de prueba"));
        String response = mockMvc.perform(post("/api/messages/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        Long messageId = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(delete("/api/messages/tasks/10/" + messageId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_otroUsuario_devuelve403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "Mensaje de prueba"));
        String response = mockMvc.perform(post("/api/messages/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        Long messageId = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(delete("/api/messages/tasks/10/" + messageId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", 1).claim("roles", java.util.List.of("ROLE_ADMIN")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void markAsRead_conMensajesDelOtroLado_losMarca() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "Mensaje del admin"));
        mockMvc.perform(post("/api/messages/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", 1).claim("roles", java.util.List.of("ROLE_ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));

        mockMvc.perform(put("/api/messages/tasks/10/read")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1));

        mockMvc.perform(get("/api/messages/tasks/10/unread-count")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }
}
