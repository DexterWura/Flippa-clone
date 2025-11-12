package com.flippa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "website_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "listing_id", nullable = false, unique = true)
    private Listing listing;
    
    @Column(length = 100)
    private String domain;
    
    @Column(length = 50)
    private String platform; // WordPress, Shopify, Custom, etc.
    
    @Column(length = 20)
    private String age; // e.g., "2 years"
    
    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyRevenue;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyTraffic;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal bounceRate;
    
    @Column(length = 50)
    private String primaryTrafficSource;
    
    @Column(length = 1000)
    private String trafficDataJson; // JSON string for detailed traffic data
    
    @Column(length = 1000)
    private String revenueDataJson; // JSON string for revenue graphs
    
    @Column(length = 50)
    private String alexaRank;
    
    @Column(length = 50)
    private String mozRank;
    
    @Column(length = 50)
    private String domainAuthority;
    
    @Column(length = 1000)
    private String socialMediaLinks; // JSON string
    
    @Column(length = 1000)
    private String technologies; // JSON array of technologies used
    
    @Column(nullable = false)
    private Boolean autoFetched = false;
    
    @Column(length = 1000)
    private String fetchError;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastFetchedAt;
}

