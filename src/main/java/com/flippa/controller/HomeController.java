package com.flippa.controller;

import com.flippa.entity.Listing;
import com.flippa.service.ListingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {
    
    private final ListingService listingService;
    
    public HomeController(ListingService listingService) {
        this.listingService = listingService;
    }
    
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        try {
            List<Listing> featuredListings = listingService.getFeaturedListings();
            List<Listing> activeListings = listingService.getAllActiveListings();
            
            model.addAttribute("featuredListings", featuredListings != null ? featuredListings : java.util.Collections.emptyList());
            model.addAttribute("activeListings", activeListings != null ? activeListings : java.util.Collections.emptyList());
            return "home";
        } catch (Exception e) {
            // Log error and return empty lists
            org.slf4j.LoggerFactory.getLogger(HomeController.class).error("Error loading home page: {}", e.getMessage(), e);
            model.addAttribute("featuredListings", java.util.Collections.emptyList());
            model.addAttribute("activeListings", java.util.Collections.emptyList());
            return "home";
        }
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
    public String listingDetails(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        Listing listing = listingService.findById(id)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        model.addAttribute("listing", listingService.convertToDTO(listing));
        return "listing-details";
    }
}

