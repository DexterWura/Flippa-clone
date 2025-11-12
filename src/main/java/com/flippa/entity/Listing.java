package com.flippa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Listing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, length = 5000)
    private String description;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ListingType type;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.DRAFT;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal startingBid;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal currentBid;
    
    @Column(length = 500)
    private String websiteUrl;
    
    @Column(length = 500)
    private String imageUrl; // Deprecated - use listingImages instead
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ListingMode listingMode = ListingMode.NORMAL;
    
    @Column(nullable = false)
    private Boolean featured = false;
    
    @Column(nullable = false)
    private Boolean verified = false;
    
    @Column(length = 2000)
    private String verificationNotes;
    
    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private WebsiteInfo websiteInfo;
    
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ListingImage> listingImages = new ArrayList<>();
    
    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private DomainVerification domainVerification;
    
    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private SocialMediaVerification socialMediaVerification;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private LocalDateTime auctionEndDate;
    
    public enum ListingType {
        WEBSITE,
        SOCIAL_MEDIA_ACCOUNT,
        DOMAIN,
        MOBILE_APP,
        SAAS,
        ECOMMERCE_STORE,
        OTHER
    }
    
    public enum ListingStatus {
        DRAFT,
        PENDING_REVIEW,
        ACTIVE,
        SOLD,
        CANCELLED,
        SUSPENDED
    }
    
    public enum ListingMode {
        NORMAL,
        AUCTION
    }
}

