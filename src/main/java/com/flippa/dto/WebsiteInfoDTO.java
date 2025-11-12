package com.flippa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteInfoDTO {
    
    private Long id;
    private String domain;
    private String platform;
    private String age;
    private BigDecimal monthlyRevenue;
    private BigDecimal monthlyTraffic;
    private BigDecimal bounceRate;
    private String primaryTrafficSource;
    private String trafficDataJson;
    private String revenueDataJson;
    private String alexaRank;
    private String mozRank;
    private String domainAuthority;
    private String socialMediaLinks;
    private String technologies;
    private Boolean autoFetched;
    private String fetchError;
}

