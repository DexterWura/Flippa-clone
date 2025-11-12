package com.flippa.service;

import com.flippa.dto.UserRegistrationDTO;
import com.flippa.entity.Role;
import com.flippa.entity.User;
import com.flippa.repository.RoleRepository;
import com.flippa.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    
    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }
    
    @Transactional
    public User registerUser(UserRegistrationDTO registrationDTO, HttpServletRequest request) {
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        User user = new User();
        user.setEmail(registrationDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setPhoneNumber(registrationDTO.getPhoneNumber());
        user.setEnabled(true);
        user.setBanned(false);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        
        // Assign default roles
        Set<Role> roles = new HashSet<>();
        roleRepository.findByName(Role.RoleType.ROLE_USER).ifPresent(roles::add);
        roleRepository.findByName(Role.RoleType.ROLE_BUYER).ifPresent(roles::add);
        roleRepository.findByName(Role.RoleType.ROLE_SELLER).ifPresent(roles::add);
        user.setRoles(roles);
        
        User savedUser = userRepository.save(user);
        
        auditLogService.logAction(savedUser, "USER_REGISTERED", "User", 
                                 savedUser.getId().toString(), 
                                 "New user registered: " + savedUser.getEmail(), request);
        
        logger.info("User registered successfully: {}", savedUser.getEmail());
        return savedUser;
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    @Transactional
    public void banUser(Long userId, String reason, User adminUser, HttpServletRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setBanned(true);
        user.setBanReason(reason);
        userRepository.save(user);
        
        auditLogService.logAction(adminUser, "USER_BANNED", "User", 
                                 userId.toString(), 
                                 "User banned: " + reason, request);
        
        logger.info("User banned: {} by admin: {}", user.getEmail(), adminUser.getEmail());
    }
    
    @Transactional
    public void unbanUser(Long userId, User adminUser, HttpServletRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setBanned(false);
        user.setBanReason(null);
        userRepository.save(user);
        
        auditLogService.logAction(adminUser, "USER_UNBANNED", "User", 
                                 userId.toString(), 
                                 "User unbanned", request);
        
        logger.info("User unbanned: {} by admin: {}", user.getEmail(), adminUser.getEmail());
    }
    
    @Transactional
    public void toggleUserRole(Long userId, Role.RoleType roleType, boolean enable, 
                               User adminUser, HttpServletRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Role role = roleRepository.findByName(roleType)
            .orElseThrow(() -> new RuntimeException("Role not found"));
        
        if (enable) {
            user.getRoles().add(role);
            auditLogService.logAction(adminUser, "USER_ROLE_ENABLED", "User", 
                                     userId.toString(), 
                                     "Role enabled: " + roleType, request);
        } else {
            user.getRoles().remove(role);
            auditLogService.logAction(adminUser, "USER_ROLE_DISABLED", "User", 
                                     userId.toString(), 
                                     "Role disabled: " + roleType, request);
        }
        
        userRepository.save(user);
        logger.info("User role toggled: {} - {} - {}", user.getEmail(), roleType, enable);
    }
}

