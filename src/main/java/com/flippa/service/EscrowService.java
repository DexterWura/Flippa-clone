package com.flippa.service;

import com.flippa.dto.EscrowDTO;
import com.flippa.entity.Escrow;
import com.flippa.entity.Listing;
import com.flippa.entity.User;
import com.flippa.repository.EscrowRepository;
import com.flippa.repository.ListingRepository;
import com.flippa.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EscrowService {
    
    private static final Logger logger = LoggerFactory.getLogger(EscrowService.class);
    private final EscrowRepository escrowRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final AuditLogService auditLogService;
    
    public EscrowService(EscrowRepository escrowRepository, ListingRepository listingRepository,
                        UserRepository userRepository, PaymentService paymentService, 
                        AuditLogService auditLogService) {
        this.escrowRepository = escrowRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.auditLogService = auditLogService;
    }
    
    @Transactional
    public Escrow createEscrow(Long listingId, Long buyerId, Escrow.PaymentGateway paymentGateway,
                               String buyerNotes, HttpServletRequest request) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new RuntimeException("Listing is not available for purchase");
        }
        
        // Get buyer from repository using buyerId
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new RuntimeException("Buyer not found"));
        User seller = listing.getSeller();
        
        if (buyer.getId().equals(seller.getId())) {
            throw new RuntimeException("Cannot buy your own listing");
        }
        
        Escrow escrow = new Escrow();
        escrow.setListing(listing);
        escrow.setBuyer(buyer);
        escrow.setSeller(seller);
        escrow.setAmount(listing.getPrice());
        escrow.setPaymentGateway(paymentGateway);
        escrow.setBuyerNotes(buyerNotes);
        escrow.setStatus(Escrow.EscrowStatus.PENDING_PAYMENT);
        
        Escrow savedEscrow = escrowRepository.save(escrow);
        
        auditLogService.logAction(buyer, "ESCROW_CREATED", "Escrow", 
                                 savedEscrow.getId().toString(), 
                                 "Escrow created for listing: " + listing.getTitle(), request);
        
        logger.info("Escrow created: {} for listing: {}", savedEscrow.getId(), listingId);
        return savedEscrow;
    }
    
    public Optional<Escrow> findById(Long id) {
        return escrowRepository.findById(id);
    }
    
    public List<Escrow> findByBuyerId(Long buyerId) {
        return escrowRepository.findByBuyerId(buyerId);
    }
    
    public List<Escrow> findBySellerId(Long sellerId) {
        return escrowRepository.findBySellerId(sellerId);
    }
    
    public List<Escrow> getDisputes() {
        return escrowRepository.findByDisputeRaisedTrue();
    }
    
    @Transactional
    public void markPaymentReceived(Long escrowId, String transactionId, HttpServletRequest request) {
        Escrow escrow = escrowRepository.findById(escrowId)
            .orElseThrow(() -> new RuntimeException("Escrow not found"));
        
        escrow.setStatus(Escrow.EscrowStatus.PAYMENT_RECEIVED);
        escrow.setPaymentTransactionId(transactionId);
        escrow.setPaymentReceivedAt(java.time.LocalDateTime.now());
        escrowRepository.save(escrow);
        
        auditLogService.logAction(null, "ESCROW_PAYMENT_RECEIVED", "Escrow", 
                                 escrowId.toString(), 
                                 "Payment received for escrow", request);
        
        logger.info("Payment received for escrow: {}", escrowId);
    }
    
    @Transactional
    public void raiseDispute(Long escrowId, String reason, User user, HttpServletRequest request) {
        Escrow escrow = escrowRepository.findById(escrowId)
            .orElseThrow(() -> new RuntimeException("Escrow not found"));
        
        if (!escrow.getBuyer().getId().equals(user.getId()) && 
            !escrow.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to raise dispute");
        }
        
        escrow.setDisputeRaised(true);
        escrow.setDisputeReason(reason);
        escrow.setDisputeRaisedAt(java.time.LocalDateTime.now());
        escrow.setStatus(Escrow.EscrowStatus.DISPUTE_RAISED);
        escrowRepository.save(escrow);
        
        auditLogService.logAction(user, "ESCROW_DISPUTE_RAISED", "Escrow", 
                                 escrowId.toString(), 
                                 "Dispute raised: " + reason, request);
        
        logger.info("Dispute raised for escrow: {} by user: {}", escrowId, user.getEmail());
    }
    
    @Transactional
    public void resolveDispute(Long escrowId, String resolution, String resolutionNotes,
                               Escrow.EscrowStatus finalStatus, User adminUser, HttpServletRequest request) {
        Escrow escrow = escrowRepository.findById(escrowId)
            .orElseThrow(() -> new RuntimeException("Escrow not found"));
        
        escrow.setDisputeResolution(resolution);
        escrow.setAdminResolutionNotes(resolutionNotes);
        escrow.setResolvedByAdmin(adminUser);
        escrow.setStatus(finalStatus);
        escrow.setDisputeRaised(false); // Mark dispute as resolved
        escrow.setDisputeResolvedAt(java.time.LocalDateTime.now());
        escrowRepository.save(escrow);
        
        auditLogService.logAction(adminUser, "ESCROW_DISPUTE_RESOLVED", "Escrow", 
                                 escrowId.toString(), 
                                 "Dispute resolved: " + resolution, request);
        
        logger.info("Dispute resolved for escrow: {} by admin: {}", escrowId, adminUser.getEmail());
    }
    
    @Transactional
    public void completeTransfer(Long escrowId, User user, HttpServletRequest request) {
        Escrow escrow = escrowRepository.findById(escrowId)
            .orElseThrow(() -> new RuntimeException("Escrow not found"));
        
        if (!escrow.getSeller().getId().equals(user.getId()) && 
            !user.getRoles().stream().anyMatch(r -> r.getName().name().contains("ADMIN"))) {
            throw new RuntimeException("Unauthorized to complete transfer");
        }
        
        escrow.setStatus(Escrow.EscrowStatus.TRANSFER_COMPLETED);
        escrow.setTransferCompletedAt(java.time.LocalDateTime.now());
        
        // Mark listing as sold
        escrow.getListing().setStatus(Listing.ListingStatus.SOLD);
        listingRepository.save(escrow.getListing());
        
        escrowRepository.save(escrow);
        
        auditLogService.logAction(user, "ESCROW_TRANSFER_COMPLETED", "Escrow", 
                                 escrowId.toString(), 
                                 "Transfer completed", request);
        
        logger.info("Transfer completed for escrow: {}", escrowId);
    }
    
    public EscrowDTO convertToDTO(Escrow escrow) {
        EscrowDTO dto = new EscrowDTO();
        dto.setId(escrow.getId());
        dto.setListingId(escrow.getListing().getId());
        dto.setListingTitle(escrow.getListing().getTitle());
        dto.setBuyerId(escrow.getBuyer().getId());
        dto.setBuyerName(escrow.getBuyer().getFullName());
        dto.setSellerId(escrow.getSeller().getId());
        dto.setSellerName(escrow.getSeller().getFullName());
        dto.setAmount(escrow.getAmount());
        dto.setStatus(escrow.getStatus());
        dto.setPaymentGateway(escrow.getPaymentGateway());
        dto.setPaymentTransactionId(escrow.getPaymentTransactionId());
        dto.setBuyerNotes(escrow.getBuyerNotes());
        dto.setSellerNotes(escrow.getSellerNotes());
        dto.setAdminResolutionNotes(escrow.getAdminResolutionNotes());
        dto.setDisputeRaised(escrow.getDisputeRaised());
        dto.setDisputeReason(escrow.getDisputeReason());
        dto.setDisputeResolution(escrow.getDisputeResolution());
        dto.setCreatedAt(escrow.getCreatedAt());
        dto.setPaymentReceivedAt(escrow.getPaymentReceivedAt());
        dto.setTransferCompletedAt(escrow.getTransferCompletedAt());
        return dto;
    }
}

