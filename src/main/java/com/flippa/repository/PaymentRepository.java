package com.flippa.repository;

import com.flippa.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByEscrowId(Long escrowId);
}

