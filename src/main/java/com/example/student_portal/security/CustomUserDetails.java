package com.example.student_portal.security;

import com.example.student_portal.entity.PortalUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
// Custom implementation of UserDetails to integrate PortalUser with Spring Security
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String role;

    // Maps PortalUser entity to Spring Security user details
    public CustomUserDetails(PortalUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole().name();
    }

    @Override
    // Converts user role into Spring Security authority format (e.g., ROLE_ADMIN)
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    // Uses email as the username for authentication
    public String getUsername() {
        return email;
    }
}