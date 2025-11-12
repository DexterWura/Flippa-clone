package com.flippa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false, length = 100)
    private String action;
    
    @Column(nullable = false, length = 50)
    private String entityType;
    
    @Column(length = 100)
    private String entityId;
    
    @Column(length = 2000)
    private String description;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(length = 500)
    private String userAgent;
    
    @Column(length = 2000)
    private String requestData;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LogLevel level = LogLevel.INFO;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}

