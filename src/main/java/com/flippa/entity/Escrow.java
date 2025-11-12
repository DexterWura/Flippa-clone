package com.flippa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "escrows")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Escrow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EscrowStatus status = EscrowStatus.PENDING_PAYMENT;
    
    @Column(length = 100)
    private String paymentTransactionId;
    
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private PaymentGateway paymentGateway;
    
    @Column(length = 2000)
    private String buyerNotes;
    
    @Column(length = 2000)
    private String sellerNotes;
    
    @Column(length = 2000)
    private String adminResolutionNotes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_admin_id")
    private User resolvedByAdmin;
    
    @Column(nullable = false)
    private Boolean disputeRaised = false;
    
    @Column(length = 2000)
    private String disputeReason;
    
    @Column(length = 2000)
    private String disputeResolution;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private LocalDateTime paymentReceivedAt;
    private LocalDateTime transferInitiatedAt;
    private LocalDateTime transferCompletedAt;
    private LocalDateTime disputeRaisedAt;
    private LocalDateTime disputeResolvedAt;
    
    public enum EscrowStatus {
        PENDING_PAYMENT,
        PAYMENT_RECEIVED,
        TRANSFER_IN_PROGRESS,
        TRANSFER_COMPLETED,
        DISPUTE_RAISED,
        DISPUTE_RESOLVED,
        CANCELLED,
        REFUNDED
    }
    
    public enum PaymentGateway {
        PAYPAL,
        PAYNOW_ZIM,
        STRIPE,
        BANK_TRANSFER
    }
}

