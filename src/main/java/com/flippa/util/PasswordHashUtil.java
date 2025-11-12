package com.flippa.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class for generating password hashes during development.
 * This can be used to generate BCrypt hashes for initial data.
 */
public class PasswordHashUtil {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("Hash for 'password': " + encoder.encode("password"));
    }
}

