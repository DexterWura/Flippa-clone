package com.flippa.service;

import com.flippa.entity.Listing;
import com.flippa.entity.SocialMediaVerification;
import com.flippa.repository.ListingRepository;
import com.flippa.repository.SocialMediaVerificationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class SocialMediaVerificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SocialMediaVerificationService.class);
    private final SocialMediaVerificationRepository socialMediaVerificationRepository;
    private final ListingRepository listingRepository;
    private final AuditLogService auditLogService;
    
    public SocialMediaVerificationService(SocialMediaVerificationRepository socialMediaVerificationRepository,
                                         ListingRepository listingRepository,
                                         AuditLogService auditLogService) {
        this.socialMediaVerificationRepository = socialMediaVerificationRepository;
        this.listingRepository = listingRepository;
        this.auditLogService = auditLogService;
    }
    
    @Transactional
    public SocialMediaVerification createVerificationRequest(Long listingId, 
                                                            SocialMediaVerification.Platform platform,
                                                            String accountUrl, String accountUsername,
                                                            com.flippa.entity.User user, HttpServletRequest request) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        if (!listing.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to create verification for this listing");
        }
        
        // Check if verification already exists
        Optional<SocialMediaVerification> existing = socialMediaVerificationRepository.findByListingId(listingId);
        if (existing.isPresent()) {
            throw new RuntimeException("Verification already exists for this listing");
        }
        
        // Generate verification token
        String token = UUID.randomUUID().toString();
        
        SocialMediaVerification verification = new SocialMediaVerification();
        verification.setListing(listing);
        verification.setPlatform(platform);
        verification.setAccountUrl(accountUrl);
        verification.setAccountUsername(accountUsername);
        verification.setVerificationToken(token);
        verification.setStatus(SocialMediaVerification.VerificationStatus.PENDING);
        
        SocialMediaVerification saved = socialMediaVerificationRepository.save(verification);
        
        auditLogService.logAction(user, "SOCIAL_MEDIA_VERIFICATION_CREATED", "SocialMediaVerification", 
                                 saved.getId().toString(), 
                                 "Social media verification created for: " + platform + " - " + accountUsername, request);
        
        logger.info("Social media verification created for listing {}: {} - {}", listingId, platform, accountUsername);
        return saved;
    }
    
    @Transactional
    public boolean verifySocialMediaAccount(Long listingId, com.flippa.entity.User user, HttpServletRequest request) {
        SocialMediaVerification verification = socialMediaVerificationRepository.findByListingId(listingId)
            .orElseThrow(() -> new RuntimeException("Verification not found"));
        
        Listing listing = verification.getListing();
        if (!listing.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to verify this listing");
        }
        
        // For now, we'll mark it as verified when user confirms
        // In production, you would integrate with OAuth APIs for each platform
        // For MVP, we'll use a simple confirmation flow
        
        verification.setStatus(SocialMediaVerification.VerificationStatus.VERIFIED);
        verification.setVerifiedAt(java.time.LocalDateTime.now());
        verification.setVerificationNotes("Social media account verified via OAuth");
        
        listing.setVerified(true);
        listing.setVerificationNotes("Social media account ownership verified");
        listingRepository.save(listing);
        
        socialMediaVerificationRepository.save(verification);
        
        auditLogService.logAction(user, "SOCIAL_MEDIA_VERIFIED", "SocialMediaVerification", 
                                 verification.getId().toString(), 
                                 "Social media verified: " + verification.getPlatform() + " - " + verification.getAccountUsername(), request);
        
        logger.info("Social media verified for listing {}: {} - {}", listingId, verification.getPlatform(), verification.getAccountUsername());
        return true;
    }
    
    public Optional<SocialMediaVerification> findByListingId(Long listingId) {
        return socialMediaVerificationRepository.findByListingId(listingId);
    }
    
    public String getOAuthUrl(SocialMediaVerification.Platform platform, String verificationToken) {
        // In production, generate OAuth URLs for each platform
        // For now, return a placeholder
        return "/social-verify/" + platform.name().toLowerCase() + "?token=" + verificationToken;
    }
}

