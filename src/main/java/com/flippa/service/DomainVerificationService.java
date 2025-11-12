package com.flippa.service;

import com.flippa.entity.DomainVerification;
import com.flippa.entity.Listing;
import com.flippa.repository.DomainVerificationRepository;
import com.flippa.repository.ListingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

@Service
public class DomainVerificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DomainVerificationService.class);
    private final DomainVerificationRepository domainVerificationRepository;
    private final ListingRepository listingRepository;
    private final AuditLogService auditLogService;
    
    public DomainVerificationService(DomainVerificationRepository domainVerificationRepository,
                                    ListingRepository listingRepository,
                                    AuditLogService auditLogService) {
        this.domainVerificationRepository = domainVerificationRepository;
        this.listingRepository = listingRepository;
        this.auditLogService = auditLogService;
    }
    
    @Transactional
    public DomainVerification createVerificationRequest(Long listingId, String domain, 
                                                       com.flippa.entity.User user, HttpServletRequest request) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        if (!listing.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to create verification for this listing");
        }
        
        // Check if verification already exists
        Optional<DomainVerification> existing = domainVerificationRepository.findByListingId(listingId);
        if (existing.isPresent()) {
            throw new RuntimeException("Verification already exists for this listing");
        }
        
        // Generate verification token
        String token = UUID.randomUUID().toString();
        
        DomainVerification verification = new DomainVerification();
        verification.setListing(listing);
        verification.setDomain(domain);
        verification.setVerificationToken(token);
        verification.setStatus(DomainVerification.VerificationStatus.PENDING);
        
        DomainVerification saved = domainVerificationRepository.save(verification);
        
        auditLogService.logAction(user, "DOMAIN_VERIFICATION_CREATED", "DomainVerification", 
                                 saved.getId().toString(), 
                                 "Domain verification created for: " + domain, request);
        
        logger.info("Domain verification created for listing {}: {}", listingId, domain);
        return saved;
    }
    
    public String generateVerificationTxtContent(String token) {
        return "flippa-verification=" + token;
    }
    
    @Transactional
    public boolean verifyDomain(Long listingId, com.flippa.entity.User user, HttpServletRequest request) {
        DomainVerification verification = domainVerificationRepository.findByListingId(listingId)
            .orElseThrow(() -> new RuntimeException("Verification not found"));
        
        Listing listing = verification.getListing();
        if (!listing.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to verify this listing");
        }
        
        String token = verification.getVerificationToken();
        String domain = verification.getDomain();
        
        // Check for verification file at domain/.well-known/flippa-verification.txt
        // or domain/flippa-verification.txt
        String[] verificationPaths = {
            "https://" + domain + "/.well-known/flippa-verification.txt",
            "https://" + domain + "/flippa-verification.txt",
            "http://" + domain + "/.well-known/flippa-verification.txt",
            "http://" + domain + "/flippa-verification.txt"
        };
        
        boolean verified = false;
        for (String path : verificationPaths) {
            try {
                if (checkVerificationFile(path, token)) {
                    verified = true;
                    break;
                }
            } catch (Exception e) {
                logger.debug("Failed to check verification file at {}: {}", path, e.getMessage());
            }
        }
        
        if (verified) {
            verification.setStatus(DomainVerification.VerificationStatus.VERIFIED);
            verification.setVerifiedAt(java.time.LocalDateTime.now());
            verification.setVerificationNotes("Domain verified successfully");
            
            listing.setVerified(true);
            listing.setVerificationNotes("Domain ownership verified");
            listingRepository.save(listing);
            
            domainVerificationRepository.save(verification);
            
            auditLogService.logAction(user, "DOMAIN_VERIFIED", "DomainVerification", 
                                     verification.getId().toString(), 
                                     "Domain verified: " + domain, request);
            
            logger.info("Domain verified for listing {}: {}", listingId, domain);
            return true;
        } else {
            verification.setStatus(DomainVerification.VerificationStatus.FAILED);
            verification.setVerificationNotes("Verification file not found. Please ensure the file is accessible at your domain.");
            domainVerificationRepository.save(verification);
            
            logger.warn("Domain verification failed for listing {}: {}", listingId, domain);
            return false;
        }
    }
    
    private boolean checkVerificationFile(String url, String expectedToken) throws IOException {
        try {
            URL verificationUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) verificationUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "FlippaCloneBot/1.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()))) {
                    String content = reader.readLine();
                    if (content != null && content.contains("flippa-verification=" + expectedToken)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking verification file: {}", e.getMessage());
        }
        return false;
    }
    
    public Optional<DomainVerification> findByListingId(Long listingId) {
        return domainVerificationRepository.findByListingId(listingId);
    }
}

