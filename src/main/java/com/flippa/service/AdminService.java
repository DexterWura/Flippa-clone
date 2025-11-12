package com.flippa.service;

import com.flippa.dto.SystemConfigDTO;
import com.flippa.entity.SystemConfig;
import com.flippa.entity.User;
import com.flippa.repository.SystemConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final SystemConfigRepository systemConfigRepository;
    private final AuditLogService auditLogService;
    
    public AdminService(SystemConfigRepository systemConfigRepository, 
                       AuditLogService auditLogService) {
        this.systemConfigRepository = systemConfigRepository;
        this.auditLogService = auditLogService;
    }
    
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }
    
    public Optional<SystemConfig> getConfigByKey(String key) {
        return systemConfigRepository.findByConfigKey(key);
    }
    
    @Transactional
    public SystemConfig updateConfig(String key, String value, String description,
                                     User adminUser, HttpServletRequest request) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
            .orElse(new SystemConfig());
        
        if (config.getId() == null) {
            config.setConfigKey(key);
        }
        
        config.setConfigValue(value);
        if (description != null) {
            config.setDescription(description);
        }
        config.setUpdatedBy(adminUser);
        
        SystemConfig savedConfig = systemConfigRepository.save(config);
        
        auditLogService.logAction(adminUser, "CONFIG_UPDATED", "SystemConfig", 
                                 key, 
                                 "Config updated: " + key + " = " + value, request);
        
        logger.info("System config updated: {} = {} by admin: {}", key, value, adminUser.getEmail());
        return savedConfig;
    }
    
    @Transactional
    public void toggleConfig(String key, boolean enabled, User adminUser, HttpServletRequest request) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
            .orElseThrow(() -> new RuntimeException("Config not found: " + key));
        
        config.setEnabled(enabled);
        config.setUpdatedBy(adminUser);
        systemConfigRepository.save(config);
        
        auditLogService.logAction(adminUser, "CONFIG_TOGGLED", "SystemConfig", 
                                 key, 
                                 "Config toggled: " + key + " = " + enabled, request);
        
        logger.info("System config toggled: {} = {} by admin: {}", key, enabled, adminUser.getEmail());
    }
    
    public boolean isPaymentGatewayEnabled(String gateway) {
        Optional<SystemConfig> config = systemConfigRepository.findByConfigKey(
            "payment.gateway." + gateway.toLowerCase() + ".enabled");
        return config.map(SystemConfig::getEnabled).orElse(true);
    }
    
    public String getPaymentConfigValue(String gateway, String configName) {
        String key = "payment.gateway." + gateway.toLowerCase() + "." + configName;
        return systemConfigRepository.findByConfigKey(key)
            .map(SystemConfig::getConfigValue)
            .orElse("");
    }
    
    public String getPaymentConfigValue(String gateway, String configName, String defaultValue) {
        String key = "payment.gateway." + gateway.toLowerCase() + "." + configName;
        return systemConfigRepository.findByConfigKey(key)
            .map(SystemConfig::getConfigValue)
            .orElse(defaultValue);
    }
    
    public SystemConfigDTO convertToDTO(SystemConfig config) {
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setId(config.getId());
        dto.setConfigKey(config.getConfigKey());
        dto.setConfigValue(config.getConfigValue());
        dto.setDescription(config.getDescription());
        dto.setEnabled(config.getEnabled());
        dto.setConfigType(config.getConfigType());
        return dto;
    }
    
    /**
     * Gets the system name from configuration.
     * Falls back to "Flippa Clone" if not configured.
     */
    public String getSystemName() {
        return systemConfigRepository.findByConfigKey("system.name")
            .map(SystemConfig::getConfigValue)
            .filter(value -> value != null && !value.trim().isEmpty())
            .orElse("Flippa Clone");
    }
    
    /**
     * Checks if auto-approve for listings is enabled.
     * Defaults to false if not configured.
     */
    public boolean isAutoApproveEnabled() {
        return systemConfigRepository.findByConfigKey("listing.auto-approve.enabled")
            .map(config -> {
                if (config.getEnabled() && "true".equalsIgnoreCase(config.getConfigValue())) {
                    return true;
                }
                return false;
            })
            .orElse(false);
    }
}

