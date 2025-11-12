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
    public String listingDetails(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        Listing listing = listingService.findById(id)
            .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        model.addAttribute("listing", listingService.convertToDTO(listing));
        return "listing-details";
    }
}

