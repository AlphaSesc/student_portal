package com.example.student_portal.service;

import com.example.student_portal.entity.PortalUser;
import com.example.student_portal.exception.InvalidCredentialsException;
import com.example.student_portal.exception.UserNotFoundException;
import com.example.student_portal.exception.ResourceAlreadyExistsException;
import com.example.student_portal.repository.PortalUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
// Service responsible for user registration and authentication logic
public class PortalUserService {

    private final PortalUserRepository portalUserRepository;
    private final PasswordEncoder passwordEncoder;

    public PortalUserService(PortalUserRepository portalUserRepository, PasswordEncoder passwordEncoder) {
        this.portalUserRepository = portalUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Registers a new user after validating uniqueness and encoding password
    public PortalUser registerUser(PortalUser user) {
        if (portalUserRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("Email already registered");
        }
        // Encode password before storing for security
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return portalUserRepository.save(user);
    }

    // Retrieves user by email (used internally or during authentication)
    public Optional<PortalUser> findByUsername(String email) {
        return portalUserRepository.findByEmail(email);
    }

    // Authenticates user by verifying email and password
    public PortalUser authenticate(String email, String password) {

        // Fetch user from database
        PortalUser user = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Validate raw password against encoded password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        return user;
    }
}