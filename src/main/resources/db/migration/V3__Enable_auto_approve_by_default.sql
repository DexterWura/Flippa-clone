-- Enable auto-approve for listings by default
-- This allows new listings to be immediately visible without admin review
UPDATE system_configs 
SET config_value = 'true', enabled = TRUE 
WHERE config_key = 'listing.auto-approve.enabled';

-- If the config doesn't exist, create it
INSERT INTO system_configs (config_key, config_value, description, enabled, config_type)
SELECT 'listing.auto-approve.enabled', 'true', 'Auto-approve listings without admin review', TRUE, 'BOOLEAN'
WHERE NOT EXISTS (SELECT 1 FROM system_configs WHERE config_key = 'listing.auto-approve.enabled');

