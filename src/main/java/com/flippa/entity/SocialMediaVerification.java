package com.flippa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_media_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialMediaVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false, unique = true)
    private Listing listing;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Platform platform;
    
    @Column(nullable = false, length = 500)
    private String accountUrl;
    
    @Column(nullable = false, length = 200)
    private String accountUsername;
    
    @Column(nullable = false, length = 100, unique = true)
    private String verificationToken;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VerificationStatus status = VerificationStatus.PENDING;
    
    @Column(length = 2000)
    private String verificationNotes;
    
    @Column
    private LocalDateTime verifiedAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum Platform {
        FACEBOOK,
        TWITTER,
        INSTAGRAM,
        TIKTOK,
        LINKEDIN,
        YOUTUBE,
        SNAPCHAT,
        PINTEREST,
        OTHER
    }
    
    public enum VerificationStatus {
        PENDING,
        VERIFIED,
        FAILED,
        EXPIRED
    }
}

