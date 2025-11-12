package com.flippa.controller;

import com.flippa.entity.Escrow;
import com.flippa.entity.Listing;
import com.flippa.entity.User;
import com.flippa.service.EscrowService;
import com.flippa.service.ListingService;
import com.flippa.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/escrow")
public class EscrowController {
    
    private final EscrowService escrowService;
    private final ListingService listingService;
    private final UserService userService;
    
    public EscrowController(EscrowService escrowService, ListingService listingService,
                           UserService userService) {
        this.escrowService = escrowService;
        this.listingService = listingService;
        this.userService = userService;
    }
    
    @PostMapping("/create")
    public String createEscrow(@RequestParam Long listingId,
                              @RequestParam String paymentGateway,
                              @RequestParam(required = false) String buyerNotes,
                              Authentication authentication,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        try {
            User buyer = getCurrentUser(authentication);
            Escrow.PaymentGateway gateway = Escrow.PaymentGateway.valueOf(paymentGateway.toUpperCase());
            
            escrowService.createEscrow(listingId, buyer.getId(), gateway, buyerNotes, request);
            redirectAttributes.addFlashAttribute("success", "Escrow created. Please proceed with payment.");
            return "redirect:/escrow/my-escrows";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create escrow: " + e.getMessage());
            return "redirect:/listings/" + listingId;
        }
    }
    
    @GetMapping("/my-escrows")
    public String myEscrows(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        model.addAttribute("buyerEscrows", escrowService.findByBuyerId(user.getId()));
        model.addAttribute("sellerEscrows", escrowService.findBySellerId(user.getId()));
        return "my-escrows";
    }
    
    @GetMapping("/{id}")
    public String escrowDetails(@PathVariable Long id, Authentication authentication, Model model) {
        Escrow escrow = escrowService.findById(id)
            .orElseThrow(() -> new RuntimeException("Escrow not found"));
        
        User user = getCurrentUser(authentication);
        if (!escrow.getBuyer().getId().equals(user.getId()) && 
            !escrow.getSeller().getId().equals(user.getId())) {
            return "redirect:/escrow/my-escrows";
        }
        
        model.addAttribute("escrow", escrowService.convertToDTO(escrow));
        return "escrow-details";
    }
    
    @PostMapping("/{id}/dispute")
    public String raiseDispute(@PathVariable Long id,
                               @RequestParam String reason,
                               Authentication authentication,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            escrowService.raiseDispute(id, reason, user, request);
            redirectAttributes.addFlashAttribute("success", "Dispute raised successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to raise dispute: " + e.getMessage());
        }
        return "redirect:/escrow/" + id;
    }
    
    @PostMapping("/{id}/complete")
    public String completeTransfer(@PathVariable Long id,
                                  Authentication authentication,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            escrowService.completeTransfer(id, user, request);
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to complete transfer: " + e.getMessage());
        }
        return "redirect:/escrow/" + id;
    }
    
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

