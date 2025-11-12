package com.flippa.service;

import com.flippa.entity.Listing;
import com.flippa.entity.WebsiteInfo;
import com.flippa.repository.ListingRepository;
import com.flippa.repository.WebsiteInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class WebsiteInfoFetchService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebsiteInfoFetchService.class);
    private final WebsiteInfoRepository websiteInfoRepository;
    private final ListingRepository listingRepository;
    private final WebClient webClient;
    
    @Value("${app.website-info.enabled:true}")
    private boolean enabled;
    
    @Value("${app.website-info.timeout:5000}")
    private int timeout;
    
    @Value("${app.website-info.user-agent:Mozilla/5.0 (compatible; FlippaCloneBot/1.0)}")
    private String userAgent;
    
    public WebsiteInfoFetchService(WebsiteInfoRepository websiteInfoRepository,
                                  ListingRepository listingRepository) {
        this.websiteInfoRepository = websiteInfoRepository;
        this.listingRepository = listingRepository;
        this.webClient = WebClient.builder()
            .defaultHeader("User-Agent", userAgent)
            .build();
    }
    
    @Async
    @Transactional
    public void fetchAndSaveWebsiteInfo(Long listingId, String websiteUrl) {
        if (!enabled) {
            logger.debug("Website info fetching is disabled");
            return;
        }
        
        try {
            Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
            
            WebsiteInfo websiteInfo = websiteInfoRepository.findByListingId(listingId)
                .orElse(new WebsiteInfo());
            
            websiteInfo.setListing(listing);
            websiteInfo.setLastFetchedAt(LocalDateTime.now());
            websiteInfo.setAutoFetched(true);
            
            // Extract domain from URL
            String domain = extractDomain(websiteUrl);
            websiteInfo.setDomain(domain);
            
            // Fetch website content (simplified - in production, use proper APIs)
            try {
                String htmlContent = webClient.get()
                    .uri(URI.create(websiteUrl))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
                
                // Parse basic info from HTML (simplified)
                websiteInfo.setPlatform(detectPlatform(htmlContent));
                
                // In production, integrate with:
                // - Google Analytics API for traffic
                // - SimilarWeb API for traffic data
                // - Revenue APIs if available
                // - Domain authority APIs (Moz, Ahrefs)
                
                // Mock data for demonstration
                websiteInfo.setMonthlyTraffic(java.math.BigDecimal.valueOf(10000));
                websiteInfo.setBounceRate(java.math.BigDecimal.valueOf(45.5));
                websiteInfo.setPrimaryTrafficSource("Organic Search");
                
                websiteInfo.setFetchError(null);
                
            } catch (Exception e) {
                logger.warn("Failed to fetch website content: {}", e.getMessage());
                websiteInfo.setFetchError("Failed to fetch: " + e.getMessage());
            }
            
            websiteInfoRepository.save(websiteInfo);
            logger.info("Website info fetched and saved for listing: {}", listingId);
            
        } catch (Exception e) {
            logger.error("Error fetching website info for listing {}: {}", listingId, e.getMessage(), e);
        }
    }
    
    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain != null ? domain.replaceFirst("^www\\.", "") : url;
        } catch (Exception e) {
            return url;
        }
    }
    
    private String detectPlatform(String htmlContent) {
        if (htmlContent == null) return "Unknown";
        
        htmlContent = htmlContent.toLowerCase();
        
        if (htmlContent.contains("wp-content") || htmlContent.contains("wordpress")) {
            return "WordPress";
        } else if (htmlContent.contains("shopify")) {
            return "Shopify";
        } else if (htmlContent.contains("squarespace")) {
            return "Squarespace";
        } else if (htmlContent.contains("wix")) {
            return "Wix";
        } else if (htmlContent.contains("react") || htmlContent.contains("reactjs")) {
            return "React";
        } else if (htmlContent.contains("vue")) {
            return "Vue.js";
        } else if (htmlContent.contains("angular")) {
            return "Angular";
        }
        
        return "Custom";
    }
}

