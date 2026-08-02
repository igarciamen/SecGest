package com.igarciamen.documents.controller;

import com.igarciamen.documents.service.TaskClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// TaskClient se sustituye por un mock: "tasks" no esta levantado durante los
// tests. El almacenamiento SI es real (una carpeta bajo target/, que se limpia
// despues de cada test), para probar tambien la escritura/lectura en disco.
@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskClient taskClient;

    @Autowired
    private com.igarciamen.documents.repository.DocumentRepository documentRepo;


    @AfterEach
    void cleanUp() throws Exception {
        documentRepo.deleteAll();

        Path dir = Path.of("./target/test-uploads");
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> p.toFile().delete());
        }
    }

    @Test
    void upload_sinToken_devuelve401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "contenido".getBytes());

        mockMvc.perform(multipart("/api/documents/tasks/10").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_conAccesoConcedido_devuelve201() throws Exception {
        doNothing().when(taskClient).verifyAccessOrThrow(10L);
        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", "contenido de prueba".getBytes());

        mockMvc.perform(multipart("/api/documents/tasks/10").file(file)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("contrato.pdf"))
                .andExpect(jsonPath("$.uploaderRole").value("ROLE_USER"));
    }

    @Test
    void upload_sinAccesoALaTarea_devuelve403() throws Exception {
        doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(taskClient).verifyAccessOrThrow(10L);
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

        mockMvc.perform(multipart("/api/documents/tasks/10").file(file)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 999))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAndDownload_flujoCompleto() throws Exception {
        doNothing().when(taskClient).verifyAccessOrThrow(10L);
        MockMultipartFile file = new MockMultipartFile("file", "entregable.txt", "text/plain", "hola mundo".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/documents/tasks/10").file(file)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("userId", 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long documentId = ((Number) com.jayway.jsonpath.JsonPath.read(uploadResponse, "$.id")).longValue();

        mockMvc.perform(get("/api/documents/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].originalFilename").value("entregable.txt"));

        mockMvc.perform(get("/api/documents/" + documentId + "/download")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("entregable.txt")));
    }

    @Test
    void download_documentoInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/documents/999/download")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_elPropioSubidorPuedeBorrarlo() throws Exception {
        doNothing().when(taskClient).verifyAccessOrThrow(10L);
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/documents/tasks/10").file(file)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(j -> j.claim("userId", 5).claim("roles", java.util.List.of("ROLE_USER")))))
                .andReturn().getResponse().getContentAsString();
        Long documentId = ((Number) com.jayway.jsonpath.JsonPath.read(uploadResponse, "$.id")).longValue();

        mockMvc.perform(delete("/api/documents/" + documentId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(j -> j.claim("userId", 5).claim("roles", java.util.List.of("ROLE_USER")))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/documents/tasks/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void delete_otroClienteQueNoLoSubio_devuelve403() throws Exception {
        doNothing().when(taskClient).verifyAccessOrThrow(10L);
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/documents/tasks/10").file(file)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(j -> j.claim("userId", 5).claim("roles", java.util.List.of("ROLE_USER")))))
                .andReturn().getResponse().getContentAsString();
        Long documentId = ((Number) com.jayway.jsonpath.JsonPath.read(uploadResponse, "$.id")).longValue();

        mockMvc.perform(delete("/api/documents/" + documentId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(j -> j.claim("userId", 999).claim("roles", java.util.List.of("ROLE_USER")))))
                .andExpect(status().isForbidden());
    }
}
