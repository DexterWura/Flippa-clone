package com.flippa.service;

import com.flippa.entity.AuditLog;
import com.flippa.entity.User;
import com.flippa.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository auditLogRepository;
    
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    @Transactional
    public void logAction(User user, String action, String entityType, String entityId, 
                         String description, HttpServletRequest request) {
        try {
            AuditLog log = new AuditLog();
            log.setUser(user);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDescription(description);
            log.setLevel(AuditLog.LogLevel.INFO);
            
            if (request != null) {
                log.setIpAddress(getClientIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }
            
            auditLogRepository.save(log);
            logger.info("Audit log created: {} - {} - {}", action, entityType, entityId);
        } catch (Exception e) {
            logger.error("Failed to create audit log", e);
        }
    }
    
    @Transactional
    public void logError(User user, String action, String entityType, String entityId, 
                        String description, HttpServletRequest request) {
        try {
            AuditLog log = new AuditLog();
            log.setUser(user);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDescription(description);
            log.setLevel(AuditLog.LogLevel.ERROR);
            
            if (request != null) {
                log.setIpAddress(getClientIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }
            
            auditLogRepository.save(log);
            logger.error("Audit error log created: {} - {} - {}", action, entityType, entityId);
        } catch (Exception e) {
            logger.error("Failed to create audit error log", e);
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

