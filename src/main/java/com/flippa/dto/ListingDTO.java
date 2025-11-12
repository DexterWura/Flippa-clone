package com.flippa.dto;

import com.flippa.entity.Listing;
import com.flippa.validation.ConditionalWebsiteUrl;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConditionalWebsiteUrl
public class ListingDTO {
    
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
    
    @NotNull(message = "Type is required")
    private Listing.ListingType type;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    
    private BigDecimal startingBid;
    private BigDecimal currentBid;
    
    private String websiteUrl;
    private String imageUrl;
    private String category;
    
    private Boolean featured = false;
    private Listing.ListingStatus status;
    
    private Long sellerId;
    private String sellerName;
    
    private LocalDateTime createdAt;
    private LocalDateTime auctionEndDate;
    
    private WebsiteInfoDTO websiteInfo;
}

