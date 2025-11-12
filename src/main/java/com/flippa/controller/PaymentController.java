package com.flippa.controller;

import com.flippa.entity.Payment;
import com.flippa.entity.User;
import com.flippa.service.PaymentService;
import com.flippa.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/payment")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    private final UserService userService;
    
    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }
    
    @PostMapping("/initiate")
    public String initiatePayment(@RequestParam Long escrowId,
                                 @RequestParam String gateway,
                                 Authentication authentication,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            Payment.PaymentGateway paymentGateway = Payment.PaymentGateway.valueOf(gateway.toUpperCase());
            
            Payment payment = paymentService.initiatePayment(escrowId, paymentGateway, user, request);
            
            // For PayNow, redirect directly to PayNow payment page
            if (paymentGateway == Payment.PaymentGateway.PAYNOW_ZIM && payment.getCallbackUrl() != null) {
                return "redirect:" + payment.getCallbackUrl();
            }
            
            redirectAttributes.addFlashAttribute("success", "Payment initiated. Please complete the payment.");
            return "redirect:/payment/" + payment.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to initiate payment: " + e.getMessage());
            return "redirect:/escrow/" + escrowId;
        }
    }
    
    @GetMapping("/{id}")
    public String paymentDetails(@PathVariable Long id, Authentication authentication, Model model) {
        Payment payment = paymentService.findById(id)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        // For PayNow payments, check status if still processing
        if (payment.getGateway() == Payment.PaymentGateway.PAYNOW_ZIM 
            && payment.getStatus() == Payment.PaymentStatus.PROCESSING) {
            try {
                paymentService.checkPayNowPaymentStatus(payment.getId());
                // Refresh payment after status check
                payment = paymentService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));
            } catch (Exception e) {
                logger.warn("Failed to check PayNow payment status: {}", e.getMessage());
            }
        }
        
        model.addAttribute("payment", paymentService.convertToDTO(payment));
        return "payment-details";
    }
    
    @PostMapping("/callback")
    @GetMapping("/callback")
    public String paymentCallback(@RequestParam(required = false) String reference,
                                 @RequestParam(required = false) String paynowreference,
                                 @RequestParam(required = false) String amount,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String pollurl,
                                 @RequestParam(required = false) String hash,
                                 @RequestParam(required = false) String gateway,
                                 HttpServletRequest request) {
        try {
            // PayNow callback parameters
            if (reference != null || paynowreference != null) {
                // This is a PayNow callback
                String merchantReference = reference != null ? reference : paynowreference;
                // Extract escrow ID from reference (format: ESCROW_123)
                if (merchantReference != null && merchantReference.startsWith("ESCROW_")) {
                    String escrowIdStr = merchantReference.replace("ESCROW_", "");
                    Long escrowId = Long.parseLong(escrowIdStr);
                    
                    // Find payment by escrow
                    Payment payment = paymentService.findByEscrowId(escrowId)
                        .stream()
                        .filter(p -> p.getGateway() == Payment.PaymentGateway.PAYNOW_ZIM)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Payment not found"));
                    
                    // Check payment status using poll URL
                    paymentService.checkPayNowPaymentStatus(payment.getId());
                    
                    return "redirect:/escrow/" + escrowId;
                }
            }
            
            // PayPal or other gateway callback
            Payment.PaymentGateway paymentGateway = gateway != null ? 
                Payment.PaymentGateway.valueOf(gateway.toUpperCase()) : Payment.PaymentGateway.PAYPAL;
            
            boolean success = "Paid".equalsIgnoreCase(status) || "paid".equalsIgnoreCase(status) 
                            || "success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status);
            String responseData = request.getQueryString();
            
            paymentService.processPaymentCallback(null, null, 
                                                 paymentGateway, success, responseData, request);
            
            return "redirect:/payment/success";
        } catch (Exception e) {
            return "redirect:/payment/error";
        }
    }
    
    @GetMapping("/my-payments")
    public String myPayments(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        model.addAttribute("payments", paymentService.findByUserId(user.getId()));
        return "my-payments";
    }
    
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

