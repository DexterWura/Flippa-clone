package com.flippa.security;

import com.flippa.entity.User;
import com.flippa.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        if (!user.getEnabled()) {
            throw new UsernameNotFoundException("User account is disabled");
        }
        
        if (user.getBanned()) {
            throw new UsernameNotFoundException("User account is banned");
        }
        
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            getAuthorities(user)
        );
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return user.getRoles().stream()
            .filter(role -> role.getEnabled())
            .map(role -> {
                // Role enum already has "ROLE_" prefix (e.g., ROLE_ADMIN)
                // Spring Security's hasAnyRole("ADMIN") will match "ROLE_ADMIN" authority
                String roleName = role.getName().name();
                return new SimpleGrantedAuthority(roleName);
            })
            .collect(Collectors.toList());
    }
}

