package com.igarciamen.tasks.model;

import com.igarciamen.tasks.enums.PaymentMethod;
import com.igarciamen.tasks.enums.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Registro de pago. method distingue el pago simulado (Bloque 7 original) del
// TPV real via Redsys (Bloque 7 ampliado). orderNumber solo se usa con Redsys:
// es el "Ds_Order" que identifica la operacion frente a la pasarela y que llega
// de vuelta en la notificacion de confirmacion.
@Entity
@Table(name = "payments", schema = "public")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    // Solo relevante para TPV_REDSYS. Debe ser unico: se usa para localizar el
    // pago cuando llega la notificacion de Redsys.
    @Column(length = 12, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    public Payment() {}

    public Payment(Long taskId, BigDecimal amount, PaymentMethod method, PaymentStatus status) {
        this.taskId = taskId;
        this.amount = amount;
        this.method = method;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
