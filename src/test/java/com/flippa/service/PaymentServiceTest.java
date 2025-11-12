package com.flippa.service;

import com.flippa.entity.Escrow;
import com.flippa.entity.Listing;
import com.flippa.entity.Payment;
import com.flippa.entity.User;
import com.flippa.repository.EscrowRepository;
import com.flippa.repository.PaymentRepository;
import com.flippa.service.PayNowZimService.PaymentInitiationResult;
import com.flippa.service.PayNowZimService.PaymentStatusResult;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EscrowRepository escrowRepository;

    @Mock
    private PayPalService payPalService;

    @Mock
    private PayNowZimService payNowZimService;

    @Mock
    private AdminService adminService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PaymentService paymentService;

    private Escrow escrow;
    private User buyer;
    private User seller;
    private Listing listing;
    private Payment payment;

    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setId(1L);
        buyer.setEmail("buyer@example.com");

        seller = new User();
        seller.setId(2L);
        seller.setEmail("seller@example.com");

        listing = new Listing();
        listing.setId(1L);
        listing.setTitle("Test Listing");
        listing.setSeller(seller);

        escrow = new Escrow();
        escrow.setId(1L);
        escrow.setBuyer(buyer);
        escrow.setSeller(seller);
        escrow.setListing(listing);
        escrow.setAmount(new BigDecimal("1000.00"));
        escrow.setStatus(Escrow.EscrowStatus.PENDING_PAYMENT);

        payment = new Payment();
        payment.setId(1L);
        payment.setEscrow(escrow);
        payment.setUser(buyer);
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setGateway(Payment.PaymentGateway.PAYNOW_ZIM);
        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        payment.setTransactionId("TXN123");
        payment.setGatewayTransactionId("POLL_URL_123");
    }

    @Test
    void testInitiatePayment_PayPal_Success() {
        // Arrange
        when(escrowRepository.findById(1L)).thenReturn(Optional.of(escrow));
        when(adminService.isPaymentGatewayEnabled("paypal")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(payPalService.createPayment(any(), anyString(), anyString())).thenReturn("PAYPAL_TXN_123");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/payment"));

        // Act
        Payment result = paymentService.initiatePayment(1L, Payment.PaymentGateway.PAYPAL, buyer, request);

        // Assert
        assertNotNull(result);
        verify(payPalService, times(1)).createPayment(any(), anyString(), anyString());
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(auditLogService, times(1)).logAction(any(), eq("PAYMENT_INITIATED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testInitiatePayment_PayNowZim_Success() {
        // Arrange
        PaymentInitiationResult initResult = new PaymentInitiationResult(
            true, 
            "https://paynow.co.zw/pay", 
            "https://paynow.co.zw/poll", 
            null
        );

        when(escrowRepository.findById(1L)).thenReturn(Optional.of(escrow));
        when(adminService.isPaymentGatewayEnabled("paynow-zim")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(payNowZimService.initiateWebPayment(any(), anyString(), anyString(), anyString()))
            .thenReturn(initResult);

        // Act
        Payment result = paymentService.initiatePayment(1L, Payment.PaymentGateway.PAYNOW_ZIM, buyer, request);

        // Assert
        assertNotNull(result);
        verify(payNowZimService, times(1)).initiateWebPayment(any(), anyString(), anyString(), anyString());
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
    }

    @Test
    void testInitiatePayment_Unauthorized() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(999L);

        when(escrowRepository.findById(1L)).thenReturn(Optional.of(escrow));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.initiatePayment(1L, Payment.PaymentGateway.PAYPAL, otherUser, request);
        });

        assertEquals("Unauthorized to initiate payment", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void testInitiatePayment_GatewayDisabled() {
        // Arrange
        when(escrowRepository.findById(1L)).thenReturn(Optional.of(escrow));
        when(adminService.isPaymentGatewayEnabled("paypal")).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.initiatePayment(1L, Payment.PaymentGateway.PAYPAL, buyer, request);
        });

        assertEquals("PayPal is currently disabled", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void testProcessPaymentCallback_Success() {
        // Arrange
        when(paymentRepository.findByGatewayTransactionId("GATEWAY_TXN_123")).thenReturn(Optional.of(payment));
        when(escrowRepository.save(any(Escrow.class))).thenReturn(escrow);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        paymentService.processPaymentCallback("TXN123", "GATEWAY_TXN_123", 
            Payment.PaymentGateway.PAYPAL, true, "SUCCESS", request);

        // Assert
        assertEquals(Payment.PaymentStatus.COMPLETED, payment.getStatus());
        assertNotNull(payment.getCompletedAt());
        assertEquals(Escrow.EscrowStatus.PAYMENT_RECEIVED, escrow.getStatus());
        verify(escrowRepository, times(1)).save(escrow);
        verify(paymentRepository, times(1)).save(payment);
        verify(auditLogService, times(1)).logAction(any(), eq("PAYMENT_COMPLETED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testProcessPaymentCallback_Failed() {
        // Arrange
        when(paymentRepository.findByGatewayTransactionId("GATEWAY_TXN_123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        paymentService.processPaymentCallback("TXN123", "GATEWAY_TXN_123", 
            Payment.PaymentGateway.PAYPAL, false, "FAILED", request);

        // Assert
        assertEquals(Payment.PaymentStatus.FAILED, payment.getStatus());
        assertEquals("Payment failed", payment.getFailureReason());
        verify(paymentRepository, times(1)).save(payment);
        verify(auditLogService, times(1)).logAction(any(), eq("PAYMENT_FAILED"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void testCheckPayNowPaymentStatus_Paid() {
        // Arrange
        PaymentStatusResult statusResult = new PaymentStatusResult(
            true, 
            true, 
            "Payment confirmed"
        );

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(payNowZimService.checkPaymentStatus("POLL_URL_123")).thenReturn(statusResult);
        when(escrowRepository.save(any(Escrow.class))).thenReturn(escrow);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        paymentService.checkPayNowPaymentStatus(1L);

        // Assert
        assertEquals(Payment.PaymentStatus.COMPLETED, payment.getStatus());
        assertNotNull(payment.getCompletedAt());
        assertEquals(Escrow.EscrowStatus.PAYMENT_RECEIVED, escrow.getStatus());
        verify(payNowZimService, times(1)).checkPaymentStatus("POLL_URL_123");
        verify(escrowRepository, times(1)).save(escrow);
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void testCheckPayNowPaymentStatus_NotPayNowPayment() {
        // Arrange
        payment.setGateway(Payment.PaymentGateway.PAYPAL);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.checkPayNowPaymentStatus(1L);
        });

        assertEquals("Payment is not a PayNow payment", exception.getMessage());
        verify(payNowZimService, never()).checkPaymentStatus(anyString());
    }

    @Test
    void testFindByTransactionId() {
        // Arrange
        when(paymentRepository.findByTransactionId("TXN123")).thenReturn(Optional.of(payment));

        // Act
        Optional<Payment> result = paymentService.findByTransactionId("TXN123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("TXN123", result.get().getTransactionId());
    }
}

