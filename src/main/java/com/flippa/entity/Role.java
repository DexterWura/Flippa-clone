package com.flippa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RoleType name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum RoleType {
        ROLE_USER,
        ROLE_SELLER,
        ROLE_BUYER,
        ROLE_ADMIN,
        ROLE_SUPER_ADMIN
    }
}

