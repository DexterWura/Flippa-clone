package com.flippa.controller;

import com.flippa.dto.UserRegistrationDTO;
import com.flippa.entity.User;
import com.flippa.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/login")
    public String login(Model model, @org.springframework.web.bind.annotation.RequestParam(required = false) String error) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        return "login";
    }
    
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("userRegistration", new UserRegistrationDTO());
        return "register";
    }
    
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("userRegistration") UserRegistrationDTO registrationDTO,
                          BindingResult result, HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register";
        }
        
        try {
            if (userService.findByEmail(registrationDTO.getEmail()).isPresent()) {
                result.rejectValue("email", "error.email", "Email already exists");
                return "register";
            }
            
            userService.registerUser(registrationDTO, request);
            redirectAttributes.addFlashAttribute("success", 
                "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            result.reject("error.registration", "Registration failed: " + e.getMessage());
            return "register";
        }
    }
}

