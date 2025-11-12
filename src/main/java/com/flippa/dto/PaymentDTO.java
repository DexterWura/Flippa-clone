package com.flippa.dto;

import com.flippa.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    
    private Long id;
    private Long escrowId;
    private Long userId;
    private BigDecimal amount;
    private Payment.PaymentStatus status;
    private Payment.PaymentGateway gateway;
    private String transactionId;
    private String gatewayTransactionId;
    private String callbackUrl;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}

