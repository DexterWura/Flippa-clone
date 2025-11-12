package com.flippa.controller;

import com.flippa.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice to inject common attributes into all templates.
 * This ensures the system name is available everywhere.
 */
@ControllerAdvice
public class GlobalControllerAdvice {
    
    private final AdminService adminService;
    
    @Autowired
    public GlobalControllerAdvice(AdminService adminService) {
        this.adminService = adminService;
    }
    
    /**
     * Adds system name to all templates.
     * Falls back to "Flippa Clone" if not configured.
     */
    @ModelAttribute("systemName")
    public String getSystemName() {
        return adminService.getSystemName();
    }
}

