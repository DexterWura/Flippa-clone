package com.flippa.service;

import com.flippa.dto.PaymentDTO;
import com.flippa.entity.Escrow;
import com.flippa.entity.Payment;
import com.flippa.entity.User;
import com.flippa.repository.EscrowRepository;
import com.flippa.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final EscrowRepository escrowRepository;
    private final PayPalService payPalService;
    private final PayNowZimService payNowZimService;
    private final AdminService adminService;
    private final AuditLogService auditLogService;
    
    public PaymentService(PaymentRepository paymentRepository, EscrowRepository escrowRepository,
                         PayPalService payPalService, PayNowZimService payNowZimService,
                         AdminService adminService, AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.escrowRepository = escrowRepository;
        this.payPalService = payPalService;
        this.payNowZimService = payNowZimService;
        this.adminService = adminService;
        this.auditLogService = auditLogService;
    }
    
    private boolean isPayPalEnabled() {
        return adminService.isPaymentGatewayEnabled("paypal");
    }
    
    private boolean isPayNowZimEnabled() {
        return adminService.isPaymentGatewayEnabled("paynow-zim");
    }
    
    @Transactional
    public Payment initiatePayment(Long escrowId, Payment.PaymentGateway gateway, 
                                   User user, HttpServletRequest request) {
        Escrow escrow = escrowRepository.findById(escrowId)
            .orElseThrow(() -> new RuntimeException("Escrow not found"));
        
        if (!escrow.getBuyer().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to initiate payment");
        }
        
        // Check if gateway is enabled
        if (gateway == Payment.PaymentGateway.PAYPAL && !isPayPalEnabled()) {
            throw new RuntimeException("PayPal is currently disabled");
        }
        if (gateway == Payment.PaymentGateway.PAYNOW_ZIM && !isPayNowZimEnabled()) {
            throw new RuntimeException("PayNow Zim is currently disabled");
        }
        
        Payment payment = new Payment();
        payment.setEscrow(escrow);
        payment.setUser(user);
        payment.setAmount(escrow.getAmount());
        payment.setGateway(gateway);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // Initiate payment with gateway
        try {
            String gatewayTransactionId = null;
            if (gateway == Payment.PaymentGateway.PAYPAL) {
                gatewayTransactionId = payPalService.createPayment(escrow.getAmount(), 
                    "Payment for listing: " + escrow.getListing().getTitle(),
                    request.getRequestURL().toString() + "/callback");
            } else if (gateway == Payment.PaymentGateway.PAYNOW_ZIM) {
                PayNowZimService.PaymentInitiationResult result = payNowZimService.initiateWebPayment(
                    escrow.getAmount(),
                    "ESCROW_" + escrow.getId(),
                    user.getEmail(),
                    "Payment for listing: " + escrow.getListing().getTitle()
                );
                
                if (result.isSuccess()) {
                    gatewayTransactionId = result.getPollUrl(); // Store poll URL as transaction ID
                    savedPayment.setCallbackUrl(result.getRedirectUrl()); // Store redirect URL
                } else {
                    throw new RuntimeException("PayNow payment initiation failed: " + result.getError());
                }
            }
            
            savedPayment.setGatewayTransactionId(gatewayTransactionId);
            savedPayment.setStatus(Payment.PaymentStatus.PROCESSING);
            savedPayment = paymentRepository.save(savedPayment);
            
        } catch (Exception e) {
            logger.error("Failed to initiate payment with gateway: {}", e.getMessage(), e);
            savedPayment.setStatus(Payment.PaymentStatus.FAILED);
            savedPayment.setFailureReason(e.getMessage());
            savedPayment = paymentRepository.save(savedPayment);
        }
        
        auditLogService.logAction(user, "PAYMENT_INITIATED", "Payment", 
                                 savedPayment.getId().toString(), 
                                 "Payment initiated via " + gateway, request);
        
        logger.info("Payment initiated: {} for escrow: {}", savedPayment.getId(), escrowId);
        return savedPayment;
    }
    
    @Transactional
    public void processPaymentCallback(String transactionId, String gatewayTransactionId,
                                      Payment.PaymentGateway gateway, boolean success,
                                      String responseData, HttpServletRequest request) {
        Payment payment = paymentRepository.findByGatewayTransactionId(gatewayTransactionId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setGatewayResponse(responseData);
        
        if (success) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setCompletedAt(java.time.LocalDateTime.now());
            
            // Update escrow status
            Escrow escrow = payment.getEscrow();
            escrow.setStatus(Escrow.EscrowStatus.PAYMENT_RECEIVED);
            escrow.setPaymentTransactionId(transactionId);
            escrow.setPaymentReceivedAt(java.time.LocalDateTime.now());
            escrowRepository.save(escrow);
            
            auditLogService.logAction(payment.getUser(), "PAYMENT_COMPLETED", "Payment", 
                                     payment.getId().toString(), 
                                     "Payment completed via " + gateway, request);
            
            logger.info("Payment completed: {}", payment.getId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed");
            
            auditLogService.logAction(payment.getUser(), "PAYMENT_FAILED", "Payment", 
                                     payment.getId().toString(), 
                                     "Payment failed via " + gateway, request);
            
            logger.warn("Payment failed: {}", payment.getId());
        }
        
        paymentRepository.save(payment);
    }
    
    public Optional<Payment> findByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }
    
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }
    
    public List<Payment> findByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }
    
    public List<Payment> findByEscrowId(Long escrowId) {
        return paymentRepository.findByEscrowId(escrowId);
    }
    
    @Transactional
    public void checkPayNowPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (payment.getGateway() != Payment.PaymentGateway.PAYNOW_ZIM) {
            throw new RuntimeException("Payment is not a PayNow payment");
        }
        
        // Use poll URL stored in gatewayTransactionId
        String pollUrl = payment.getGatewayTransactionId();
        if (pollUrl == null || pollUrl.isEmpty()) {
            logger.warn("No poll URL found for PayNow payment: {}", paymentId);
            return;
        }
        
        PayNowZimService.PaymentStatusResult statusResult = payNowZimService.checkPaymentStatus(pollUrl);
        
        if (statusResult.isRequestSuccess() && statusResult.isPaid()) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setCompletedAt(java.time.LocalDateTime.now());
            
            // Update escrow status
            Escrow escrow = payment.getEscrow();
            escrow.setStatus(Escrow.EscrowStatus.PAYMENT_RECEIVED);
            escrow.setPaymentTransactionId(payment.getTransactionId());
            escrow.setPaymentReceivedAt(java.time.LocalDateTime.now());
            escrowRepository.save(escrow);
            
            paymentRepository.save(payment);
            
            logger.info("PayNow payment confirmed as paid: {}", paymentId);
        } else {
            logger.info("PayNow payment status check: {}", statusResult.getMessage());
        }
    }
    
    public PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setEscrowId(payment.getEscrow().getId());
        dto.setUserId(payment.getUser().getId());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus());
        dto.setGateway(payment.getGateway());
        dto.setTransactionId(payment.getTransactionId());
        dto.setGatewayTransactionId(payment.getGatewayTransactionId());
        dto.setCallbackUrl(payment.getCallbackUrl());
        dto.setFailureReason(payment.getFailureReason());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setCompletedAt(payment.getCompletedAt());
        return dto;
    }
}

