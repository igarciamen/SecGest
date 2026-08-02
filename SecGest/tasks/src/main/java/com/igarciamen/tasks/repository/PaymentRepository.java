package com.igarciamen.tasks.repository;

import com.igarciamen.tasks.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTaskId(Long taskId);
    Optional<Payment> findByOrderNumber(String orderNumber);
}
