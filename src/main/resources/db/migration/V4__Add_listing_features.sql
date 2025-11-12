-- Create categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create listing_images table
CREATE TABLE IF NOT EXISTS listing_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

-- Create domain_verifications table
CREATE TABLE IF NOT EXISTS domain_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL UNIQUE,
    domain VARCHAR(500) NOT NULL,
    verification_token VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    verification_notes VARCHAR(2000),
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

-- Create social_media_verifications table
CREATE TABLE IF NOT EXISTS social_media_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL UNIQUE,
    platform VARCHAR(50) NOT NULL,
    account_url VARCHAR(500) NOT NULL,
    account_username VARCHAR(200) NOT NULL,
    verification_token VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    verification_notes VARCHAR(2000),
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

-- Add new columns to listings table
ALTER TABLE listings ADD COLUMN IF NOT EXISTS category_id BIGINT;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS listing_mode VARCHAR(50) NOT NULL DEFAULT 'NORMAL';

-- Add foreign key for category
ALTER TABLE listings ADD CONSTRAINT fk_listing_category 
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_listing_images_listing_id ON listing_images(listing_id);
CREATE INDEX IF NOT EXISTS idx_domain_verifications_listing_id ON domain_verifications(listing_id);
CREATE INDEX IF NOT EXISTS idx_domain_verifications_token ON domain_verifications(verification_token);
CREATE INDEX IF NOT EXISTS idx_social_media_verifications_listing_id ON social_media_verifications(listing_id);
CREATE INDEX IF NOT EXISTS idx_social_media_verifications_token ON social_media_verifications(verification_token);
CREATE INDEX IF NOT EXISTS idx_listings_category_id ON listings(category_id);
CREATE INDEX IF NOT EXISTS idx_listings_listing_mode ON listings(listing_mode);

