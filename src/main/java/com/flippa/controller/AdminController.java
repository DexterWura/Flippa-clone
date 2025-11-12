package com.flippa.controller;

import com.flippa.entity.Escrow;
import com.flippa.entity.Listing;
import com.flippa.entity.SystemConfig;
import com.flippa.entity.User;
import com.flippa.service.AdminService;
import com.flippa.service.EscrowService;
import com.flippa.service.ListingService;
import com.flippa.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    private final UserService userService;
    private final ListingService listingService;
    private final EscrowService escrowService;
    
    public AdminController(AdminService adminService, UserService userService,
                          ListingService listingService, EscrowService escrowService) {
        this.adminService = adminService;
        this.userService = userService;
        this.listingService = listingService;
        this.escrowService = escrowService;
    }
    
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.findAll().size());
        model.addAttribute("totalListings", listingService.getAllActiveListings().size());
        model.addAttribute("totalDisputes", escrowService.getDisputes().size());
        return "admin/dashboard";
    }
    
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }
    
    @PostMapping("/users/{id}/ban")
    public String banUser(@PathVariable Long id, @RequestParam String reason,
                         Authentication authentication, HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentUser(authentication);
            userService.banUser(id, reason, admin, request);
            redirectAttributes.addFlashAttribute("success", "User banned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to ban user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
    
    @PostMapping("/users/{id}/unban")
    public String unbanUser(@PathVariable Long id, Authentication authentication,
                           HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentUser(authentication);
            userService.unbanUser(id, admin, request);
            redirectAttributes.addFlashAttribute("success", "User unbanned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to unban user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
    
    @PostMapping("/users/{id}/toggle-role")
    public String toggleUserRole(@PathVariable Long id, @RequestParam String roleType,
                                @RequestParam boolean enable, Authentication authentication,
                                HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentUser(authentication);
            com.flippa.entity.Role.RoleType role = com.flippa.entity.Role.RoleType.valueOf(roleType);
            userService.toggleUserRole(id, role, enable, admin, request);
            redirectAttributes.addFlashAttribute("success", "User role updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update role: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
    
    @GetMapping("/listings")
    public String listings(Model model) {
        model.addAttribute("listings", listingService.getAllActiveListings());
        model.addAttribute("pendingListings", listingService.getPendingReviewListings());
        return "admin/listings";
    }
    
    @PostMapping("/listings/{id}/activate")
    public String activateListing(@PathVariable Long id, Authentication authentication,
                                 HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentUser(authentication);
            listingService.activateListing(id, admin, request);
            redirectAttributes.addFlashAttribute("success", "Listing activated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to activate listing: " + e.getMessage());
        }
        return "redirect:/admin/listings";
    }
    
    @GetMapping("/disputes")
    public String disputes(Model model) {
        model.addAttribute("disputes", escrowService.getDisputes());
        return "admin/disputes";
    }
    
    @PostMapping("/disputes/{id}/resolve")
    public String resolveDispute(@PathVariable Long id, @RequestParam String resolution,
                                @RequestParam String resolutionNotes, @RequestParam String finalStatus,
                                Authentication authentication, HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentUser(authentication);
            Escrow.EscrowStatus status = Escrow.EscrowStatus.valueOf(finalStatus);
            escrowService.resolveDispute(id, resolution, resolutionNotes, status, admin, request);
            redirectAttributes.addFlashAttribute("success", "Dispute resolved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to resolve dispute: " + e.getMessage());
        }
        return "redirect:/admin/disputes";
    }
    
    @GetMapping("/settings")
    public String settings(Model model) {
        List<SystemConfig> allConfigs = adminService.getAllConfigs();
        model.addAttribute("configs", allConfigs);
        
        // Extract system name config
        SystemConfig systemName = allConfigs.stream()
            .filter(c -> c.getConfigKey().equals("system.name"))
            .findFirst().orElse(null);
        
        // Extract payment gateway configs for easier template access
        SystemConfig paypalEnabled = allConfigs.stream()
            .filter(c -> c.getConfigKey().equals("payment.gateway.paypal.enabled"))
            .findFirst().orElse(null);
        SystemConfig paypalClientId = allConfigs.stream()
            .filter(c -> c.getConfigKey().equals("payment.gateway.paypal.client-id"))
            .findFirst().orElse(null);
        SystemConfig paypalClientSecret = allConfigs.stream()
            .filter(c -> c.getConfigKey().equals("payment.gateway.paypal.client-secret"))
            .findFirst().orElse(null);
        SystemConfig paypalMode = allConfigs.stream()
            .filter(c -> c.getConfigKey().equals("payment.gateway.paypal.mode"))
            .findFirst().orElse(null);
        
        SystemConfig paynowEnabled = allConfigs.stream()
            .filter(c -> c.getConfigKey().equals("payment.gateway.paynow-zim.enabled"))
            .findFirst().orElse(null);
        SystemConfig paynowIntegrationId = allConfigs.stream()
            .filter(c -> c.getConfigKey().equals("payment.gateway.paynow-zim.integration-id"))
            .findFirst().orElse(null);
        SystemConfig paynowIntegrationKey = allConfigs.stream()
            .filter(c -> c.getConfigKey().equals("payment.gateway.paynow-zim.integration-key"))
            .findFirst().orElse(null);
        SystemConfig paynowReturnUrl = allConfigs.stream()
            .filter(c -> c.getConfigKey().equals("payment.gateway.paynow-zim.return-url"))
            .findFirst().orElse(null);
        SystemConfig paynowResultUrl = allConfigs.stream()
            .filter(c -> c.getConfigKey().equals("payment.gateway.paynow-zim.result-url"))
            .findFirst().orElse(null);
        
        model.addAttribute("systemName", systemName);
        model.addAttribute("paypalEnabled", paypalEnabled);
        model.addAttribute("paypalClientId", paypalClientId);
        model.addAttribute("paypalClientSecret", paypalClientSecret);
        model.addAttribute("paypalMode", paypalMode);
        model.addAttribute("paynowEnabled", paynowEnabled);
        model.addAttribute("paynowIntegrationId", paynowIntegrationId);
        model.addAttribute("paynowIntegrationKey", paynowIntegrationKey);
        model.addAttribute("paynowReturnUrl", paynowReturnUrl);
        model.addAttribute("paynowResultUrl", paynowResultUrl);
        
        return "admin/settings";
    }
    
    @PostMapping("/settings/update")
    public String updateSetting(@RequestParam String key, @RequestParam String value,
                               @RequestParam(required = false) String description,
                               Authentication authentication, HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentUser(authentication);
            adminService.updateConfig(key, value, description, admin, request);
            redirectAttributes.addFlashAttribute("success", "Setting updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update setting: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }
    
    @PostMapping("/settings/{key}/toggle")
    public String toggleSetting(@PathVariable String key, @RequestParam boolean enabled,
                               Authentication authentication, HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentUser(authentication);
            adminService.toggleConfig(key, enabled, admin, request);
            redirectAttributes.addFlashAttribute("success", "Setting toggled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to toggle setting: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }
    
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

