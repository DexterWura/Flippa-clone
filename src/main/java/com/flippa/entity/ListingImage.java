package com.flippa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "listing_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    
    @Column(nullable = false, length = 500)
    private String filePath;
    
    @Column(nullable = false, length = 500)
    private String fileName;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false, length = 100)
    private String contentType;
    
    @Column(nullable = false)
    private Boolean isPrimary = false;
    
    @Column(nullable = false)
    private Integer displayOrder = 0;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

