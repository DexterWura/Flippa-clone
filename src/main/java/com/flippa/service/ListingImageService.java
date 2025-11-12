package com.flippa.service;

import com.flippa.entity.Listing;
import com.flippa.entity.ListingImage;
import com.flippa.repository.ListingImageRepository;
import com.flippa.repository.ListingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ListingImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ListingImageService.class);
    private final ListingImageRepository listingImageRepository;
    private final ListingRepository listingRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    
    public ListingImageService(ListingImageRepository listingImageRepository,
                              ListingRepository listingRepository,
                              FileStorageService fileStorageService,
                              AuditLogService auditLogService) {
        this.listingImageRepository = listingImageRepository;
        this.listingRepository = listingRepository;
        this.fileStorageService = fileStorageService;
        this.auditLogService = auditLogService;
    }
    
    @Transactional
    public ListingImage uploadImage(Long listingId, MultipartFile file, boolean isPrimary,
                                   com.flippa.entity.User user, HttpServletRequest request) throws IOException {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        if (!listing.getSeller().getId().equals(user.getId()) && 
            !user.getRoles().stream().anyMatch(r -> r.getName().name().contains("ADMIN"))) {
            throw new RuntimeException("Unauthorized to upload images for this listing");
        }
        
        // Store file
        String filePath = fileStorageService.storeFile(file, "listings/" + listingId);
        
        // Create image entity
        ListingImage image = new ListingImage();
        image.setListing(listing);
        image.setFilePath(filePath);
        image.setFileName(file.getOriginalFilename());
        image.setFileSize(file.getSize());
        image.setContentType(file.getContentType());
        image.setIsPrimary(isPrimary);
        
        // Set display order
        List<ListingImage> existingImages = listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listingId);
        image.setDisplayOrder(existingImages.size());
        
        // If this is primary, unset other primary images
        if (isPrimary) {
            listingImageRepository.findByListingIdAndIsPrimaryTrue(listingId)
                .ifPresent(existingPrimary -> {
                    existingPrimary.setIsPrimary(false);
                    listingImageRepository.save(existingPrimary);
                });
        }
        
        ListingImage savedImage = listingImageRepository.save(image);
        
        auditLogService.logAction(user, "LISTING_IMAGE_UPLOADED", "ListingImage", 
                                 savedImage.getId().toString(), 
                                 "Image uploaded for listing: " + listingId, request);
        
        logger.info("Image uploaded for listing {}: {}", listingId, filePath);
        return savedImage;
    }
    
    @Transactional
    public void deleteImage(Long imageId, com.flippa.entity.User user, HttpServletRequest request) {
        ListingImage image = listingImageRepository.findById(imageId)
            .orElseThrow(() -> new RuntimeException("Image not found"));
        
        Listing listing = image.getListing();
        if (!listing.getSeller().getId().equals(user.getId()) && 
            !user.getRoles().stream().anyMatch(r -> r.getName().name().contains("ADMIN"))) {
            throw new RuntimeException("Unauthorized to delete this image");
        }
        
        // Delete file from storage
        fileStorageService.deleteFile(image.getFilePath());
        
        // Delete from database
        listingImageRepository.delete(image);
        
        auditLogService.logAction(user, "LISTING_IMAGE_DELETED", "ListingImage", 
                                 imageId.toString(), 
                                 "Image deleted for listing: " + listing.getId(), request);
        
        logger.info("Image deleted: {}", imageId);
    }
    
    @Transactional
    public void setPrimaryImage(Long imageId, com.flippa.entity.User user, HttpServletRequest request) {
        ListingImage image = listingImageRepository.findById(imageId)
            .orElseThrow(() -> new RuntimeException("Image not found"));
        
        Listing listing = image.getListing();
        if (!listing.getSeller().getId().equals(user.getId()) && 
            !user.getRoles().stream().anyMatch(r -> r.getName().name().contains("ADMIN"))) {
            throw new RuntimeException("Unauthorized to modify this image");
        }
        
        // Unset other primary images
        listingImageRepository.findByListingIdAndIsPrimaryTrue(listing.getId())
            .ifPresent(existingPrimary -> {
                existingPrimary.setIsPrimary(false);
                listingImageRepository.save(existingPrimary);
            });
        
        // Set this as primary
        image.setIsPrimary(true);
        listingImageRepository.save(image);
        
        auditLogService.logAction(user, "LISTING_IMAGE_SET_PRIMARY", "ListingImage", 
                                 imageId.toString(), 
                                 "Primary image set for listing: " + listing.getId(), request);
        
        logger.info("Primary image set for listing {}: {}", listing.getId(), imageId);
    }
    
    public List<ListingImage> getListingImages(Long listingId) {
        return listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listingId);
    }
    
    public Optional<ListingImage> getPrimaryImage(Long listingId) {
        return listingImageRepository.findByListingIdAndIsPrimaryTrue(listingId);
    }
}

