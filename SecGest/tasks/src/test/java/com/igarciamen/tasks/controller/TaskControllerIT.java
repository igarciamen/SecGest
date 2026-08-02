package com.igarciamen.tasks.controller;

import tools.jackson.databind.ObjectMapper;
import com.igarciamen.tasks.repository.TaskRepository;
import com.igarciamen.tasks.service.CategoryClient;
import com.igarciamen.tasks.service.EmailClient;
import com.igarciamen.tasks.service.UserClient;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// CategoryClient/UserClient/EmailClient se sustituyen por mocks (@MockitoBean): ni
// "categories", ni "users", ni "notifications" estan levantados durante los tests.
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private com.igarciamen.tasks.repository.PaymentRepository paymentRepo;

    @MockitoBean
    private CategoryClient categoryClient;

    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private EmailClient emailClient;

    @BeforeEach
    void setUp() {
        when(categoryClient.fetchCategoryOrThrow(eq(1L)))
                .thenReturn(Map.of("id", 1, "name", "Agenda"));
        when(categoryClient.fetchCategoryOrThrow(eq(99L)))
                .thenThrow(new EntityNotFoundException("Category not found: 99"));
        when(userClient.fetchUserOrThrow(eq(5L)))
                .thenReturn(Map.of("id", 5, "username", "marco", "email", "marco@mail.com"));
    }

    @AfterEach
    void cleanUp() {
        paymentRepo.deleteAll();
        taskRepo.deleteAll();
    }

    @Test
    void create_sinToken_devuelve401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("categoryId", 1, "title", "Agendar reunion"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_conRolAdmin_devuelve403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("categoryId", 1, "title", "Agendar reunion"));

        mockMvc.perform(post("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", 1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_conTokenValido_devuelve201() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("categoryId", 1, "title", "Agendar reunion", "description", "Con el proveedor"));

        mockMvc.perform(post("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Agendar reunion"))
                .andExpect(jsonPath("$.categoryName").value("Agenda"))
                .andExpect(jsonPath("$.status").value("PENDIENTE_REVISION"));
    }

    @Test
    void create_categoriaInexistente_devuelve404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("categoryId", 99, "title", "Da igual"));

        mockMvc.perform(post("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void mine_devuelveSoloLasTareasDelUsuarioDelToken() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("categoryId", 1, "title", "Tarea de Marco"));

        mockMvc.perform(post("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 8)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/mine")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Tarea de Marco"));
    }

    @Test
    void pending_sinRolAdmin_devuelve403() throws Exception {
        mockMvc.perform(get("/api/tasks/pending")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void pending_conRolAdmin_devuelveTareasDeTodosLosClientes() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("categoryId", 1, "title", "Tarea pendiente"));
        mockMvc.perform(post("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/pending")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void budget_conRolAdmin_presupuestaYEnviaCorreo() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of("categoryId", 1, "title", "Agendar reunion"));
        String createResponse = mockMvc.perform(post("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(createResponse).get("id").asLong();

        String budgetBody = objectMapper.writeValueAsString(Map.of("price", 20.0));

        mockMvc.perform(put("/api/tasks/" + taskId + "/budget")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRESUPUESTADA"))
                .andExpect(jsonPath("$.price").value(20.0));

        verify(emailClient).sendBudgetEmail(eq("marco@mail.com"), eq("marco"), eq("Agendar reunion"), eq("Agenda"), any());
    }

    @Test
    void budget_conRolUser_devuelve403() throws Exception {
        String budgetBody = objectMapper.writeValueAsString(Map.of("price", 20.0));

        mockMvc.perform(put("/api/tasks/999/budget")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void budget_idInexistente_devuelve404() throws Exception {
        String budgetBody = objectMapper.writeValueAsString(Map.of("price", 20.0));

        mockMvc.perform(put("/api/tasks/999/budget")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetBody))
                .andExpect(status().isNotFound());
    }

    private Long createAndBudgetTask() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of("categoryId", 1, "title", "Agendar reunion"));
        String createResponse = mockMvc.perform(post("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/tasks/" + taskId + "/budget")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("price", 20.0))))
                .andExpect(status().isOk());

        return taskId;
    }

    @Test
    void accept_conElDuenoDeLaTarea_devuelve200YEnviaCorreo() throws Exception {
        Long taskId = createAndBudgetTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/accept")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEPTADA"));

        verify(emailClient).sendGenericEmail(eq("marco@mail.com"), any(), any());
    }

    @Test
    void accept_conOtroUsuario_devuelve403() throws Exception {
        Long taskId = createAndBudgetTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/accept")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 8))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reject_vuelveAPendienteRevisionYPermiteReenviar() throws Exception {
        Long taskId = createAndBudgetTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/reject")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.price").doesNotExist());

        String editBody = objectMapper.writeValueAsString(Map.of("categoryId", 1, "title", "Titulo editado"));
        mockMvc.perform(put("/api/tasks/" + taskId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Titulo editado"));
    }

    @Test
    void update_conTareaYaPresupuestada_devuelve409() throws Exception {
        Long taskId = createAndBudgetTask();

        String editBody = objectMapper.writeValueAsString(Map.of("categoryId", 1, "title", "Da igual"));
        mockMvc.perform(put("/api/tasks/" + taskId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editBody))
                .andExpect(status().isConflict());
    }

    @Test
    void accept_conRolAdmin_devuelve403() throws Exception {
        Long taskId = createAndBudgetTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/accept")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    private Long createBudgetAndAcceptTask() throws Exception {
        Long taskId = createAndBudgetTask();
        mockMvc.perform(put("/api/tasks/" + taskId + "/accept")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk());
        return taskId;
    }

    @Test
    void all_sinRolAdmin_devuelve403() throws Exception {
        mockMvc.perform(get("/api/tasks/all")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void all_sinFiltro_devuelveTareasDeCualquierCliente() throws Exception {
        createAndBudgetTask();

        mockMvc.perform(get("/api/tasks/all")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void all_conFiltroDeEstado_devuelveSoloEsasTareas() throws Exception {
        Long acceptedId = createBudgetAndAcceptTask();

        mockMvc.perform(get("/api/tasks/all").param("status", "ACEPTADA")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(acceptedId));
    }

    @Test
    void pay_conElDuenoDeLaTarea_devuelve200YEnviaRecibo() throws Exception {
        Long taskId = createBudgetAndAcceptTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/pay")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGADA"));

        verify(emailClient, org.mockito.Mockito.times(2)).sendGenericEmail(eq("marco@mail.com"), any(), any());
    }

    @Test
    void pay_conOtroUsuario_devuelve403() throws Exception {
        Long taskId = createBudgetAndAcceptTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/pay")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 8))))
                .andExpect(status().isForbidden());
    }

    @Test
    void pay_conTareaNoAceptada_devuelve409() throws Exception {
        Long taskId = createAndBudgetTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/pay")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isConflict());
    }

    @Test
    void startRedsysPayment_conElDuenoDeLaTarea_devuelveDatosFirmados() throws Exception {
        Long taskId = createBudgetAndAcceptTask();

        mockMvc.perform(post("/api/tasks/" + taskId + "/pay/redsys/start")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionUrl").value(org.hamcrest.Matchers.containsString("redsys.es")))
                .andExpect(jsonPath("$.dsSignatureVersion").value("HMAC_SHA256_V1"))
                .andExpect(jsonPath("$.dsMerchantParameters").exists())
                .andExpect(jsonPath("$.dsSignature").exists());
    }

    @Test
    void startRedsysPayment_sinToken_devuelve401() throws Exception {
        Long taskId = createBudgetAndAcceptTask();

        mockMvc.perform(post("/api/tasks/" + taskId + "/pay/redsys/start"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void redsysNotify_sinFirmaValida_devuelve403YNoCambiaLaTarea() throws Exception {
        Long taskId = createBudgetAndAcceptTask();

        mockMvc.perform(post("/api/tasks/" + taskId + "/pay/redsys/start")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/payments/redsys/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("Ds_SignatureVersion", "HMAC_SHA256_V1")
                        .param("Ds_MerchantParameters", "eyJEU19PUkRFUiI6IjAwMDEifQ==")
                        .param("Ds_Signature", "firma-completamente-inventada"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOne_elDuenoPuedeVerla() throws Exception {
        Long taskId = createAndBudgetTask();

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId));
    }

    // In TaskControllerIT.java, update the failing test:
    @Test
    void getOne_elAdminPuedeVerCualquiera() throws Exception {
        Long taskId = createAndBudgetTask();

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.claim("userId", 999)
                                        .claim("roles", List.of("ROLE_ADMIN")))))
                .andExpect(status().isOk());
    }

    @Test
    void getOne_otroClienteNoAdmin_devuelve403() throws Exception {
        Long taskId = createAndBudgetTask();

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 999))))
                .andExpect(status().isForbidden());
    }

    private Long createBudgetAcceptAndPayTask() throws Exception {
        Long taskId = createBudgetAndAcceptTask();
        mockMvc.perform(put("/api/tasks/" + taskId + "/pay")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk());
        return taskId;
    }

    @Test
    void deliver_conRolAdmin_marcaComoEntregadaYEnviaCorreo() throws Exception {
        Long taskId = createBudgetAcceptAndPayTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/deliver")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGADA"));

        verify(emailClient, org.mockito.Mockito.atLeastOnce()).sendGenericEmail(eq("marco@mail.com"), any(), any());
    }

    @Test
    void deliver_conRolUser_devuelve403() throws Exception {
        Long taskId = createBudgetAcceptAndPayTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/deliver")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deliver_tareaNoPagada_devuelve409() throws Exception {
        Long taskId = createBudgetAndAcceptTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/deliver")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());
    }

    @Test
    void complete_conValoracion_devuelve200YGuardaLaValoracion() throws Exception {
        Long taskId = createBudgetAcceptAndPayTask();
        mockMvc.perform(put("/api/tasks/" + taskId + "/deliver")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        String body = objectMapper.writeValueAsString(Map.of("rating", 5, "ratingComment", "Perfecto"));

        mockMvc.perform(put("/api/tasks/" + taskId + "/complete")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETADA"))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void complete_sinBody_confirmaSinValorar() throws Exception {
        Long taskId = createBudgetAcceptAndPayTask();
        mockMvc.perform(put("/api/tasks/" + taskId + "/deliver")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(put("/api/tasks/" + taskId + "/complete")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETADA"));
    }

    @Test
    void complete_conOtroUsuario_devuelve403() throws Exception {
        Long taskId = createBudgetAcceptAndPayTask();
        mockMvc.perform(put("/api/tasks/" + taskId + "/deliver")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(put("/api/tasks/" + taskId + "/complete")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 8))))
                .andExpect(status().isForbidden());
    }

    @Test
    void complete_tareaNoEntregada_devuelve409() throws Exception {
        Long taskId = createBudgetAcceptAndPayTask();

        mockMvc.perform(put("/api/tasks/" + taskId + "/complete")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5))))
                .andExpect(status().isConflict());
    }


    @Test
    void calendar_sinRolAdmin_devuelve403() throws Exception {
        mockMvc.perform(get("/api/tasks/calendar")
                        .param("from", "2026-07-01T00:00:00")
                        .param("to", "2026-07-31T23:59:59")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void calendar_conRolAdmin_devuelveTareasConDueDateEnElRango() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "categoryId", 1, "title", "Tarea con fecha",
                "dueDate", "2026-07-15T10:00:00"));
        mockMvc.perform(post("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/calendar")
                        .param("from", "2026-07-01T00:00:00")
                        .param("to", "2026-07-31T23:59:59")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dueDate").value("2026-07-15T10:00:00"));
    }

    @Test
    void calendar_fueraDelRango_noApareceLaTarea() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "categoryId", 1, "title", "Tarea de agosto",
                "dueDate", "2026-08-05T10:00:00"));
        mockMvc.perform(post("/api/tasks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(j -> j.claim("userId", 5)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/calendar")
                        .param("from", "2026-07-01T00:00:00")
                        .param("to", "2026-07-31T23:59:59")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void metrics_sinRolAdmin_devuelve403() throws Exception {
        mockMvc.perform(get("/api/tasks/metrics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void metrics_conRolAdmin_devuelveDatosCorrectos() throws Exception {
        Long taskId = createBudgetAcceptAndPayTask(); // ya en PAGADA, precio 20.0

        mockMvc.perform(get("/api/tasks/metrics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasksByStatus.PAGADA").value(1))
                .andExpect(jsonPath("$.totalRevenue").value(20.0))
                .andExpect(jsonPath("$.averageRating").doesNotExist());
    }
}
