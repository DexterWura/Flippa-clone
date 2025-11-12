package com.flippa.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Custom authentication success handler that redirects users based on their roles.
 * - Admin/SUPER_ADMIN users → /admin/dashboard
 * - Regular users → /home (or their originally requested page)
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        // Check if user has admin role
        boolean isAdmin = authorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                            auth.getAuthority().equals("ROLE_SUPER_ADMIN"));
        
        if (isAdmin) {
            // Redirect admins to admin dashboard
            response.sendRedirect("/admin");
        } else {
            // Redirect regular users to home
            // Check if there's a saved request (user tried to access a protected page)
            String redirectUrl = request.getParameter("redirect");
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                response.sendRedirect(redirectUrl);
            } else {
                response.sendRedirect("/home");
            }
        }
    }
}

