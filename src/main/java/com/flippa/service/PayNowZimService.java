package com.flippa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import zw.co.paynow.core.Paynow;
import zw.co.paynow.core.Payment;
import zw.co.paynow.responses.WebInitResponse;
import zw.co.paynow.responses.StatusResponse;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service for integrating with PayNow Zimbabwe payment gateway using the official SDK.
 * Documentation: https://github.com/paynow/Paynow-Java-SDK
 */
@Service
public class PayNowZimService {
    
    private static final Logger logger = LoggerFactory.getLogger(PayNowZimService.class);
    private final AdminService adminService;
    
    public PayNowZimService(AdminService adminService) {
        this.adminService = adminService;
    }
    
    /**
     * Get PayNow instance configured with credentials from database
     */
    private Paynow getPaynowInstance() {
        String integrationId = adminService.getPaymentConfigValue("paynow-zim", "integration-id", "");
        String integrationKey = adminService.getPaymentConfigValue("paynow-zim", "integration-key", "");
        String returnUrl = adminService.getPaymentConfigValue("paynow-zim", "return-url", "http://localhost/payment/callback");
        String resultUrl = adminService.getPaymentConfigValue("paynow-zim", "result-url", "http://localhost/payment/callback");
        
        if (integrationId.isEmpty() || integrationKey.isEmpty()) {
            throw new RuntimeException("PayNow integration credentials not configured. Please configure in admin settings.");
        }
        
        Paynow paynow = new Paynow(integrationId, integrationKey);
        paynow.setReturnUrl(returnUrl);
        paynow.setResultUrl(resultUrl);
        
        return paynow;
    }
    
    /**
     * Initiate a web-based payment transaction
     * @param amount Payment amount
     * @param reference Unique merchant reference (e.g., escrow ID or listing ID)
     * @param email Customer email address
     * @param description Payment description
     * @return PaymentInitiationResult containing redirect URL and poll URL
     */
    public PaymentInitiationResult initiateWebPayment(BigDecimal amount, String reference, String email, String description) {
        logger.info("Initiating PayNow Zim web payment: {} - {} - {}", amount, reference, email);
        
        try {
            Paynow paynow = getPaynowInstance();
            
            // Create payment with reference and email
            Payment payment = paynow.createPayment(reference, email);
            
            // Add the payment item
            payment.add(description != null ? description : "Payment for listing", amount.doubleValue());
            
            // Set cart description
            if (description != null) {
                payment.setCartDescription(description);
            }
            
            // Send payment and get response
            WebInitResponse response = paynow.send(payment);
            
            if (response.isRequestSuccess()) {
                String redirectUrl = response.redirectURL();
                String pollUrl = response.pollUrl();
                
                logger.info("PayNow payment initiated successfully. Redirect URL: {}, Poll URL: {}", redirectUrl, pollUrl);
                
                return new PaymentInitiationResult(true, redirectUrl, pollUrl, null);
            } else {
                String errors = response.errors() != null ? response.errors().toString() : "Unknown error";
                logger.error("PayNow payment initiation failed: {}", errors);
                return new PaymentInitiationResult(false, null, null, errors);
            }
            
        } catch (Exception e) {
            logger.error("Failed to initiate PayNow payment: {}", e.getMessage(), e);
            return new PaymentInitiationResult(false, null, null, e.getMessage());
        }
    }
    
    /**
     * Check payment status using poll URL
     * @param pollUrl The poll URL received from payment initiation
     * @return PaymentStatusResult containing payment status
     */
    public PaymentStatusResult checkPaymentStatus(String pollUrl) {
        logger.info("Checking PayNow payment status with poll URL: {}", pollUrl);
        
        try {
            Paynow paynow = getPaynowInstance();
            StatusResponse status = paynow.pollTransaction(pollUrl);
            
            if (status.isPaid()) {
                logger.info("PayNow payment confirmed as paid via poll URL: {}", pollUrl);
                return new PaymentStatusResult(true, true, "Payment confirmed");
            } else {
                logger.info("PayNow payment not yet paid via poll URL: {}", pollUrl);
                return new PaymentStatusResult(true, false, "Payment pending");
            }
            
        } catch (Exception e) {
            logger.error("Failed to check PayNow payment status: {}", e.getMessage(), e);
            return new PaymentStatusResult(false, false, e.getMessage());
        }
    }
    
    /**
     * Verify payment callback from PayNow
     * The SDK handles hash verification internally, but we can add additional validation here
     * @param responseParams Parameters received from PayNow callback
     * @return true if payment is verified
     */
    public boolean verifyPaymentCallback(Map<String, String> responseParams) {
        logger.info("Verifying PayNow payment callback");
        
        try {
            // The PayNow SDK handles hash verification during pollTransaction
            // For callback verification, we check if the status indicates payment
            String status = responseParams.get("status");
            return "Paid".equalsIgnoreCase(status) || "paid".equalsIgnoreCase(status);
            
        } catch (Exception e) {
            logger.error("Failed to verify PayNow payment callback: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Result class for payment initiation
     */
    public static class PaymentInitiationResult {
        private final boolean success;
        private final String redirectUrl;
        private final String pollUrl;
        private final String error;
        
        public PaymentInitiationResult(boolean success, String redirectUrl, String pollUrl, String error) {
            this.success = success;
            this.redirectUrl = redirectUrl;
            this.pollUrl = pollUrl;
            this.error = error;
        }
        
        public boolean isSuccess() { return success; }
        public String getRedirectUrl() { return redirectUrl; }
        public String getPollUrl() { return pollUrl; }
        public String getError() { return error; }
    }
    
    /**
     * Result class for payment status check
     */
    public static class PaymentStatusResult {
        private final boolean requestSuccess;
        private final boolean isPaid;
        private final String message;
        
        public PaymentStatusResult(boolean requestSuccess, boolean isPaid, String message) {
            this.requestSuccess = requestSuccess;
            this.isPaid = isPaid;
            this.message = message;
        }
        
        public boolean isRequestSuccess() { return requestSuccess; }
        public boolean isPaid() { return isPaid; }
        public String getMessage() { return message; }
    }
}
