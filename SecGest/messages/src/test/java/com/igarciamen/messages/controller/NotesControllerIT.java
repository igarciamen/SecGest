package com.igarciamen.messages.controller;

import tools.jackson.databind.ObjectMapper;
import com.igarciamen.messages.repository.InternalNoteRepository;
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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotesControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InternalNoteRepository noteRepo;

    @MockitoBean
    private TaskClient taskClient;

    @BeforeEach
    void setUp() {
        doNothing().when(taskClient).verifyAccessOrThrow(10L);
    }

    @AfterEach
    void cleanUp() {
        noteRepo.deleteAll();
    }

    @Test
    void list_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/notes/tasks/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_conRolUser_devuelve403() throws Exception {
        mockMvc.perform(get("/api/notes/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void addAndList_conRolAdmin_funcionaCorrectamente() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", "Cliente prefiere videollamada"));

        mockMvc.perform(post("/api/notes/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", 1).claim("roles", List.of("ROLE_ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorUserId").value(1))
                .andExpect(jsonPath("$.content").value("Cliente prefiere videollamada"));

        mockMvc.perform(get("/api/notes/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void add_contenidoVacio_devuelve400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("content", ""));

        mockMvc.perform(post("/api/notes/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", 1).claim("roles", List.of("ROLE_ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
