-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    bio VARCHAR(500),
    profile_picture_url VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    banned BOOLEAN NOT NULL DEFAULT FALSE,
    ban_reason VARCHAR(1000),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create user_roles junction table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create listings table
CREATE TABLE IF NOT EXISTS listings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    price DECIMAL(19, 2) NOT NULL,
    starting_bid DECIMAL(19, 2),
    current_bid DECIMAL(19, 2),
    website_url VARCHAR(500),
    image_url VARCHAR(500),
    category VARCHAR(50),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_notes VARCHAR(2000),
    auction_end_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create website_info table
CREATE TABLE IF NOT EXISTS website_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL UNIQUE,
    domain VARCHAR(100),
    platform VARCHAR(50),
    age VARCHAR(20),
    monthly_revenue DECIMAL(19, 2),
    monthly_traffic DECIMAL(19, 2),
    bounce_rate DECIMAL(5, 2),
    primary_traffic_source VARCHAR(50),
    traffic_data_json VARCHAR(1000),
    revenue_data_json VARCHAR(1000),
    alexa_rank VARCHAR(50),
    moz_rank VARCHAR(50),
    domain_authority VARCHAR(50),
    social_media_links VARCHAR(1000),
    technologies VARCHAR(1000),
    auto_fetched BOOLEAN NOT NULL DEFAULT FALSE,
    fetch_error VARCHAR(1000),
    last_fetched_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

-- Create escrows table
CREATE TABLE IF NOT EXISTS escrows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    payment_transaction_id VARCHAR(100),
    payment_gateway VARCHAR(50),
    buyer_notes VARCHAR(2000),
    seller_notes VARCHAR(2000),
    admin_resolution_notes VARCHAR(2000),
    resolved_by_admin_id BIGINT,
    dispute_raised BOOLEAN NOT NULL DEFAULT FALSE,
    dispute_reason VARCHAR(2000),
    dispute_resolution VARCHAR(2000),
    payment_received_at TIMESTAMP,
    transfer_initiated_at TIMESTAMP,
    transfer_completed_at TIMESTAMP,
    dispute_raised_at TIMESTAMP,
    dispute_resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (resolved_by_admin_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    escrow_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    gateway VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(200) NOT NULL UNIQUE,
    gateway_transaction_id VARCHAR(200),
    gateway_response VARCHAR(1000),
    failure_reason VARCHAR(2000),
    callback_url VARCHAR(500),
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (escrow_id) REFERENCES escrows(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create system_configs table
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(2000),
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config_type VARCHAR(50),
    updated_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (updated_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create audit_logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100),
    description VARCHAR(2000),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_data VARCHAR(2000),
    level VARCHAR(20) NOT NULL DEFAULT 'INFO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_listings_seller_id ON listings(seller_id);
CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_type ON listings(type);
CREATE INDEX idx_escrows_buyer_id ON escrows(buyer_id);
CREATE INDEX idx_escrows_seller_id ON escrows(seller_id);
CREATE INDEX idx_escrows_status ON escrows(status);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_escrow_id ON payments(escrow_id);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

