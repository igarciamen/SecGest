package com.igarciamen.tasks.service;

import com.igarciamen.tasks.enums.PaymentMethod;
import com.igarciamen.tasks.enums.PaymentStatus;
import com.igarciamen.tasks.enums.TaskStatus;
import com.igarciamen.tasks.model.Payment;
import com.igarciamen.tasks.model.Task;
import com.igarciamen.tasks.payloads.request.BudgetTaskRequest;
import com.igarciamen.tasks.payloads.request.CompleteTaskRequest;
import com.igarciamen.tasks.payloads.request.CreateTaskRequest;
import com.igarciamen.tasks.payloads.request.UpdateTaskRequest;
import com.igarciamen.tasks.payloads.response.RedsysFormResponse;
import com.igarciamen.tasks.repository.PaymentRepository;
import com.igarciamen.tasks.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.igarciamen.tasks.payloads.response.MetricsResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepo;
    private final PaymentRepository paymentRepo;
    private final CategoryClient categoryClient;
    private final UserClient userClient;
    private final EmailClient emailClient;
    private final RedsysService redsysService;

    public TaskService(TaskRepository taskRepo, PaymentRepository paymentRepo, CategoryClient categoryClient,
                       UserClient userClient, EmailClient emailClient, RedsysService redsysService) {
        this.taskRepo = taskRepo;
        this.paymentRepo = paymentRepo;
        this.categoryClient = categoryClient;
        this.userClient = userClient;
        this.emailClient = emailClient;
        this.redsysService = redsysService;
    }

    public Task create(Long clientUserId, CreateTaskRequest req) {
        // Valida la categoria llamando al microservicio "categories" (RestTemplate),
        // no con una consulta a base de datos local: son bases de datos distintas.
        Map<String, Object> category;
        try {
            category = categoryClient.fetchCategoryOrThrow(req.getCategoryId());
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo validar la categoria: " + e.getMessage());
        }

        Long categoryId = ((Number) category.get("id")).longValue();
        Task task = new Task(clientUserId, categoryId, req.getTitle(), req.getDescription());
        task.setDueDate(req.getDueDate());
        task.setContactPhone(req.getContactPhone());
        task.setRelevantUrl(req.getRelevantUrl());
        if (req.getConfidentialityLevel() != null) {
            task.setConfidentialityLevel(req.getConfidentialityLevel());
        }
        return taskRepo.save(task);
    }

    public List<Task> listMine(Long clientUserId) {
        return taskRepo.findByClientUserIdOrderByCreatedAtDesc(clientUserId);
    }

    // Detalle de una tarea concreta (Bloque 8, lo necesita "documents" para
    // comprobar si quien sube/descarga un archivo tiene permiso sobre esa tarea).
    // Accesible para el propio cliente dueño, o para el admin (isAdmin=true);
    // cualquier otro caso da 403.
    public Task getOne(Long taskId, Long requesterId, boolean isAdmin) {
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));

        if (!isAdmin && !task.getClientUserId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta tarea");
        }
        return task;
    }

    // Panel del admin: todas las tareas pendientes de presupuestar, de cualquier cliente.
    public List<Task> listPending() {
        return taskRepo.findByStatusOrderByCreatedAtAsc(TaskStatus.PENDIENTE_REVISION);
    }

    // Vista general del admin (Bloque 7): todas las tareas, o filtradas por un
    // estado concreto si se indica (ej. ACEPTADA, para ver que falta por cobrar).
    public List<Task> listAllForAdmin(TaskStatus statusFilter) {
        if (statusFilter == null) {
            return taskRepo.findAllByOrderByCreatedAtDesc();
        }
        return taskRepo.findByStatusOrderByCreatedAtDesc(statusFilter);
    }

    // Calendario del admin (Bloque 10): tareas con fecha limite dentro de un rango
// (normalmente, el mes que se este viendo). Cualquier estado, no solo pendientes
// -- el admin quiere ver todo lo que tiene fecha limite en ese periodo.
    public List<Task> listForCalendar(java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return taskRepo.findByDueDateBetweenOrderByDueDateAsc(from, to);
    }

    public Task budget(Long taskId, BudgetTaskRequest req) {
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task not found: " + taskId));

        if (task.getStatus() != TaskStatus.PENDIENTE_REVISION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden presupuestar tareas en PENDIENTE_REVISION (estado actual: "
                            + task.getStatus() + ")");
        }

        task.setPrice(req.getPrice());
        task.setStatus(TaskStatus.PRESUPUESTADA);
        Task saved = taskRepo.save(task);

        notifyBudgetByEmail(saved);

        return saved;
    }

    // El envio de email NUNCA deshace el presupuesto ya guardado: si "users" o
    // "notifications" estan caidos, la tarea se queda igualmente en PRESUPUESTADA
    // y solo se registra un aviso en el log.
    private void notifyBudgetByEmail(Task task) {
        try {
            Map<String, Object> user = userClient.fetchUserOrThrow(task.getClientUserId());
            String email = (String) user.get("email");
            String username = (String) user.get("username");
            String categoryName = fetchCategoryName(task.getCategoryId());

            emailClient.sendBudgetEmail(email, username, task.getTitle(), categoryName, task.getPrice());
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de presupuesto de la tarea {}: {}",
                    task.getId(), e.getMessage());
        }
    }

    // El cliente acepta un presupuesto: PRESUPUESTADA -> ACEPTADA + email de confirmacion.
    public Task accept(Long taskId, Long clientUserId) {
        Task task = findOwnedTask(taskId, clientUserId);

        if (task.getStatus() != TaskStatus.PRESUPUESTADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede aceptar una tarea PRESUPUESTADA (estado actual: " + task.getStatus() + ")");
        }

        task.setStatus(TaskStatus.ACEPTADA);
        Task saved = taskRepo.save(task);

        notifyGenericEmail(saved, "Presupuesto aceptado: " + saved.getTitle(),
                "Has aceptado el presupuesto de \"" + saved.getTitle() + "\" por "
                        + saved.getPrice() + " €. En breve nos ponemos manos a la obra.");

        return saved;
    }

    // El cliente rechaza un presupuesto: PRESUPUESTADA -> PENDIENTE_REVISION de nuevo.
    // El precio rechazado se archiva en priceHistory antes de vaciarlo, para no perder
    // esa informacion (permite editar y reenviar sin volver a escribir todo desde cero).
    public Task reject(Long taskId, Long clientUserId) {
        Task task = findOwnedTask(taskId, clientUserId);

        if (task.getStatus() != TaskStatus.PRESUPUESTADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede rechazar una tarea PRESUPUESTADA (estado actual: " + task.getStatus() + ")");
        }

        if (task.getPrice() != null) {
            task.getPriceHistory().add(task.getPrice());
        }
        task.setPrice(null);
        task.setStatus(TaskStatus.PENDIENTE_REVISION);

        return taskRepo.save(task);
    }

    // Edicion de una tarea propia, solo permitida mientras esta en PENDIENTE_REVISION
    // (recien creada, o vuelta a este estado tras un rechazo).
    public Task update(Long taskId, Long clientUserId, UpdateTaskRequest req) {
        Task task = findOwnedTask(taskId, clientUserId);

        if (task.getStatus() != TaskStatus.PENDIENTE_REVISION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede editar una tarea en PENDIENTE_REVISION (estado actual: " + task.getStatus() + ")");
        }

        Map<String, Object> category;
        try {
            category = categoryClient.fetchCategoryOrThrow(req.getCategoryId());
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo validar la categoria: " + e.getMessage());
        }

        task.setCategoryId(((Number) category.get("id")).longValue());
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setDueDate(req.getDueDate());
        task.setContactPhone(req.getContactPhone());
        task.setRelevantUrl(req.getRelevantUrl());
        if (req.getConfidentialityLevel() != null) {
            task.setConfidentialityLevel(req.getConfidentialityLevel());
        }

        return taskRepo.save(task);
    }

    // Pago simulado (Bloque 7 original): sin pasarela real conectada. Se mantiene
    // disponible aparte del TPV real, para poder seguir probando el resto del
    // flujo sin depender de que la notificacion de Redsys sea alcanzable.
    public Task pay(Long taskId, Long clientUserId) {
        Task task = findOwnedTask(taskId, clientUserId);

        if (task.getStatus() != TaskStatus.ACEPTADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede pagar una tarea ACEPTADA (estado actual: " + task.getStatus() + ")");
        }

        Payment payment = new Payment(task.getId(), task.getPrice(), PaymentMethod.SIMULADO, PaymentStatus.SIMULADO_OK);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        task.setStatus(TaskStatus.PAGADA);
        Task saved = taskRepo.save(task);

        notifyGenericEmail(saved, "Recibo de pago: " + saved.getTitle(),
                "Hemos recibido tu pago de " + saved.getPrice() + " € por \"" + saved.getTitle()
                        + "\". Gracias por confiar en SecreGest.");

        return saved;
    }

    // Inicia un pago real via TPV Redsys: crea el Payment en PENDIENTE (todavia
    // no se cobra nada, ni cambia el estado de la tarea) y devuelve los datos
    // firmados que el frontend necesita para redirigir al cliente a la pasarela.
    // La confirmacion de verdad llega despues, por confirmRedsysPayment().
    public RedsysFormResponse startRedsysPayment(Long taskId, Long clientUserId) {
        Task task = findOwnedTask(taskId, clientUserId);

        if (task.getStatus() != TaskStatus.ACEPTADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede pagar una tarea ACEPTADA (estado actual: " + task.getStatus() + ")");
        }

        String orderNumber = redsysService.generateOrderNumber(task.getId());

        Payment payment = new Payment(task.getId(), task.getPrice(), PaymentMethod.TPV_REDSYS, PaymentStatus.PENDIENTE);
        payment.setOrderNumber(orderNumber);
        paymentRepo.save(payment);

        String merchantParams = redsysService.buildMerchantParametersBase64(
                orderNumber, task.getPrice(), "SecreGest - " + task.getTitle());
        String signature = redsysService.generateSignature(orderNumber, merchantParams);

        return new RedsysFormResponse(redsysService.getGatewayUrl(), merchantParams, signature);
    }

    // Llamado por PaymentNotificationController cuando Redsys notifica el resultado
    // (servidor a servidor, no por la redireccion del navegador). dsResponseCode
    // en Redsys: 0000-0099 significa OK; cualquier otro valor es un fallo.
    public void confirmRedsysPayment(String orderNumber, String dsResponseCode) {
        Payment payment = paymentRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe ningun pago con Ds_Order: " + orderNumber));

        boolean success = isSuccessResponseCode(dsResponseCode);
        payment.setStatus(success ? PaymentStatus.COMPLETADO : PaymentStatus.FALLIDO);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        if (!success) {
            log.warn("Pago Redsys fallido para el pedido {}: Ds_Response={}", orderNumber, dsResponseCode);
            return;
        }

        Task task = taskRepo.findById(payment.getTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task not found: " + payment.getTaskId()));
        task.setStatus(TaskStatus.PAGADA);
        Task saved = taskRepo.save(task);

        notifyGenericEmail(saved, "Recibo de pago: " + saved.getTitle(),
                "Hemos recibido tu pago de " + saved.getPrice() + " € por \"" + saved.getTitle()
                        + "\" a traves del TPV. Gracias por confiar en SecreGest.");
    }

    private boolean isSuccessResponseCode(String dsResponseCode) {
        try {
            int code = Integer.parseInt(dsResponseCode);
            return code >= 0 && code <= 99;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // El admin marca el encargo como entregado: PAGADA -> ENTREGADA + email al cliente.
    public Task deliver(Long taskId) {
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));

        if (task.getStatus() != TaskStatus.PAGADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede marcar como entregada una tarea PAGADA (estado actual: " + task.getStatus() + ")");
        }

        task.setStatus(TaskStatus.ENTREGADA);
        task.setDeliveredAt(LocalDateTime.now());
        Task saved = taskRepo.save(task);

        notifyGenericEmail(saved, "Encargo entregado: " + saved.getTitle(),
                "Hemos entregado \"" + saved.getTitle() + "\". Revisalo y confirma la recepcion "
                        + "desde \"Mis tareas\" cuando quieras -- nos encantaria conocer tu opinion.");

        return saved;
    }

    // El cliente confirma la recepcion: ENTREGADA -> COMPLETADA, con valoracion
    // opcional (puede confirmar sin puntuar).
    public Task complete(Long taskId, Long clientUserId, CompleteTaskRequest req) {
        Task task = findOwnedTask(taskId, clientUserId);

        if (task.getStatus() != TaskStatus.ENTREGADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede confirmar una tarea ENTREGADA (estado actual: " + task.getStatus() + ")");
        }

        task.setStatus(TaskStatus.COMPLETADA);
        task.setCompletedAt(LocalDateTime.now());
        if (req != null) {
            task.setRating(req.getRating());
            task.setRatingComment(req.getRatingComment());
        }

        return taskRepo.save(task);
    }

    // Busca la tarea y comprueba que pertenece al cliente autenticado: nadie puede
    // aceptar, rechazar o editar la tarea de otro cliente.
    private Task findOwnedTask(Long taskId, Long clientUserId) {
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));

        if (!task.getClientUserId().equals(clientUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta tarea no pertenece al usuario autenticado");
        }
        return task;
    }

    private void notifyGenericEmail(Task task, String asunto, String mensaje) {
        try {
            Map<String, Object> user = userClient.fetchUserOrThrow(task.getClientUserId());
            String email = (String) user.get("email");
            emailClient.sendGenericEmail(email, asunto, mensaje);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de confirmacion de la tarea {}: {}",
                    task.getId(), e.getMessage());
        }
    }

    // Usado por TaskController para incluir el nombre de la categoria en la respuesta,
    // sin duplicar esa informacion en la propia tabla "tasks".
    public String fetchCategoryName(Long categoryId) {
        try {
            Object name = categoryClient.fetchCategoryOrThrow(categoryId).get("name");
            return name != null ? name.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Metricas del panel de admin (Bloque 12). Con el volumen de datos de un TFG,
// calcularlo en memoria sobre findAll() es mas simple y legible que escribir
// varias consultas JPQL de agregacion -- si el proyecto creciera mucho, aqui
// es donde se sustituiria por consultas @Query con COUNT/SUM/AVG.
    public MetricsResponse getMetrics() {
        List<Task> all = taskRepo.findAll();

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        BigDecimal revenue = all.stream()
                .filter(t -> t.getStatus() == TaskStatus.PAGADA
                        || t.getStatus() == TaskStatus.ENTREGADA
                        || t.getStatus() == TaskStatus.COMPLETADA)
                .map(Task::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in7Days = now.plusDays(7);
        long upcoming = all.stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> t.getStatus() != TaskStatus.COMPLETADA && t.getStatus() != TaskStatus.RECHAZADA)
                .filter(t -> !t.getDueDate().isBefore(now) && !t.getDueDate().isAfter(in7Days))
                .count();

        java.util.OptionalDouble avg = all.stream()
                .map(Task::getRating)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average();
        Double averageRating = avg.isPresent() ? Math.round(avg.getAsDouble() * 10) / 10.0 : null;

        return new MetricsResponse(byStatus, revenue, upcoming, averageRating);
    }
}
