package com.igarciamen.categories.controller;

import tools.jackson.databind.ObjectMapper;
import com.igarciamen.categories.repository.TaskCategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskCategoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskCategoryRepository categoryRepo;

    @AfterEach
    void cleanUp() {
        categoryRepo.deleteAll();
    }

    private Long createCategory(String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name, "description", "x"));
        String response = mockMvc.perform(post("/api/categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void listAll_esPublico_devuelveSoloActivas() throws Exception {
        Long id = createCategory("Agenda");

        mockMvc.perform(delete("/api/categories/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listAllForAdmin_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/categories/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listAllForAdmin_conRolUser_devuelve403() throws Exception {
        mockMvc.perform(get("/api/categories/all")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAllForAdmin_incluyeActivasEInactivas() throws Exception {
        Long id = createCategory("Agenda");
        mockMvc.perform(delete("/api/categories/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(get("/api/categories/all")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void getOne_devuelveLaCategoriaAunqueEsteDesactivada() throws Exception {
        Long id = createCategory("Agenda");
        mockMvc.perform(delete("/api/categories/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(get("/api/categories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Agenda"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void create_conCamposOpcionales_devuelve201() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Transcripcion", "description", "Audio a texto",
                "icon", "bi-mic", "colorHex", "#ff9900",
                "basePrice", 15.0, "estimatedMinutes", 30));

        mockMvc.perform(post("/api/categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.icon").value("bi-mic"))
                .andExpect(jsonPath("$.colorHex").value("#ff9900"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivate_conRolAdmin_desactivaYDesapareceDelListadoPublico() throws Exception {
        Long id = createCategory("Agenda");

        mockMvc.perform(delete("/api/categories/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Pero sigue existiendo y siendo consultable por id (no se ha borrado la fila).
        mockMvc.perform(get("/api/categories/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void activate_reactivaUnaCategoriaDesactivada() throws Exception {
        Long id = createCategory("Agenda");
        mockMvc.perform(delete("/api/categories/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(put("/api/categories/" + id + "/activate")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void update_idInexistente_devuelve404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "Agenda", "description", "x"));

        mockMvc.perform(put("/api/categories/999")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_nombreDuplicado_devuelve409() throws Exception {
        createCategory("Agenda");
        String body = objectMapper.writeValueAsString(Map.of("name", "Agenda", "description", "Otra vez"));

        mockMvc.perform(post("/api/categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}
