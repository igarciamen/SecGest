package com.igarciamen.tasks.service;

import com.igarciamen.tasks.enums.TaskStatus;
import com.igarciamen.tasks.model.Payment;
import com.igarciamen.tasks.model.Task;
import com.igarciamen.tasks.payloads.request.BudgetTaskRequest;
import com.igarciamen.tasks.payloads.request.CompleteTaskRequest;
import com.igarciamen.tasks.payloads.request.CreateTaskRequest;
import com.igarciamen.tasks.payloads.request.UpdateTaskRequest;
import com.igarciamen.tasks.repository.PaymentRepository;
import com.igarciamen.tasks.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepo;

    @Mock
    private PaymentRepository paymentRepo;

    @Mock
    private CategoryClient categoryClient;

    @Mock
    private UserClient userClient;

    @Mock
    private EmailClient emailClient;

    @Mock
    private RedsysService redsysService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void create_creaTareaCuandoLaCategoriaExiste() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setCategoryId(1L);
        req.setTitle("Agendar reunion");
        req.setDescription("Con el proveedor X el jueves");

        when(categoryClient.fetchCategoryOrThrow(1L))
                .thenReturn(Map.of("id", 1, "name", "Agenda"));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });

        Task result = taskService.create(5L, req);

        assertEquals(100L, result.getId());
        assertEquals(5L, result.getClientUserId());
        assertEquals(1L, result.getCategoryId());
        assertEquals("PENDIENTE_REVISION", result.getStatus().name());
    }

    @Test
    void create_lanza404SiLaCategoriaNoExiste() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setCategoryId(99L);
        req.setTitle("Da igual");

        when(categoryClient.fetchCategoryOrThrow(99L))
                .thenThrow(new EntityNotFoundException("Category not found: 99"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.create(5L, req));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void create_lanza503SiCategoriesNoResponde() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setCategoryId(1L);
        req.setTitle("Da igual");

        when(categoryClient.fetchCategoryOrThrow(1L))
                .thenThrow(new IllegalStateException("Error calling the categories service: timeout"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.create(5L, req));

        assertEquals(503, ex.getStatusCode().value());
    }

    @Test
    void listMine_devuelveSoloLasTareasDeEseCliente() {
        Task t1 = new Task(5L, 1L, "Tarea 1", "desc");
        Task t2 = new Task(5L, 1L, "Tarea 2", "desc");

        when(taskRepo.findByClientUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(t2, t1));

        List<Task> result = taskService.listMine(5L);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getClientUserId().equals(5L)));
    }

    @Test
    void listPending_devuelveTareasEnPendienteRevision() {
        Task t1 = new Task(5L, 1L, "Tarea 1", "desc");
        when(taskRepo.findByStatusOrderByCreatedAtAsc(TaskStatus.PENDIENTE_REVISION))
                .thenReturn(List.of(t1));

        List<Task> result = taskService.listPending();

        assertEquals(1, result.size());
    }

    @Test
    void budget_fijaPrecioYCambiaEstadoAPresupuestada() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        BudgetTaskRequest req = new BudgetTaskRequest();
        req.setPrice(new BigDecimal("20.00"));

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(5L)).thenReturn(Map.of("id", 5, "username", "marco", "email", "marco@mail.com"));
        when(categoryClient.fetchCategoryOrThrow(1L)).thenReturn(Map.of("id", 1, "name", "Agenda"));

        Task result = taskService.budget(10L, req);

        assertEquals(new BigDecimal("20.00"), result.getPrice());
        assertEquals(TaskStatus.PRESUPUESTADA, result.getStatus());
        verify(emailClient).sendBudgetEmail("marco@mail.com", "marco", "Agendar reunion", "Agenda", new BigDecimal("20.00"));
    }

    @Test
    void budget_lanza404SiLaTareaNoExiste() {
        BudgetTaskRequest req = new BudgetTaskRequest();
        req.setPrice(new BigDecimal("20.00"));
        when(taskRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.budget(99L, req));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void budget_lanza409SiLaTareaNoEstaPendienteDeRevision() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PRESUPUESTADA);
        BudgetTaskRequest req = new BudgetTaskRequest();
        req.setPrice(new BigDecimal("20.00"));

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.budget(10L, req));

        assertEquals(409, ex.getStatusCode().value());
        verify(taskRepo, never()).save(any());
    }

    @Test
    void budget_siFallaElEmail_laTareaQuedaPresupuestadaIgualmente() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        BudgetTaskRequest req = new BudgetTaskRequest();
        req.setPrice(new BigDecimal("20.00"));

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(5L)).thenThrow(new IllegalStateException("users caido"));

        Task result = taskService.budget(10L, req);

        assertEquals(TaskStatus.PRESUPUESTADA, result.getStatus());
        assertEquals(new BigDecimal("20.00"), result.getPrice());
    }

    @Test
    void accept_cambiaEstadoAAceptadaYEnviaCorreo() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PRESUPUESTADA);
        task.setPrice(new BigDecimal("20.00"));

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(5L)).thenReturn(Map.of("id", 5, "username", "marco", "email", "marco@mail.com"));

        Task result = taskService.accept(10L, 5L);

        assertEquals(TaskStatus.ACEPTADA, result.getStatus());
        verify(emailClient).sendGenericEmail(eq("marco@mail.com"), any(), any());
    }

    @Test
    void accept_lanza403SiLaTareaNoEsDelCliente() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PRESUPUESTADA);
        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.accept(10L, 999L));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void accept_lanza409SiNoEstaPresupuestada() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PENDIENTE_REVISION);
        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.accept(10L, 5L));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void reject_vuelveAPendienteRevisionYArchivaElPrecio() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PRESUPUESTADA);
        task.setPrice(new BigDecimal("20.00"));

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.reject(10L, 5L);

        assertEquals(TaskStatus.PENDIENTE_REVISION, result.getStatus());
        assertNull(result.getPrice());
        assertEquals(1, result.getPriceHistory().size());
        assertEquals(new BigDecimal("20.00"), result.getPriceHistory().get(0));
    }

    @Test
    void reject_lanza403SiLaTareaNoEsDelCliente() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PRESUPUESTADA);
        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.reject(10L, 999L));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void update_editaLaTareaMientrasEstaPendienteDeRevision() {
        Task task = new Task(5L, 1L, "Titulo viejo", "desc vieja");
        task.setId(10L);
        task.setStatus(TaskStatus.PENDIENTE_REVISION);

        UpdateTaskRequest req = new UpdateTaskRequest();
        req.setCategoryId(2L);
        req.setTitle("Titulo nuevo");
        req.setDescription("desc nueva");

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(categoryClient.fetchCategoryOrThrow(2L)).thenReturn(Map.of("id", 2, "name", "Redaccion"));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.update(10L, 5L, req);

        assertEquals("Titulo nuevo", result.getTitle());
        assertEquals(2L, result.getCategoryId());
    }

    @Test
    void update_lanza409SiNoEstaPendienteDeRevision() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PRESUPUESTADA);

        UpdateTaskRequest req = new UpdateTaskRequest();
        req.setCategoryId(1L);
        req.setTitle("Da igual");

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.update(10L, 5L, req));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void listAllForAdmin_sinFiltro_devuelveTodas() {
        Task t1 = new Task(5L, 1L, "Tarea 1", "desc");
        when(taskRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(t1));

        List<Task> result = taskService.listAllForAdmin(null);

        assertEquals(1, result.size());
        verify(taskRepo, never()).findByStatusOrderByCreatedAtDesc(any());
    }

    @Test
    void listAllForAdmin_conFiltro_usaElEstadoIndicado() {
        Task t1 = new Task(5L, 1L, "Tarea 1", "desc");
        t1.setStatus(TaskStatus.ACEPTADA);
        when(taskRepo.findByStatusOrderByCreatedAtDesc(TaskStatus.ACEPTADA)).thenReturn(List.of(t1));

        List<Task> result = taskService.listAllForAdmin(TaskStatus.ACEPTADA);

        assertEquals(1, result.size());
        verify(taskRepo, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void pay_cambiaEstadoAPagadaYRegistraElPago() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.ACEPTADA);
        task.setPrice(new BigDecimal("20.00"));

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(5L)).thenReturn(Map.of("id", 5, "username", "marco", "email", "marco@mail.com"));

        Task result = taskService.pay(10L, 5L);

        assertEquals(TaskStatus.PAGADA, result.getStatus());
        verify(paymentRepo).save(argThat((Payment p) ->
                p.getTaskId().equals(10L) && p.getAmount().equals(new BigDecimal("20.00"))));
        verify(emailClient).sendGenericEmail(eq("marco@mail.com"), any(), any());
    }

    @Test
    void pay_lanza409SiNoEstaAceptada() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PRESUPUESTADA);

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.pay(10L, 5L));

        assertEquals(409, ex.getStatusCode().value());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void pay_lanza403SiLaTareaNoEsDelCliente() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.ACEPTADA);

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.pay(10L, 999L));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void startRedsysPayment_creaUnPagoPendienteYDevuelveLosDatosDelFormulario() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.ACEPTADA);
        task.setPrice(new BigDecimal("20.00"));

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(redsysService.generateOrderNumber(10L)).thenReturn("000112345678");
        when(redsysService.buildMerchantParametersBase64(eq("000112345678"), eq(new BigDecimal("20.00")), any()))
                .thenReturn("params-base64");
        when(redsysService.generateSignature("000112345678", "params-base64")).thenReturn("firma-base64");
        when(redsysService.getGatewayUrl()).thenReturn("https://sis-t.redsys.es:25443/sis/realizarPago");
        when(paymentRepo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var form = taskService.startRedsysPayment(10L, 5L);

        assertEquals("params-base64", form.getDsMerchantParameters());
        assertEquals("firma-base64", form.getDsSignature());
        verify(paymentRepo).save(argThat((Payment p) ->
                p.getMethod() == com.igarciamen.tasks.enums.PaymentMethod.TPV_REDSYS
                        && p.getStatus() == com.igarciamen.tasks.enums.PaymentStatus.PENDIENTE
                        && "000112345678".equals(p.getOrderNumber())));
    }

    @Test
    void startRedsysPayment_lanza409SiNoEstaAceptada() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PRESUPUESTADA);

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.startRedsysPayment(10L, 5L));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void confirmRedsysPayment_conCodigoDeExito_marcaElPagoYLaTareaComoCompletados() {
        Payment payment = new Payment(10L, new BigDecimal("20.00"),
                com.igarciamen.tasks.enums.PaymentMethod.TPV_REDSYS, com.igarciamen.tasks.enums.PaymentStatus.PENDIENTE);
        payment.setOrderNumber("000112345678");

        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.ACEPTADA);
        task.setPrice(new BigDecimal("20.00"));

        when(paymentRepo.findByOrderNumber("000112345678")).thenReturn(Optional.of(payment));
        when(paymentRepo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(5L)).thenReturn(Map.of("id", 5, "username", "marco", "email", "marco@mail.com"));

        taskService.confirmRedsysPayment("000112345678", "0000");

        assertEquals(com.igarciamen.tasks.enums.PaymentStatus.COMPLETADO, payment.getStatus());
        assertEquals(TaskStatus.PAGADA, task.getStatus());
        verify(emailClient).sendGenericEmail(eq("marco@mail.com"), any(), any());
    }

    @Test
    void confirmRedsysPayment_conCodigoDeError_marcaElPagoComoFallidoYNoTocaLaTarea() {
        Payment payment = new Payment(10L, new BigDecimal("20.00"),
                com.igarciamen.tasks.enums.PaymentMethod.TPV_REDSYS, com.igarciamen.tasks.enums.PaymentStatus.PENDIENTE);
        payment.setOrderNumber("000112345678");

        when(paymentRepo.findByOrderNumber("000112345678")).thenReturn(Optional.of(payment));
        when(paymentRepo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.confirmRedsysPayment("000112345678", "0180"); // codigo de denegacion tipico de Redsys

        assertEquals(com.igarciamen.tasks.enums.PaymentStatus.FALLIDO, payment.getStatus());
        verify(taskRepo, never()).save(any());
    }

    @Test
    void confirmRedsysPayment_lanza404SiElPedidoNoExiste() {
        when(paymentRepo.findByOrderNumber("no-existe")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.confirmRedsysPayment("no-existe", "0000"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getOne_elDuenoPuedeVerla() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        Task result = taskService.getOne(10L, 5L, false);

        assertEquals(10L, result.getId());
    }

    @Test
    void getOne_elAdminPuedeVerCualquiera() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        Task result = taskService.getOne(10L, 999L, true);

        assertEquals(10L, result.getId());
    }

    @Test
    void getOne_otroClienteNoAdminLanza403() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.getOne(10L, 999L, false));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getOne_lanza404SiNoExiste() {
        when(taskRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.getOne(99L, 5L, false));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void deliver_cambiaEstadoAEntregadaYEnviaCorreo() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PAGADA);

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.fetchUserOrThrow(5L)).thenReturn(Map.of("id", 5, "username", "marco", "email", "marco@mail.com"));

        Task result = taskService.deliver(10L);

        assertEquals(TaskStatus.ENTREGADA, result.getStatus());
        assertNotNull(result.getDeliveredAt());
        verify(emailClient).sendGenericEmail(eq("marco@mail.com"), any(), any());
    }

    @Test
    void deliver_lanza409SiNoEstaPagada() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.ACEPTADA);

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.deliver(10L));

        assertEquals(409, ex.getStatusCode().value());
        verify(taskRepo, never()).save(any());
    }

    @Test
    void complete_conValoracion_cambiaEstadoYGuardaLaValoracion() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.ENTREGADA);

        CompleteTaskRequest req = new CompleteTaskRequest();
        req.setRating(5);
        req.setRatingComment("Genial, muy rapido");

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.complete(10L, 5L, req);

        assertEquals(TaskStatus.COMPLETADA, result.getStatus());
        assertNotNull(result.getCompletedAt());
        assertEquals(5, result.getRating());
        assertEquals("Genial, muy rapido", result.getRatingComment());
    }

    @Test
    void complete_sinValoracion_confirmaIgualmente() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.ENTREGADA);

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.complete(10L, 5L, null);

        assertEquals(TaskStatus.COMPLETADA, result.getStatus());
        assertNull(result.getRating());
    }

    @Test
    void complete_lanza409SiNoEstaEntregada() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.PAGADA);

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.complete(10L, 5L, null));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void complete_lanza403SiLaTareaNoEsDelCliente() {
        Task task = new Task(5L, 1L, "Agendar reunion", "desc");
        task.setId(10L);
        task.setStatus(TaskStatus.ENTREGADA);

        when(taskRepo.findById(10L)).thenReturn(Optional.of(task));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.complete(10L, 999L, null));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void listForCalendar_devuelveTareasConDueDateEnElRango() {
        Task t1 = new Task(5L, 1L, "Tarea con fecha", "desc");
        t1.setDueDate(LocalDateTime.of(2026, 7, 15, 10, 0));
        when(taskRepo.findByDueDateBetweenOrderByDueDateAsc(any(), any())).thenReturn(List.of(t1));

        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 31, 23, 59, 59);
        List<Task> result = taskService.listForCalendar(from, to);

        assertEquals(1, result.size());
        verify(taskRepo).findByDueDateBetweenOrderByDueDateAsc(from, to);
    }

    @Test
    void getMetrics_calculaConteosIngresosYValoracionMedia() {
        Task pagada = new Task(5L, 1L, "Tarea pagada", "desc");
        pagada.setStatus(TaskStatus.PAGADA);
        pagada.setPrice(new BigDecimal("20.00"));

        Task completada = new Task(5L, 1L, "Tarea completada", "desc");
        completada.setStatus(TaskStatus.COMPLETADA);
        completada.setPrice(new BigDecimal("30.00"));
        completada.setRating(5);

        Task pendiente = new Task(5L, 1L, "Tarea pendiente", "desc");
        pendiente.setStatus(TaskStatus.PENDIENTE_REVISION);
        pendiente.setDueDate(LocalDateTime.now().plusDays(2));

        when(taskRepo.findAll()).thenReturn(List.of(pagada, completada, pendiente));

        var metrics = taskService.getMetrics();

        assertEquals(1L, metrics.getTasksByStatus().get("PAGADA"));
        assertEquals(1L, metrics.getTasksByStatus().get("COMPLETADA"));
        assertEquals(1L, metrics.getTasksByStatus().get("PENDIENTE_REVISION"));
        assertEquals(new BigDecimal("50.00"), metrics.getTotalRevenue());
        assertEquals(1, metrics.getUpcomingDueCount());
        assertEquals(5.0, metrics.getAverageRating());
    }

    @Test
    void getMetrics_sinTareas_devuelveValoresVacios() {
        when(taskRepo.findAll()).thenReturn(List.of());

        var metrics = taskService.getMetrics();

        assertTrue(metrics.getTasksByStatus().isEmpty());
        assertEquals(BigDecimal.ZERO, metrics.getTotalRevenue());
        assertEquals(0, metrics.getUpcomingDueCount());
        assertNull(metrics.getAverageRating());
    }

    @Test
    void getMetrics_noCuentaComoProximaUnaTareaYaCompletada() {
        Task completadaConFecha = new Task(5L, 1L, "Tarea", "desc");
        completadaConFecha.setStatus(TaskStatus.COMPLETADA);
        completadaConFecha.setDueDate(LocalDateTime.now().plusDays(1));

        when(taskRepo.findAll()).thenReturn(List.of(completadaConFecha));

        var metrics = taskService.getMetrics();

        assertEquals(0, metrics.getUpcomingDueCount());
    }
}
