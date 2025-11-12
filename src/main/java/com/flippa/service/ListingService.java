package com.flippa.service;

import com.flippa.dto.ListingDTO;
import com.flippa.dto.WebsiteInfoDTO;
import com.flippa.entity.Listing;
import com.flippa.entity.User;
import com.flippa.entity.WebsiteInfo;
import com.flippa.repository.ListingRepository;
import com.flippa.repository.WebsiteInfoRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ListingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ListingService.class);
    private final ListingRepository listingRepository;
    private final WebsiteInfoRepository websiteInfoRepository;
    private final WebsiteInfoFetchService websiteInfoFetchService;
    private final AuditLogService auditLogService;
    private final AdminService adminService;
    
    public ListingService(ListingRepository listingRepository, 
                         WebsiteInfoRepository websiteInfoRepository,
                         WebsiteInfoFetchService websiteInfoFetchService,
                         AuditLogService auditLogService,
                         AdminService adminService) {
        this.listingRepository = listingRepository;
        this.websiteInfoRepository = websiteInfoRepository;
        this.websiteInfoFetchService = websiteInfoFetchService;
        this.auditLogService = auditLogService;
        this.adminService = adminService;
    }
    
    @Transactional
    public Listing createListing(ListingDTO listingDTO, User seller, HttpServletRequest request) {
        Listing listing = new Listing();
        listing.setSeller(seller);
        listing.setTitle(listingDTO.getTitle());
        listing.setDescription(listingDTO.getDescription());
        listing.setType(listingDTO.getType());
        listing.setPrice(listingDTO.getPrice());
        listing.setStartingBid(listingDTO.getStartingBid());
        listing.setCurrentBid(listingDTO.getCurrentBid());
        listing.setWebsiteUrl(listingDTO.getWebsiteUrl());
        listing.setImageUrl(listingDTO.getImageUrl());
        listing.setCategory(listingDTO.getCategory());
        listing.setFeatured(listingDTO.getFeatured() != null ? listingDTO.getFeatured() : false);
        
        // Check if auto-approve is enabled
        boolean autoApprove = adminService.isAutoApproveEnabled();
        if (autoApprove) {
            listing.setStatus(Listing.ListingStatus.ACTIVE);
            logger.info("Listing auto-approved and set to ACTIVE (auto-approve enabled)");
        } else {
            listing.setStatus(Listing.ListingStatus.PENDING_REVIEW);
            logger.info("Listing set to PENDING_REVIEW (auto-approve disabled - requires admin approval)");
        }
        
        listing.setAuctionEndDate(listingDTO.getAuctionEndDate());
        
        Listing savedListing = listingRepository.save(listing);
        
        // Auto-fetch website info if URL is provided
        if (listingDTO.getWebsiteUrl() != null && !listingDTO.getWebsiteUrl().isEmpty()) {
            try {
                websiteInfoFetchService.fetchAndSaveWebsiteInfo(savedListing.getId(), listingDTO.getWebsiteUrl());
            } catch (Exception e) {
                logger.warn("Failed to auto-fetch website info for listing {}: {}", 
                           savedListing.getId(), e.getMessage());
            }
        }
        
        auditLogService.logAction(seller, "LISTING_CREATED", "Listing", 
                                 savedListing.getId().toString(), 
                                 "Listing created: " + savedListing.getTitle(), request);
        
        logger.info("Listing created: {} by user: {}", savedListing.getId(), seller.getEmail());
        return savedListing;
    }
    
    public List<Listing> getAllActiveListings() {
        return listingRepository.findByStatus(Listing.ListingStatus.ACTIVE);
    }
    
    public List<Listing> getFeaturedListings() {
        return listingRepository.findByFeaturedTrueAndStatus(Listing.ListingStatus.ACTIVE);
    }
    
    public List<Listing> searchListings(String query) {
        return listingRepository.searchListings(query, Listing.ListingStatus.ACTIVE);
    }
    
    public Optional<Listing> findById(Long id) {
        return listingRepository.findById(id);
    }
    
    public List<Listing> findBySellerId(Long sellerId) {
        return listingRepository.findBySellerId(sellerId);
    }
    
    @Transactional
    public Listing updateListing(Long id, ListingDTO listingDTO, User user, HttpServletRequest request) {
        Listing listing = listingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        if (!listing.getSeller().getId().equals(user.getId()) && 
            !user.getRoles().stream().anyMatch(r -> r.getName().name().contains("ADMIN"))) {
            throw new RuntimeException("Unauthorized to update this listing");
        }
        
        listing.setTitle(listingDTO.getTitle());
        listing.setDescription(listingDTO.getDescription());
        listing.setPrice(listingDTO.getPrice());
        listing.setWebsiteUrl(listingDTO.getWebsiteUrl());
        listing.setImageUrl(listingDTO.getImageUrl());
        listing.setCategory(listingDTO.getCategory());
        
        Listing updatedListing = listingRepository.save(listing);
        
        auditLogService.logAction(user, "LISTING_UPDATED", "Listing", 
                                 id.toString(), 
                                 "Listing updated: " + listing.getTitle(), request);
        
        return updatedListing;
    }
    
    @Transactional
    public void activateListing(Long id, User adminUser, HttpServletRequest request) {
        Listing listing = listingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        listing.setStatus(Listing.ListingStatus.ACTIVE);
        listingRepository.save(listing);
        
        auditLogService.logAction(adminUser, "LISTING_ACTIVATED", "Listing", 
                                 id.toString(), 
                                 "Listing activated", request);
    }
    
    public ListingDTO convertToDTO(Listing listing) {
        ListingDTO dto = new ListingDTO();
        dto.setId(listing.getId());
        dto.setTitle(listing.getTitle());
        dto.setDescription(listing.getDescription());
        dto.setType(listing.getType());
        dto.setPrice(listing.getPrice());
        dto.setStartingBid(listing.getStartingBid());
        dto.setCurrentBid(listing.getCurrentBid());
        dto.setWebsiteUrl(listing.getWebsiteUrl());
        dto.setImageUrl(listing.getImageUrl());
        dto.setCategory(listing.getCategory());
        dto.setFeatured(listing.getFeatured());
        dto.setStatus(listing.getStatus());
        dto.setSellerId(listing.getSeller().getId());
        dto.setSellerName(listing.getSeller().getFullName());
        dto.setCreatedAt(listing.getCreatedAt());
        dto.setAuctionEndDate(listing.getAuctionEndDate());
        
        if (listing.getWebsiteInfo() != null) {
            WebsiteInfoDTO infoDTO = new WebsiteInfoDTO();
            WebsiteInfo info = listing.getWebsiteInfo();
            infoDTO.setDomain(info.getDomain());
            infoDTO.setPlatform(info.getPlatform());
            infoDTO.setMonthlyRevenue(info.getMonthlyRevenue());
            infoDTO.setMonthlyTraffic(info.getMonthlyTraffic());
            infoDTO.setTrafficDataJson(info.getTrafficDataJson());
            infoDTO.setRevenueDataJson(info.getRevenueDataJson());
            dto.setWebsiteInfo(infoDTO);
        }
        
        return dto;
    }
}

