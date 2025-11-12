package com.flippa.controller;

import com.flippa.dto.ListingDTO;
import com.flippa.entity.Listing;
import com.flippa.entity.SocialMediaVerification;
import com.flippa.entity.User;
import com.flippa.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/my-listings")
public class ListingController {
    
    private final ListingService listingService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ListingImageService listingImageService;
    private final DomainVerificationService domainVerificationService;
    private final SocialMediaVerificationService socialMediaVerificationService;
    
    public ListingController(ListingService listingService, UserService userService,
                           CategoryService categoryService, ListingImageService listingImageService,
                           DomainVerificationService domainVerificationService,
                           SocialMediaVerificationService socialMediaVerificationService) {
        this.listingService = listingService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.listingImageService = listingImageService;
        this.domainVerificationService = domainVerificationService;
        this.socialMediaVerificationService = socialMediaVerificationService;
    }
    
    @GetMapping
    public String myListings(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        List<Listing> listings = listingService.findBySellerId(user.getId());
        model.addAttribute("listings", listings);
        return "my-listings";
    }
    
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("listing", new ListingDTO());
        model.addAttribute("categories", categoryService.getAllEnabledCategories());
        return "listing-form";
    }
    
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("listing") ListingDTO listingDTO,
                       BindingResult result, Authentication authentication,
                       HttpServletRequest request, RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllEnabledCategories());
            return "listing-form";
        }
        
        try {
            User user = getCurrentUser(authentication);
            Listing createdListing = listingService.createListing(listingDTO, user, request);
            
            // Show appropriate message based on listing status
            if (createdListing.getStatus() == Listing.ListingStatus.ACTIVE) {
                redirectAttributes.addFlashAttribute("success", 
                    "Listing created successfully and is now live! View it on the listings page.");
            } else if (createdListing.getStatus() == Listing.ListingStatus.DRAFT) {
                redirectAttributes.addFlashAttribute("success", 
                    "Listing created! Please verify your domain/website ownership to activate it.");
                return "redirect:/my-listings/" + createdListing.getId() + "/verify";
            } else {
                redirectAttributes.addFlashAttribute("success", 
                    "Listing created successfully! It's pending admin review and will be visible once approved.");
            }
            
            return "redirect:/my-listings";
        } catch (Exception e) {
            result.reject("error.creation", "Failed to create listing: " + e.getMessage());
            model.addAttribute("categories", categoryService.getAllEnabledCategories());
            return "listing-form";
        }
    }
    
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        Listing listing = listingService.findById(id)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        if (!listing.getSeller().getId().equals(user.getId())) {
            return "redirect:/my-listings";
        }
        
        model.addAttribute("listing", listingService.convertToDTO(listing));
        model.addAttribute("categories", categoryService.getAllEnabledCategories());
        model.addAttribute("images", listingImageService.getListingImages(id));
        return "listing-form";
    }
    
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("listing") ListingDTO listingDTO,
                        BindingResult result, Authentication authentication,
                        HttpServletRequest request, RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllEnabledCategories());
            model.addAttribute("images", listingImageService.getListingImages(id));
            return "listing-form";
        }
        
        try {
            User user = getCurrentUser(authentication);
            listingService.updateListing(id, listingDTO, user, request);
            redirectAttributes.addFlashAttribute("success", "Listing updated successfully!");
            return "redirect:/my-listings";
        } catch (Exception e) {
            result.reject("error.update", "Failed to update listing: " + e.getMessage());
            model.addAttribute("categories", categoryService.getAllEnabledCategories());
            model.addAttribute("images", listingImageService.getListingImages(id));
            return "listing-form";
        }
    }
    
    @PostMapping("/{id}/images")
    public String uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                             @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
                             Authentication authentication, HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            listingImageService.uploadImage(id, file, isPrimary, user, request);
            redirectAttributes.addFlashAttribute("success", "Image uploaded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload image: " + e.getMessage());
        }
        return "redirect:/my-listings/" + id + "/edit";
    }
    
    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Long id, @PathVariable Long imageId,
                             Authentication authentication, HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            listingImageService.deleteImage(imageId, user, request);
            redirectAttributes.addFlashAttribute("success", "Image deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete image: " + e.getMessage());
        }
        return "redirect:/my-listings/" + id + "/edit";
    }
    
    @PostMapping("/{id}/images/{imageId}/set-primary")
    public String setPrimaryImage(@PathVariable Long id, @PathVariable Long imageId,
                                 Authentication authentication, HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            listingImageService.setPrimaryImage(imageId, user, request);
            redirectAttributes.addFlashAttribute("success", "Primary image set successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to set primary image: " + e.getMessage());
        }
        return "redirect:/my-listings/" + id + "/edit";
    }
    
    @GetMapping("/{id}/verify")
    public String verifyListing(@PathVariable Long id, Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        Listing listing = listingService.findById(id)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        if (!listing.getSeller().getId().equals(user.getId())) {
            return "redirect:/my-listings";
        }
        
        model.addAttribute("listing", listingService.convertToDTO(listing));
        
        // Check verification type
        if (listing.getType() == Listing.ListingType.WEBSITE || listing.getType() == Listing.ListingType.DOMAIN) {
            domainVerificationService.findByListingId(id).ifPresent(verification -> {
                model.addAttribute("domainVerification", verification);
                model.addAttribute("verificationTxt", domainVerificationService.generateVerificationTxtContent(verification.getVerificationToken()));
            });
        } else if (listing.getType() == Listing.ListingType.SOCIAL_MEDIA_ACCOUNT) {
            socialMediaVerificationService.findByListingId(id).ifPresent(verification -> {
                model.addAttribute("socialMediaVerification", verification);
            });
        }
        
        return "listing-verify";
    }
    
    @PostMapping("/{id}/verify/domain")
    public String verifyDomain(@PathVariable Long id, @RequestParam String domain,
                              Authentication authentication, HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            domainVerificationService.createVerificationRequest(id, domain, user, request);
            redirectAttributes.addFlashAttribute("success", "Verification request created! Download the TXT file and upload it to your domain.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create verification: " + e.getMessage());
        }
        return "redirect:/my-listings/" + id + "/verify";
    }
    
    @PostMapping("/{id}/verify/domain/check")
    public String checkDomainVerification(@PathVariable Long id, Authentication authentication,
                                         HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            boolean verified = domainVerificationService.verifyDomain(id, user, request);
            if (verified) {
                redirectAttributes.addFlashAttribute("success", "Domain verified successfully! Your listing is now active.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Verification failed. Please ensure the TXT file is accessible at your domain.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to verify domain: " + e.getMessage());
        }
        return "redirect:/my-listings/" + id + "/verify";
    }
    
    @PostMapping("/{id}/verify/social")
    public String verifySocialMedia(@PathVariable Long id, @RequestParam String platform,
                                   @RequestParam String accountUrl, @RequestParam String accountUsername,
                                   Authentication authentication, HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            SocialMediaVerification.Platform platformEnum = SocialMediaVerification.Platform.valueOf(platform.toUpperCase());
            socialMediaVerificationService.createVerificationRequest(id, platformEnum, accountUrl, accountUsername, user, request);
            redirectAttributes.addFlashAttribute("success", "Verification request created! Please complete OAuth login.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create verification: " + e.getMessage());
        }
        return "redirect:/my-listings/" + id + "/verify";
    }
    
    @PostMapping("/{id}/verify/social/confirm")
    public String confirmSocialMediaVerification(@PathVariable Long id, Authentication authentication,
                                                HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            boolean verified = socialMediaVerificationService.verifySocialMediaAccount(id, user, request);
            if (verified) {
                redirectAttributes.addFlashAttribute("success", "Social media account verified successfully! Your listing is now active.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Verification failed. Please try again.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to verify social media account: " + e.getMessage());
        }
        return "redirect:/my-listings/" + id + "/verify";
    }
    
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

