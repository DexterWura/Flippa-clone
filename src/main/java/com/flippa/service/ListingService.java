package com.flippa.service;

import com.flippa.dto.ListingDTO;
import com.flippa.dto.WebsiteInfoDTO;
import com.flippa.entity.Category;
import com.flippa.entity.Listing;
import com.flippa.entity.ListingImage;
import com.flippa.entity.User;
import com.flippa.entity.WebsiteInfo;
import com.flippa.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;
    private final WebsiteInfoFetchService websiteInfoFetchService;
    private final AuditLogService auditLogService;
    private final AdminService adminService;
    
    public ListingService(ListingRepository listingRepository, 
                         WebsiteInfoRepository websiteInfoRepository,
                         CategoryRepository categoryRepository,
                         WebsiteInfoFetchService websiteInfoFetchService,
                         AuditLogService auditLogService,
                         AdminService adminService) {
        this.listingRepository = listingRepository;
        this.websiteInfoRepository = websiteInfoRepository;
        this.categoryRepository = categoryRepository;
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
        listing.setFeatured(listingDTO.getFeatured() != null ? listingDTO.getFeatured() : false);
        listing.setListingMode(listingDTO.getListingMode() != null ? listingDTO.getListingMode() : Listing.ListingMode.NORMAL);
        
        // Set category
        if (listingDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(listingDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
            listing.setCategory(category);
        }
        
        // Handle auction mode
        if (listing.getListingMode() == Listing.ListingMode.AUCTION) {
            if (listingDTO.getStartingBid() == null) {
                throw new RuntimeException("Starting bid is required for auction listings");
            }
            listing.setStartingBid(listingDTO.getStartingBid());
            listing.setCurrentBid(listingDTO.getStartingBid());
            
            // Calculate auction end date
            if (listingDTO.getAuctionDays() != null && listingDTO.getAuctionDays() > 0) {
                listing.setAuctionEndDate(java.time.LocalDateTime.now().plusDays(listingDTO.getAuctionDays()));
            } else {
                throw new RuntimeException("Auction days must be specified for auction listings");
            }
        } else {
            listing.setAuctionEndDate(listingDTO.getAuctionEndDate());
        }
        
        // For WEBSITE and DOMAIN types, require verification before activation
        boolean requiresVerification = listingDTO.getType() == Listing.ListingType.WEBSITE || 
                                       listingDTO.getType() == Listing.ListingType.DOMAIN;
        
        if (requiresVerification) {
            // Listing must be verified before it can be active
            listing.setStatus(Listing.ListingStatus.DRAFT);
            listing.setVerified(false);
            logger.info("Listing set to DRAFT - requires domain/website verification");
        } else {
            // Check if auto-approve is enabled
            boolean autoApprove = adminService.isAutoApproveEnabled();
            if (autoApprove) {
                listing.setStatus(Listing.ListingStatus.ACTIVE);
                logger.info("Listing auto-approved and set to ACTIVE (auto-approve enabled)");
            } else {
                listing.setStatus(Listing.ListingStatus.PENDING_REVIEW);
                logger.info("Listing set to PENDING_REVIEW (auto-approve disabled - requires admin approval)");
            }
        }
        
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
    
    public List<Listing> getPendingReviewListings() {
        return listingRepository.findByStatus(Listing.ListingStatus.PENDING_REVIEW);
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
        
        // Update category
        if (listingDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(listingDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
            listing.setCategory(category);
        } else {
            listing.setCategory(null);
        }
        
        // Update listing mode and auction settings
        if (listingDTO.getListingMode() != null) {
            listing.setListingMode(listingDTO.getListingMode());
            if (listingDTO.getListingMode() == Listing.ListingMode.AUCTION) {
                if (listingDTO.getStartingBid() != null) {
                    listing.setStartingBid(listingDTO.getStartingBid());
                }
                if (listingDTO.getAuctionDays() != null && listingDTO.getAuctionDays() > 0) {
                    listing.setAuctionEndDate(java.time.LocalDateTime.now().plusDays(listingDTO.getAuctionDays()));
                }
            }
        }
        
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
        dto.setFeatured(listing.getFeatured());
        dto.setStatus(listing.getStatus());
        dto.setSellerId(listing.getSeller().getId());
        dto.setSellerName(listing.getSeller().getFullName());
        dto.setCreatedAt(listing.getCreatedAt());
        dto.setAuctionEndDate(listing.getAuctionEndDate());
        dto.setListingMode(listing.getListingMode());
        dto.setVerified(listing.getVerified());
        dto.setRequiresVerification(listing.getType() == Listing.ListingType.WEBSITE || 
                                    listing.getType() == Listing.ListingType.DOMAIN);
        
        // Set category
        if (listing.getCategory() != null) {
            dto.setCategoryId(listing.getCategory().getId());
            dto.setCategoryName(listing.getCategory().getName());
        }
        
        // Set listing images
        if (listing.getListingImages() != null && !listing.getListingImages().isEmpty()) {
            dto.setListingImages(new java.util.ArrayList<>(listing.getListingImages()));
        }
        
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

