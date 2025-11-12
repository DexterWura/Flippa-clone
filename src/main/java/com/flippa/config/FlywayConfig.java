package com.flippa.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;

/**
 * Flyway configuration to handle migration checksum mismatches in development.
 * This will automatically repair checksums if migrations are modified.
 */
@Configuration
public class FlywayConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private FlywayProperties flywayProperties;
    
    /**
     * Automatically repair Flyway checksums on startup if validation fails.
     * This is useful in development when migration files are modified.
     * 
     * WARNING: Only use this in development! In production, migration files should never be modified.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void repairFlywayIfNeeded() {
        // Only repair in development (when validate-on-migrate is false or not set)
        if (!flywayProperties.isValidateOnMigrate()) {
            try {
                Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(flywayProperties.getLocations().toArray(new String[0]))
                    .baselineOnMigrate(flywayProperties.isBaselineOnMigrate())
                    .load();
                
                // Check if repair is needed
                try {
                    flyway.validate();
                    logger.info("Flyway validation passed - no repair needed");
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("checksum")) {
                        logger.warn("Flyway checksum mismatch detected. Running repair...");
                        flyway.repair();
                        logger.info("Flyway repair completed successfully");
                    } else {
                        throw e;
                    }
                }
            } catch (Exception e) {
                logger.error("Error during Flyway repair: {}", e.getMessage());
                // Don't fail startup - let the normal Flyway migration handle it
            }
        }
    }
}

