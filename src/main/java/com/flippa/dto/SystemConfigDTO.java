package com.flippa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigDTO {
    
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private Boolean enabled;
    private String configType;
}

