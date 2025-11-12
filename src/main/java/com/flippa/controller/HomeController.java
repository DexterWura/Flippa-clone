package com.flippa.controller;

import com.flippa.dto.ListingDTO;
import com.flippa.entity.Listing;
import com.flippa.entity.User;
import com.flippa.service.ListingService;
import com.flippa.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {
    
    private final ListingService listingService;
    private final UserService userService;
    
    public HomeController(ListingService listingService, UserService userService) {
        this.listingService = listingService;
        this.userService = userService;
    }
    
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // Simplified: Don't call service methods that might fail
        // Home page is now standalone HTML and doesn't need these attributes
        // model.addAttribute("featuredListings", java.util.Collections.emptyList());
        // model.addAttribute("activeListings", java.util.Collections.emptyList());
        return "home";
    }
    
    @GetMapping("/listings")
    public String listings(@RequestParam(required = false) String search, Model model) {
        List<Listing> listings;
        if (search != null && !search.isEmpty()) {
            listings = listingService.searchListings(search);
        } else {
            listings = listingService.getAllActiveListings();
        }
        
        model.addAttribute("listings", listings);
        model.addAttribute("search", search);
        return "listings";
    }
    
    @GetMapping("/listings/{id}")
    public String listingDetails(@PathVariable Long id, Model model, Authentication authentication) {
        Listing listing = listingService.findById(id)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        // Check if current user is the seller
        boolean isSeller = false;
        if (authentication != null && authentication.isAuthenticated()) {
            Optional<User> currentUser = userService.findByEmail(authentication.getName());
            if (currentUser.isPresent() && currentUser.get().getId().equals(listing.getSeller().getId())) {
                isSeller = true;
            }
        }
        
        ListingDTO listingDTO = listingService.convertToDTO(listing);
        model.addAttribute("listing", listingDTO);
        model.addAttribute("isSeller", isSeller);
        
        // Add images separately for template access
        if (listing.getListingImages() != null && !listing.getListingImages().isEmpty()) {
            model.addAttribute("listingImages", listing.getListingImages());
        }
        
        return "listing-details";
    }
}

