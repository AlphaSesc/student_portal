package com.example.student_portal.security;

import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.exception.InvalidCredentialsException;
import com.example.student_portal.repository.PortalUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
// Service used by Spring Security to load user details during authentication
public class CustomUserDetailsService implements UserDetailsService {

    private final PortalUserRepository portalUserRepository;

    public CustomUserDetailsService(PortalUserRepository portalUserRepository) {
        this.portalUserRepository = portalUserRepository;
    }

    @Override
    // Fetches user by email and throws exception if not found
    public UserDetails loadUserByUsername(String email) {
        PortalUser user = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        return new CustomUserDetails(user);
    }
}