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
public class AuthenticatedUserService {

    private final PortalUserRepository portalUserRepository;

    public PortalUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new UserNotFoundException("Authenticated user not found");
        }

        String email = authentication.getName();

        return portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public PortalUser getCurrentStudentUser() {
        PortalUser portalUser = getCurrentUser();

        if (portalUser.getRole() != UserRole.STUDENT) {
            throw new UnauthorizedOperationException("Only students can access this feature");
        }

        return portalUser;
    }
}