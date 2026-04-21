package com.example.student_portal.service;

import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.entity.UserRole;
import com.example.student_portal.exception.UnauthorizedOperationException;
import com.example.student_portal.exception.UserNotFoundException;
import com.example.student_portal.repository.PortalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// Service to retrieve the currently authenticated user from the security context
public class AuthenticatedUserService {

    private final PortalUserRepository portalUserRepository;

    public PortalUser getCurrentUser() {
        // Retrieve authentication object set by JwtAuthenticationFilter
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If no authentication is present, user is not logged in
        if (authentication == null || authentication.getName() == null) {
            throw new UserNotFoundException("Authenticated user not found");
        }

        // Email is stored as username in security context
        String email = authentication.getName();

        // Fetch full user entity from database
        return portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public PortalUser getCurrentStudentUser() {
        // Get authenticated user
        PortalUser portalUser = getCurrentUser();

        // Ensure only STUDENT role can access certain operations
        if (portalUser.getRole() != UserRole.STUDENT) {
            throw new UnauthorizedOperationException("Only students can access this feature");
        }

        return portalUser;
    }
}