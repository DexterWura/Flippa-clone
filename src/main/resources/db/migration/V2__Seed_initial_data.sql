-- Insert default roles
INSERT INTO roles (id, name, description, enabled) VALUES
(1, 'ROLE_USER', 'Basic user role', TRUE),
(2, 'ROLE_SELLER', 'User can sell listings', TRUE),
(3, 'ROLE_BUYER', 'User can buy listings', TRUE),
(4, 'ROLE_ADMIN', 'Administrator role', TRUE),
(5, 'ROLE_SUPER_ADMIN', 'Super administrator with full access', TRUE)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_USER');

-- Insert default system configurations
INSERT INTO system_configs (id, config_key, config_value, description, enabled, config_type) VALUES
(1, 'payment.gateway.paypal.enabled', 'true', 'Enable/disable PayPal payment gateway', TRUE, 'BOOLEAN'),
(2, 'payment.gateway.paypal.client-id', '', 'PayPal Client ID', TRUE, 'STRING'),
(3, 'payment.gateway.paypal.client-secret', '', 'PayPal Client Secret', TRUE, 'STRING'),
(4, 'payment.gateway.paypal.mode', 'sandbox', 'PayPal Mode (sandbox or live)', TRUE, 'STRING'),
(5, 'payment.gateway.paynow-zim.enabled', 'true', 'Enable/disable PayNow Zim payment gateway', TRUE, 'BOOLEAN'),
(6, 'payment.gateway.paynow-zim.integration-id', '', 'PayNow Zim Integration ID', TRUE, 'STRING'),
(7, 'payment.gateway.paynow-zim.integration-key', '', 'PayNow Zim Integration Key', TRUE, 'STRING'),
(8, 'payment.gateway.paynow-zim.return-url', 'http://localhost/payment/callback', 'PayNow Zim Return URL', TRUE, 'STRING'),
(9, 'payment.gateway.paynow-zim.result-url', 'http://localhost/payment/callback', 'PayNow Zim Result URL', TRUE, 'STRING'),
(10, 'website.info.auto-fetch.enabled', 'true', 'Enable automatic website info fetching', TRUE, 'BOOLEAN'),
(11, 'listing.auto-approve.enabled', 'false', 'Auto-approve listings without admin review', FALSE, 'BOOLEAN'),
(12, 'escrow.dispute.auto-escalate', 'false', 'Auto-escalate disputes after 7 days', FALSE, 'BOOLEAN')
WHERE NOT EXISTS (SELECT 1 FROM system_configs WHERE config_key = 'payment.gateway.paypal.enabled');

-- Create a default super admin user (password: password)
-- BCrypt hash for 'password' - Generated with BCryptPasswordEncoder
INSERT INTO users (id, email, password, first_name, last_name, enabled, banned, email_verified) VALUES
(1, 'admin@flippa.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'Super', 'Admin', TRUE, FALSE, TRUE)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@flippa.com');

-- Also create admin user with username 'admin' (password: password)
INSERT INTO users (id, email, password, first_name, last_name, enabled, banned, email_verified) VALUES
(2, 'admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'Admin', 'User', TRUE, FALSE, TRUE)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin');

-- Create a test user with password 'password'
INSERT INTO users (id, email, password, first_name, last_name, enabled, banned, email_verified) VALUES
(3, 'test@flippa.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'Test', 'User', TRUE, FALSE, TRUE)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'test@flippa.com');

-- Assign super admin role to admin users
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE (u.email = 'admin@flippa.com' OR u.email = 'admin') AND r.name = 'ROLE_SUPER_ADMIN'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

-- Assign default roles to test user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'test@flippa.com' AND r.name IN ('ROLE_USER', 'ROLE_BUYER', 'ROLE_SELLER')
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);
