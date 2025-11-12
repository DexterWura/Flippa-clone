package com.flippa.controller;

import com.flippa.dto.ListingDTO;
import com.flippa.entity.Listing;
import com.flippa.entity.User;
import com.flippa.service.ListingService;
import com.flippa.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/my-listings")
public class ListingController {
    
    private final ListingService listingService;
    private final UserService userService;
    
    public ListingController(ListingService listingService, UserService userService) {
        this.listingService = listingService;
        this.userService = userService;
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
        return "listing-form";
    }
    
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("listing") ListingDTO listingDTO,
                       BindingResult result, Authentication authentication,
                       HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "listing-form";
        }
        
        try {
            User user = getCurrentUser(authentication);
            listingService.createListing(listingDTO, user, request);
            redirectAttributes.addFlashAttribute("success", "Listing created successfully!");
            return "redirect:/my-listings";
        } catch (Exception e) {
            result.reject("error.creation", "Failed to create listing: " + e.getMessage());
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
        return "listing-form";
    }
    
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("listing") ListingDTO listingDTO,
                        BindingResult result, Authentication authentication,
                        HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "listing-form";
        }
        
        try {
            User user = getCurrentUser(authentication);
            listingService.updateListing(id, listingDTO, user, request);
            redirectAttributes.addFlashAttribute("success", "Listing updated successfully!");
            return "redirect:/my-listings";
        } catch (Exception e) {
            result.reject("error.update", "Failed to update listing: " + e.getMessage());
            return "listing-form";
        }
    }
    
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

