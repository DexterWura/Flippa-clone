package com.flippa.dto;

import com.flippa.entity.Escrow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscrowDTO {
    
    private Long id;
    private Long listingId;
    private String listingTitle;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private BigDecimal amount;
    private Escrow.EscrowStatus status;
    private Escrow.PaymentGateway paymentGateway;
    private String paymentTransactionId;
    private String buyerNotes;
    private String sellerNotes;
    private String adminResolutionNotes;
    private Boolean disputeRaised;
    private String disputeReason;
    private String disputeResolution;
    private LocalDateTime createdAt;
    private LocalDateTime paymentReceivedAt;
    private LocalDateTime transferCompletedAt;
}

