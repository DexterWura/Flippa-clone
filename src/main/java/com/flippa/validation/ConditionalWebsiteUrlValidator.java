package com.flippa.validation;

import com.flippa.dto.ListingDTO;
import com.flippa.entity.Listing;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConditionalWebsiteUrlValidator implements ConstraintValidator<ConditionalWebsiteUrl, ListingDTO> {
    
    @Override
    public void initialize(ConditionalWebsiteUrl constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(ListingDTO listingDTO, ConstraintValidatorContext context) {
        if (listingDTO == null || listingDTO.getType() == null) {
            return true; // Let @NotNull handle null checks
        }
        
        // Website URL is required for WEBSITE and DOMAIN types
        if (listingDTO.getType() == Listing.ListingType.WEBSITE || 
            listingDTO.getType() == Listing.ListingType.DOMAIN) {
            
            if (listingDTO.getWebsiteUrl() == null || listingDTO.getWebsiteUrl().trim().isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Website URL is required for " + listingDTO.getType().name() + " listings"
                ).addPropertyNode("websiteUrl").addConstraintViolation();
                return false;
            }
        }
        
        return true;
    }
}

