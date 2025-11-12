package com.flippa.config;

import com.flippa.entity.Role;
import com.flippa.entity.SystemConfig;
import com.flippa.entity.User;
import com.flippa.repository.RoleRepository;
import com.flippa.repository.SystemConfigRepository;
import com.flippa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Data initializer to ensure admin users have correct password hashes and roles.
 * This runs after Flyway migrations to fix any password/role issues.
 */
@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final PasswordEncoder passwordEncoder;
    
    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                          SystemConfigRepository systemConfigRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Initializing application data...");
        
        // Ensure system name config exists
        ensureSystemNameConfig();
        
        // Fix admin users - ensure they have correct password hash and roles
        fixAdminUsers();
        
        logger.info("Data initialization complete.");
    }
    
    private void ensureSystemNameConfig() {
        systemConfigRepository.findByConfigKey("system.name")
            .orElseGet(() -> {
                SystemConfig config = new SystemConfig();
                config.setConfigKey("system.name");
                config.setConfigValue("Flippa Clone");
                config.setDescription("System/Application name displayed throughout the application");
                config.setEnabled(true);
                config.setConfigType("STRING");
                return systemConfigRepository.save(config);
            });
        
        // Ensure auto-approve is enabled by default
        systemConfigRepository.findByConfigKey("listing.auto-approve.enabled")
            .ifPresentOrElse(config -> {
                // Update to enabled if it's disabled
                if (!config.getEnabled() || !"true".equalsIgnoreCase(config.getConfigValue())) {
                    config.setConfigValue("true");
                    config.setEnabled(true);
                    systemConfigRepository.save(config);
                    logger.info("Enabled auto-approve for listings");
                }
            }, () -> {
                SystemConfig config = new SystemConfig();
                config.setConfigKey("listing.auto-approve.enabled");
                config.setConfigValue("true");
                config.setDescription("Auto-approve listings without admin review");
                config.setEnabled(true);
                config.setConfigType("BOOLEAN");
                systemConfigRepository.save(config);
                logger.info("Created auto-approve config (enabled)");
            });
    }
    
    private void fixAdminUsers() {
        // Fix admin@flippa.com
        userRepository.findByEmail("admin@flippa.com").ifPresent(user -> {
            boolean updated = false;
            
            // Update password hash if it doesn't match
            String correctHash = passwordEncoder.encode("password");
            if (!passwordEncoder.matches("password", user.getPassword())) {
                user.setPassword(correctHash);
                updated = true;
                logger.info("Updated password hash for admin@flippa.com");
            }
            
            // Ensure SUPER_ADMIN role is assigned
            Role superAdminRole = roleRepository.findByName(Role.RoleType.ROLE_SUPER_ADMIN)
                .orElseThrow(() -> new RuntimeException("ROLE_SUPER_ADMIN not found"));
            
            if (!user.getRoles().contains(superAdminRole)) {
                user.getRoles().add(superAdminRole);
                updated = true;
                logger.info("Assigned ROLE_SUPER_ADMIN to admin@flippa.com");
            }
            
            if (updated) {
                userRepository.save(user);
            }
        });
        
        // Fix admin user (email: admin)
        userRepository.findByEmail("admin").ifPresent(user -> {
            boolean updated = false;
            
            // Update password hash if it doesn't match
            if (!passwordEncoder.matches("password", user.getPassword())) {
                user.setPassword(passwordEncoder.encode("password"));
                updated = true;
                logger.info("Updated password hash for admin");
            }
            
            // Ensure SUPER_ADMIN role is assigned
            Role superAdminRole = roleRepository.findByName(Role.RoleType.ROLE_SUPER_ADMIN)
                .orElseThrow(() -> new RuntimeException("ROLE_SUPER_ADMIN not found"));
            
            if (!user.getRoles().contains(superAdminRole)) {
                user.getRoles().add(superAdminRole);
                updated = true;
                logger.info("Assigned ROLE_SUPER_ADMIN to admin");
            }
            
            if (updated) {
                userRepository.save(user);
            }
        });
        
        // Fix test user
        userRepository.findByEmail("test@flippa.com").ifPresent(user -> {
            boolean updated = false;
            
            // Update password hash if it doesn't match
            if (!passwordEncoder.matches("password", user.getPassword())) {
                user.setPassword(passwordEncoder.encode("password"));
                updated = true;
                logger.info("Updated password hash for test@flippa.com");
            }
            
            // Ensure basic roles are assigned
            Set<Role> basicRoles = new HashSet<>();
            roleRepository.findByName(Role.RoleType.ROLE_USER).ifPresent(basicRoles::add);
            roleRepository.findByName(Role.RoleType.ROLE_BUYER).ifPresent(basicRoles::add);
            roleRepository.findByName(Role.RoleType.ROLE_SELLER).ifPresent(basicRoles::add);
            
            boolean rolesChanged = false;
            for (Role role : basicRoles) {
                if (!user.getRoles().contains(role)) {
                    user.getRoles().add(role);
                    rolesChanged = true;
                }
            }
            
            if (rolesChanged) {
                updated = true;
                logger.info("Assigned basic roles to test@flippa.com");
            }
            
            if (updated) {
                userRepository.save(user);
            }
        });
    }
}

