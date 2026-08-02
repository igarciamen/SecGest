package com.igarciamen.notifications.controller;

import tools.jackson.databind.ObjectMapper;
import com.igarciamen.notifications.service.IEmailService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// IEmailService se sustituye por un mock: no se manda ningun correo real durante los tests.
@SpringBootTest
@AutoConfigureMockMvc
class EmailControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IEmailService emailService;

    @Test
    void notificarPresupuesto_sinToken_devuelve401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "destinatario", "cliente@mail.com", "nombreCliente", "Marco",
                "tituloTarea", "Agendar reunion", "nombreCategoria", "Agenda", "precio", 20.0));

        mockMvc.perform(post("/notificar-presupuesto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void notificarPresupuesto_conRolUser_devuelve403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "destinatario", "cliente@mail.com", "nombreCliente", "Marco",
                "tituloTarea", "Agendar reunion", "nombreCategoria", "Agenda", "precio", 20.0));

        mockMvc.perform(post("/notificar-presupuesto")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void notificarPresupuesto_conRolAdmin_llamaAlServicioYDevuelve200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "destinatario", "cliente@mail.com", "nombreCliente", "Marco",
                "tituloTarea", "Agendar reunion", "nombreCategoria", "Agenda", "precio", 20.0));

        mockMvc.perform(post("/notificar-presupuesto")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(emailService).enviarPresupuesto(Mockito.any());
    }

    @Test
    void enviarCorreo_conRolAdmin_llamaAlServicio() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "destinatario", "cliente@mail.com", "asunto", "Prueba", "mensaje", "Hola"));

        mockMvc.perform(post("/enviar-correo")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(emailService).enviarCorreo(Mockito.any());
    }
}
